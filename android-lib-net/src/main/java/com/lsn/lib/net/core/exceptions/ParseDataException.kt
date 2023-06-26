package com.lsn.lib.net.core.exceptions

import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.Response
import java.io.IOException


/**
 * @Author : lsn
 * @CreateTime : 2023/3/22 上午 10:43
 * @Description :
 */
class ParseDataException(code: String, message: String?, response: Response) : IOException(message) {
    val errorCode: String
    private val requestMethod : String //请求方法，Get/Post等
    private val httpUrl : HttpUrl //请求Url及查询参数
    private val responseHeaders : Headers //响应头


    init {
        errorCode = code
        val request = response.request
        requestMethod = request.method
        httpUrl = request.url
        responseHeaders = response.headers
    }


    fun getRequestMethod(): String {
        return requestMethod
    }

    fun getRequestUrl(): String {
        return httpUrl.toString()
    }

    fun getHttpUrl(): HttpUrl {
        return httpUrl
    }

    fun getResponseHeaders(): Headers {
        return responseHeaders
    }

    override fun getLocalizedMessage(): String? {
        return errorCode
    }

    override fun toString(): String {
        return """${javaClass.name}:
$requestMethod $httpUrl

Code=$errorCode message=$message
$responseHeaders"""
    }
}