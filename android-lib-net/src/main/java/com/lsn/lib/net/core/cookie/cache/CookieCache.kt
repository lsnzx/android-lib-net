package com.lsn.lib.net.core.cookie.cache

import okhttp3.Cookie

/**
 * @Author : lsn
 * @CreateTime : 2023/3/9 下午 03:24
 * @Description :
 */
open interface CookieCache : Iterable<Cookie?> {
    /**
     * Add all the new cookies to the session, existing cookies will be overwritten.
     *
     * @param cookies
     */
    fun addAll(cookies: Collection<Cookie>)

    /**
     * Clear all the cookies from the session.
     */
    fun clear()
}