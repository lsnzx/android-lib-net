package com.lsn.lib.net.core.download;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Author: lsn
 * Blog: https://www.jianshu.com/u/a3534a2292e8
 * Date: 2021/1/14
 * Description
 */
public interface DownloadingListener {
    /**
     * @param url        下载链接
     * @param downloaded 已经下载的长度
     * @param length     文件的总大小
     * @param usedTime   耗时,单位ms
     * 默认450左右毫秒回调一次,该方法默认回调在子线程， 如果加上注解MainThread注解修饰
     * 该方法， 会回调到主线程
     */
    void onProgress(String url, long downloaded, long length, long usedTime);

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface MainThread {
    }
}