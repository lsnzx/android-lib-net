package com.lsn.lib.net.core.cookie.cache

import okhttp3.Cookie


/**
 * @Author : lsn
 * @CreateTime : 2023/3/9 下午 03:25
 * @Description :
 */
class SetCookieCache : CookieCache {
    private val cookies: MutableSet<IdentifiableCookie>

    init {
        cookies = HashSet()
    }

    override fun addAll(newCookies: Collection<Cookie>) {
        for (cookie in IdentifiableCookie.decorateAll(newCookies)) {
            cookies.remove(cookie)
            cookies.add(cookie)
        }
    }

    override fun clear() {
        cookies.clear()
    }

    override fun iterator(): Iterator<Cookie> {
        return SetCookieCacheIterator()
    }

    private inner class SetCookieCacheIterator :
        MutableIterator<Cookie> {
        private val iterator: MutableIterator<IdentifiableCookie>

        init {
            iterator = cookies.iterator()
        }

        override fun hasNext(): Boolean {
            return iterator.hasNext()
        }

        override fun next(): Cookie {
            return iterator.next().cookie
        }

        override fun remove() {
            iterator.remove()
        }
    }
}