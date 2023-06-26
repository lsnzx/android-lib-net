package com.lsn.lib.net.core.cache;

import androidx.arch.core.util.Function;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import okhttp3.Call;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;

/**
 * @Author : lsn
 * @CreateTime : 2023/3/9 下午 01:33
 * @Description :
 */
public class HttpPlugins {

    private static final HttpPlugins plugins = new HttpPlugins();

    private OkHttpClient okClient;

    private List<String> excludeCacheKeys = Collections.emptyList();


    private Function<String, String> decoder;


    private IFileCache cache;
    private CacheStrategy cacheStrategy = new CacheStrategy(CacheMode.ONLY_NETWORK);

    private HttpPlugins() {
    }

    public static HttpPlugins init(OkHttpClient okHttpClient) {
        plugins.okClient = okHttpClient;
        return plugins;
    }

    public static boolean isInit() {
        return plugins.okClient != null;
    }

    public static OkHttpClient getOkHttpClient() {
        if (plugins.okClient == null) {
            init(getDefaultOkHttpClient());
        }
        return plugins.okClient;
    }

    public static OkHttpClient.Builder newOkClientBuilder() {
        return getOkHttpClient().newBuilder();
    }

    public HttpPlugins setDebug(boolean debug) {
        return setDebug(debug, false, -1);
    }

    public HttpPlugins setDebug(boolean debug, boolean segmentPrint) {
        return setDebug(debug, segmentPrint, -1);
    }

    public HttpPlugins setDebug(boolean debug, boolean segmentPrint, int indentSpaces) {
//        LogUtil.setDebug(debug, segmentPrint, indentSpaces);
        return this;
    }


    public HttpPlugins setCache(File directory, long maxSize) {
        return setCache(directory, maxSize, CacheMode.ONLY_NETWORK, Long.MAX_VALUE);
    }

    public HttpPlugins setCache(File directory, long maxSize, long cacheValidTime) {
        return setCache(directory, maxSize, CacheMode.ONLY_NETWORK, cacheValidTime);
    }

    public HttpPlugins setCache(File directory, long maxSize, CacheMode cacheMode) {
        return setCache(directory, maxSize, cacheMode, Long.MAX_VALUE);
    }

    public HttpPlugins setCache(File directory, long maxSize, CacheMode cacheMode, long cacheValidTime) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize > 0 required but it was " + maxSize);
        }
        if (cacheValidTime <= 0) {
            throw new IllegalArgumentException("cacheValidTime > 0 required but it was " + cacheValidTime);
        }
        CacheManager httpCache = new CacheManager(directory, maxSize);
        cache = httpCache.getInternalCache();
        cacheStrategy = new CacheStrategy(cacheMode, cacheValidTime);
        return this;
    }

    public static CacheStrategy getCacheStrategy() {
        return new CacheStrategy(plugins.cacheStrategy);
    }

    public static IFileCache getCache() {
        return plugins.cache;
    }

    /**
     * Call {@link HttpPlugins#setCache(File, long)} setCache method to set the cache directory and size before using the cache
     */
    public static IFileCache getCacheOrThrow() {
        final IFileCache cache = plugins.cache;
        if (cache == null) {
            throw new IllegalArgumentException("Call 'setCache(File,long)' method to set the cache directory and size before using the cache");
        }
        return cache;
    }

    public HttpPlugins setExcludeCacheKeys(String... keys) {
        excludeCacheKeys = Arrays.asList(keys);
        return this;
    }

    public static List<String> getExcludeCacheKeys() {
        return plugins.excludeCacheKeys;
    }

    //Cancel all requests
    public static void cancelAll() {
        cancelAll(plugins.okClient);
    }

    //Cancel the request according to tag
    public static void cancelAll(Object tag) {
        cancelAll(plugins.okClient, tag);
    }

    public static void cancelAll(@Nullable OkHttpClient okClient) {
        if (okClient == null) {
            return;
        }
        okClient.dispatcher().cancelAll();
    }

    public static void cancelAll(@Nullable OkHttpClient okClient, @Nullable Object tag) {
        if (tag == null || okClient == null) {
            return;
        }
        Dispatcher dispatcher = okClient.dispatcher();

        for (Call call : dispatcher.queuedCalls()) {
            if (tag.equals(call.request().tag())) {
                call.cancel();
            }
        }

        for (Call call : dispatcher.runningCalls()) {
            if (tag.equals(call.request().tag())) {
                call.cancel();
            }
        }
    }

    //Default OkHttpClient object in RxHttp
    private static OkHttpClient getDefaultOkHttpClient() {
        return new OkHttpClient.Builder().build();
    }
}
