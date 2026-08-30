package com.strategyquant.plugin.Databank.impl.Rename;

import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.project.ProjectEngine;
import com.strategyquant.tradinglib.project.SQProject;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.util.Map;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DatabankRenameServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(DatabankRenameServlet.class);

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      switch (var1) {
         case "rename":
            return this.onRename(var2);
         default:
            throw new Exception("Unknown command '" + var1 + "'.");
      }
   }

   private String onRename(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"projectName", "databankName", "strategies"});
      JSONObject var2 = new JSONObject();
      String var3 = this.tryGetParamValue(var1, "projectName");
      String var4 = this.tryGetParamValue(var1, "databankName");
      String var5 = this.getParam(var1, "strategies", "all");
      SQProject var6 = ProjectEngine.get(var3);
      if (!ProjectEngine.projectExists(var3)) {
         throw new Exception("Project '" + var3 + "' doesn't exist.");
      }

      if (!var6.getDatabanks().containsKey(var4)) {
         throw new Exception("Databank '" + var4 + "' doesn't exist.");
      }

      Databank var7 = (Databank)var6.getDatabanks().get(var4);
      String[] var8 = var5.split(",");
      if (var8.length == 1 && var8[0].equals("all")) {
         var8 = new String[var7.size()];
         var7.getRecordKeys().toArray(var8);
      }

      String var9 = null;
      String var10 = "";
      String var11 = "";
      if (var8.length == 1) {
         var9 = this.tryGetParamValue(var1, "name");
      } else {
         try {
            var10 = this.tryGetParamValue(var1, "prefix");
         } catch (Exception var29) {
         }

         try {
            var11 = this.tryGetParamValue(var1, "postfix");
         } catch (Exception var28) {
         }
      }

      String var12 = "RenameStrategies";
      String var13 = "RenameStrategies";
      double var14 = 0.0;
      var6.publisher.resetLastData("progress-channel");
      var6.getProgress().update(var13, 0.0, null);

      for (int var18 = 0; var18 < var8.length; var18++) {
         String var19 = var8[var18];
         ResultsGroup var16 = null;

         try {
            var16 = var7.getLocked(var19, var12);
            ResultsGroup var17 = var16.clone();
            if (var8.length == 1) {
               var17.setName(var9);
            } else {
               var17.setName(var10 + var17.getName() + var11);
            }

            var7.remove(var19, true, true, false, true, var12);
            var7.add(var17, true);
         } catch (Exception var27) {
            Log.error("Error while renaming strategy '" + var19 + "'", var27);
         } finally {
            if (var16 != null) {
               var16.releaseLock(var12);
            }

            var14++;
            int var22 = (int)(var14 / var8.length * 100.0);
            var6.getProgress().update("RenameStrategies", var22, null);
         }
      }

      var6.getProgress().update(var13, 100.0, null);
      var2.put("success", "ok");
      return var2.toString();
   }
}
