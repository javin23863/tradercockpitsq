package com.strategyquant.webguilib.servlet;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.exception.ISQException;
import com.strategyquant.webguilib.config.RemoteAccessConfig;
import com.strategyquant.webguilib.server.JettyServer;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpJSONServlet extends AbstractServlet {
   private static final Logger Log = LoggerFactory.getLogger(HttpJSONServlet.class);
   public static long requestsHandled = 0L;
   public static long requestErrors = 0L;
   public static final Object lock = new Object();
   public boolean IgnoreErrorMessage = false;
   private boolean chromiumBrowser = false;

   @Override
   protected void doGet(HttpServletRequest var1, HttpServletResponse var2) throws ServletException, IOException {
      super.doGet(var1, var2);
      synchronized (lock) {
         requestsHandled++;
      }

      String var23 = var1.getRequestURI();
      String var4 = "ok";
      short var5 = 200;
      HashMap var6 = new HashMap();

      try {
         if (var1.getParameterMap() != null && !var1.getParameterMap().isEmpty()) {
            var6.putAll(var1.getParameterMap());
            this.tryLoadFilePart(var1, var6);
         } else {
            this.getRequestParams(var1, var6);
         }

         String var7 = var1.getRemoteHost();
         String var27 = "" + var1.getRemotePort();
         var6.put("remoteIP", new String[]{var7});
         var6.put("remotePort", new String[]{var27});
         String var9 = var1.getPathInfo().substring(1);
         String var10 = var1.getHeader("browserToken");
         String var11 = var1.getHeader("sq-auth-token");
         if (Log.isDebugEnabled()) {
            Log.debug("Incoming command: " + var1.getContextPath() + " " + var1.getPathInfo());
         }

         this.dumpParams(var1.getParameterMap());
         this.checkBrowser(var10);
         this.checkLicense(var23);
         if (!this.chromiumBrowser) {
            if (!RemoteAccessConfig.getInstance().isRemoteAccess()) {
               Log.error("Unauthorized request. Remote access is disabled");
               JSONObject var29 = new JSONObject();
               var29.put("error", "Remote access disabled");
               var29.put("disabled", true);
               var2.setStatus(401);
               var2.getWriter().print(var29.toString());
               var2.flushBuffer();
               return;
            }

            if (RemoteAccessConfig.getInstance().isPasswordRequired() && !var9.equals("login") && !var9.equals("getremoteaccess")) {
               int var12 = -1;

               try {
                  var11 = this.getParam(var6, "token")[0];
               } catch (Exception var19) {
               }

               try {
                  if (Log.isDebugEnabled()) {
                     Log.debug("Incoming remote access token: " + var11 + ", generated token: " + RemoteAccessConfig.getInstance().getToken());
                  }

                  var12 = Integer.parseInt(var11);
               } catch (Exception var18) {
                  Log.error("Cannot parse remote access token");
               }

               if (var11 == null || var12 != RemoteAccessConfig.getInstance().getToken()) {
                  Log.error("Unauthorized request '" + var23 + "'. Invalid token");
                  JSONObject var13 = new JSONObject();
                  var13.put("error", "Unauthorized access");
                  var2.setStatus(401);
                  var2.getWriter().print(var13.toString());
                  var2.flushBuffer();
                  return;
               }
            }
         }

         if (this.isBinaryContentType(var23)) {
            byte[] var28 = this.executeBinary(var9, var1.getParameterMap(), var1.getMethod());
            var2.setHeader("Access-Control-Allow-Origin", "*");
            var2.addHeader("Connection", "close");
            var2.setContentType(this.getBinaryContentType(var23));
            var2.setStatus(var5);
            var2.getOutputStream().write(var28);
            return;
         }

         var4 = this.execute(var9, var6, var1.getMethod());
         if (var4 == null) {
            throw new NullPointerException("Request not processed.");
         }
      } catch (Exception var21) {
         Log.error("Execution failed. Request URL: " + var1.getRequestURI() + ". ", var21);
         synchronized (lock) {
            requestErrors++;
         }

         var5 = 200;
         var4 = apiErrorJSON(null, var21);
      } catch (Error var22) {
         Log.error("Execution failed. Request URL: " + var1.getRequestURI() + ". ", var22);
         synchronized (lock) {
            requestErrors++;
         }

         var5 = 200;
         var4 = apiErrorJSON(null, var22);
      }

      var2.setCharacterEncoding("utf-8");
      var2.setHeader("Access-Control-Allow-Origin", "*");
      var2.addHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, PUT, DELETE, HEAD");
      var2.addHeader("Access-Control-Allow-Headers", "*");
      var2.addHeader("Connection", "close");
      String var25 = "text/html";
      if (var23.endsWith(".css")) {
         var25 = "text/css";
      } else if (var23.endsWith(".js")) {
         var25 = "text/javascript";
      }

      var2.getWriter().print(var4);
      var2.setContentType(var25);
      var2.setStatus(var5);
      var2.flushBuffer();
   }

   private String getBinaryContentType(String var1) {
      String var2 = SQUtils.getExtension(var1.replaceAll("\\?.*$", ""));
      switch (var2) {
         case "png":
            return "image/png";
         case "jpg":
            return "image/jpeg";
         case "gif":
            return "image/gif";
         case "eot":
            return "application/vnd.ms-fontobject";
         case "otf":
            return "font/otf";
         case "svg":
            return "image/svg+xml";
         case "ttf":
            return "font/ttf";
         case "woff":
            return "font/woff";
         case "woff2":
            return "font/woff2";
         default:
            return "application/x-binary";
      }
   }

   private boolean isBinaryContentType(String var1) {
      String var2 = SQUtils.getExtension(var1.replaceAll("\\?.*$", ""));
      switch (var2) {
         case "png":
         case "jpg":
         case "gif":
         case "ico":
         case "eot":
         case "otf":
         case "svg":
         case "ttf":
         case "woff":
         case "woff2":
            return true;
         default:
            return false;
      }
   }

   private void checkLicense(String var1) throws Exception {
      if (!var1.startsWith("/about")) {
         if (MainApp.checkProduct("SQUANT") && !MainApp.v571hfnsHw().gWfGtoRYJG()) {
            throw new Exception(L.t("Invalid license. Please Update license key.", new Object[0]));
         }
      }
   }

   public static String apiErrorJSON(String var0, Throwable var1) {
      JSONObject var2 = new JSONObject();
      if (var1 == null && var0 == null) {
         var0 = "Internal server error";
      } else {
         var0 = var0 == null ? "" : var0.replace("\"", "'") + "<br>";
         if (var1 != null) {
            String var3 = var1.getMessage() != null ? var1.getMessage().replace("\"", "'") : var1.getClass().getName();
            var0 = var0 + var3;
         }
      }

      var2.put("error", var0);
      if (var1 instanceof ISQException) {
         var2.put("errorCode", ((ISQException)var1).getCode());
      }

      Log.error(var0, var1);
      return var2.toString();
   }

   public static String apiErrorJSONNoLog(String var0, Exception var1) {
      JSONObject var2 = new JSONObject();
      if (var1 == null && var0 == null) {
         var0 = "Internal server error";
      } else {
         var0 = var0 == null ? "" : var0.replace("\"", "'") + "<br>";
         if (var1 != null) {
            String var3 = var1.getMessage() != null ? var1.getMessage().replace("\"", "'") : var1.getClass().getName();
            var0 = var0 + var3;
         }
      }

      var2.put("error", var0);
      if (var1 instanceof ISQException) {
         var2.put("errorCode", ((ISQException)var1).getCode());
      }

      return var2.toString();
   }

   private void checkBrowser(String var1) {
      if (var1 != null) {
         try {
            int var2 = Integer.parseInt(var1);
            if (var2 == JettyServer.getBrowserToken()) {
               this.chromiumBrowser = true;
               if (Log.isDebugEnabled()) {
                  Log.debug("Chromium browser detected");
               }
            }
         } catch (Exception var3) {
            Log.error("Cannot parse browser token");
            this.chromiumBrowser = false;
         }
      } else {
         this.chromiumBrowser = false;
      }
   }

   public boolean isChromiumBrowser() {
      return this.chromiumBrowser;
   }

   protected Map<String, Object> getParameters(String[] var1, Map<String, String[]> var2) {
      HashMap var3 = new HashMap();
      String var4 = null;

      try {
         var4 = ((String[])var2.get("resultKey"))[0];
      } catch (Exception var21) {
      }

      byte var5 = 0;

      try {
         var5 = Byte.parseByte(((String[])var2.get("direction"))[0]);
      } catch (Exception var20) {
      }

      byte var6 = 127;

      try {
         var6 = Byte.parseByte(((String[])var2.get("sampleType"))[0]);
      } catch (Exception var19) {
      }

      int var9 = -1;
      boolean var10 = false;

      int var7;
      try {
         var7 = Integer.parseInt(((String[])var2.get("posStart"))[0]);
      } catch (Exception var18) {
         var7 = 0;
      }

      int var8;
      try {
         var8 = Integer.parseInt(((String[])var2.get("count"))[0]);
      } catch (Exception var17) {
         var8 = 100;
      }

      try {
         var9 = Integer.parseInt(((String[])var2.get("sortColumn"))[0]);
         var10 = ((String[])var2.get("sortDirection"))[0].equals("asc");
      } catch (Exception var16) {
      }

      var3.put("resultKey", var4);
      var3.put("direction", var5);
      var3.put("sampleType", var6);
      var3.put("posStart", var7);
      var3.put("count", var8);
      var3.put("sortColumn", var9);
      var3.put("asc", var10);
      if (var2.containsKey("project")) {
         var3.put("projectName", ((String[])var2.get("project"))[0]);
      } else {
         var3.put("projectName", var1 != null && var1.length > 0 ? var1[0] : "N/A");
      }

      if (var2.containsKey("databank")) {
         var3.put("databankName", ((String[])var2.get("databank"))[0]);
      } else {
         var3.put("databankName", var1 != null && var1.length > 1 ? var1[1] : "N/A");
      }

      if (var2.containsKey("strategy")) {
         var3.put("strategyName", ((String[])var2.get("strategy"))[0]);
      } else {
         var3.put("strategyName", var1 != null && var1.length > 2 ? var1[2] : "N/A");
      }

      try {
         String var11 = this.tryGetParam(var2, "backtestID")[0];
         var3.put("backtestID", var11);
      } catch (Exception var15) {
      }

      try {
         boolean var22 = Boolean.parseBoolean(this.tryGetParam(var2, "algoWizard")[0]);
         var3.put("algoWizard", var22);
      } catch (Exception var14) {
      }

      try {
         var3.put("viewXML", this.tryGetParam(var2, "viewXML")[0]);
      } catch (Exception var13) {
      }

      try {
         var3.put("userSecretCode", this.tryGetParam(var2, "userSecretCode")[0]);
      } catch (Exception var12) {
      }

      return var3;
   }

   protected Map<String, String[]> getRgParams(Map<String, Object> var1) {
      HashMap var2 = new HashMap();
      var2.put("projectName", new String[]{(String)var1.get("projectName")});
      var2.put("databankName", new String[]{(String)var1.get("databankName")});
      var2.put("strategyName", new String[]{(String)var1.get("strategyName")});
      if (var1.containsKey("algoWizard")) {
         var2.put("algoWizard", new String[]{var1.get("algoWizard").toString()});
      }

      if (var1.containsKey("backtestID")) {
         var2.put("backtestID", new String[]{var1.get("backtestID").toString()});
      }

      if (var1.containsKey("userSecretCode")) {
         var2.put("userSecretCode", new String[]{var1.get("userSecretCode").toString()});
      }

      return var2;
   }
}
