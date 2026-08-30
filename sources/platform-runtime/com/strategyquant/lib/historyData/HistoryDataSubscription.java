/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.historyData;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.app.MainApp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HistoryDataSubscription {
    public static final Logger Log = LoggerFactory.getLogger(HistoryDataSubscription.class);
    private List<String> freeFutures = new LinkedList<String>(Arrays.asList("E6", "QR", "EW"));
    private List<String> freeEquities = new LinkedList<String>(Arrays.asList("AAPL", "SPY_benchmark"));
    private static HistoryDataSubscription instance;
    private boolean equitiesEOD = false;
    private boolean equitiesMinute = false;
    private boolean futuresEOD = false;
    private boolean futuresMinute = false;

    private HistoryDataSubscription() {
        this.update();
    }

    public static HistoryDataSubscription getInstance() {
        if (instance == null) {
            instance = new HistoryDataSubscription();
        }
        return instance;
    }

    private void verify() throws Exception {
        String[] stringArray;
        ArrayList<NameValuePair> arrayList = new ArrayList<NameValuePair>();
        arrayList.add((NameValuePair)new BasicNameValuePair("license", MainApp.v571hfnsHw().eHvc2JguAd()));
        arrayList.add((NameValuePair)new BasicNameValuePair("h", MainApp.v571hfnsHw().nmFllxIfvN()));
        String string = SQUtils.httpsPost("https://api.strategyquant.com/datasubscription/verify", arrayList, false);
        JSONObject jSONObject = null;
        try {
            jSONObject = new JSONObject(string);
        }
        catch (Exception exception) {
            Log.error("Error while parsing subscription's response: '{}'", (Object)string);
            throw exception;
        }
        if (jSONObject.getString("result").equals("error")) {
            Log.error("Error while checking subscriptions: " + jSONObject.get("errors").toString());
            throw new Exception(jSONObject.get("errors").toString());
        }
        this.equitiesEOD = jSONObject.getBoolean("equities_eod");
        this.equitiesMinute = jSONObject.getBoolean("equities_minute");
        this.futuresEOD = jSONObject.getBoolean("futures_eod");
        this.futuresMinute = jSONObject.getBoolean("futures_minute");
        if (jSONObject.has("futures")) {
            for (String string2 : stringArray = jSONObject.getString("futures").split(",")) {
                if (this.freeFutures.contains(string2)) continue;
                this.freeFutures.add(string2);
            }
        }
        if (jSONObject.has("equities")) {
            for (String string2 : stringArray = jSONObject.getString("equities").split(",")) {
                if (this.freeEquities.contains(string2)) continue;
                this.freeEquities.add(string2);
            }
        }
        Log.info("Data subscription verified: equities(eod:{}, min:{}), futures(eod:{}, min:{})", new Object[]{this.equitiesEOD, this.equitiesMinute, this.futuresEOD, this.futuresMinute});
    }

    public boolean isEquitiesEODActive() {
        return this.equitiesEOD;
    }

    public boolean isEquitiesMinuteActive() {
        return this.equitiesMinute;
    }

    public boolean isFuturesEODActive() {
        return this.futuresEOD;
    }

    public boolean isFuturesMinuteActive() {
        return this.futuresMinute;
    }

    public boolean isAllowedEquity(String string, boolean bl) {
        for (String string2 : this.freeEquities) {
            if (bl) {
                string2 = string2 + ".D";
            }
            if (!string.equals(string2)) continue;
            return true;
        }
        if (bl) {
            return this.isEquitiesEODActive();
        }
        return this.isEquitiesMinuteActive();
    }

    public boolean isAllowedFuture(String string, String string2, boolean bl) {
        if ("BMF".equals(string2) && MainApp.isBrazilianEdition()) {
            return true;
        }
        for (String string3 : this.freeFutures) {
            if (string.startsWith("@")) {
                string3 = "@" + string3;
            }
            if (!string.startsWith(string3)) continue;
            return true;
        }
        if (bl) {
            return this.isFuturesEODActive();
        }
        return this.isFuturesMinuteActive();
    }

    public boolean isFreeSymbol(String string, boolean bl, boolean bl2) {
        if (bl) {
            for (String string2 : this.freeFutures) {
                if (string.startsWith("@")) {
                    string2 = "@" + string2;
                }
                if (!string.startsWith(string2)) continue;
                return true;
            }
            return false;
        }
        for (String string3 : this.freeEquities) {
            if (bl2) {
                string3 = string3 + ".D";
            }
            if (!string.equals(string3)) continue;
            return true;
        }
        return false;
    }

    public String getFreeEquities() {
        return String.join((CharSequence)", ", this.freeEquities);
    }

    public String getFreeFutures() {
        return String.join((CharSequence)", ", this.freeFutures);
    }

    public void update() {
        try {
            this.verify();
        }
        catch (Exception exception) {
            Log.error("Verifying data subscription failed. Exc.", (Throwable)exception);
        }
    }
}

