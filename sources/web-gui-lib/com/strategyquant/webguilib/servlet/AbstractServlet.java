package com.strategyquant.webguilib.servlet;

import com.strategyquant.lib.L;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractServlet extends HttpServlet {
   private static final Logger Log = LoggerFactory.getLogger(AbstractServlet.class);

   protected void doPost(HttpServletRequest var1, HttpServletResponse var2) throws ServletException, IOException {
      this.doGet(var1, var2);
   }

   protected void doGet(HttpServletRequest var1, HttpServletResponse var2) throws ServletException, IOException {
   }

   protected String execute(String var1, String var2, Map<String, String[]> var3, String var4) throws Exception {
      return null;
   }

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      return null;
   }

   protected byte[] executeBinary(String var1, Map<String, String[]> var2, String var3) throws Exception {
      return null;
   }

   protected void dumpParams(Map<String, String[]> var1) {
      for (String var3 : var1.keySet()) {
         String[] var4 = (String[])var1.get(var3);
         String var5 = "";

         for (String var9 : var4) {
            var5 = var5 + var9 + " ";
         }

         Log.debug(var3 + ": " + var5);
      }
   }

   public void getRequestParams(HttpServletRequest var1, Map<String, String[]> var2) {
      if (var1.getMethod().equals("GET")) {
         var2.putAll(var1.getParameterMap());
      }

      if (!this.tryLoadFilePart(var1, var2)) {
         try {
            ServletInputStream var4 = var1.getInputStream();
            byte[] var5 = IOUtils.toByteArray(var4);
            String var6 = "";

            try {
               GZIPInputStream var7 = new GZIPInputStream(new ByteArrayInputStream(var5));
               var6 = IOUtils.toString(var7, "UTF-8");
            } catch (Exception var14) {
               var6 = IOUtils.toString(new ByteArrayInputStream(var5), "UTF-8");
            }

            String[] var17 = var6.split("&");

            for (String var11 : var17) {
               String[] var12 = var11.split("=");
               if (var12 != null && var12.length == 2) {
                  var12[0] = URLDecoder.decode(var12[0], "UTF-8");
                  var12[1] = URLDecoder.decode(var12[1], "UTF-8");
                  if (var2.containsKey(var12[0])) {
                     String[] var13 = (String[])var2.get(var12[0]);
                     var13 = Arrays.copyOf(var13, var13.length + 1);
                     var13[var13.length - 1] = var12[1];
                     var2.put(var12[0], var13);
                  } else {
                     var2.put(var12[0], new String[]{var12[1]});
                  }
               }
            }
         } catch (IOException var15) {
            Log.error("Cannot get request body");
         }
      }
   }

   protected String getParam(Map<String, String[]> var1, String var2, String var3) {
      return !var1.containsKey(var2) ? var3 : ((String[])var1.get(var2))[0];
   }

   protected String[] getParam(Map<String, String[]> var1, String var2) {
      return !var1.containsKey(var2) ? null : (String[])var1.get(var2);
   }

   protected void checkParamExists(Map<String, String[]> var1, String[] var2) throws Exception {
      for (String var6 : var2) {
         if (this.getParam(var1, var6) == null) {
            throw new Exception(L.t("Parameter '%s' is missing.", new Object[]{var6}));
         }
      }
   }

   protected String[] tryGetParam(Map<String, String[]> var1, String var2) throws Exception {
      if (!var1.containsKey(var2)) {
         throw new Exception(L.t("Parameter '%s' is missing.", new Object[]{var2}));
      } else {
         return (String[])var1.get(var2);
      }
   }

   protected String tryGetParamValue(Map<String, String[]> var1, String var2) throws Exception {
      if (!var1.containsKey(var2)) {
         throw new Exception(L.t("Parameter '%s' is missing.", new Object[]{var2}));
      } else {
         return ((String[])var1.get(var2))[0];
      }
   }

   protected JSONObject tryGetJsonObject(Map<String, String[]> var1, String var2) throws Exception {
      JSONObject var3 = new JSONObject();
      String var4 = var2 + "[";

      for (String var6 : var1.keySet()) {
         if (var6.startsWith(var4)) {
            int var7 = var6.indexOf("]");
            String var8 = var6.substring(var4.length(), var7);
            var3.put(var8, ((String[])var1.get(var6))[0]);
         }
      }

      return var3;
   }

   protected JSONArray tryGetJsonArray(Map<String, String[]> var1, String var2) throws Exception {
      JSONArray var3 = new JSONArray();
      String var4 = var2 + "[";

      for (String var6 : var1.keySet()) {
         if (var6.startsWith(var4)) {
            int var7 = var6.indexOf("]");
            int var8 = Integer.parseInt(var6.substring(var4.length(), var7));
            String var9 = var6.substring(var7 + 2, var6.indexOf("]", var7 + 1));

            while (var8 > var3.length() - 1) {
               var3.put(new JSONObject());
            }

            JSONObject var10 = var3.getJSONObject(var8);
            var10.put(var9, ((String[])var1.get(var6))[0]);
         }
      }

      return var3;
   }

   protected boolean tryLoadFilePart(HttpServletRequest var1, Map<String, String[]> var2) {
      InputStream var3 = null;
      String var4 = var1.getContentType();

      try {
         if (var4 != null && var4.startsWith("multipart/form-data")) {
            Part var5 = var1.getPart("file");
            if (var5 == null) {
               return false;
            }

            var3 = var5.getInputStream();
            byte[] var6 = var3.readAllBytes();
            byte[] var7 = Base64.getEncoder().encode(var6);
            var2.put("fileName", new String[]{this.getFileName(var5)});
            var2.put("file", new String[]{new String(var7)});
            return true;
         } else {
            return false;
         }
      } catch (Exception var20) {
         Log.error("Failed to read uploaded file", var20);
         return false;
      } finally {
         if (var3 != null) {
            try {
               var3.close();
            } catch (Exception var19) {
            }
         }
      }
   }

   private String getFileName(Part var1) {
      String var2 = var1.getHeader("content-disposition");

      for (String var6 : var1.getHeader("content-disposition").split(";")) {
         if (var6.trim().startsWith("filename")) {
            return var6.substring(var6.indexOf(61) + 1).trim().replace("\"", "");
         }
      }

      return null;
   }
}
