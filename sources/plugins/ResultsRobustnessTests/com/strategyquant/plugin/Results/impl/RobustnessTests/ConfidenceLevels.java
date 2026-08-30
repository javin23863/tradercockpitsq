package com.strategyquant.plugin.Results.impl.RobustnessTests;

import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.Result;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.databank.DatabankTableColumnEntry;
import com.strategyquant.tradinglib.databank.DatabankTableView;
import com.strategyquant.tradinglib.robustnesstests.RobustnessResults;
import java.util.ArrayList;
import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfidenceLevels {
   private static final Logger Log = LoggerFactory.getLogger(ConfidenceLevels.class);

   public JSONArray print(ResultsGroup var1, Result var2, String var3, DatabankTableView var4) {
      JSONArray var5 = new JSONArray();
      HashMap var6 = (HashMap)var2.get(var3 + "_ConfidenceLevelsStats");
      if (var6 == null) {
         return var5;
      }

      RobustnessResults var7 = (RobustnessResults)var2.get("RobustnessOriginalResults");
      if (var7 != null) {
         var5.put(this.writeStats(0, var7.getOriginalStats(), var4, var1));
         var5.put(this.writeStats(50, (SQStats)var6.get(50), var4, var1));
         var5.put(this.writeStats(60, (SQStats)var6.get(60), var4, var1));
         var5.put(this.writeStats(70, (SQStats)var6.get(70), var4, var1));
         var5.put(this.writeStats(80, (SQStats)var6.get(80), var4, var1));
         var5.put(this.writeStats(90, (SQStats)var6.get(90), var4, var1));
         var5.put(this.writeStats(92, (SQStats)var6.get(92), var4, var1));
         var5.put(this.writeStats(95, (SQStats)var6.get(95), var4, var1));
         var5.put(this.writeStats(97, (SQStats)var6.get(97), var4, var1));
         var5.put(this.writeStats(98, (SQStats)var6.get(98), var4, var1));
         var5.put(this.writeStats(99, (SQStats)var6.get(99), var4, var1));
         var5.put(this.writeStats(100, (SQStats)var6.get(100), var4, var1));
      }

      return var5;
   }

   private JSONObject writeStats(int var1, SQStats var2, DatabankTableView var3, ResultsGroup var4) {
      JSONObject var5 = new JSONObject();
      JSONArray var6 = new JSONArray();
      var6.put(var1 == 0 ? "Original" : var1 + " %");
      ArrayList var7 = var3.columns;

      for (int var8 = 0; var8 < var7.size(); var8++) {
         DatabankColumn var9 = ((DatabankTableColumnEntry)var7.get(var8)).tableColumn;

         try {
            String var10 = var9.printHtmlFormatedValue(var2);
            var6.put(var10);
         } catch (Exception var11) {
            Log.error("RT column value couldn't be printed.", var11);
            var6.put("N/A");
         }
      }

      var5.put("id", "r" + var1);
      var5.put("data", var6);
      return var5;
   }
}
