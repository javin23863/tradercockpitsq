package com.strategyquant.tradinglib.strategy;

import com.strategyquant.pluginlib.program.IProgram;
import com.strategyquant.pluginlib.program.Program;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.project.SQProject;
import com.strategyquant.tradinglib.results.SpecialValues;
import com.strategyquant.tradinglib.results.file.MissingInstrumentException;
import org.json.JSONObject;

public class StrategyLoader {
   private static boolean cancelLoadingRecords = false;
   private static boolean waitingForInstrument = false;

   public static ResultsGroup loadStrategy(SQProject var0, String var1, String var2, double var3) throws Exception {
      IProgram var5 = Program.get("Loader");

      while (!cancelLoadingRecords) {
         try {
            ResultsGroup var6 = (ResultsGroup)var5.call("loadFile", new Object[]{var2});
            var6.updated = true;
            if (var6.specialValues().containsKey(SpecialValues.IsMergedPortfolio)) {
               var6.setMergedResults(StrategyMerger.loadSplitResultsFromSource(var2));
            }

            if (cancelLoadingRecords) {
               var0.getProgress().update("load", 100.0, "canceled");
               return null;
            }

            var0.getProgress().update("load", var3, null);
            return var6;
         } catch (MissingInstrumentException var9) {
            var0.getProgress()
               .update(
                  "load", var3, null, "selectInstrument", new JSONObject().put("reportName", var1).put("symbol", var9.getSymbol()).put("databankLoading", true)
               );
            waitingForInstrument = true;

            while (waitingForInstrument) {
               try {
                  Thread.sleep(500L);
               } catch (Exception var8) {
               }
            }
         } catch (OutOfMemoryError var10) {
            var0.getProgress().update("load", 100.0, "Some strategies were not loaded. " + var10.getMessage());
            throw var10;
         }
      }

      var0.getProgress().update("load", 100.0, "canceled");
      return null;
   }

   public static void init() {
      cancelLoadingRecords = false;
      waitingForInstrument = false;
   }

   public static void cancel() {
      cancelLoadingRecords = true;
      waitingForInstrument = false;
   }

   public static void instrumentSelected() {
      waitingForInstrument = false;
   }

   public static boolean isCanceled() {
      return cancelLoadingRecords;
   }
}
