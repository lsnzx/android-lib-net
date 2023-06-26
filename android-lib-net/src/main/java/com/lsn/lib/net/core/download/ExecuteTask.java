package com.lsn.lib.net.core.download;

/**
 * Author: lsn
 * Blog: https://www.jianshu.com/u/a3534a2292e8
 * Date: 2021/1/14
 * Description
 */
public interface ExecuteTask {
    DownloadTask cancelDownload();

    DownloadTask pauseDownload();

    DownloadTask getDownloadTask();
}
