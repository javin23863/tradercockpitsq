/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.app.webserver;

import com.strategyquant.lib.L;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractTextServlet
extends HttpServlet {
    private static final Logger Log = LoggerFactory.getLogger(AbstractTextServlet.class);

    protected void doPost(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        this.doGet(httpServletRequest, httpServletResponse);
    }

    protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
    }

    protected String execute(String string, String string2, Map<String, String[]> map, String string3) throws Exception {
        return this.execute(string, string2, map, string3, null);
    }

    protected String execute(String string, String string2, Map<String, String[]> map, String string3, String string4) throws Exception {
        return null;
    }

    protected String execute(String string, Map<String, String[]> map, String string2) throws Exception {
        return null;
    }

    protected byte[] executeBinary(String string, Map<String, String[]> map, String string2) throws Exception {
        return null;
    }

    protected void dumpParams(Map<String, String[]> map) {
        for (String string : map.keySet()) {
            String[] stringArray = map.get(string);
            String string2 = "";
            for (String string3 : stringArray) {
                string2 = string2 + string3 + " ";
            }
            Log.debug(string + ": " + string2);
        }
    }

    public void getRequestParams(HttpServletRequest httpServletRequest, Map<String, String[]> map) {
        if (httpServletRequest.getMethod().equals("GET")) {
            map.putAll(httpServletRequest.getParameterMap());
        }
        if (this.tryLoadFilePart(httpServletRequest, map)) {
            return;
        }
        try {
            String[] stringArray;
            ServletInputStream servletInputStream = httpServletRequest.getInputStream();
            byte[] byArray = IOUtils.toByteArray((InputStream)servletInputStream);
            String string = "";
            try {
                stringArray = new GZIPInputStream(new ByteArrayInputStream(byArray));
                string = IOUtils.toString((InputStream)stringArray, (String)"UTF-8");
            }
            catch (Exception exception) {
                string = IOUtils.toString((InputStream)new ByteArrayInputStream(byArray), (String)"UTF-8");
            }
            for (String string2 : stringArray = string.split("&")) {
                String[] stringArray2 = string2.split("=");
                if (stringArray2 == null || stringArray2.length != 2) continue;
                stringArray2[0] = URLDecoder.decode(stringArray2[0], "UTF-8");
                stringArray2[1] = URLDecoder.decode(stringArray2[1], "UTF-8");
                if (map.containsKey(stringArray2[0])) {
                    String[] stringArray3 = map.get(stringArray2[0]);
                    stringArray3 = Arrays.copyOf(stringArray3, stringArray3.length + 1);
                    stringArray3[stringArray3.length - 1] = stringArray2[1];
                    map.put(stringArray2[0], stringArray3);
                    continue;
                }
                map.put(stringArray2[0], new String[]{stringArray2[1]});
            }
        }
        catch (IOException iOException) {
            Log.error("Cannot get request body");
        }
    }

    protected String getParam(Map<String, String[]> map, String string, String string2) {
        if (!map.containsKey(string)) {
            return string2;
        }
        return map.get(string)[0];
    }

    protected String[] getParam(Map<String, String[]> map, String string) {
        if (!map.containsKey(string)) {
            return null;
        }
        return map.get(string);
    }

    protected void checkParamExists(Map<String, String[]> map, String[] stringArray) throws Exception {
        for (String string : stringArray) {
            if (this.getParam(map, string) != null) continue;
            throw new Exception(L.t("Parameter '%s' is missing.", string));
        }
    }

    protected String[] tryGetParam(Map<String, String[]> map, String string) throws Exception {
        if (!map.containsKey(string)) {
            throw new Exception(L.t("Parameter '%s' is missing.", string));
        }
        return map.get(string);
    }

    protected String tryGetParamValue(Map<String, String[]> map, String string) throws Exception {
        if (!map.containsKey(string)) {
            throw new Exception(L.t("Parameter '%s' is missing.", string));
        }
        return map.get(string)[0];
    }

    protected JSONObject tryGetJsonObject(Map<String, String[]> map, String string) throws Exception {
        JSONObject jSONObject = new JSONObject();
        String string2 = string + "[";
        for (String string3 : map.keySet()) {
            if (!string3.startsWith(string2)) continue;
            int n = string3.indexOf("]");
            String string4 = string3.substring(string2.length(), n);
            jSONObject.put(string4, (Object)map.get(string3)[0]);
        }
        return jSONObject;
    }

    protected JSONArray tryGetJsonArray(Map<String, String[]> map, String string) throws Exception {
        JSONArray jSONArray = new JSONArray();
        String string2 = string + "[";
        for (String string3 : map.keySet()) {
            if (!string3.startsWith(string2)) continue;
            int n = string3.indexOf("]");
            int n2 = Integer.parseInt(string3.substring(string2.length(), n));
            String string4 = string3.substring(n + 2, string3.indexOf("]", n + 1));
            while (n2 > jSONArray.length() - 1) {
                jSONArray.put((Object)new JSONObject());
            }
            JSONObject jSONObject = jSONArray.getJSONObject(n2);
            jSONObject.put(string4, (Object)map.get(string3)[0]);
        }
        return jSONArray;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    protected boolean tryLoadFilePart(HttpServletRequest httpServletRequest, Map<String, String[]> map) {
        InputStream inputStream = null;
        String string = httpServletRequest.getContentType();
        try {
            if (string == null || !string.startsWith("multipart/form-data")) {
                boolean bl = false;
                return bl;
            }
            Part part = httpServletRequest.getPart("file");
            if (part == null) {
                boolean bl = false;
                return bl;
            }
            inputStream = part.getInputStream();
            byte[] byArray = inputStream.readAllBytes();
            byte[] byArray2 = Base64.getEncoder().encode(byArray);
            map.put("fileName", new String[]{this.getFileName(part)});
            map.put("file", new String[]{new String(byArray2)});
            boolean bl = true;
            return bl;
        }
        catch (Exception exception) {
            Log.error("Failed to read uploaded file", (Throwable)exception);
            boolean bl = false;
            return bl;
        }
        finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                }
                catch (Exception exception) {}
            }
        }
    }

    private String getFileName(Part part) {
        String string = part.getHeader("content-disposition");
        for (String string2 : part.getHeader("content-disposition").split(";")) {
            if (!string2.trim().startsWith("filename")) continue;
            return string2.substring(string2.indexOf(61) + 1).trim().replace("\"", "");
        }
        return null;
    }
}

