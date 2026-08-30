/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.qdm;

import com.google.common.io.Files;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.constants.SQPaths;
import com.strategyquant.lib.utils.Unzipper;
import com.strategyquant.qdm.QDMDb;
import com.strategyquant.qdm.activity.QDMActivity;
import com.strategyquant.qdm.banners.Banners;
import com.strategyquant.qdm.symbols.SymbolsStats;
import java.io.File;
import java.util.ArrayList;
import javax.net.ssl.SSLContext;
import org.apache.http.HttpEntity;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QDM {
    public static final Logger Log = LoggerFactory.getLogger(QDM.class);
    private static QDM instance;
    private long startTime;
    private QDMDb db = new QDMDb();
    public Banners banners = new Banners(this.db);
    public SymbolsStats symbols = new SymbolsStats(this.db);
    public QDMActivity activity = new QDMActivity(this.db);

    private QDM() {
    }

    public static QDM getInstance() {
        if (instance == null) {
            instance = new QDM();
        }
        return instance;
    }

    public void start() {
        long l;
        this.startTime = System.currentTimeMillis();
        if (!MainApp.runInConsole()) {
            this.donwloadZipUpdate();
            this.banners.loadAvailable();
        }
        if ((l = this.loadDailyStats()) != -1L && l != SQTime.getDateInMs(System.currentTimeMillis())) {
            this.sendDailyStats();
            this.resetDailyStats();
        }
        this.activity.increase(MainApp.runInConsole() ? 7 : 0);
    }

    public void end() {
        int n = (int)((System.currentTimeMillis() - this.startTime) / 1000L);
        this.activity.increase(6, n);
        this.saveDailyStats();
    }

    private void donwloadZipUpdate() {
        try {
            byte[] byArray = SQUtils.loadDataFromServer("https://api.strategyquant.com/qdm/update.zip");
            File file = new File(MainApp.getDataPath() + "/internal/web/QDM/data");
            SQUtils.deleteDirectory(file.getAbsolutePath());
            Unzipper.unzip(byArray, file);
            file = new File(MainApp.getDataPath() + "/internal/web/QDM/data/dukascopy.csv");
            if (file.exists()) {
                Files.copy((File)file, (File)new File(SQPaths.pluginsDirPath + "/DataSourceDukascopy/dukascopy.csv"));
            }
        }
        catch (Exception exception) {
            Log.error("Cannot download zip update. Exc.", (Throwable)exception);
        }
    }

    public long loadDailyStats() {
        this.banners.loadStats();
        this.symbols.loadStats();
        this.activity.loadStats();
        return this.activity.getDateOfLastStats();
    }

    public void saveDailyStats() {
        this.banners.saveStats();
        this.symbols.saveStats();
        this.activity.saveStats();
    }

    private void resetDailyStats() {
        this.banners.resetStats();
        this.symbols.resetStats();
        this.activity.resetStats();
    }

    private void sendDailyStats() {
        try {
            SSLContext sSLContext = new SSLContextBuilder().loadTrustMaterial(null, (x509CertificateArray, string) -> true).build();
            CloseableHttpClient closeableHttpClient = HttpClients.custom().setSslcontext(sSLContext).setHostnameVerifier((X509HostnameVerifier)new AllowAllHostnameVerifier()).build();
            HttpPost httpPost = new HttpPost("https://api.strategyquant.com/recheck/qdm");
            ArrayList<BasicNameValuePair> arrayList = new ArrayList<BasicNameValuePair>();
            arrayList.add(new BasicNameValuePair("hw", MainApp.v571hfnsHw().nmFllxIfvN()));
            arrayList.add(new BasicNameValuePair("t", this.activity.getDateOfLastStats() / 1000L + ""));
            arrayList.add(new BasicNameValuePair("b", this.banners.toString()));
            arrayList.add(new BasicNameValuePair("s", this.symbols.toString()));
            arrayList.add(new BasicNameValuePair("a", this.activity.toString()));
            httpPost.setEntity((HttpEntity)new UrlEncodedFormEntity(arrayList));
            CloseableHttpResponse closeableHttpResponse = closeableHttpClient.execute((HttpUriRequest)httpPost);
            if (closeableHttpResponse.getStatusLine().getStatusCode() != 200) {
                String string2 = ", Response: " + SQUtils.inputStreamToString(closeableHttpResponse.getEntity().getContent());
                throw new Exception("HTML status code - " + closeableHttpResponse.getStatusLine().getStatusCode() + string2);
            }
        }
        catch (Exception exception) {
            Log.error("Cannot send dat. Exc: ", (Throwable)exception);
        }
        catch (Error error) {
            Log.error("Cannot send dat. Exc: ", (Throwable)error);
        }
    }
}

