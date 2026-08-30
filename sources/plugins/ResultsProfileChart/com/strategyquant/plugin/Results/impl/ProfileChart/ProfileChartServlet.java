package com.strategyquant.plugin.Results.impl.ProfileChart;

import com.strategyquant.lib.L;
import com.strategyquant.lib.snippets.compile.SQStructure;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.databank.RecordNotFoundException;
import com.strategyquant.tradinglib.results.IResultsGroupProvider;
import com.strategyquant.tradinglib.results.SpecialValues;
import com.strategyquant.webguilib.BrowserGUI;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProfileChartServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(ProfileChartServlet.class);
   private static final String LOCK_PROFILECHART = "ProfileChart";
   private final IResultsGroupProvider rgProvider;

   public ProfileChartServlet(IResultsGroupProvider var1) {
      this.rgProvider = var1;
   }

   protected void doGet(HttpServletRequest var1, HttpServletResponse var2) throws ServletException, IOException {
      String var3 = var1.getPathInfo();
      if (var3 != null && var3.startsWith("/view/")) {
         try {
            String var4 = this.onView(var3.substring(6));
            var2.setCharacterEncoding("UTF-8");
            var2.setContentType("image/svg+xml");
            var2.setHeader("Access-Control-Allow-Origin", "*");
            var2.getWriter().print(var4);
         } catch (Exception var5) {
            Log.error("ProfileChart view failed", var5);
            var2.setStatus(404);
            var2.getWriter().print(var5.getMessage());
         }

         var2.flushBuffer();
      } else {
         super.doGet(var1, var2);
      }
   }

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      switch (var1) {
         case "paths":
            return this.onPaths(var2);
         default:
            throw new Exception(L.t("Unknown command '%s'.", new Object[]{var1}));
      }
   }

   protected byte[] executeBinary(String var1, Map<String, String[]> var2, String var3) throws Exception {
      if (var1 != null && var1.startsWith("file/")) {
         return this.onFile(var1.substring(5));
      } else {
         throw new Exception(L.t("Unknown command '%s'.", new Object[]{var1}));
      }
   }

   private String onPaths(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();
      ResultsGroup var3 = null;

      try {
         var3 = this.rgProvider.get(var1, "ProfileChart");
         String var4 = var3.specialValues().getString(SpecialValues.ProfileChartPaths, "");
         JSONArray var5 = new JSONArray();
         if (var4 != null && !var4.trim().isEmpty()) {
            for (String var9 : var4.split(",")) {
               String var10 = var9.trim();
               if (!var10.isEmpty()) {
                  int var11 = Math.max(var10.lastIndexOf(47), var10.lastIndexOf(92));
                  String var12 = var11 >= 0 ? var10.substring(var11 + 1) : var10;
                  var5.put(var12);
               }
            }
         }

         var2.put("profileChartPaths", var5);
         var2.put("baseUrl", BrowserGUI.getInstance().getAppUrl() + "/profilechart/view/");
      } catch (RecordNotFoundException var17) {
         Log.info("onPaths: " + var17.getMessage());
         return apiErrorJSONNoLog(null, var17);
      } catch (Exception var18) {
         return apiErrorJSON(L.t("Getting profile chart paths failed", new Object[0]), var18);
      } finally {
         if (var3 != null) {
            var3.releaseLock("ProfileChart");
            Object var20 = null;
         }
      }

      var2.put("success", "ok");
      return var2.toString();
   }

   private String onView(String var1) throws Exception {
      String var2 = URLDecoder.decode(var1, StandardCharsets.UTF_8);
      int var3 = Math.max(var2.lastIndexOf(47), var2.lastIndexOf(92));
      if (var3 >= 0) {
         var2 = var2.substring(var3 + 1);
      }

      if (!var2.contains("..") && !var2.isEmpty()) {
         File var4 = new File(SQStructure.PROFILE_CHART_DIR);
         File var5 = new File(var4, var2);
         if (var5.getCanonicalPath().startsWith(var4.getCanonicalPath()) && var5.isFile()) {
            String var6 = new String(Files.readAllBytes(var5.toPath()), StandardCharsets.UTF_8);
            if (var6.startsWith("<?xml")) {
               int var7 = var6.indexOf("?>");
               if (var7 >= 0) {
                  var6 = var6.substring(var7 + 2).trim();
               }
            }

            return var6;
         } else {
            throw new FileNotFoundException(L.t("Profile chart file not found.", new Object[0]));
         }
      } else {
         throw new IllegalArgumentException(L.t("Invalid filename.", new Object[0]));
      }
   }

   private byte[] onFile(String var1) throws Exception {
      String var2 = URLDecoder.decode(var1, StandardCharsets.UTF_8);
      int var3 = Math.max(var2.lastIndexOf(47), var2.lastIndexOf(92));
      if (var3 >= 0) {
         var2 = var2.substring(var3 + 1);
      }

      if (!var2.contains("..") && !var2.isEmpty()) {
         File var4 = new File(SQStructure.PROFILE_CHART_DIR);
         File var5 = new File(var4, var2);
         if (var5.getCanonicalPath().startsWith(var4.getCanonicalPath()) && var5.isFile()) {
            return Files.readAllBytes(var5.toPath());
         } else {
            throw new FileNotFoundException(L.t("Profile chart file not found.", new Object[0]));
         }
      } else {
         throw new IllegalArgumentException(L.t("Invalid filename.", new Object[0]));
      }
   }
}
