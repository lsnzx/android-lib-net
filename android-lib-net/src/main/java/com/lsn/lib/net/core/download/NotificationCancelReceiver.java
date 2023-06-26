package com.lsn.lib.net.core.download;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

/**
 * Author: lsn
 * Blog: https://www.jianshu.com/u/a3534a2292e8
 * Date: 2021/1/14
 * Description
 */
public class NotificationCancelReceiver extends BroadcastReceiver {

    public static final String ACTION = "com.download.cancelled";
    private static final String TAG = Runtime.PREFIX + NotificationCancelReceiver.class.getSimpleName();

    public NotificationCancelReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Runtime.getInstance().log(TAG, "action:" + action);
        if (Runtime.getInstance().append(context, ACTION).equals(action)) {
            try {
                String url = intent.getStringExtra("TAG");
                if (!TextUtils.isEmpty(url)) {
                    DownloadImpl.getInstance(context).cancel(url);
                } else {
                    Runtime.getInstance().logError(action, " error url empty");
                }
            } catch (Throwable ignore) {
                if (Runtime.getInstance().isDebug()) {
                    ignore.printStackTrace();
                }
            }

        }
    }
}
