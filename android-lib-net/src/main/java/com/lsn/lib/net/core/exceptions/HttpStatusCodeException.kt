package com.lsn.lib.net.core.exceptions

import com.lsn.lib.net.core.cache.OkHttpCompat
import okhttp3.*
import java.io.IOException


/**
 * @Author : lsn
 * @CreateTime : 2023/3/22 上午 10:42
 * @Description : Http 状态码 code < 200 || code >= 300, 抛出此异常
 * 可通过[.getLocalizedMessage]方法获取code
 */
class HttpStatusCodeException(response: Response) : IOException(response.message) {
    private val protocol //http协议
            : Protocol
    val statusCode //Http响应状态吗
            : Int
    val requestMethod //请求方法，Get/Post等
            : String
    val httpUrl //请求Url及查询参数
            : HttpUrl
    val responseHeaders //响应头
            : Headers
    val responseBody: ResponseBody?

    @get:Throws(IOException::class)
    var result //返回结果
            : String? = null
        get() {
            if (field == null) {
                field = responseBody!!.string()
            }
            return field
        }
        private set

    init {
        protocol = response.protocol
        statusCode = response.code
        val request = response.request
        requestMethod = request.method
        httpUrl = request.url
        responseHeaders = response.headers
        responseBody = response.body
    }

    override fun getLocalizedMessage(): String {
        return statusCode.toString()
    }

    val requestUrl: String
        get() = httpUrl.toString()

    override fun toString(): String {
        return """<------ ${OkHttpCompat.getOkHttpUserAgent()} request end ------>
${javaClass.name}:
$requestMethod $httpUrl

$protocol $statusCode $message
$responseHeaders"""
    }
}