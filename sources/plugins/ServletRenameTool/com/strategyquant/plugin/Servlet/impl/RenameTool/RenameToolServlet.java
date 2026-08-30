package com.strategyquant.plugin.Servlet.impl.RenameTool;

import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.project.ProjectEngine;
import com.strategyquant.tradinglib.project.SQProject;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.util.ArrayList;
import java.util.Map;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RenameToolServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(RenameToolServlet.class);
   private static final String lockName = "RenameToolServlet";

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      switch (var1) {
         case "rename":
            return this.onRename(var2);
         default:
            throw new Exception("Unknown command '" + var1 + "'.");
      }
   }

   private String onRename(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      String var3 = this.tryGetParam(var1, "projectName")[0];
      String var4 = this.tryGetParam(var1, "databankName")[0];
      String var5 = this.tryGetParam(var1, "strategies")[0];
      String[] var6 = var5.split(",");
      SQProject var7 = ProjectEngine.get(var3);
      Databank var8 = (Databank)var7.getDatabanks().get(var4);
      boolean var9 = var6[0].equals("all");
      ArrayList var10 = var8.getRecordKeys();
      RenameToolSettings.load();

      for (int var11 = 0; var11 < var10.size(); var11++) {
         String var12 = (String)var10.get(var11);
         if (!var9) {
            boolean var13 = false;

            for (int var14 = 0; var14 < var6.length; var14++) {
               if (var6[var14].equals(var12)) {
                  var13 = true;
                  break;
               }
            }

            if (!var13) {
               continue;
            }
         }

         ResultsGroup var21 = null;
         ResultsGroup var22 = null;

         try {
            var21 = var8.getLocked(var12, "RenameToolServlet");
            var22 = var21.clone();
         } catch (Exception var19) {
            Log.error("Error processing strategy '" + var12 + "'", var19);
         } finally {
            if (var21 != null) {
               var21.releaseLock("RenameToolServlet");
            }
         }

         var8.remove(var12, true, true, false, true, "RenameToolServlet");
         RenameToolGenerator.processStrategy(var22);
         var8.add(var22, true);
      }

      var2.put("success", "Strategies renamed.");
      return var2.toString();
   }
}
