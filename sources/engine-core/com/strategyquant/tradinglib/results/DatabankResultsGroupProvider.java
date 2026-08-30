package com.strategyquant.tradinglib.results;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.backtest.SQBacktester;
import com.strategyquant.tradinglib.project.ProjectEngine;
import java.util.Map;

public class DatabankResultsGroupProvider implements IResultsGroupProvider {
   @Override
   public ResultsGroup get(Map<String, String[]> var1, String var2) throws Exception {
      String var3 = var1.containsKey("projectName") ? this.tryGetParam(var1, "projectName")[0] : this.tryGetParam(var1, "project")[0];
      boolean var4 = false;

      try {
         var4 = Boolean.parseBoolean(this.tryGetParam(var1, "algoWizard")[0]);
      } catch (Exception var9) {
         try {
            String var6 = var1.containsKey("strategy") ? "strategy" : "strategyName";
            var4 = var4 || this.tryGetParam(var1, var6)[0].startsWith("AlgoWizard");
         } catch (Exception var8) {
         }
      }

      if (var4) {
         String var10 = this.tryGetParam(var1, "backtestID")[0];
         return SQBacktester.getResultsGroup(var10);
      } else {
         String var5 = var1.containsKey("databankName") ? this.tryGetParam(var1, "databankName")[0] : this.tryGetParam(var1, "databank")[0];
         String var11 = var1.containsKey("strategyName") ? this.tryGetParam(var1, "strategyName")[0] : this.tryGetParam(var1, "strategy")[0];
         Databank var7 = ProjectEngine.get(var3).getDatabanks().get(var5);
         if (var7 == null) {
            throw new Exception(L.t("Databank '%s' not found in project '%s'!", new Object[]{var5, var3}));
         } else {
            return var7.getLocked(var11, var2);
         }
      }
   }

   private String[] tryGetParam(Map<String, String[]> var1, String var2) throws Exception {
      if (!var1.containsKey(var2)) {
         throw new Exception("Parameter '" + var2 + "' is missing.");
      } else {
         return (String[])var1.get(var2);
      }
   }

   @Override
   public String getFilePath(ResultsGroup var1) {
      return var1.getFilePath();
   }
}
