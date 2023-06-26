package com.lsn.lib.net.core.download;

import android.os.Looper;

/**
 * Author: lsn
 * Blog: https://www.jianshu.com/u/a3534a2292e8
 * Date: 2021/1/14
 * Description
 */
public final class GlobalQueue {
    private static volatile DispatchThread mMainQueue = null;

    public static DispatchThread getMainQueue() {
        if (mMainQueue == null) {
            synchronized (GlobalQueue.class) {
                if (mMainQueue == null) {
                    mMainQueue = new DispatchThread(Looper.getMainLooper());
                }
            }
        }
        return mMainQueue;
    }
}
