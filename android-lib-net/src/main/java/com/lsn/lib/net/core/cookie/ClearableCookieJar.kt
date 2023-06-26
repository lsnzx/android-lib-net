package com.lsn.lib.net.core.cookie

import okhttp3.CookieJar

/**
 * @Author : lsn
 * @CreateTime : 2023/3/9 下午 03:42
 * @Description :
 */
open interface ClearableCookieJar : CookieJar {
    /**
     * Clear all the session cookies while maintaining the persisted ones.
     */
    fun clearSession()

    /**
     * Clear all the cookies from persistence and from the cache.
     */
    fun clear()
}