package com.strategyquant.webguilib.server;

import com.strategyquant.lib.L;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.constants.SQPaths;
import com.strategyquant.webguilib.WebServer;
import com.strategyquant.webguilib.servlet.DirServlet;
import com.strategyquant.webguilib.servlet.LanguageServlet;
import com.strategyquant.webguilib.servlet.MainServlet;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractUIWebServer {
   public static final Logger Log = LoggerFactory.getLogger(AbstractUIWebServer.class);
   private Server server;
   protected WebServer webServer;
   public static boolean useHttps = false;
   public static final int portFrom = 8080;
   public static final int portTo = 8090;
   public static int port = -1;
   private HandlerList handlers;
   private static int browserToken = -1;
   protected static AbstractUIWebServer instance;

   public AbstractUIWebServer(WebServer var1) {
      this.webServer = var1;
      instance = this;
      useHttps = Boolean.parseBoolean(MainApp.settings().get("SSLUse", "false"));
   }

   public static AbstractUIWebServer getInstance() {
      return instance;
   }

   public void start() throws Exception {
      Handler[] var1 = this.loadAllHandlers();
      this.handlers = new HandlerList();
      this.handlers.setHandlers(var1);
      browserToken = getBrowserToken();
      Log.debug("generated token: " + browserToken);

      try {
         port = Integer.parseInt(MainApp.settings().get("WebServerPort", "-1"));
      } catch (Exception var7) {
         Log.info("No valid websocket port specified in settings");
      }

      if (port > 0) {
         this.startServer(port, this.handlers);
      } else {
         boolean var2 = false;

         for (int var3 = 8080; var3 <= 8090; var3++) {
            try {
               this.startServer(var3, this.handlers);
               port = var3;
               var2 = true;
               break;
            } catch (Exception var8) {
               try {
                  this.server.stop();
               } catch (Exception var6) {
               }

               Log.warn("Cannot start webserver on port " + var3 + ". Trying next port...", var8);
            }
         }

         if (!var2) {
            throw new Exception(L.t("Cannot start webserver - no ports available in range %d - %d. Try to set the port manually", new Object[]{8080, 8090}));
         }
      }

      Log.info(L.t("GUI started, you can access it on http://localhost:%d", new Object[]{port}));
      MainApp.settings().set("WebServerPortUsed", port + "");
      MainApp.settings().set("BrowserToken", browserToken + "");
      MainApp.settings().save();
      this.serverStarted(port);
   }

   public static int getBrowserToken() {
      if (browserToken == -1) {
         browserToken = new Date().toString().hashCode();
      }

      return browserToken;
   }

   public static void setBrowserToken(int var0) {
      browserToken = var0;
   }

   public void restartServer() throws Exception {
      try {
         this.server.stop();
      } catch (Exception var2) {
         Log.error("Cannot stop Jetty. Exc.", var2);
      }

      this.startServer(port, this.handlers);
   }

   public void startServer(int var1, HandlerList var2) throws Exception {
      int var3 = var1;
      Server var4 = useHttps ? new Server() : new Server(var3);
      if (Log.isDebugEnabled()) {
         var4.setDumpAfterStart(false);
      }

      if (useHttps) {
         HttpConfiguration var5 = new HttpConfiguration();
         var5.setSecurePort(var3);
         var5.setSecureScheme("https");
         String var6 = MainApp.settings().get("SSLKeystorePath", "");
         String var7 = MainApp.settings().get("SSLKeystorePass", "");
         String var8 = var7;
         if (!new File(var6).exists()) {
            var6 = MainApp.getDataPath() + var6;
         }

         org.eclipse.jetty.util.ssl.SslContextFactory.Server var9 = new org.eclipse.jetty.util.ssl.SslContextFactory.Server();
         var9.setKeyStorePath(var6);
         var9.setKeyStorePassword(var7);
         var9.setKeyManagerPassword(var8);
         var9.setWantClientAuth(false);
         var9.setNeedClientAuth(false);
         HttpConfiguration var10 = new HttpConfiguration();
         var10.setSecureScheme("https");
         var10.setSecurePort(var3);
         var10.setOutputBufferSize(32786);
         var10.setRequestHeaderSize(8192);
         var10.setResponseHeaderSize(8192);
         SecureRequestCustomizer var11 = new SecureRequestCustomizer();
         var11.setSniHostCheck(false);
         var10.addCustomizer(var11);
         ServerConnector var12 = new ServerConnector(
            var4, new ConnectionFactory[]{new SslConnectionFactory(var9, "http/1.1"), new HttpConnectionFactory(var10)}
         );
         var12.setPort(var3);
         var4.addConnector(var12);
      }

      var4.setHandler(var2);
      this.configureWebSocket();
      var4.start();
      Log.info("Started Jetty server: {}", Server.getVersion());
   }

   private void configureWebSocket() {
      ServletContextHandler var1 = null;

      for (Handler var5 : this.handlers.getHandlers()) {
         Handler var6 = var5;
         if (var5 instanceof GzipHandler) {
            GzipHandler var7 = (GzipHandler)var5;
            var6 = var7.getHandler();
         }

         if (var6 instanceof ServletContextHandler) {
            ServletContextHandler var8 = (ServletContextHandler)var6;
            if ("/websocket".equals(var8.getContextPath())) {
               var1 = var8;
               break;
            }
         }
      }

      if (var1 != null) {
         JettyWebSocketServletContainerInitializer.configure(var1, null);
      }
   }

   private Handler[] loadAllHandlers() {
      String var1 = MainApp.getProduct() + "/index.html";
      Log.info("Loading app path: " + var1);
      ServletContextHandler var2 = new ServletContextHandler(1);
      var2.setContextPath("/main");
      var2.addServlet(new ServletHolder(new MainServlet(this.webServer)), "/*");
      ServletContextHandler var3 = new ServletContextHandler(1);
      var3.setContextPath("/language");
      var3.addServlet(new ServletHolder(new LanguageServlet()), "/*");
      ServletContextHandler var4 = new ServletContextHandler(1);
      var4.setContextPath("/dirs");
      var4.addServlet(new ServletHolder(new DirServlet()), "/*");
      ResourceHandler var5 = new ResourceHandler();
      var5.setDirectoriesListed(true);
      var5.setWelcomeFiles(new String[]{var1, "/index.html"});
      var5.setResourceBase(this.webServer.getWebPath());
      ResourceHandler var6 = new ResourceHandler();
      var6.setDirectoriesListed(true);
      Resource var7 = Resource.newResource(new File(this.webServer.getPluginsPath()));
      ResourceCollection var8;
      if (MainApp.checkProduct("QDM")) {
         var8 = new ResourceCollection(new Resource[]{var7});
      } else {
         Resource var9 = Resource.newResource(new File(SQPaths.userDirPath + "/extend/Plugins"));
         Resource var10 = Resource.newResource(new File(SQPaths.userDirPath + "/extend/ResultsPlugins"));
         var8 = new ResourceCollection(new Resource[]{var7, var9, var10});
      }

      var6.setBaseResource(var8);
      ContextHandler var14 = new ContextHandler("/plugins");
      var14.setHandler(var6);
      ArrayList var15 = new ArrayList();
      var15.add(this.getGzipHandler(var5));
      var15.add(this.getGzipHandler(var14));
      var15.add(this.getGzipHandler(var2));
      var15.add(this.getGzipHandler(var3));
      var15.add(this.getGzipHandler(var4));
      this.loadCustomHandlers(var15);
      var15.add(new DefaultHandler());
      Handler[] var11 = new Handler[var15.size()];

      for (int var12 = 0; var12 < var15.size(); var12++) {
         Handler var13 = (Handler)var15.get(var12);
         if (var13 instanceof ContextHandler) {
            ((ContextHandler)var13).setClassLoader(this.getClass().getClassLoader());
         } else if (var13 instanceof GzipHandler && ((GzipHandler)var13).getHandler() instanceof ContextHandler) {
            ((ContextHandler)((GzipHandler)var13).getHandler()).setClassLoader(this.getClass().getClassLoader());
         }

         var11[var12] = var13;
      }

      return var11;
   }

   protected abstract void loadCustomHandlers(List<Handler> var1);

   protected abstract void serverStarted(int var1);

   protected GzipHandler getGzipHandler(Handler var1) {
      GzipHandler var2 = new GzipHandler();
      var2.setIncludedMimeTypes(new String[]{"text/html", "text/plain", "text/xml", "text/css", "application/javascript", "text/javascript"});
      var2.setIncludedMethods(new String[]{"POST", "GET"});
      var2.setHandler(var1);
      return var2;
   }

   public void stop() throws Exception {
      this.server.stop();
      this.server.destroy();
   }

   public String getState() {
      return this.server.getState();
   }
}
