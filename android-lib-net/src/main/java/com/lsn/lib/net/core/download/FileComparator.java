package com.lsn.lib.net.core.download;

import java.io.File;

/**
 * Author: lsn
 * Blog: https://www.jianshu.com/u/a3534a2292e8
 * Date: 2021/1/14
 * Description
 */
public interface FileComparator {

    int COMPARE_RESULT_SUCCESSFUL = 1;
    int COMPARE_RESULT_REDOWNLOAD_COVER = 2;
    int COMPARE_RESULT_REDOWNLOAD_RENAME = 3;

    int compare(String url, File originFile, String inputMD5, String originFileMD5);

    interface FileComparatorFactory {
        FileComparator newFileComparator();
    }
}
