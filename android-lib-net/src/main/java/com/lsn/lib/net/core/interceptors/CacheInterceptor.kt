package com.lsn.lib.net.core.interceptors

import com.google.gson.Gson
import com.lsn.lib.net.core.entity.UrlBodyEntity
import com.lsn.lib.net.core.cache.*
import com.lsn.lib.net.core.exceptions.CacheReadFailedException
import com.lsn.lib.utils.util.CacheDiskUtils
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException


/**
 * @Author : lsn
 * @CreateTime : 2023/3/9 上午 11:50
 * @Description :
 */
class CacheInterceptor(
    private val cache: IFileCache,
    private val gson: Gson,
    private val cacheMode: CacheMode,
    private val cacheTime: Long
) : Interceptor {

    companion object {
        const val GET = "GET"
        const val POST = "POST"
        const val PUT = "PUT"
        const val DELETE = "DELETE"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val cacheResponse = beforeReadCache(request)
        if (cacheResponse != null) return cacheResponse  //缓存有效，直接返回
        try {
            //发起请求
            val response = chain.proceed(request)
            return if (!cacheModeIs(CacheMode.ONLY_NETWORK)) {
                //非ONLY_NETWORK模式下,请求成功，写入缓存
                chain.request().body?.toString()
                try {
                    // 缓存
                    val cacheKey = getCacheKey(request)
                    cache.put(response, cacheKey)
                } catch (e: Exception) {
                    response
                }
            } else {
                response
            }
        } catch (e: Throwable) {
            var networkResponse: Response? = null
            if (cacheModeIs(CacheMode.REQUEST_NETWORK_FAILED_READ_CACHE)) {
                //请求失败，读取缓存
                networkResponse = getCacheResponse(request, cacheTime)
            }
            return networkResponse ?: throw e
        }
    }

    private fun getCacheKey(request: Request): String {
        val toJson = gson.toJson(request.url)
        val fromJson = gson.fromJson(toJson, UrlBodyEntity::class.java)
        val buildCache = CacheDiskUtils.buildCacheKey(request.method, fromJson.url, request.body)
        return buildCache
    }


    private fun beforeReadCache(request: Request): Response? {
        return if (cacheModeIs(CacheMode.ONLY_CACHE, CacheMode.READ_CACHE_FAILED_REQUEST_NETWORK)) {
            //读取缓存
            val cacheResponse = getCacheResponse(request, cacheTime)
            if (cacheResponse == null) {
                if (cacheModeIs(CacheMode.ONLY_CACHE)) throw CacheReadFailedException("Cache read failed")
                return null
            }
            cacheResponse
        } else null
    }

    private fun cacheModeIs(vararg cacheModes: CacheMode): Boolean {
        val cacheMode = cacheMode
        cacheModes.forEach {
            if (it == cacheMode) return true
        }
        return false
    }

    @Throws(IOException::class)
    private fun getCacheResponse(request: Request, validTime: Long): Response? {
        val cacheKey = getCacheKey(request)
        val cacheResponse = cache[request, cacheKey]
        return if (cacheResponse != null) {
            // Verify cache validity
            val receivedTime = OkHttpCompat.receivedResponseAtMillis(cacheResponse)
            if (validTime == Long.MAX_VALUE || System.currentTimeMillis() - receivedTime <= validTime) {
                return cacheResponse
            } else {
                // 缓存存在且过期，清除
                cache.remove(cacheKey)
                return null //Cache expired, return null
            }
        } else null
    }
}