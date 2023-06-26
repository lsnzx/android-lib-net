package com.lsn.lib.net.core.cache

/**
 * @Author : lsn
 * @CreateTime : 2023/3/9 上午 10:43
 * @Description :
 */

enum class CacheMode {
    /**
     * 仅请求网络，默认模式  （不写缓存）
     */
    ONLY_NETWORK,

    /**
     * 仅读取缓存  （不写缓存）
     */
    ONLY_CACHE,

    /**
     * 请求成功后，写入缓存 跟[.ONLY_NETWORK] 默认模式相比，仅多了写缓存的操作
     */
    NETWORK_SUCCESS_WRITE_CACHE,

    /**
     * 先读取缓存，失败后再请求网络  (网络请求成功，写缓存)
     */
    READ_CACHE_FAILED_REQUEST_NETWORK,

    /**
     * 先请求网络，失败后再读取缓存  (网络请求成功，写缓存)
     */
    REQUEST_NETWORK_FAILED_READ_CACHE
}