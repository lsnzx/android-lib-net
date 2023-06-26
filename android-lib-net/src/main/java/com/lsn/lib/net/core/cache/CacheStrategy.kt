package com.lsn.lib.net.core.cache


/**
 * @Author : lsn
 * @CreateTime : 2023/3/9 上午 10:43
 * @Description :
 */
class CacheStrategy {

    private var mCacheKey //缓存读写时的key
            : String? = null
    private var mCacheValidTime = Long.MAX_VALUE //缓存有效时间  默认Long.MAX_VALUE，代表永久有效

    private var mCacheMode //缓存模式
            : CacheMode? = null

    constructor(cacheStrategy:CacheStrategy) {
        mCacheKey = cacheStrategy.mCacheKey
        mCacheValidTime = cacheStrategy.mCacheValidTime
        mCacheMode = cacheStrategy.mCacheMode
    }

    constructor(cacheMode: CacheMode?) {
        mCacheMode = cacheMode
    }

    constructor(cacheMode: CacheMode, cacheValidTime: Long) {
        mCacheMode = cacheMode
        mCacheValidTime = cacheValidTime
    }

    fun getCacheKey(): String? {
        return mCacheKey
    }

    fun setCacheKey(key: String?) {
        mCacheKey = key
    }

    fun getCacheValidTime(): Long {
        return mCacheValidTime
    }

    fun setCacheValidTime(validTime: Long) {
        mCacheValidTime = validTime
    }

    fun getCacheMode(): CacheMode? {
        return mCacheMode
    }

    fun setCacheMode(cacheMode: CacheMode?) {
        mCacheMode = cacheMode
    }

}