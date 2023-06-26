package com.lsn.lib.net.core.cache

import okhttp3.Headers
import okhttp3.Request
import okhttp3.Response
import java.util.*


/**
 * @Author : lsn
 * @CreateTime : 2023/3/9 上午 10:42
 * @Description :
 */
object HeadersVary {

    /**
     * Returns true if none of the Vary headers have changed between `cachedRequest` and `newRequest`.
     */
    fun varyMatches(
        cachedResponse: Response, cachedRequest: Headers, newRequest: Request
    ): Boolean {
        for (field in varyFields(cachedResponse)) {
            if (!equal(cachedRequest.values(field!!), newRequest.headers(field))) return false
        }
        return true
    }

    /**
     * Returns the subset of the headers in `response`'s request that impact the content of
     * response's body.
     */
    fun varyHeaders(response: Response): Headers {
        // Use the request headers sent over the network, since that's what the
        // response varies on. Otherwise OkHttp-supplied headers like
        // "Accept-Encoding: gzip" may be lost.
        val requestHeaders = response.networkResponse!!.request.headers
        val responseHeaders = response.headers
        return varyHeaders(requestHeaders, responseHeaders)
    }

    /**
     * Returns the subset of the headers in `requestHeaders` that impact the content of
     * response's body.
     */
    private fun varyHeaders(requestHeaders: Headers, responseHeaders: Headers): Headers {
        val varyFields = varyFields(responseHeaders)
        if (varyFields.isEmpty()) return Headers.Builder().build()
        val result = Headers.Builder()
        var i = 0
        val size = requestHeaders.size
        while (i < size) {
            val fieldName = requestHeaders.name(i)
            if (varyFields.contains(fieldName)) {
                result.add(fieldName, requestHeaders.value(i))
            }
            i++
        }
        return result.build()
    }

    private fun varyFields(response: Response): Set<String?> {
        return varyFields(response.headers)
    }

    /**
     * Returns the names of the request headers that need to be checked for equality when caching.
     */
    private fun varyFields(responseHeaders: Headers): Set<String> {
        var result = Collections.emptySet<String>()
        var i = 0
        val size = responseHeaders.size
        while (i < size) {
            if (!"Vary".equals(responseHeaders.name(i), ignoreCase = true)) {
                i++
                continue
            }
            val value = responseHeaders.value(i)
            if (result.isEmpty()) {
                result = TreeSet(java.lang.String.CASE_INSENSITIVE_ORDER)
            }
            for (varyField in value.split(",".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()) {
                result.add(varyField.trim { it <= ' ' })
            }
            i++
        }
        return result
    }

    /**
     * Returns true if two possibly-null objects are equal.
     */
    private fun equal(a: Any, b: Any): Boolean {
        return a == b
    }

}