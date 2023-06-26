package com.lsn.lib.net.core.interceptors

import android.text.TextUtils
import com.google.gson.Gson
import com.lsn.lib.net.core.code.Code
import com.lsn.lib.net.core.entity.ResponseApi
import com.lsn.lib.net.core.exceptions.ParseDataException
import com.lsn.lib.net.core.getOkCode
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody


/**
 * @Author : lsn
 * @CreateTime : 2023/3/30 上午 10:17
 * @Description :
 */
class ResponseParseInterceptor(var gson: Gson) : Interceptor {


    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        val body = response.body
        if (body != null) {
            val string = body.string()
            if (!TextUtils.isEmpty(string)) {
                val responseApi = gson.fromJson(string, ResponseApi::class.java)
                return if (responseApi.code == getOkCode()) {
                    response.newBuilder()
                        .body(ResponseBody.create(body.contentType(), string))
                        .build()
                } else {
                    throw ParseDataException(
                        Code.Exception.NO_SERVICE_DATA_EXCEPTION_CODE.toString(),
                        "状态码异常 信息 : " + responseApi.message,
                        response
                    )
                }
            } else {
                // body.string 异常
                throw ParseDataException(
                    Code.Exception.NO_BODY_EXCEPTION_CODE.toString(),
                    "请求异常 Body.String异常",
                    response
                )
            }
        } else {
            // body 为空
            throw ParseDataException(
                Code.Exception.NO_BODY_STRING_EXCEPTION_CODE.toString(),
                "请求异常 Body异常",
                response
            )
        }
    }
}