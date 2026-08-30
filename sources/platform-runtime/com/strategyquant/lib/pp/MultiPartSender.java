/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.pp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

public class MultiPartSender {
    private final String boundary;
    private static final String LINE_FEED = "\r\n";
    private HttpURLConnection httpConn;
    private String charset;
    private OutputStream outputStream;
    private PrintWriter writer;

    public MultiPartSender(String string, String string2) throws IOException {
        this.charset = string2;
        this.boundary = "===" + System.currentTimeMillis() + "===";
        URL uRL = new URL(string);
        this.httpConn = (HttpURLConnection)uRL.openConnection();
        this.httpConn.setUseCaches(false);
        this.httpConn.setDoOutput(true);
        this.httpConn.setDoInput(true);
        this.httpConn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + this.boundary);
        this.httpConn.setRequestProperty("User-Agent", "CodeJava Agent");
        this.httpConn.setRequestProperty("Test", "Bonjour");
        this.outputStream = this.httpConn.getOutputStream();
        this.writer = new PrintWriter((Writer)new OutputStreamWriter(this.outputStream, string2), true);
    }

    public void addFormField(String string, String string2) {
        this.writer.append("--" + this.boundary).append(LINE_FEED);
        this.writer.append("Content-Disposition: form-data; name=\"" + string + "\"").append(LINE_FEED);
        this.writer.append("Content-Type: text/plain; charset=" + this.charset).append(LINE_FEED);
        this.writer.append(LINE_FEED);
        this.writer.append(string2).append(LINE_FEED);
        this.writer.flush();
    }

    public void addFilePart(String string, String string2, byte[] byArray) throws IOException {
        this.writer.append("--" + this.boundary).append(LINE_FEED);
        this.writer.append("Content-Disposition: form-data; name=\"" + string + "\"; filename=\"" + string2 + "\"").append(LINE_FEED);
        this.writer.append("Content-Type: " + URLConnection.guessContentTypeFromName(string2)).append(LINE_FEED);
        this.writer.append("Content-Transfer-Encoding: binary").append(LINE_FEED);
        this.writer.append(LINE_FEED);
        this.writer.flush();
        this.outputStream.write(byArray);
        this.outputStream.flush();
        this.writer.append(LINE_FEED);
        this.writer.flush();
    }

    public void addHeaderField(String string, String string2) {
        this.writer.append(string + ": " + string2).append(LINE_FEED);
        this.writer.flush();
    }

    public List<String> finish() throws IOException {
        BufferedReader bufferedReader;
        ArrayList<String> arrayList = new ArrayList<String>();
        this.writer.append(LINE_FEED).flush();
        this.writer.append("--" + this.boundary + "--").append(LINE_FEED);
        this.writer.close();
        int n = this.httpConn.getResponseCode();
        if (n == 200) {
            bufferedReader = new BufferedReader(new InputStreamReader(this.httpConn.getInputStream()));
            String string = null;
            while ((string = bufferedReader.readLine()) != null) {
                arrayList.add(string);
            }
        } else {
            throw new IOException("Server returned non-OK status: " + n);
        }
        bufferedReader.close();
        this.httpConn.disconnect();
        return arrayList;
    }
}

