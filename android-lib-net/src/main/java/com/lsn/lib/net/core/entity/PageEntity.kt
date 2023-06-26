package com.lsn.lib.net.core.entity


/**
 * @Author : lsn
 * @CreateTime : 2023/3/30 上午 08:28
 * @Description :
 */
data class PageEntity<T>(
    var curPage: Int = 1,
    var datas: List<T> = ArrayList(),
    var offset: Int = 0,
    var over: Boolean = false,
    var pageCount: Int = 0,
    var size: Int = 0,
    var total: Int = 0,
)