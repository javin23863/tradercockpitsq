package com.strategyquant.webguilib.websocket;

import com.strategyquant.tradinglib.project.websocket.SQWebSocket;
import jakarta.servlet.annotation.WebServlet;
import java.time.Duration;
import org.eclipse.jetty.websocket.server.JettyWebSocketServlet;
import org.eclipse.jetty.websocket.server.JettyWebSocketServletFactory;

@WebServlet(name = "WebSocket Servlet", urlPatterns = "/update")
public class SQWebSocketServlet extends JettyWebSocketServlet {
   public void configure(JettyWebSocketServletFactory var1) {
      var1.setIdleTimeout(Duration.ofMillis(Long.MAX_VALUE));
      var1.register(SQWebSocket.class);
   }
}
