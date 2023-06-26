package com.lsn.lib.net.core.download;

import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Author: lsn
 * Blog: https://www.jianshu.com/u/a3534a2292e8
 * Date: 2021/1/14
 * Description
 */
public class DispatchPairExchanger<V> extends Exchanger<V> {
    private final long mThreadId;
    private final String mThreadName;

    public DispatchPairExchanger() {
        mThreadId = Thread.currentThread().getId();
        mThreadName = Thread.currentThread().getName();
    }

    V exchange0(V x) throws InterruptedException {
        return super.exchange(x);
    }

    V exchange0(V x, long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
        return super.exchange(x, timeout, unit);
    }

    @Override
    public V exchange(V x) throws InterruptedException {
        long id = Thread.currentThread().getId();
        if (id != mThreadId) {
            throw new RuntimeException("you must call exchange in the thread id:" + id + " thread name:" + mThreadName);
        }
        return super.exchange(x);
    }

    @Override
    public V exchange(V x, long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
        long id = Thread.currentThread().getId();
        if (id != mThreadId) {
            throw new RuntimeException("you must call exchange in the thread id:" + id + " thread name:" + mThreadName);
        }
        return super.exchange(x, timeout, unit);
    }
}
