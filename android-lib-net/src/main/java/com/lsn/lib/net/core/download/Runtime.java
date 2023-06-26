package com.lsn.lib.net.core.download;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import com.lsn.lib.net.BuildConfig;
import com.lsn.lib.utils.util.DeviceUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Author: lsn
 * Blog: https://www.jianshu.com/u/a3534a2292e8
 * Date: 2021/1/14
 * Description
 */
public final class Runtime {

    private static final Runtime sInstance = new Runtime();
    private DownloadTask sDefaultDownloadTask;
    private AtomicInteger mIDGenerator;
    private AtomicInteger mThreadGlobalCounter;
    private File mDownloadDir = null;
    private static Pattern DISPOSITION_PATTERN = Pattern.compile("attachment;\\s*filename\\*\\s*=\\s*\"*([^\"]*)'\\S*'([^\"]*)\"*");
    private static final Pattern CONTENT_DISPOSITION_WITHOUT_ASTERISK_PATTERN =
            Pattern.compile("attachment;\\s*filename\\s*=\\s*\"*([^\"\\n]*)\"*");
    static final String PREFIX = "Download-";
//    boolean DEBUG = BuildConfig.DEBUG;
    boolean DEBUG = false;
    private String authority;
    private StorageEngine mStorageEngine;
    private StorageEngine.StorageEngineFactory mStorageEngineFactory;
    private FileComparator.FileComparatorFactory mFileComparatorFactory;
    private FileComparator mFileComparator;

    public void setDebug(boolean debug) {
        this.DEBUG = debug;
    }

    public boolean isDebug() {
        return DEBUG;
    }

    private Runtime() {
        mIDGenerator = new AtomicInteger(1);
        mThreadGlobalCounter = new AtomicInteger(1);
    }

    public static Runtime getInstance() {
        return sInstance;
    }

    public StorageEngine getStorageEngine(Context context) {
        StorageEngine storageEngine = this.mStorageEngine;
        if (null == storageEngine) {
            storageEngine = this.mStorageEngine = getStorageEngineFactory().newStoraEngine(context);
        }
        return storageEngine;
    }

    StorageEngine.StorageEngineFactory getStorageEngineFactory() {
        StorageEngine.StorageEngineFactory storageEngineFactory = this.mStorageEngineFactory;
        if (null == mStorageEngineFactory) {
            storageEngineFactory = this.mStorageEngineFactory = new DefaultStorageEngine.DefaultStorageEngineFactory();
        }
        return storageEngineFactory;
    }

    @NonNull
    public FileComparator getFileComparator() {
        FileComparator fileComparator = this.mFileComparator;
        if (fileComparator == null) {
            fileComparator = this.mFileComparator = getFileComparatorFactory().newFileComparator();
        }
        return fileComparator;
    }

    FileComparator.FileComparatorFactory getFileComparatorFactory() {
        FileComparator.FileComparatorFactory fileComparatorFactory = this.mFileComparatorFactory;
        if (fileComparatorFactory == null) {
            fileComparatorFactory = new DefaultFileComparator.DefaultFileComparatorFactory();
        }
        return fileComparatorFactory;
    }

    public void setFileComparatorFactory(FileComparator.FileComparatorFactory fileComparatorFactory) {
        this.mFileComparatorFactory = fileComparatorFactory;
        this.mFileComparator = null;
    }

    public void setStorageEngineFactory(StorageEngine.StorageEngineFactory storageEngineFactory) {
        mStorageEngineFactory = storageEngineFactory;
        this.mStorageEngine = null;
    }

    public synchronized DownloadTask getDefaultDownloadTask() {
        if (null == sDefaultDownloadTask) {
            createDefaultDownloadTask();
        }
        return sDefaultDownloadTask.clone();
    }

    private synchronized void createDefaultDownloadTask() {
        sDefaultDownloadTask = new DownloadTask();
        sDefaultDownloadTask.setBreakPointDownload(true)
                .setIcon(android.R.drawable.stat_sys_download)
                .setConnectTimeOut(6000)
                .setBlockMaxTime(10 * 60 * 1000)
                .setDownloadTimeOut(Long.MAX_VALUE)
                .setParallelDownload(true)
                .setEnableIndicator(false)
                .closeAutoOpen()
                .setForceDownload(true);
    }

    public synchronized void setDownloadTask(DownloadTask downloadTask) {
        sDefaultDownloadTask = downloadTask;
    }

    public String getIdentify(Context context) {
        return append(context, "Downloader");
    }

    public String getVersion() {
        return DeviceUtils.getSDKVersionName();
    }

    public int generateGlobalId() {
        return mIDGenerator.getAndIncrement();
    }

    public int generateGlobalThreadId() {
        return mThreadGlobalCounter.getAndIncrement();
    }

    public File createFile(Context context, Extra extra) {
        return createFile(context, extra, null);
    }

