package com.strategyquant.plugin.Results.impl.RobustnessTests;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.Result;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.crosscheck.MonteCarloCrossCheckMethod;
import com.strategyquant.tradinglib.databank.DatabankTableColumnEntry;
import com.strategyquant.tradinglib.databank.DatabankTableView;
import java.util.ArrayList;
import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RiskOfRuins {
   private static final Logger Log = LoggerFactory.getLogger(RiskOfRuins.class);

   public JSONArray print(Result var1, String var2, int var3, DatabankTableView var4) {
      JSONArray var5 = new JSONArray();
      HashMap var6 = (HashMap)var1.get(String.format(var2 + "_RiskOfRuins%d", var3));
      if (var6 == null) {
         return var5;
      }

      SettingsMap var7 = var1.getSettings();
      double var8 = var7.getDouble("InitialDeposit", 10000.0);
      int var10 = 0;

      for (double var14 : MonteCarloCrossCheckMethod.InitialCapitalVariants) {
         var5.put(this.writeStats(var10, var14, var8, (SQStats)var6.get(var14), var4));
         var10++;
      }

      return var5;
   }

   private JSONObject writeStats(int var1, double var2, double var4, SQStats var6, DatabankTableView var7) {
      JSONObject var8 = new JSONObject();
      JSONArray var9 = new JSONArray();
      var9.put(var4 * var2);
      var9.put(SQUtils.d2(var6.getDouble("RiskOfRuin")) + " %");
      var9.put(SQUtils.d2(var6.getDouble("ProfitProbability")) + " %");
      ArrayList var10 = var7.columns;

      for (int var11 = 0; var11 < var10.size(); var11++) {
         DatabankColumn var12 = ((DatabankTableColumnEntry)var10.get(var11)).tableColumn;

         try {
            String var13 = var12.printValue(var6);
            var9.put(var13);
         } catch (Exception var14) {
            Log.error("RT column value couldn't be printed.", var14);
            var9.put("N/A");
         }
      }

      var8.put("id", "r" + var1);
      var8.put("data", var9);
      return var8;
   }
}
