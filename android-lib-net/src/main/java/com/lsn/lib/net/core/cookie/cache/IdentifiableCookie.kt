package com.lsn.lib.net.core.cookie.cache

import okhttp3.Cookie


/**
 * @Author : lsn
 * @CreateTime : 2023/3/9 下午 03:24
 * @Description :
 *
 * This class decorates a Cookie to re-implements equals() and hashcode() methods in order to identify
 * the cookie by the following attributes: name, domain, path, secure & hostOnly.
 *
 * This new behaviour will be useful in determining when an already existing cookie in session must be overwritten.
 *
 */
internal class IdentifiableCookie(val cookie: Cookie) {

    override fun equals(other: Any?): Boolean {
        if (other !is IdentifiableCookie) return false
        val that = other

        return that.cookie.name == cookie.name && that.cookie.domain == cookie.domain && that.cookie.path == cookie.path && that.cookie.secure == cookie.secure && that.cookie.hostOnly == cookie.hostOnly
    }

    override fun hashCode(): Int {
        var hash = 17
        hash = 31 * hash + cookie.name.hashCode()
        hash = 31 * hash + cookie.domain.hashCode()
        hash = 31 * hash + cookie.path.hashCode()
        hash = 31 * hash + if (cookie.secure) 0 else 1
        hash = 31 * hash + if (cookie.hostOnly) 0 else 1
        return hash
    }

    companion object {
        fun decorateAll(cookies: Collection<Cookie>): List<IdentifiableCookie> {
            val identifiableCookies: MutableList<IdentifiableCookie> = ArrayList(cookies.size)
            for (cookie in cookies) {
                identifiableCookies.add(IdentifiableCookie(cookie))
            }
            return identifiableCookies
        }
    }
}