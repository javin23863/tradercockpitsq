/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.app.webserver;

import com.strategyquant.lib.L;
import com.strategyquant.lib.app.MainApp;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebSocket(maxTextMessageSize=0x100000)
public class MainAppWebSocket {
    private static final Logger Log = LoggerFactory.getLogger(MainAppWebSocket.class);
    private Session session;
    private int confirmedAction = -1;

    public MainAppWebSocket() {
        Log.info("MainAppWebSocket initialized");
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        String string = session.getRemoteAddress().toString();
        Log.info("Incoming connection from {}, protocol version: {}", (Object)string, (Object)session.getProtocolVersion());
        this.session = session;
        MainApp.electronWS = this;
    }

    @OnWebSocketMessage
    public void onMessage(String string) {
        Log.info("Got message: '" + string + "'");
        this.handleMessage(new JSONObject(string));
    }

    private void handleMessage(JSONObject jSONObject) {
        String string = jSONObject.getString("action");
        if (string == null) {
            return;
        }
        switch (string) {
            case "confirmDialogResponse": {
                this.confirmDialogResponse(jSONObject);
                break;
            }
            case "exitApp": {
                this.exitApp(jSONObject);
            }
        }
    }

    @OnWebSocketError
    public void onError(Session session, Throwable throwable) {
        Log.debug("Websocket error", throwable);
    }

    @OnWebSocketClose
    public void onClose(int n, String string) {
        Log.debug("Connection closed - status code: " + n + ", reason: " + string);
    }

    public void sendMessage(JSONObject jSONObject) {
        try {
            if (this.session == null || !this.session.isOpen()) {
                return;
            }
            this.session.getRemote().sendString(jSONObject.toString());
        }
        catch (Exception exception) {
            Log.error("Websocket sendMessage error - " + exception.getMessage(), (Throwable)exception);
        }
    }

    public void sendMessage(String string, String string2) {
        try {
            String string3 = new JSONObject().put(string, (Object)string2).toString();
            if (this.session == null || !this.session.isOpen()) {
                return;
            }
            this.session.getRemote().sendString(string3);
        }
        catch (Exception exception) {
            Log.error("Websocket sendMessage error - " + exception.getMessage(), (Throwable)exception);
        }
    }

    public void sendMessage(String string, JSONObject jSONObject) {
        try {
            String string2 = new JSONObject().put(string, (Object)jSONObject).toString();
            if (this.session == null || !this.session.isOpen()) {
                return;
            }
            this.session.getRemote().sendString(string2);
        }
        catch (Exception exception) {
            Log.error("Websocket sendMessage error - " + exception.getMessage(), (Throwable)exception);
        }
    }

    public int awaitConfirmation(String string, String string2) {
        Log.info("Show confirmation dialog: " + string);
        if (MainApp.electronWS == null) {
            Log.error("Failed to show confirmation dialog, browser is not ready.");
            return -1;
        }
        this.confirmedAction = -1;
        MainApp.sendMessage("confirmDialogShow", new JSONObject().put("id", System.currentTimeMillis()).put("message", (Object)string2).put("title", (Object)string));
        Thread thread = new Thread(){

            @Override
            public void run() {
                while (MainAppWebSocket.this.confirmedAction == -1) {
                    try {
                        Thread.sleep(500L);
                    }
                    catch (Exception exception) {}
                }
            }
        };
        thread.start();
        try {
            thread.join();
        }
        catch (InterruptedException interruptedException) {
            Log.error("Waiting for confirmation interrupted.", (Throwable)interruptedException);
        }
        return this.confirmedAction;
    }

    private void confirmDialogResponse(JSONObject jSONObject) {
        this.confirmedAction = jSONObject.getInt("response");
    }

    private void exitApp(JSONObject jSONObject) {
        new Thread(){

            @Override
            public void run() {
                int n = MainAppWebSocket.this.awaitConfirmation(L.t("User Confirmation", new Object[0]), L.t("Are you sure you want to exit?", new Object[0]));
                if (n == 1) {
                    MainApp.exitApp();
                }
            }
        }.start();
    }

    public void showErrorDialog(String string, String string2) {
        this.showDialog("error", string, string2);
    }

    public void showInfoDialog(String string, String string2) {
        this.showDialog("info", string, string2);
    }

    public void showWarningDialog(String string, String string2) {
        this.showDialog("warning", string, string2);
    }

    public void showDialog(String string, String string2, String string3) {
        Log.info("Show dialog...\n" + string + "\n" + string2 + "\n" + string3);
        if (MainApp.electronWS == null) {
            Log.error("Cannot show dialog, browser is not ready.");
            return;
        }
        MainApp.sendMessage("showDialog", new JSONObject().put("id", System.currentTimeMillis()).put("type", (Object)string).put("message", (Object)string3).put("title", (Object)string2));
    }
}

