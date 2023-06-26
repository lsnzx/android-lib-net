package com.lsn.lib.net.core.download;

import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;

/**
 * Author: lsn
 * Blog: https://www.jianshu.com/u/a3534a2292e8
 * Date: 2021/1/14
 * Description
 */
public class SameThreadExchanger<V> extends Exchanger<V> {

    private V v;

    public SameThreadExchanger() {
    }

    void setV(V v) {
        this.v = v;
    }

    @Override
    public V exchange(V x, long timeout, TimeUnit unit) {
        return exchange(v);
    }

    @Override
    public V exchange(V x) {
        try {
            V v = this.v;
            return v;
        } finally {
            this.v = null;
        }
    }
}
