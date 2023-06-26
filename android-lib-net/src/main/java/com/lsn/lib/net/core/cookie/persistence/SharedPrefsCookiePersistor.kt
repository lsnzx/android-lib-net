package com.lsn.lib.net.core.cookie.persistence

import android.content.Context
import android.content.SharedPreferences
import okhttp3.Cookie


/**
 * @Author : lsn
 * @CreateTime : 2023/3/9 下午 03:27
 * @Description :
 */
class SharedPrefsCookiePersistor(sharedPreferences: SharedPreferences) :
    CookiePersistor {
    private val sharedPreferences: SharedPreferences

    constructor(context: Context) : this(
        context.getSharedPreferences(
            "CookiePersistence",
            Context.MODE_PRIVATE
        )
    ) {
    }

    init {
        this.sharedPreferences = sharedPreferences
    }

    override fun loadAll(): List<Cookie> {
        val cookies: MutableList<Cookie> = ArrayList(sharedPreferences.all.size)

        for (entity in sharedPreferences.all){
            val serializedCookie = entity.value as String
            val cookie = SerializableCookie().decode(serializedCookie)
            if (cookie != null) {
                cookies.add(cookie)
            }
        }
        return cookies
    }

    override fun saveAll(cookies: Collection<Cookie>) {
        val editor = sharedPreferences.edit()
        for (cookie in cookies) {
            editor.putString(createCookieKey(cookie), SerializableCookie().encode(cookie))
        }
        editor.commit()
    }

    override fun removeAll(cookies: Collection<Cookie>) {
        val editor = sharedPreferences.edit()
        for (cookie in cookies) {
            editor.remove(createCookieKey(cookie))
        }
        editor.commit()
    }

    override fun clear() {
        sharedPreferences.edit().clear().commit()
    }

    companion object {
        private fun createCookieKey(cookie: Cookie): String {
            return (if (cookie.secure) "https" else "http") + "://" + cookie.domain + cookie.path + "|" + cookie.name
        }
    }
}