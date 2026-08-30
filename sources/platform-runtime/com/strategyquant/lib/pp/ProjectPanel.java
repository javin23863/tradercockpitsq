/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.pp;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.pp.MultiPartSender;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.jdom2.Element;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectPanel {
    public static final Logger Log = LoggerFactory.getLogger(ProjectPanel.class);
    public static final String PROJECT_REPORTED_BUGS = "22940eb4";
    public static final int TYPE_BUG = 5;
    public static final int STATUS_NEW = 6;
    public static final int PRIORITY_NORMAL = 15;
    public String token = null;
    private static AtomicInteger lastExcHash = new AtomicInteger();

    public void login(String string, String string2) throws Exception {
        String string3 = SQUtils.httpGet("https://roadmap.strategyquant.com/api/login?u=" + string + "&p=" + string2);
        JSONObject jSONObject = new JSONObject(string3);
        this.token = jSONObject.getString("token");
        Log.info("Login success.");
    }

    public void createTask(String string, String string2, String[] stringArray, Map<String, String> map, String string3, int n, int n2, int n3) throws Exception {
        String string42;
        if (this.token == null) {
            throw new Exception("Authorization token is null. Call login().");
        }
        String string5 = "id_project=" + string3 + "&type=" + n + "&status=" + n2 + "&subject=" + string + "&description=" + string2 + "&priority=" + n3;
        int n4 = 0;
        if (stringArray != null) {
            for (int i = 0; i < stringArray.length; ++i) {
                string42 = stringArray[i];
                if (string42 == null) continue;
                string5 = string5 + "&uploadFilesId[" + n4++ + "]=" + string42;
            }
        }
        if (map != null) {
            for (String string42 : map.keySet()) {
                string5 = string5 + "&" + string42 + "=" + map.get(string42);
            }
        }
        HashMap<String, String> hashMap = new HashMap<String, String>();
        hashMap.put("TS-AUTH-TOKEN", this.token);
        string42 = SQUtils.httpPost("https://roadmap.strategyquant.com/api/tasks/create", string5, hashMap);
        JSONObject jSONObject = new JSONObject(string42);
        Log.info("pp response: " + jSONObject.toString());
        Log.info("Task created.");
    }

    public String[] uploadFiles(Element element, File file) {
        String string = null;
        String string2 = null;
        try {
            string = this.uploadFile("taskConfig.xml", XMLUtil.elementToString(element).getBytes());
        }
        catch (Exception exception) {
            // empty catch block
        }
        try {
            if (file != null) {
                string2 = this.uploadFile(file.getName(), SQUtils.fileToBytes(file));
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        if (string2 == null) {
            return new String[]{string};
        }
        return new String[]{string, string2};
    }

    private String uploadFile(String string, byte[] byArray) {
        try {
            MultiPartSender multiPartSender = new MultiPartSender("http://roadmap.strategyquant.com/api/files/upload?id_task=", "UTF-8");
            multiPartSender.addHeaderField("TS-AUTH-TOKEN", this.token);
            multiPartSender.addFilePart("uploadFiles", string, byArray);
            List<String> list = multiPartSender.finish();
            JSONObject jSONObject = new JSONObject(list.get(0));
            JSONObject jSONObject2 = jSONObject.getJSONObject("data");
            try {
                JSONObject jSONObject3 = jSONObject2.getJSONObject("id_file");
                return (String)jSONObject3.keys().next();
            }
            catch (Exception exception) {
                JSONArray jSONArray = jSONObject2.getJSONArray("errorMsg");
                throw new Exception("Cannot read file if from server response. Error: " + jSONArray.toString());
            }
        }
        catch (Exception exception) {
            Log.info("Error while uploading '" + string + "'. Reason: " + exception.getMessage());
            return null;
        }
    }

    public static void reportBug(Element element, File file, String string, String string2) {
    }
}

