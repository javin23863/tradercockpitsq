/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.app.webserver;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.app.impl.MainAppSettings;
import com.strategyquant.lib.app.webserver.MainAppHttpHandler;
import com.strategyquant.lib.app.webserver.MainAppWebSocketServlet;
import jakarta.servlet.Servlet;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainAppWebServer {
    public static final Logger Log = LoggerFactory.getLogger(MainAppWebServer.class);
    private final int portFrom = 5050;
    private final int portTo = 5060;
    public static int port = -1;
    private String productCode;
    private String PortSettingKey;

    public MainAppWebServer(String string) {
        this.productCode = string;
        this.PortSettingKey = "AppWebServerPort" + this.productCode;
    }

    public Server createServer() throws Exception {
        Server server = null;
        try {
            port = Integer.parseInt(MainAppSettings.getInstance().get(this.PortSettingKey, "-1"));
        }
        catch (Exception exception) {
            Log.debug("No valid websocket port specified in settings");
        }
        if (port > 0) {
            if (this.isAppAlreadyRunning(port)) {
                throw new Exception(String.format("The app is already running on port %d", port));
            }
            try {
                server = this.startServer(port);
            }
            catch (Exception exception) {
                Log.debug("Cannot start webserver on port " + port + ". Trying next port...", (Throwable)exception);
            }
        }
        if (server == null) {
            for (int i = 5050; i <= 5060; ++i) {
                if (this.isAppAlreadyRunning(i)) {
                    throw new Exception(String.format("The app is already running on port %d", i));
                }
                try {
                    server = this.startServer(i);
                    port = i;
                    break;
                }
                catch (Exception exception) {
                    Log.debug("Cannot start webserver on port " + i + ". Trying next port...", (Throwable)exception);
                    continue;
                }
            }
        }
        if (server == null) {
            throw new Exception(String.format("Cannot start webserver on port %d - %d.", 5050, 5060));
        }
        MainAppSettings.getInstance().set(this.PortSettingKey, port + "");
        Log.debug(String.format("MainAppWebServer port: %d", port));
        return server;
    }

    private boolean isAppAlreadyRunning(int n) {
        try {
            Log.debug(String.format("Checking if the app is already running on port %d ...", n));
            String string = SQUtils.httpGet("http://localhost:" + n + "/status", 500);
            JSONObject jSONObject = new JSONObject(string);
            String string2 = jSONObject.getString("product");
            String string3 = jSONObject.getString("path");
            if (MainApp.getDataPath().equals(string3) && this.productCode.equals(string2)) {
                return true;
            }
        }
        catch (Error | Exception throwable) {
            // empty catch block
        }
        return false;
    }

    private Server startServer(int n) throws Exception {
        Server server = null;
        try {
            Log.debug(String.format("Running server on port %d ...", n));
            server = new Server();
            ServerConnector serverConnector = new ServerConnector(server);
            serverConnector.setPort(n);
            server.setConnectors(new Connector[]{serverConnector});
            HandlerList handlerList = this.loadHandlers(server);
            server.setHandler((Handler)handlerList);
            this.configureWebSocket(handlerList);
            server.start();
            Log.info("Server started on port " + n);
            return server;
        }
        catch (Exception exception) {
            throw new Exception(String.format("Cannot start webserver on port %d.", n));
        }
    }

    private void configureWebSocket(HandlerList handlerList) {
        GzipHandler gzipHandler = null;
        Handler[] handlerArray = handlerList.getHandlers();
        int n = handlerArray.length;
        for (int i = 0; i < n; ++i) {
            GzipHandler gzipHandler2;
            Handler handler;
            Handler handler2 = handler = handlerArray[i];
            if (handler instanceof GzipHandler) {
                gzipHandler2 = (GzipHandler)handler;
                handler2 = gzipHandler2.getHandler();
            }
            if (!(handler2 instanceof ServletContextHandler) || !"/websocket".equals((gzipHandler2 = (ServletContextHandler)handler2).getContextPath())) continue;
            gzipHandler = gzipHandler2;
            break;
        }
        if (gzipHandler != null) {
            JettyWebSocketServletContainerInitializer.configure(gzipHandler, null);
        }
    }

    private HandlerList loadHandlers(Server server) {
        ServletContextHandler servletContextHandler = new ServletContextHandler(1);
        servletContextHandler.setContextPath("/websocket");
        servletContextHandler.addServlet(new ServletHolder((Servlet)new MainAppWebSocketServlet()), "/app");
        ServletContextHandler servletContextHandler2 = new ServletContextHandler(1);
        servletContextHandler2.setContextPath("/");
        servletContextHandler2.addServlet(new ServletHolder((Servlet)new MainAppHttpHandler()), "/*");
        HandlerList handlerList = new HandlerList();
        handlerList.addHandler((Handler)servletContextHandler);
        handlerList.addHandler((Handler)servletContextHandler2);
        return handlerList;
    }
}

