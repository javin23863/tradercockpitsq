package com.strategyquant.plugin.Results.impl.PortfolioCorrelation.overlappingTrades;

import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.memory.CpuInfo;
import com.strategyquant.lib.utils.JsonCreator;
import com.strategyquant.plugin.Results.impl.PortfolioCorrelation.PortfolioCorrelationInterruptException;
import com.strategyquant.plugin.Results.impl.PortfolioCorrelation.PortfolioCorrelationResultsSender;
import com.strategyquant.tradinglib.ResultsGroup;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OverlappingTradesComputer {
   private static final Logger Log = LoggerFactory.getLogger(OverlappingTradesComputer.class);
   private HashMap<String, OverlappingResult> results = new HashMap<>();
   public boolean stopped;

   public JSONObject compute(ResultsGroup var1, PortfolioCorrelationResultsSender var2) throws Exception {
      this.stopped = false;
      int var3 = CpuInfo.getAvailableProcessors();
      ExecutorService var4 = Executors.newFixedThreadPool(var3);

      try {
         this.results.clear();
         ArrayList var6 = new ArrayList();
         List var7 = var1.getResultKeys();

         for (String var9 : var7) {
            if (this.stopped) {
               throw new PortfolioCorrelationInterruptException();
            }

            if (!var9.equals("Portfolio") && !var9.startsWith("WF:") && !var9.startsWith("CrossCheck_")) {
               for (String var11 : var7) {
                  if (this.stopped) {
                     throw new PortfolioCorrelationInterruptException();
                  }

                  if (!var11.equals("Portfolio") && !var11.startsWith("WF:") && !var11.startsWith("CrossCheck_") && !var9.equals(var11)) {
                     String var5 = this.key(var9, var11);
                     if (!this.results.containsKey(var5)) {
                        this.results.put(var5, null);
                        var6.add(var4.submit(new OverlappingTradesTask(var9, var11, var1)));
                     }
                  }
               }
            }
         }

         int var18 = 0;

         for (Future var13 : var6) {
            if (this.stopped) {
               throw new PortfolioCorrelationInterruptException();
            }

            OverlappingResult var17 = (OverlappingResult)var13.get();
            var18++;
            double var19 = 50.0 + (double)var18 / this.results.size() * 100.0 / 2.0;
            var2.progress((int)var19);
            String var16 = this.key(var17.symbol1, var17.symbol2);
            this.results.put(var16, var17);
         }

         return this.printResults(var7);
      } catch (PortfolioCorrelationInterruptException var14) {
         var4.shutdownNow();
         throw var14;
      } catch (Exception var15) {
         Log.error("Error while computing overlapping trades.", var15);
         throw var15;
      }
   }

   private JSONObject printResults(List<String> var1) {
      JSONObject var2 = new JSONObject();
      Object var3 = null;
      JSONArray var5 = new JSONArray();
      var5.put("");
      JSONArray var6 = new JSONArray();

      for (int var7 = 0; var7 < var1.size(); var7++) {
         String var8 = (String)var1.get(var7);
         if (!var8.equals("Portfolio") && !var8.startsWith("WF:") && !var8.startsWith("CrossCheck_")) {
            var5.put(var8);
            JSONArray var9 = new JSONArray();
            var9.put(var8);

            for (String var11 : var1) {
               if (!var11.equals("Portfolio") && !var11.startsWith("WF:") && !var11.startsWith("CrossCheck_")) {
                  if (var8.equals(var11)) {
                     var9.put("");
                  } else {
                     var3 = this.key(var8, var11);
                     OverlappingResult var4 = this.results.get(var3);
                     var9.put(var4.count);
                  }
               }
            }

            JSONObject var13 = new JSONObject();
            var13.put("id", "r" + var7);
            var13.put("data", var9);
            var6.put(var13);
         }
      }

      var2.put("columns", var5);
      var2.put("rows", var6);
      return var2;
   }

   private String key(String var1, String var2) {
      return var1.compareTo(var2) > 0 ? var1 + "-" + var2 : var2 + "-" + var1;
   }

   public String list(String var1, String var2, int var3, int var4) {
      JsonCreator var5 = new JsonCreator();
      var5.beginObject();
      var5.put("rows");
      String var6 = this.key(var1, var2);
      OverlappingResult var7 = this.results.get(var6);
      var5.beginArray();

      for (int var8 = var3; var8 < var7.data.size(); var8++) {
         OverlappingTrade var9 = var7.data.get(var8);
         if (var8 >= var3 + var4) {
            break;
         }

         if (var8 != var3) {
            var5.separator();
         }

         var5.beginObject();
         var5.put("data");
         var5.beginArray();
         var5.setValue(var8 + 1, true);
         var5.setValue(SQTime.toString(var9.from1), true);
         var5.setValue(SQTime.toString(var9.to1), true);
         var5.setValue(SQTime.toString(var9.from2), true);
         var5.setValue(SQTime.toString(var9.to2), true);
         var5.setValue(var9.time, false);
         var5.endArray(true);
         var5.put("id", "r" + var8, false);
         var5.endObject(false);
      }

      var5.endArray(true);
      var5.put("pos", var3, true);
      var5.put("total_count", var7.data.size(), true);
      var5.put("success", "ok", false);
      var5.endObject(false);
      return var5.toJson();
   }

   public void stop() {
      this.stopped = true;
   }

   public void clear() {
      this.results.clear();
   }
}
