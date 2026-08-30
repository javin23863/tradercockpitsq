/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.utils.network;

import com.strategyquant.lib.utils.network.IHttpRequestListener;
import java.util.concurrent.Future;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpUtil {
    private static final Logger Log = LoggerFactory.getLogger(HttpUtil.class);
    private static final int connectionRequestTimeout = 2000;
    private static final int socketTimeout = 2000;
    private static final int connectTimeout = 2000;

    public static void sendAsyncRequest(String string, final IHttpRequestListener iHttpRequestListener) throws Exception {
        final CloseableHttpAsyncClient closeableHttpAsyncClient = HttpAsyncClients.createDefault();
        closeableHttpAsyncClient.start();
        RequestConfig.Builder builder = RequestConfig.custom();
        builder.setConnectionRequestTimeout(2000);
        builder.setSocketTimeout(2000);
        builder.setConnectTimeout(2000);
        HttpGet httpGet = new HttpGet(string);
        httpGet.setConfig(builder.build());
        final Future future = closeableHttpAsyncClient.execute((HttpUriRequest)httpGet, null);
        new Thread(){

            @Override
            public void run() {
                try {
                    iHttpRequestListener.onResponse((HttpResponse)future.get());
                }
                catch (Exception exception) {
                    Log.error("Cannot get response", (Throwable)exception);
                }
                finally {
                    try {
                        closeableHttpAsyncClient.close();
                    }
                    catch (Exception exception) {
                        Log.error("Cannot close client", (Throwable)exception);
                    }
                }
            }
        }.start();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static HttpResponse sendRequest(String string, String string2) throws Exception {
        Log.debug("Sending request to " + string);
        CloseableHttpAsyncClient closeableHttpAsyncClient = HttpAsyncClients.createDefault();
        closeableHttpAsyncClient.start();
        try {
            RequestConfig.Builder builder = RequestConfig.custom();
            builder.setConnectionRequestTimeout(2000);
            builder.setSocketTimeout(2000);
            builder.setConnectTimeout(2000);
            HttpGet httpGet = new HttpGet(string);
            if (string2 != null) {
                httpGet.addHeader("sq-auth-token", string2);
            }
            httpGet.setConfig(builder.build());
            Future future = closeableHttpAsyncClient.execute((HttpUriRequest)httpGet, null);
            HttpResponse httpResponse = (HttpResponse)future.get();
            return httpResponse;
        }
        finally {
            try {
                closeableHttpAsyncClient.close();
            }
            catch (Exception exception) {
                Log.error("Cannot close client", (Throwable)exception);
            }
        }
    }
}

