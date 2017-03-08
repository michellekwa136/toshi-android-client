package com.tokenbrowser.manager.network.interceptor;


import com.tokenbrowser.view.BaseApplication;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.Response;

import static com.google.common.net.HttpHeaders.CACHE_CONTROL;

public class ReadFromCacheInterceptor implements Interceptor {

    @Override
    public Response intercept(final Chain chain) throws IOException, IllegalStateException {
        final Response response = chain.proceed( chain.request() );

        CacheControl cacheControl = new CacheControl.Builder()
                .maxAge(60, TimeUnit.SECONDS)
                .build();

        if (!BaseApplication.get().isNetworkAvailable()) {
            cacheControl = new CacheControl.Builder()
                    .maxAge(14, TimeUnit.DAYS)
                    .build();
        }

        return response.newBuilder()
                .header(CACHE_CONTROL, cacheControl.toString())
                .build();
    }
}
