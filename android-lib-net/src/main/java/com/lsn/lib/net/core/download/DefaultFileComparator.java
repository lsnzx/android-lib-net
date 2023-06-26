package com.lsn.lib.net.core.download;

import java.io.File;

/**
 * Author: lsn
 * Blog: https://www.jianshu.com/u/a3534a2292e8
 * Date: 2021/1/14
 * Description
 */
public class DefaultFileComparator implements FileComparator {

    @Override
    public int compare(String url, File originFile, String inputMD5, String originFileMD5) {
        if (inputMD5 == null) {
            inputMD5 = "";
        }
        if (inputMD5.trim().equalsIgnoreCase(originFileMD5)) {
            return FileComparator.COMPARE_RESULT_SUCCESSFUL;
        } else {
            return FileComparator.COMPARE_RESULT_REDOWNLOAD_RENAME;
        }
    }

    static class DefaultFileComparatorFactory implements FileComparatorFactory {
        @Override
        public FileComparator newFileComparator() {
            return new DefaultFileComparator();
        }
    }
}