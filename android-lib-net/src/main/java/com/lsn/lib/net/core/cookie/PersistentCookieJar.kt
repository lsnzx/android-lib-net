package com.lsn.lib.net.core.cookie

import com.lsn.lib.net.core.cookie.cache.CookieCache
import com.lsn.lib.net.core.cookie.persistence.CookiePersistor
import okhttp3.Cookie
import okhttp3.HttpUrl


/**
 * @Author : lsn
 * @CreateTime : 2023/3/9 下午 03:23
 * @Description :
 */
class PersistentCookieJar(cache: CookieCache, persistor: CookiePersistor) :
    ClearableCookieJar {
    private val cache: CookieCache
    private val persistor: CookiePersistor

    init {
        this.cache = cache
        this.persistor = persistor
        this.cache.addAll(persistor.loadAll())
    }

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cache.addAll(cookies)
        persistor.saveAll(filterPersistentCookies(cookies))
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val cookiesToRemove: MutableList<Cookie> = ArrayList()
        val validCookies: MutableList<Cookie> = ArrayList()

        val it: MutableIterator<Cookie> = cache.iterator() as MutableIterator<Cookie>
        it?.let { itemCookie ->
            while (itemCookie.hasNext()) {
                val currentCookie = itemCookie.next()
                if (isCookieExpired(currentCookie!!)) {
                    cookiesToRemove.add(currentCookie)
                    itemCookie.remove()
                } else if (currentCookie.matches(url)) {
                    validCookies.add(currentCookie)
                }
            }
        }


        /*val it: MutableIterator<Cookie> = cache.iterator()
        while (it.hasNext()) {
            val currentCookie = it.next()
            if (isCookieExpired(currentCookie)) {
                cookiesToRemove.add(currentCookie)
                it.remove()
            } else if (currentCookie.matches(url!!)) {
                validCookies.add(currentCookie)
            }
        }*/
        persistor.removeAll(cookiesToRemove)
        return validCookies
    }

    @Synchronized
    override fun clearSession() {
        cache.clear()
        cache.addAll(persistor.loadAll())
    }

    @Synchronized
    override fun clear() {
        cache.clear()
        persistor.clear()
    }

    companion object {
        private fun filterPersistentCookies(cookies: List<Cookie>): List<Cookie> {
            val persistentCookies: MutableList<Cookie> = ArrayList()
            for (cookie in cookies) {
                if (cookie.persistent) {
                    persistentCookies.add(cookie)
                }
            }
            return persistentCookies
        }

        private fun isCookieExpired(cookie: Cookie): Boolean {
            return cookie.expiresAt < System.currentTimeMillis()
        }
    }
}