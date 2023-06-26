package com.lsn.lib.net.core.download;

/**
 * Author: lsn
 * Blog: https://www.jianshu.com/u/a3534a2292e8
 * Date: 2021/1/14
 * Description
 */
public interface DownloadStatusListener {

    /**
     * status 改变回调
     * @param extra
     * @param status
     */
    void onDownloadStatusChanged(Extra extra,@DownloadTask.DownloadTaskStatus int status);

}
