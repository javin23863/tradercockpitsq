package com.strategyquant.webguilib.server;

import com.strategyquant.pluginlib.SQPluginManager;
import com.strategyquant.tradinglib.backtest.IBacktester;
import com.strategyquant.tradinglib.plugindef.app.IAppPlugin;
import com.strategyquant.tradinglib.results.AbstractBacktestPlugin;
import com.strategyquant.tradinglib.results.IResultPlugin;
import com.strategyquant.tradinglib.results.IResultsGroupProvider;
import com.strategyquant.tradinglib.servlet.IServletPlugin;
import com.strategyquant.webguilib.WebServer;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.server.JettyWebSocketServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JettyServer extends AbstractUIWebServer {
   public static final Logger Log = LoggerFactory.getLogger(JettyServer.class);
   private static IResultsGroupProvider rgProvider;
   private static IBacktester backtester;
   private static JettyWebSocketServlet webSocketServlet;

   public JettyServer(WebServer var1, IResultsGroupProvider var2, IBacktester var3, JettyWebSocketServlet var4) {
      super(var1);
      rgProvider = var2;
      backtester = var3;
      webSocketServlet = var4;
   }

   @Override
   protected void loadCustomHandlers(List<Handler> var1) {
      ServletContextHandler var2 = new ServletContextHandler(1);
      var2.setContextPath("/websocket");
      var2.addServlet(new ServletHolder(webSocketServlet), "/updates");
      var1.add(this.getGzipHandler(var2));
      ArrayList var3 = SQPluginManager.getPlugins(IServletPlugin.class);
      ArrayList var4 = SQPluginManager.getPlugins(IAppPlugin.class);

      for (int var5 = 0; var5 < var3.size(); var5++) {
         try {
            IServletPlugin var6 = (IServletPlugin)var3.get(var5);
            if (var6 instanceof IResultPlugin) {
               ((IResultPlugin)var6).setResultsGroupProvider(rgProvider);
            }

            if (var6 instanceof AbstractBacktestPlugin) {
               ((AbstractBacktestPlugin)var6).setBacktester(backtester);
               ((AbstractBacktestPlugin)var6).setResultsGroupProvider(rgProvider);
            }

            Handler var7 = var6.getHandler();
            if (var7 != null) {
               var1.add(this.getGzipHandler(var7));
            }
         } catch (Exception var11) {
            Log.error("Cannot load ServletPlugin '" + ((IServletPlugin)var3.get(var5)).getClass().getSimpleName() + "'", var11);
         } catch (Error var12) {
            Log.error("Cannot load ServletPlugin '" + ((IServletPlugin)var3.get(var5)).getClass().getSimpleName() + "'", var12);
         }
      }

      for (IAppPlugin var14 : var4) {
         try {
            ResourceHandler var15 = new ResourceHandler();
            var15.setWelcomeFiles(new String[]{var14.getAppCode() + "/index.html"});
            var15.setResourceBase(this.webServer.getWebPath());
            ContextHandler var8 = new ContextHandler(var14.getContextPath());
            var8.setHandler(var15);
            var1.add(this.getGzipHandler(var8));
         } catch (Exception var9) {
            Log.error("Cannot load AppPlugin '" + var14.getClass().getSimpleName() + "'", var9);
         } catch (Error var10) {
            Log.error("Cannot load AppPlugin '" + var14.getClass().getSimpleName() + "'", var10);
         }
      }
   }

   @Override
   protected void serverStarted(int var1) {
      String var2 = (useHttps ? "https" : "http") + "://localhost";
      backtester.setServerInfo(var2, var1);
   }
}
