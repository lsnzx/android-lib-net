package com.lsn.lib.net.core.download;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Author: lsn
 * Blog: https://www.jianshu.com/u/a3534a2292e8
 * Date: 2021/1/14
 * Description
 */
public final class ThreadConfig {

    private final static AtomicInteger THREAD_ID_GENETOR = new AtomicInteger(1);

    public static int getUniqueThreadId() {
        return THREAD_ID_GENETOR.getAndIncrement();
    }

}
