package com.lsn.lib.net.core.download;

import android.content.Context;

/**
 * Author: lsn
 * Blog: https://www.jianshu.com/u/a3534a2292e8
 * Date: 2021/1/14
 * Description
 */
public interface StorageEngine {

    void save(String key, String value);

    String get(String key, String defaultValue);

    public interface StorageEngineFactory {
        StorageEngine newStoraEngine(Context context);
    }
}
