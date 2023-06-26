package com.lsn.lib.net.core.cache

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.internal.cache.CacheRequest
import okhttp3.internal.cache.DiskLruCache
import okhttp3.internal.closeQuietly
import okhttp3.internal.discard
import okhttp3.internal.http.RealResponseBody
import okhttp3.internal.http.StatusLine
import okhttp3.internal.io.FileSystem
import okhttp3.internal.platform.Platform
import okio.*
import okio.ByteString.Companion.decodeBase64
import okio.ByteString.Companion.encodeUtf8
import java.io.Closeable
import java.io.File
import java.io.Flushable
import java.io.IOException
import java.security.cert.Certificate
import java.security.cert.CertificateEncodingException
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.util.concurrent.TimeUnit


/**
 * @Author : lsn
 * @CreateTime : 2023/3/9 上午 10:46
 * @Description :
 */
class CacheManager(directory: File, maxSize: Long) : Closeable,
    Flushable {
    val internalCache: IFileCache = object : IFileCache {
        @Throws(IOException::class)
        override operator fun get(request: Request, key: String): Response? {
            return this@CacheManager[request, key]
        }

        @Throws(IOException::class)
        override fun put(response: Response, key: String): Response {
            return this@CacheManager.put(response, key)
        }


        @Throws(IOException::class)
        override fun remove(key: String) {
            this@CacheManager.remove(key)
        }

        @Throws(IOException::class)
        override fun removeAll() {
            evictAll()
        }

        @Throws(IOException::class)
        override fun size(): Long {
            return this@CacheManager.size()
        }
    }
    private val cache: DiskLruCache

    /**
     * Create a cache of at most `maxSize` bytes in `directory`.
     *
     * @param directory File
     * @param maxSize   long
     */
    init {
        cache = OkHttpCompat.newDiskLruCache(
            FileSystem.SYSTEM,
            directory,
            VERSION,
            ENTRY_COUNT,
            maxSize
        )
    }

    private operator fun get(request: Request, key: String?): Response? {
        val md5Key = md5(key ?: request.url.toString())
        val snapshot: DiskLruCache.Snapshot?
        val entry: Entry
        try {
            snapshot = cache[md5Key]
            if (snapshot == null) {
                return null
            }
        } catch (e: IOException) {
            // Give up because the cache cannot be read.
            return null
        }
        try {
            entry = Entry(snapshot.getSource(ENTRY_METADATA))
        } catch (e: IOException) {
            snapshot.closeQuietly()
            return null
        }
        return entry.response(request, snapshot)
    }

    @Throws(IOException::class)
    private fun put(networkResponse: Response, key: String): Response {
        val cacheRequest = putResponse(networkResponse, key) //写响应头、message等信息
        return cacheWritingResponse(cacheRequest, networkResponse) //写
    }

    private fun putResponse(response: Response, key: String?): CacheRequest? {
        val entry = Entry(response)
        var editor: DiskLruCache.Editor? = null
        return try {
            val md5Key = md5(key ?: response.request.url.toString())
            editor = cache.edit(md5Key)
            if (editor == null) {
                return null
            }
            entry.writeTo(editor)
            CacheRequestImpl(editor)
        } catch (e: IOException) {
            abortQuietly(editor)
            null
        }
    }

    @Throws(IOException::class)
    private fun cacheWritingResponse(cacheRequest: CacheRequest?, response: Response): Response {
        // Some apps return a null body; for compatibility we treat that like a null cache request.
        if (cacheRequest == null) return response
        val cacheBodyUnbuffered = cacheRequest.body()
        val body = response.body ?: return response
        val source = body.source()
        val cacheBody = cacheBodyUnbuffered.buffer()
        val cacheWritingSource: Source = object : Source {
            var cacheRequestClosed = false

            @Throws(IOException::class)
            override fun read(sink: Buffer, byteCount: Long): Long {
                val bytesRead: Long
                try {
                    bytesRead = source.read(sink, byteCount)
                } catch (e: IOException) {
                    if (!cacheRequestClosed) {
                        cacheRequestClosed = true
                        cacheRequest.abort() // Failed to write a complete cache response.
                    }
                    throw e
                }
                if (bytesRead == -1L) {
                    if (!cacheRequestClosed) {
                        cacheRequestClosed = true
                        cacheBody.close() // The cache response is complete!
                    }
                    return -1
                }
                sink.copyTo(cacheBody.buffer(), sink.size - bytesRead, bytesRead)
                cacheBody.emitCompleteSegments()
                return bytesRead
            }

            override fun timeout(): Timeout {
                return source.timeout()
            }

            @Throws(IOException::class)
            override fun close() {
                //这里本应传入ExchangeCodec.DISCARD_STREAM_TIMEOUT_MILLIS常量，但为兼容老版本，故直接传入常量对应的值
                if (!cacheRequestClosed
                    && !this.discard(100, TimeUnit.MILLISECONDS)
                ) {
                    cacheRequestClosed = true
                    cacheRequest.abort()
                }
                source.close()
            }
        }
        val contentType = response.header("Content-Type")
        val contentLength = response.body!!.contentLength()
        return response.newBuilder()
            .body(RealResponseBody(contentType, contentLength, cacheWritingSource.buffer()))
            .build()
    }

    @Throws(IOException::class)
    private fun remove(key: String) {
        cache.remove(md5(key))
    }

    private fun abortQuietly(editor: DiskLruCache.Editor?) {
        // Give up because the cache cannot be written.
        try {
            if (editor != null) {
                editor.abort()
            }
        } catch (ignored: IOException) {
        }
    }

    /**
     * Initialize the cache. This will include reading the journal files from the storage and building
     * up the necessary in-memory cache information.
     *
     *
     * The initialization time may vary depending on the journal file size and the current actual
     * cache size. The application needs to be aware of calling this function during the
     * initialization phase and preferably in a background worker thread.
     *
     *
     * Note that if the application chooses to not call this method to initialize the cache. By
     * default, the okhttp will perform lazy initialization upon the first usage of the cache.
     *
     * @throws IOException 初始化失败
     */
    @Throws(IOException::class)
    fun initialize() {
        cache.initialize()
    }

    /**
     * Closes the cache and deletes all of its stored values. This will delete all files in the cache
     * directory including files that weren't created by the cache.
     */
    @Throws(IOException::class)
    private fun delete() {
        cache.delete()
    }

    /**
     * Deletes all values stored in the cache. In-flight writes to the cache will complete normally,
     * but the corresponding responses will not be stored.
     */
    @Throws(IOException::class)
    private fun evictAll() {
        cache.evictAll()
    }

    @Throws(IOException::class)
    fun urls(): Iterator<String> {
        return object : MutableIterator<String> {
            val delegate: MutableIterator<DiskLruCache.Snapshot> = cache.snapshots()
            var nextUrl: String? = null
            var canRemove = false
            override fun hasNext(): Boolean {
                if (nextUrl != null) return true
                canRemove = false // Prevent delegate.remove() on the wrong item!
                while (delegate.hasNext()) {
                    try {
                        delegate.next().use { snapshot ->
                            val metadata: BufferedSource =
                                snapshot.getSource(ENTRY_METADATA).buffer()
                            nextUrl = metadata.readUtf8LineStrict()
                            return true
                        }
                    } catch (ignored: IOException) {
                        // We couldn't read the metadata for this snapshot; possibly because the host filesystem
                        // has disappeared! Skip it.
                    }
                }
                return false
            }

            override fun next(): String {
                if (!hasNext()) throw NoSuchElementException()
                val result = nextUrl
                nextUrl = null
                canRemove = true
                return result!!
            }

            override fun remove() {
                check(canRemove) { "remove() before next()" }
                delegate.remove()
            }
        }
    }

    @Throws(IOException::class)
    fun size(): Long {
        return cache.size()
    }

    /**
     * @return Max size of the cache (in bytes).
     */
    fun maxSize(): Long {
        return cache.maxSize
    }

    @Throws(IOException::class)
    override fun flush() {
        cache.flush()
    }

    @Throws(IOException::class)
    override fun close() {
        cache.close()
    }

    fun directory(): File {
        return cache.directory
    }

    fun isClosed(): Boolean {
        return cache.isClosed()
    }

    private inner class CacheRequestImpl internal constructor(editor: DiskLruCache.Editor) :
        CacheRequest {
        private val editor: DiskLruCache.Editor
        private val cacheOut: Sink
        private val body: Sink
        var done = false

        init {
            this.editor = editor
            cacheOut = editor.newSink(ENTRY_BODY)
            body = object : ForwardingSink(cacheOut) {
                @Throws(IOException::class)
                override fun close() {
                    synchronized(this@CacheManager) {
                        if (done) {
                            return
                        }
                        done = true
                    }
                    super.close()
                    editor.commit()
                }
            }
        }

        override fun abort() {
            synchronized(this@CacheManager) {
                if (done) {
                    return
                }
                done = true
            }
            cacheOut.closeQuietly()
            try {
                editor.abort()
            } catch (ignored: IOException) {
            }
        }

        override fun body(): Sink {
            return body
        }
    }

    private class Entry {
        private val url: String
        private val varyHeaders: Headers
        private val requestMethod: String
        private val protocol: Protocol
        private val code: Int
        private val message: String
        private val responseHeaders: Headers
        private val handshake: Handshake?
        private val sentRequestMillis: Long
        private val receivedResponseMillis: Long

        /**
         * Reads an entry from an input stream. A typical entry looks like this:
         * <pre>`http://google.com/foo
         * GET
         * 2
         * Accept-Language: fr-CA
         * Accept-Charset: UTF-8
         * HTTP/1.1 200 OK
         * 3
         * Content-Type: image/png
         * Content-Length: 100
         * RxHttpCache-Control: max-age=600
        `</pre> *
         *
         *
         * A typical HTTPS file looks like this:
         * <pre>`https://google.com/foo
         * GET
         * 2
         * Accept-Language: fr-CA
         * Accept-Charset: UTF-8
         * HTTP/1.1 200 OK
         * 3
         * Content-Type: image/png
         * Content-Length: 100
         * Cache-Control: max-age=600
         *
         * AES_256_WITH_MD5
         * 2
         * base64-encoded peerCertificate[0]
         * base64-encoded peerCertificate[1]
         * -1
         * TLSv1.2
        `</pre> *
         * The file is newline separated. The first two lines are the URL and the request method. Next
         * is the number of HTTP Vary request header lines, followed by those lines.
         *
         *
         * Next is the response status line, followed by the number of HTTP response header lines,
         * followed by those lines.
         *
         *
         * HTTPS responses also contain SSL session information. This begins with a blank line, and
         * then a line containing the cipher suite. Next is the length of the peer certificate chain.
         * These certificates are base64-encoded and appear each on their own line. The next line
         * contains the length of the local certificate chain. These certificates are also
         * base64-encoded and appear each on their own line. A length of -1 is used to encode a null
         * array. The last line is optional. If present, it contains the TLS version.
         */
        internal constructor(`in`: Source) {
            try {
                val source = `in`.buffer()
                url = source.readUtf8LineStrict()
                requestMethod = source.readUtf8LineStrict()
                val varyHeadersBuilder = Headers.Builder()
                val varyRequestHeaderLineCount = readInt(source)
                for (i in 0 until varyRequestHeaderLineCount) {
                    addUnsafeNonAscii(varyHeadersBuilder, source.readUtf8LineStrict())
                }
                varyHeaders = varyHeadersBuilder.build()
                val statusLine: StatusLine = OkHttpCompat.parse(source.readUtf8LineStrict())
                protocol = statusLine.protocol
                code = statusLine.code
                message = statusLine.message
                val responseHeadersBuilder = Headers.Builder()
                val responseHeaderLineCount = readInt(source)
                for (i in 0 until responseHeaderLineCount) {
                    addUnsafeNonAscii(responseHeadersBuilder, source.readUtf8LineStrict())
                }
                val sendRequestMillisString: String? = responseHeadersBuilder.get(SENT_MILLIS)
                val receivedResponseMillisString: String? = responseHeadersBuilder.get(
                    RECEIVED_MILLIS
                )
                responseHeadersBuilder.removeAll(SENT_MILLIS)
                responseHeadersBuilder.removeAll(RECEIVED_MILLIS)
                sentRequestMillis = sendRequestMillisString?.toLong() ?: 0L
                receivedResponseMillis = receivedResponseMillisString?.toLong() ?: 0L
                responseHeaders = responseHeadersBuilder.build()
                handshake = if (isHttps) {
                    val blank = source.readUtf8LineStrict()
                    if (blank.length > 0) {
                        throw IOException("expected \"\" but was \"$blank\"")
                    }
                    val cipherSuiteString = source.readUtf8LineStrict()
                    val cipherSuite: CipherSuite = CipherSuite.forJavaName(cipherSuiteString)
                    val peerCertificates = readCertificateList(source)
                    val localCertificates = readCertificateList(source)
                    val tlsVersion =
                        if (!source.exhausted()) TlsVersion.forJavaName(source.readUtf8LineStrict()) else TlsVersion.SSL_3_0
                    Handshake.get(tlsVersion, cipherSuite, peerCertificates, localCertificates)
                } else {
                    null
                }
            } finally {
                `in`.close()
            }
        }

        /**
         * Add a header with the specified name and value. Does validation of header names, allowing
         * non-ASCII values.
         */
        fun addUnsafeNonAscii(builder: Headers.Builder, line: String) {
            val index = line.indexOf(":", 1)
            if (index != -1) {
                builder.addUnsafeNonAscii(line.substring(0, index), line.substring(index + 1))
            } else if (line.startsWith(":")) {
                builder.addUnsafeNonAscii("", line.substring(1))
            } else {
                builder.addUnsafeNonAscii("", line)
            }
        }

        internal constructor(response: Response) {
            url = response.request.url.toString()
            varyHeaders = HeadersVary.varyHeaders(response)
            requestMethod = response.request.method
            protocol = response.protocol
            this.code = response.code
            message = response.message
            responseHeaders = response.headers
            handshake = response.handshake
            sentRequestMillis = response.sentRequestAtMillis
            receivedResponseMillis = response.receivedResponseAtMillis
        }

        @Throws(IOException::class)
        fun writeTo(editor: DiskLruCache.Editor) {
            val sink: BufferedSink = editor.newSink(ENTRY_METADATA).buffer()
            sink.writeUtf8(url)
                .writeByte('\n'.code)
            sink.writeUtf8(requestMethod)
                .writeByte('\n'.code)
            sink.writeDecimalLong(varyHeaders.size.toLong())
                .writeByte('\n'.code)
            run {
                var i = 0
                val size = varyHeaders.size
                while (i < size) {
                    sink.writeUtf8(varyHeaders.name(i))
                        .writeUtf8(": ")
                        .writeUtf8(varyHeaders.value(i))
                        .writeByte('\n'.code)
                    i++
                }
            }
            sink.writeUtf8(StatusLine(protocol, code, message).toString())
                .writeByte('\n'.code)
            sink.writeDecimalLong((responseHeaders.size + 2).toLong())
                .writeByte('\n'.code)
            var i = 0
            val size = responseHeaders.size
            while (i < size) {
                sink.writeUtf8(responseHeaders.name(i))
                    .writeUtf8(": ")
                    .writeUtf8(responseHeaders.value(i))
                    .writeByte('\n'.code)
                i++
            }
            sink.writeUtf8(SENT_MILLIS)
                .writeUtf8(": ")
                .writeDecimalLong(sentRequestMillis)
                .writeByte('\n'.code)
            sink.writeUtf8(RECEIVED_MILLIS)
                .writeUtf8(": ")
                .writeDecimalLong(receivedResponseMillis)
                .writeByte('\n'.code)
            if (isHttps) {
                sink.writeByte('\n'.code)
                sink.writeUtf8(handshake!!.cipherSuite.javaName)
                    .writeByte('\n'.code)
                writeCertList(sink, handshake.peerCertificates)
                writeCertList(sink, handshake.localCertificates)
                sink.writeUtf8(handshake.tlsVersion.javaName).writeByte('\n'.code)
            }
            sink.close()
        }

        private val isHttps: Boolean
            private get() = url.startsWith("https://")

        @Throws(IOException::class)
        private fun readCertificateList(source: BufferedSource): List<Certificate> {
            val length = readInt(source)
            return if (length == -1) emptyList<Certificate>() else try {
                val certificateFactory = CertificateFactory.getInstance("X.509")
                val result: MutableList<Certificate> = ArrayList(length)
                for (i in 0 until length) {
                    val line = source.readUtf8LineStrict()
                    val bytes = Buffer()
                    line.decodeBase64()?.let { bytes.write(it) }
                    result.add(certificateFactory.generateCertificate(bytes.inputStream()))
                }
                result
            } catch (e: CertificateException) {
                throw IOException(e.message)
            } // OkHttp v1.2 used -1 to indicate null.
        }

        @Throws(IOException::class)
        private fun writeCertList(sink: BufferedSink, certificates: List<Certificate>) {
            try {
                sink.writeDecimalLong(certificates.size.toLong())
                    .writeByte('\n'.code)
                var i = 0
                val size = certificates.size
                while (i < size) {
                    val bytes = certificates[i].encoded
                    val line: String = ByteString.of(*bytes).base64()
                    sink.writeUtf8(line)
                        .writeByte('\n'.code)
                    i++
                }
            } catch (e: CertificateEncodingException) {
                throw IOException(e.message)
            }
        }

        fun matches(request: Request, response: Response): Boolean {
            return url == request.url.toString() && requestMethod == request.method && HeadersVary.varyMatches(
                response,
                varyHeaders,
                request
            )
        }

        fun response(request: Request, snapshot: DiskLruCache.Snapshot): Response {
            val contentType = responseHeaders["Content-Type"]
            val contentLength = responseHeaders["Content-Length"]
            return Response.Builder()
                .request(request)
                .protocol(protocol)
                .code(code)
                .message(message)
                .headers(responseHeaders)
                .body(CacheResponseBody(snapshot, contentType, contentLength))
                .handshake(handshake)
                .sentRequestAtMillis(sentRequestMillis)
                .receivedResponseAtMillis(receivedResponseMillis)
                .build()
        }

        companion object {
            /**
             * Synthetic response header: the local time when the request was sent. Platform
             */
            private val SENT_MILLIS: String = Platform.get().getPrefix() + "-Sent-Millis"

            /**
             * Synthetic response header: the local time when the response was received.
             */
            private val RECEIVED_MILLIS: String = Platform.get().getPrefix() + "-Received-Millis"
        }
    }

    private class CacheResponseBody internal constructor(
        snapshot: DiskLruCache.Snapshot,
        contentType: String?, contentLength: String?
    ) :
        ResponseBody() {
        val snapshot: DiskLruCache.Snapshot
        private val bodySource: BufferedSource
        private val contentType: String?
        private val contentLength: String?

        init {
            this.snapshot = snapshot
            this.contentType = contentType
            this.contentLength = contentLength
            val source: Source = snapshot.getSource(ENTRY_BODY)
            bodySource = object : ForwardingSource(source) {
                @Throws(IOException::class)
                override fun close() {
                    snapshot.close()
                    super.close()
                }
            }.buffer()
        }

        override fun contentType(): MediaType? {
            return contentType?.toMediaTypeOrNull()
        }

        override fun contentLength(): Long {
            return try {
                contentLength?.toLong() ?: -1
            } catch (e: NumberFormatException) {
                -1
            }
        }

        override fun source(): BufferedSource {
            return bodySource
        }
    }

    companion object {
        private const val VERSION = 201105
        private const val ENTRY_METADATA = 0
        private const val ENTRY_BODY = 1
        private const val ENTRY_COUNT = 2
        fun md5(key: String): String {
            return key.encodeUtf8().md5().hex()
        }

        @Throws(IOException::class)
        private fun readInt(source: BufferedSource): Int {
            return try {
                val result = source.readDecimalLong()
                val line = source.readUtf8LineStrict()
                if (result < 0 || result > Int.MAX_VALUE || !line.isEmpty()) {
                    throw IOException("expected an int but was \"$result$line\"")
                }
                result.toInt()
            } catch (e: NumberFormatException) {
                throw IOException(e.message)
            }
        }
    }
}