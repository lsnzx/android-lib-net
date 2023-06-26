package com.lsn.lib.net.core.download;

import android.net.Uri;

import androidx.annotation.MainThread;

/**
 * Author: lsn
 * Blog: https://www.jianshu.com/u/a3534a2292e8
 * Date: 2021/1/14
 * Description
 */
public interface DownloadListener {


    /**
     * @param url                下载链接
     * @param userAgent          UserAgent
     * @param contentDisposition ContentDisposition
     * @param mimetype           资源的媒体类型
     * @param contentLength      文件长度
     * @param extra              下载配置
     */
    @MainThread
    void onStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength, Extra extra);

    /**
     * @param throwable 如果异常，返回给异常
     * @param path      文件的绝对路径
     * @param url       下载的地址
     * @return true     处理了下载完成后续的事件 ，false 默认交给Downloader 处理
     */
    @MainThread
    boolean onResult(Throwable throwable, Uri path, String url, Extra extra);


}
