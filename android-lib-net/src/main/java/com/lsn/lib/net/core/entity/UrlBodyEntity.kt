package com.lsn.lib.net.core.entity


/**
 * @Author : lsn
 * @CreateTime : 2023/3/10 下午 06:00
 * @Description :
 */
data class UrlBodyEntity(
    val host: String,
    val isHttps: Boolean,
    val password: String,
    val pathSegments: List<String>,
    val port: Int,
    val queryNamesAndValues: List<String>,
    val scheme: String,
    val url: String,
    val username: String
)