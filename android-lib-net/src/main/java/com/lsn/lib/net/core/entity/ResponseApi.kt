package com.lsn.lib.net.core.entity


/**
 * @Author : lsn
 * @CreateTime : 2023/4/3 下午 04:39
 * @Description :
 */
data class ResponseApi<T>(val data: T?, val code: Int, val message: String)