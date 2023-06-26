package com.lsn.lib.net.core.download;

import android.net.Uri;

/**
 * Author: lsn
 * Blog: https://www.jianshu.com/u/a3534a2292e8
 * Date: 2021/1/14
 * Description
 */
public class DownloadListenerAdapter implements DownloadListener, DownloadingListener, DownloadStatusListener {

    @MainThread
    @Override
    public void onStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength, Extra extra) {
    }

    @Override
    public void onProgress(String url, long downloaded, long length, long usedTime) {
    }

    @MainThread
    @Override
    public boolean onResult(Throwable throwable, Uri path, String url, Extra extra) {
        return false;
    }

    @Override
    public void onDownloadStatusChanged(Extra extra, int status) {

    }
}
