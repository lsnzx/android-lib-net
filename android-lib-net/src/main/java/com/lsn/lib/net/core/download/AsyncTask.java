package com.lsn.lib.net.core.download;

import android.os.Looper;



/**
 * Author: lsn
 * Blog: https://www.jianshu.com/u/a3534a2292e8
 * Date: 2021/1/14
 * Description
 */
public class AsyncTask {

    private static final DispatchThread MAIN_QUEUE = new DispatchThread(Looper.getMainLooper());

    protected void publishProgress(final Integer... values) {
        MAIN_QUEUE.post(new Runnable() {
            @Override
            public void run() {
                onProgressUpdate(values);
            }
        });
    }


    protected void onProgressUpdate(Integer... values) {

    }
}
