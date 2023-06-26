package com.lsn.lib.net.core

import com.lsn.lib.net.core.code.Code


private var mCode: Int = 0

fun getOkCode(): Int {
    return mCode
}

fun setOkCode(code: Int){
    mCode = code
}

data class NetConfigEntity(
    var bridgeName: String = "",
    var apiServiceUrl: String = "",
    var appFileUrl: String = "",
    var versionName: String = "",
    var versionCode: Int = 1,
    var cacheFilePath: String = "",
    var diskCacheName: String = "",
    var diskCacheSize: Float = 1f,           // 单位MB
    var defCacheDay: Float = 1f,             // 单位天
    var connectTime: Long = 1000,            // 单位毫秒
    var readTime: Long = 1000,
    var writeTime: Long = 1000,
)