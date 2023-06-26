package com.lsn.lib.net.core.cache

import okhttp3.Request
import okhttp3.Response
import java.io.IOException

/**
 * @Author : lsn
 * @CreateTime : 2023/3/9 上午 10:41
 * @Description :
 */
interface IFileCache {

    @Throws(IOException::class)
    operator fun get(request: Request, key: String): Response?

    @Throws(IOException::class)
    fun put(response: Response, key: String): Response

    @Throws(IOException::class)
    fun remove(key: String)

    @Throws(IOException::class)
    fun removeAll()

    @Throws(IOException::class)
    fun size(): Long
}