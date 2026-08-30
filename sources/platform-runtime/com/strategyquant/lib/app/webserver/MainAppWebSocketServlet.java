/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.app.webserver;

import com.strategyquant.lib.app.webserver.MainAppWebSocket;
import jakarta.servlet.annotation.WebServlet;
import java.time.Duration;
import org.eclipse.jetty.websocket.server.JettyWebSocketServlet;
import org.eclipse.jetty.websocket.server.JettyWebSocketServletFactory;

@WebServlet(name="MainAppWebSocket Servlet", urlPatterns={"/app"})
public class MainAppWebSocketServlet
extends JettyWebSocketServlet {
    protected void configure(JettyWebSocketServletFactory jettyWebSocketServletFactory) {
        jettyWebSocketServletFactory.setIdleTimeout(Duration.ofMillis(Long.MAX_VALUE));
        jettyWebSocketServletFactory.register(MainAppWebSocket.class);
    }
}

