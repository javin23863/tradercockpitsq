/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.volumeProfile;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.volumeProfile.VolumeProfileBlocks;
import java.util.ArrayList;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VolumeProfileSubscription {
    public static final Logger Log = LoggerFactory.getLogger(VolumeProfileSubscription.class);
    private static VolumeProfileSubscription instance;
    private boolean active = false;

    private VolumeProfileSubscription() {
        this.update();
    }

    public static VolumeProfileSubscription getInstance() {
        if (instance == null) {
            instance = new VolumeProfileSubscription();
        }
        return instance;
    }

    private void verify() throws Exception {
        ArrayList<NameValuePair> arrayList = new ArrayList<NameValuePair>();
        arrayList.add((NameValuePair)new BasicNameValuePair("license", MainApp.v571hfnsHw().eHvc2JguAd()));
        arrayList.add((NameValuePair)new BasicNameValuePair("h", MainApp.v571hfnsHw().nmFllxIfvN()));
        String string = SQUtils.httpsPost("https://api.strategyquant.com/volumeprofilesubscription/verify", arrayList, false);
        JSONObject jSONObject = null;
        try {
            jSONObject = new JSONObject(string);
        }
        catch (Exception exception) {
            Log.error("Error while parsing volume profile subscription's response: '{}'", (Object)string);
            throw exception;
        }
        if (jSONObject.getString("result").equals("error")) {
            Log.error("Error while checking volume profile subscription: " + jSONObject.get("errors").toString());
            throw new Exception(jSONObject.get("errors").toString());
        }
        this.active = jSONObject.getBoolean("active");
        Log.info("Volume profile subscription verified: active={}", (Object)this.active);
    }

    public boolean isActive() {
        return this.active;
    }

    public static JSONObject toJSON() {
        JSONObject jSONObject = new JSONObject();
        jSONObject.put("active", VolumeProfileSubscription.getInstance().isActive());
        jSONObject.put("blocks", (Object)new JSONArray(new ArrayList<String>(VolumeProfileBlocks.getBlocks())));
        return jSONObject;
    }

    public void update() {
        try {
            this.verify();
        }
        catch (Exception exception) {
            Log.error("Verifying volume profile subscription failed. Exc.", (Throwable)exception);
        }
    }
}