    public File createFile(Context context, Extra extra, File dir) {
        String fileName = "";
        try {
            fileName = getFileNameByContentDisposition(extra.getContentDisposition());
            if (TextUtils.isEmpty(fileName) && !TextUtils.isEmpty(extra.getUrl())) {
                Uri mUri = Uri.parse(extra.getUrl());
                fileName = mUri.getPath().substring(mUri.getPath().lastIndexOf('/') + 1);
            }
            if (!TextUtils.isEmpty(fileName) && fileName.length() > 64) {
                fileName = fileName.substring(fileName.length() - 64, fileName.length());
            }
            if (TextUtils.isEmpty(fileName)) {
                fileName = md5(extra.getUrl());
            }
            if (fileName.contains("\"")) {
                fileName = fileName.replace("\"", "");
            }
            String path = (dir == null || !dir.isDirectory()) ? getDir(context, extra.isEnableIndicator() || extra.isAutoOpen()).getPath() : dir.getAbsolutePath();
            File pathFile = new File(path);
            if (!pathFile.exists()) {
                pathFile.mkdirs();
            }
            return createFileByName(pathFile, context, fileName, !extra.isBreakPointDownload());
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    boolean checkWifi(Context context) {
        ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity == null) {
            return false;
        }
        NetworkInfo info = connectivity.getActiveNetworkInfo();
        return info != null && info.isConnected() && info.getType() == ConnectivityManager.TYPE_WIFI;
    }

    boolean checkNetwork(Context context) {
        ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity == null) {
            return false;
        }
        NetworkInfo info = connectivity.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }

    File createFileByName(File path, Context context, String name, boolean cover) throws IOException {
        if (!path.isDirectory()) {
            return null;
        }
        File mFile = new File(path, name);
        if (mFile.exists()) {
            if (cover) {
                mFile.delete();
                mFile.createNewFile();
            }
        } else {
            mFile.createNewFile();
        }
        return mFile;
    }

    String getFileNameByContentDisposition(String contentDisposition) {
        if (TextUtils.isEmpty(contentDisposition)) {
            return "";
        }
        try {
            Matcher m = DISPOSITION_PATTERN.matcher(contentDisposition);
            if (m.find()) {
                String charset = m.group(1);
                String encodeFileName = m.group(2);
                return URLDecoder.decode(encodeFileName, charset);
            }

            m = CONTENT_DISPOSITION_WITHOUT_ASTERISK_PATTERN.matcher(contentDisposition);
            if (m.find()) {
                return m.group(1);
            }
        } catch (IllegalStateException | UnsupportedEncodingException ignore) {
        }
        return "";
    }

    File getDir(Context context, boolean isPublic) {
        File file = (mDownloadDir == null || !mDownloadDir.isDirectory()) ? context.getCacheDir() : mDownloadDir;
        file = new File(file, "download" + File.separator + (isPublic ? "public" : "private"));
        if (!file.exists()) {
            file.mkdirs();
        }
        return file;
    }

    public File getDir(Context context) {
        return getDir(context, false);
    }

    public File getDefaultDir(Context context) {
        File file = new File(context.getCacheDir(), "download");
        if (!file.exists()) {
            file.mkdirs();
        }
        return file;
    }

    public void log(String tag, String msg) {
        if (DEBUG) {
            Log.i(tag, msg);
        }
    }

    public void log(String tag, String msg, Throwable throwable) {
        if (DEBUG) {
            Log.i(tag, msg, throwable);
        }
    }

    public File uniqueFile(@NonNull DownloadTask downloadTask, @Nullable File targetDir) {
        String md5 = Runtime.getInstance().md5(downloadTask.getUrl());
        File dir = (targetDir == null || !targetDir.isDirectory()) ? Runtime.getInstance().getDir(downloadTask.getContext(), downloadTask.isEnableIndicator() || downloadTask.isAutoOpen()) : targetDir;
        File target = new File(dir, md5);
        if (!target.exists()) {
            target.mkdirs();
        }
        if (!target.isDirectory()) {
            target.delete();
            target.mkdirs();
        }
        return createFile(downloadTask.getContext(), downloadTask, target);
    }

    public void setDownloadDir(File downloadDir, String authority) {
        mDownloadDir = downloadDir;
        this.authority = authority;
    }

    public void logError(String tag, String msg) {
        if (DEBUG) {
            Log.e(tag, msg);
        }
    }

