package com.lsn.lib.net.core.download;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Author: lsn
 * Blog: https://www.jianshu.com/u/a3534a2292e8
 * Date: 2021/1/14
 * Description
 */
public class DefaultStorageEngine implements StorageEngine {
    Context mContext;

    DefaultStorageEngine(Context context) {
        this.mContext = context;
    }

    @Override
    public void save(String key, String value) {
        SharedPreferences mSharedPreferences = mContext.getSharedPreferences(Runtime.getInstance().getIdentify(mContext), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    @Override
    public String get(String key, String defaultValue) {
        SharedPreferences mSharedPreferences = mContext.getSharedPreferences(Runtime.getInstance().getIdentify(mContext), Context.MODE_PRIVATE);
        String value = mSharedPreferences.getString(key, defaultValue);
        return value;
    }

    public static class DefaultStorageEngineFactory implements StorageEngineFactory {

        @Override
        public StorageEngine newStoraEngine(Context context) {
            return new DefaultStorageEngine(context);
        }
    }
}
