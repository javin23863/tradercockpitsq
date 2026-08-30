/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.customization;

import com.strategyquant.lib.L88OaFjjon.V571hfnsHw;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.app.MainApp;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Customization {
    public static final Logger Log = LoggerFactory.getLogger(Customization.class);
    private static final String CUSTOMIZATION_URL = "https://api.strategyquant.com/sqcustomization/index?license=";

    public static JSONObject load() {
        return Customization.load(false);
    }

    public static JSONObject load(boolean bl) {
        JSONObject jSONObject = new JSONObject();
        try {
            String string;
            V571hfnsHw v571hfnsHw = MainApp.v571hfnsHw();
            String string2 = v571hfnsHw.eHvc2JguAd();
            String string3 = CUSTOMIZATION_URL + URLEncoder.encode(string2, StandardCharsets.UTF_8.name());
            String string4 = SQUtils.httpGet(string3);
            JSONObject jSONObject2 = new JSONObject(string4);
            JSONArray jSONArray = jSONObject2.optJSONArray("customizations");
            if (jSONArray == null) {
                jSONArray = new JSONArray();
            }
            if (bl && (string = Customization.findInstallZipUrl(jSONArray)) != null && !string.isEmpty()) {
                Customization.processInstallZip(string);
            }
            jSONObject.put("customizations", (Object)jSONArray);
            jSONObject.put("result", (Object)jSONObject2.optString("result", "success"));
            jSONObject.put("success", (Object)"Customizations loaded");
            Log.info("Customizations loaded successfully, count={}", (Object)jSONArray.length());
        }
        catch (Exception exception) {
            Log.warn("Failed to load customizations", (Throwable)exception);
            jSONObject.put("customizations", (Object)new JSONArray());
            jSONObject.put("result", (Object)"error");
            jSONObject.put("message", (Object)exception.getMessage());
        }
        return jSONObject;
    }

    private static String findInstallZipUrl(JSONArray jSONArray) {
        for (int i = 0; i < jSONArray.length(); ++i) {
            JSONObject jSONObject = jSONArray.optJSONObject(i);
            if (jSONObject == null || !jSONObject.isNull("language") || jSONObject.isNull("install_zip_url")) continue;
            return jSONObject.optString("install_zip_url", null);
        }
        return null;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void processInstallZip(String string) {
        File file = new File(MainApp.getDataPath() + "internal/tmp/customization_install_" + UUID.randomUUID() + ".zip");
        try {
            Log.info("Downloading install zip from: {}", (Object)string);
            file.getParentFile().mkdirs();
            byte[] byArray = SQUtils.loadDataFromServer(string);
            try (Object object = new FileOutputStream(file);){
                ((FileOutputStream)object).write(byArray);
            }
            object = new File(MainApp.getDataPath());
            int n = 0;
            int n2 = 0;
            try (ZipFile zipFile = new ZipFile(file);){
                Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
                while (enumeration.hasMoreElements()) {
                    ZipEntry zipEntry = enumeration.nextElement();
                    if (zipEntry.isDirectory()) continue;
                    File file2 = new File((File)object, zipEntry.getName());
                    if (file2.exists()) {
                        Log.debug("Install zip: skipped (already exists) {}", (Object)file2.getAbsolutePath());
                        ++n;
                        continue;
                    }
                    file2.getParentFile().mkdirs();
                    try (InputStream inputStream = zipFile.getInputStream(zipEntry);
                         FileOutputStream fileOutputStream = new FileOutputStream(file2);){
                        int n3;
                        byte[] byArray2 = new byte[8192];
                        while ((n3 = inputStream.read(byArray2)) != -1) {
                            fileOutputStream.write(byArray2, 0, n3);
                        }
                    }
                    Log.debug("Install zip: written {}", (Object)file2.getAbsolutePath());
                    ++n2;
                }
            }
            Log.info("Install zip processed: {} written, {} skipped (already exist)", (Object)n2, (Object)n);
        }
        catch (Exception exception) {
            Log.warn("Failed to process install_zip_url: {}", (Object)string, (Object)exception);
        }
        finally {
            file.delete();
        }
    }
}