    public String md5(String str) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(str.getBytes());
            return new BigInteger(1, md.digest()).toString(16);
        } catch (Exception e) {
            if (isDebug()) {
                e.printStackTrace();
            }
        }
        return "";
    }

    public String md5(File file) {
        MessageDigest digest = null;
        FileInputStream fis = null;
        byte[] buffer = new byte[1024];
        try {
            if (!file.isFile()) {
                return "";
            }
            digest = MessageDigest.getInstance("MD5");
            fis = new FileInputStream(file);
            while (true) {
                int len;
                if ((len = fis.read(buffer, 0, 1024)) == -1) {
                    fis.close();
                    break;
                }
                digest.update(buffer, 0, len);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        BigInteger bigInteger = new BigInteger(1, digest.digest());
        return String.format("%1$032x", new Object[]{bigInteger});
    }

    public String getApplicationName(Context context) {
        PackageManager packageManager = null;
        ApplicationInfo applicationInfo = null;
        try {
            packageManager = context.getApplicationContext().getPackageManager();
            applicationInfo = packageManager.getApplicationInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            applicationInfo = null;
        }
        String applicationName =
                (String) packageManager.getApplicationLabel(applicationInfo);
        return applicationName;
    }

    public Intent getCommonFileIntentCompat(Context context, DownloadTask downloadTask) {
        Intent mIntent = new Intent().setAction(Intent.ACTION_VIEW);
        setIntentDataAndType(context, mIntent, getMIMEType(downloadTask.getFile()), downloadTask.getFile(), false, downloadTask.isCustomFile() ? downloadTask.getAuthority() : getAuthority(downloadTask.getContext()));
        return mIntent;
    }

    private String getAuthority(Context context) {
        return TextUtils.isEmpty(this.authority) ? (this.authority = context.getPackageName() + ".DownloadFileProvider") : this.authority;
    }

    public Uri getUriFromFile(Context context, File file, String authority) {
        Uri uri = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            uri = FileProvider.getUriForFile(context, authority, file); //getUriFromFileForN(context, file);
        } else {
            uri = Uri.fromFile(file);
        }
        return uri;
    }

    public Uri getUriFromFileForN(Context context, File file) {
        Uri fileUri = FileProvider.getUriForFile(context, context.getPackageName() + ".DownloadFileProvider", file);
        return fileUri;
    }


    public void setIntentDataAndType(Context context,
                                     Intent intent,
                                     String type,
                                     File file,
                                     boolean writeAble, String authority) {
        if (Build.VERSION.SDK_INT >= 24) {
            intent.setDataAndType(getUriFromFile(context, file, authority), type);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if (writeAble) {
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            }
        } else {
            intent.setDataAndType(Uri.fromFile(file), type);
        }
    }

    public String getMIMEType(File f) {
        String type = "";
        String fName = f.getName();
        String end = fName.substring(fName.lastIndexOf(".") + 1, fName.length()).toLowerCase();
        if (end.equals("pdf")) {
            type = "application/pdf";
        } else if (end.equals("m4a") || end.equals("mp3") || end.equals("mid") ||
                end.equals("xmf") || end.equals("ogg") || end.equals("wav")) {
            type = "audio/*";
        } else if (end.equals("3gp") || end.equals("mp4")) {
            type = "video/*";
        } else if (end.equals("jpg") || end.equals("gif") || end.equals("png") ||
                end.equals("jpeg") || end.equals("bmp")) {
            type = "image/*";
        } else if (end.equals("apk")) {
            type = "application/vnd.android.package-archive";
        } else if (end.equals("pptx") || end.equals("ppt")) {
            type = "application/vnd.ms-powerpoint";
        } else if (end.equals("docx") || end.equals("doc")) {
            type = "application/vnd.ms-word";
        } else if (end.equals("xlsx") || end.equals("xls")) {
            type = "application/vnd.ms-excel";
        } else {
            type = "*/*";
        }
        return type;
    }

    public String append(Context context, String action) {
        StringBuffer sb = new StringBuffer();
        sb.append(getCurrentProcessName(context)).append("-").append(action);
        return sb.toString();
    }

    /**
     * from AndroidUtilCode
     *
     * @param context
     * @return
     */
    public String getCurrentProcessName(Context context) {
        String name = getCurrentProcessNameByFile(context);
        if (!TextUtils.isEmpty(name)) {
            return name;
        }
        name = getCurrentProcessNameByAms(context);
        if (!TextUtils.isEmpty(name)) {
            return name;
        }
        name = getCurrentProcessNameByReflect(context);
        return name;
    }

    private static String getCurrentProcessNameByFile(Context context) {
        try {
            File file = new File("/proc/" + android.os.Process.myPid() + "/" + "cmdline");
            BufferedReader mBufferedReader = new BufferedReader(new FileReader(file));
            String processName = mBufferedReader.readLine().trim();
            mBufferedReader.close();
            return processName;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private static String getCurrentProcessNameByAms(Context context) {
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am == null) return "";
            List<ActivityManager.RunningAppProcessInfo> info = am.getRunningAppProcesses();
            if (info == null || info.size() == 0) return "";
            int pid = android.os.Process.myPid();
            for (ActivityManager.RunningAppProcessInfo aInfo : info) {
                if (aInfo.pid == pid) {
                    if (aInfo.processName != null) {
                        return aInfo.processName;
                    }
                }
            }
        } catch (Exception e) {
            return "";
        }
        return "";
    }

    private static String getCurrentProcessNameByReflect(Context context) {
        String processName = "";
        try {
            Application app = (Application) context.getApplicationContext();
            Field loadedApkField = app.getClass().getField("mLoadedApk");
            loadedApkField.setAccessible(true);
            Object loadedApk = loadedApkField.get(app);

            Field activityThreadField = loadedApk.getClass().getDeclaredField("mActivityThread");
            activityThreadField.setAccessible(true);
            Object activityThread = activityThreadField.get(loadedApk);

            Method getProcessName = activityThread.getClass().getDeclaredMethod("getProcessName");
            processName = (String) getProcessName.invoke(activityThread);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return processName;
    }
}
