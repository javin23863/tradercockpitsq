/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.utils.network;

import com.strategyquant.lib.utils.network.IDataDownloader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

public class DataDownloader {
    public void toFile(String string, String string2, IDataDownloader iDataDownloader) throws Exception {
        try {
            CloseableHttpClient closeableHttpClient = HttpClientBuilder.create().build();
            HttpGet httpGet = new HttpGet(string);
            HttpResponse httpResponse = closeableHttpClient.execute((HttpUriRequest)httpGet);
            if (httpResponse.getStatusLine().getStatusCode() != 200) {
                throw new Exception("HTML status code - " + httpResponse.getStatusLine().getStatusCode());
            }
            HttpEntity httpEntity = httpResponse.getEntity();
            Long l = httpEntity.getContentLength();
            InputStream inputStream = httpEntity.getContent();
            File file = new File(string2);
            if (!file.exists()) {
                file.getParentFile().mkdirs();
            }
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            int n = 0;
            byte[] byArray = new byte[4096];
            int n2 = 0;
            long l2 = 0L;
            while ((n2 = inputStream.read(byArray)) != -1) {
                fileOutputStream.write(byArray, 0, n2);
                n = (int)((l2 += (long)n2) * 100L / l);
                iDataDownloader.onProgress(n, l2, l, n2);
            }
            inputStream.close();
            fileOutputStream.close();
        }
        catch (Error error) {
            throw new Exception("Error while downloading data from url '" + string + "'.\nExc.", error);
        }
        catch (Exception exception) {
            throw new Exception("Error while downloading data from url '" + string + "'.\nExc.", exception);
        }
    }

    public long getContentLength(String string) throws Exception {
        try {
            CloseableHttpClient closeableHttpClient = HttpClientBuilder.create().build();
            HttpGet httpGet = new HttpGet(string);
            HttpResponse httpResponse = closeableHttpClient.execute((HttpUriRequest)httpGet);
            if (httpResponse.getStatusLine().getStatusCode() != 200) {
                throw new Exception("HTML status code - " + httpResponse.getStatusLine().getStatusCode());
            }
            HttpEntity httpEntity = httpResponse.getEntity();
            return httpEntity.getContentLength();
        }
        catch (Error error) {
            throw new Exception("Error while obtaining content length of url '" + string + "'.\nExc.", error);
        }
        catch (Exception exception) {
            throw new Exception("Error while obtaining content length of url '" + string + "'.\nExc.", exception);
        }
    }
}

