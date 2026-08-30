package com.strategyquant.strategyquant;

import com.strategyquant.lib.app.MainApp;
import com.strategyquant.pluginlib.SQPluginManager;
import com.strategyquant.tradinglib.backtest.SQBacktester;
import com.strategyquant.tradinglib.plugindef.app.IAppPlugin;
import com.strategyquant.tradinglib.results.DatabankResultsGroupProvider;
import com.strategyquant.webguilib.BrowserGUI;
import com.strategyquant.webguilib.WebAppManager;
import com.strategyquant.webguilib.WebServer;
import com.strategyquant.webguilib.init.AutoCompiler;
import com.strategyquant.webguilib.server.AbstractUIWebServer;
import com.strategyquant.webguilib.server.JettyServer;
import com.strategyquant.webguilib.websocket.SQWebSocketServlet;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SQWebServer implements WebServer {
   private static final Logger Log = LoggerFactory.getLogger(SQWebServer.class);
   private static int portFrom;
   private static int portTo;
   private static String pluginsPath;
   private static String webPath;
   private WebAppManager webAppManager;

   public void start() throws Exception {
      checkVariables();
      this.webAppManager = new WebAppManager(this);
      if (AutoCompiler.checkFilesChanged(this)) {
         this.regenerateFiles();
      }

      new JettyServer(this, new DatabankResultsGroupProvider(), SQBacktester.get(), new SQWebSocketServlet());
      JettyServer.getInstance().start();
      this.jettyFailover();
   }

   private void jettyFailover() {
      (new Thread() {
         @Override
         public void run() {
            boolean var1 = false;
            StringBuilder var10000 = new StringBuilder().append("http://localhost:");
            JettyServer.getInstance();
            String var2 = var10000.append(AbstractUIWebServer.port).append("/main/alive").toString();
            HttpGet var3 = new HttpGet(var2);
            var3.setHeader("browserToken", JettyServer.getBrowserToken() + "");
            int var5 = 0;

            while (true) {
               try {
                  Thread.sleep(60000L);
               } catch (InterruptedException var9) {
               }

               try {
                  int var6 = -1;

                  try {
                     CloseableHttpClient var7 = HttpClientBuilder.create().build();
                     HttpResponse var4 = var7.execute(var3);
                     var6 = var4.getStatusLine().getStatusCode();
                  } catch (Exception var8) {
                     SQWebServer.Log.error("Cannot get response /main/alive. Exc.", var8);
                  }

                  if (var1 && var6 != 200) {
                     if (++var5 == 5) {
                        SQWebServer.Log.error("Jetty server failed! /main/alive response status: " + var6);
                        SQWebServer.Log.info("Restart Jetty server.");
                        JettyServer.getInstance().restartServer();
                        var5 = 0;
                     }
                  } else {
                     var5 = 0;
                  }

                  if (var6 == 200) {
                     var1 = true;
                  }
               } catch (Exception var10) {
                  SQWebServer.Log.error("Cannot restart Jetty server. Exc.", var10);
               }
            }
         }
      }).start();
   }

   private static void checkVariables() {
      if (portFrom == 0) {
         portFrom = 8080;
         portTo = 8085;
      }

      if (pluginsPath == null) {
         pluginsPath = MainApp.getDataPath() + "internal/plugins";
      }

      if (webPath == null) {
         webPath = MainApp.getDataPath() + "internal/web";
      }

      Log.debug("Port range: " + portFrom + " - " + portTo);
      Log.debug("Plugins folder path: " + pluginsPath);
      Log.debug("Web folder path: " + webPath);
   }

   public void showErrorDialog(String var1, String var2) {
      BrowserGUI.showErrorDialog(var1, var2);
   }

   public void setPortRange(int var1, int var2) {
      portFrom = var1;
      portTo = var2;
   }

   public void setPluginsFolderPath(String var1) {
      pluginsPath = var1;
   }

   public String getPluginsPath() {
      return pluginsPath;
   }

   public String getWebPath() {
      return webPath;
   }

   public int getPortFrom() {
      return portFrom;
   }

   public int getPortTo() {
      return portTo;
   }

   public int getUsedPort() {
      return JettyServer.port;
   }

   public void regenerateFiles() throws Exception {
      Log.debug("Starting to regenerate files.");
      long var1 = System.currentTimeMillis();
      AutoCompiler.compileBatchFiles(this);

      for (IAppPlugin var5 : SQPluginManager.getPlugins(IAppPlugin.class)) {
         if (!var5.getAppCode().equals("SQWIZARD") && !var5.getAppCode().equals("SQXBUSINESS")) {
            AutoCompiler.compileLayoutFiles(this, var5.getAppCode());
         }
      }

      long var6 = System.currentTimeMillis() - var1;
      Log.debug("Files regenerated - regeneration took " + var6 + "ms");
      Log.debug("Clearing browser cache...");
      BrowserGUI.clearCache();
   }

   public WebAppManager getWebAppManager() {
      return this.webAppManager;
   }

   public String getBaseURL() {
      return (JettyServer.useHttps ? "https" : "http") + "://localhost:" + JettyServer.port;
   }
}
