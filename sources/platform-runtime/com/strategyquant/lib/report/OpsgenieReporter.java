/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.report;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpsgenieReporter {
    public static final Logger Log = LoggerFactory.getLogger(OpsgenieReporter.class);
    private static final String LIVE_API_KEY = "af9eefe9-93b0-4618-b213-bfa366bd8f5b";
    private static final String DEV_API_KEY = "5b574977-72b1-4183-9ab5-ebcf1f45b4af";
    private static final String URL = "https://api.opsgenie.com/v2/alerts";
    private static final int TIMEOUT = 20;

    private static RequestConfig createConfig() {
        return RequestConfig.custom().setConnectTimeout(20000).setConnectionRequestTimeout(20000).setSocketTimeout(20000).build();
    }

    public static void createAlert(Version version, String string, String string2) {
        OpsgenieReporter.createAlert(version, string, string2, null, null, null, null);
    }

    public static void createAlert(Version version, String string, String string2, String string3, String[] stringArray, Map<String, ?> map, Priority priority) {
        Log.info(String.format("Sending alert '%s' to Opsgenie ...", string));
        if (version == null) {
            throw new IllegalArgumentException("Version must be set");
        }
        if (string == null) {
            throw new IllegalArgumentException("Message must be set");
        }
        if (string2 == null) {
            throw new IllegalArgumentException("Description must be set");
        }
        if (priority == null) {
            priority = Priority.P3;
        }
        LinkedList<String> linkedList = new LinkedList<String>();
        linkedList.add(version.name());
        if (stringArray != null) {
            linkedList.addAll(Arrays.asList(stringArray));
        }
        JSONObject jSONObject = new JSONObject();
        jSONObject.put("priority", (Object)priority.name());
        jSONObject.put("message", (Object)string);
        jSONObject.put("alias", (Object)(string3 == null ? "" : string3));
        jSONObject.put("description", (Object)string2);
        jSONObject.put("tags", (Object)stringArray);
        if (map != null) {
            jSONObject.put("details", map);
        }
        String string4 = jSONObject.toString();
        StringEntity stringEntity = new StringEntity(string4, "UTF-8");
        try (CloseableHttpClient closeableHttpClient = HttpClientBuilder.create().setDefaultRequestConfig(OpsgenieReporter.createConfig()).build();){
            HttpPost httpPost = new HttpPost(URL);
            httpPost.addHeader("content-type", ContentType.APPLICATION_JSON.getMimeType());
            httpPost.addHeader("Authorization", "GenieKey " + (version == Version.DEV ? DEV_API_KEY : LIVE_API_KEY));
            httpPost.setEntity((HttpEntity)stringEntity);
            CloseableHttpResponse closeableHttpResponse = closeableHttpClient.execute((HttpUriRequest)httpPost);
            int n = closeableHttpResponse.getStatusLine().getStatusCode();
            if (n >= 400) {
                InputStream inputStream = closeableHttpResponse.getEntity().getContent();
                byte[] byArray = IOUtils.toByteArray((InputStream)inputStream);
                String string5 = new String(byArray);
                Log.error("Error while sending message to Opsgenie, httpcode: " + n + ", message: " + string5);
            }
            Log.info(String.format("Opsgenie alert '%s' has been sent successfully.", string));
        }
        catch (Exception exception) {
            Log.error("Error while sending message to Opsgenie", (Throwable)exception);
        }
    }

    public static void main(String[] stringArray) {
        HashMap<String, Object> hashMap = new HashMap<String, Object>();
        hashMap.put("user", "Nowack");
        hashMap.put("time", new Date().getTime());
        OpsgenieReporter.createAlert(Version.DEV, "petr-test 2", "test message 123", null, new String[]{"tag A", "tag B"}, hashMap, null);
        System.out.println("Finished");
    }

    public static enum Version {
        LIVE,
        DEV;

    }

    public static enum Priority {
        P1,
        P2,
        P3,
        P4,
        P5;

    }
}

