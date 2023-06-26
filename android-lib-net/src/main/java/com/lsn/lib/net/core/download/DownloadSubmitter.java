package com.lsn.lib.net.core.download;

import java.io.File;

/**
 * Author: lsn
 * Blog: https://www.jianshu.com/u/a3534a2292e8
 * Date: 2021/1/14
 * Description
 */
public interface DownloadSubmitter {

    /**
     * submit the download task
     *
     * @param downloadTask
     */
    boolean submit(DownloadTask downloadTask);


    File submit0(DownloadTask downloadTask) throws Exception;

}
