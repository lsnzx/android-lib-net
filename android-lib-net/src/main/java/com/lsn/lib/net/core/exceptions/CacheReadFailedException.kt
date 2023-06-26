package com.lsn.lib.net.core.exceptions

import java.io.IOException


/**
 * @Author : lsn
 * @CreateTime : 2023/3/9 下午 01:38
 * @Description :
 */
class CacheReadFailedException(message: String?) : IOException(message)
