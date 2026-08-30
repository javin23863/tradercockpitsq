package com.strategyquant.tradinglib.engine.stockpicker.data.symbols;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.data.io.IDataLoader;
import com.strategyquant.datalib.data.io.VersatileData;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.TaskStoppedException;
import com.strategyquant.tradinglib.engine.stockpicker.data.timeline.PickerDataTimeline;
import com.strategyquant.tradinglib.project.StopPauseEngine;

public class SymbolDataLoader {
   public LoadedSymbolData load(DataInfo var1, PickerDataTimeline var2, StopPauseEngine var3) throws Exception {
      LoadedSymbolData var4 = new LoadedSymbolData(var2.SizeD(), var2.SizeW(), var2.SizeM());
      this._load(var1, var2, var3, var4, "D1");
      this._load(var1, var2, var3, var4, "Weekly");
      this._load(var1, var2, var3, var4, "Monthly");
      return var4;
   }

   private void _load(DataInfo var1, PickerDataTimeline var2, StopPauseEngine var3, LoadedSymbolData var4, String var5) throws Exception {
      IDataLoader var6 = null;

      try {
         var6 = DataManager.getDataLoader(
            new ChartDef("History", var1.symbol, var5, var2.dateFrom(), var2.dateTo(), var1.symbolInfo.defaultSpread, "No Session"), 1
         );
         var6.open();
         VersatileData var7 = new VersatileData();

         while (var6.hasNextTick()) {
            var6.getNextTick(var7);
            if (var3.isStopped()) {
               throw new TaskStoppedException();
            }

            long var9 = SQTime.getDateInMs(var7.time);
            switch (var5) {
               case "D1":
                  if (var2.containsTimeD(var9)) {
                     int var19 = var2.timeToIndexD(var9);
                     if (var4.getRealDataIndexStartD() < 0) {
                        var4.setRealDataIndexStartD(var19);
                     }

                     var4.setRealDataIndexEndD(var19);
                     var4.OpenD[var19] = (float)var7.open;
                     var4.HighD[var19] = (float)var7.high;
                     var4.LowD[var19] = (float)var7.low;
                     var4.CloseD[var19] = (float)var7.close;
                     var4.VolumeD[var19] = (float)var7.volume;
                  }
                  break;
               case "Weekly":
                  if (var2.containsTimeW(var9)) {
                     int var18 = var2.timeToIndexW(var9);
                     if (var4.getRealDataIndexStartW() < 0) {
                        var4.setRealDataIndexStartW(var18);
                     }

                     var4.setRealDataIndexEndW(var18);
                     var4.OpenW[var18] = (float)var7.open;
                     var4.HighW[var18] = (float)var7.high;
                     var4.LowW[var18] = (float)var7.low;
                     var4.CloseW[var18] = (float)var7.close;
                     var4.VolumeW[var18] = (float)var7.volume;
                  }
                  break;
               case "Monthly":
                  if (var2.containsTimeM(var9)) {
                     int var8 = var2.timeToIndexM(var9);
                     if (var4.getRealDataIndexStartM() < 0) {
                        var4.setRealDataIndexStartM(var8);
                     }

                     var4.setRealDataIndexEndM(var8);
                     var4.OpenM[var8] = (float)var7.open;
                     var4.HighM[var8] = (float)var7.high;
                     var4.LowM[var8] = (float)var7.low;
                     var4.CloseM[var8] = (float)var7.close;
                     var4.VolumeM[var8] = (float)var7.volume;
                  }
                  break;
               default:
                  throw new Exception(L.t("Invalid timeframe %s.", new Object[]{var5}));
            }
         }

         this.fillGaps(var5, var4);
         var6.close();
      } catch (Exception var16) {
         throw new Exception(L.t("Cannot load data for symbol %s/%s - %s", new Object[]{var1.symbol, var5, var16.getMessage()}), var16);
      } finally {
         if (var6 != null) {
            var6.close();
         }
      }
   }

   private void fillGaps(String var1, LoadedSymbolData var2) {
      float var3 = -1.0F;
      float var4 = -1.0F;
      float var5 = -1.0F;
      float var6 = -1.0F;
      float var7 = -1.0F;
      switch (var1) {
         case "D1":
            for (int var12 = var2.getRealDataIndexStartD(); var12 <= var2.getRealDataIndexEndD(); var12++) {
               if (var2.OpenD[var12] == 0.0F) {
                  if (!(var3 < 0.0F)) {
                     var2.OpenD[var12] = var3;
                     var2.HighD[var12] = var4;
                     var2.LowD[var12] = var5;
                     var2.CloseD[var12] = var6;
                     var2.VolumeD[var12] = var7;
                  }
               } else {
                  var3 = var2.OpenD[var12];
                  var4 = var2.HighD[var12];
                  var5 = var2.LowD[var12];
                  var6 = var2.CloseD[var12];
                  var7 = var2.VolumeD[var12];
               }
            }
            break;
         case "Weekly":
            for (int var11 = var2.getRealDataIndexStartW(); var11 <= var2.getRealDataIndexEndW(); var11++) {
               if (var2.OpenW[var11] == 0.0F) {
                  if (!(var3 < 0.0F)) {
                     var2.OpenW[var11] = var3;
                     var2.HighW[var11] = var4;
                     var2.LowW[var11] = var5;
                     var2.CloseW[var11] = var6;
                     var2.VolumeW[var11] = var7;
                  }
               } else {
                  var3 = var2.OpenW[var11];
                  var4 = var2.HighW[var11];
                  var5 = var2.LowW[var11];
                  var6 = var2.CloseW[var11];
                  var7 = var2.VolumeW[var11];
               }
            }
            break;
         case "Monthly":
            for (int var10 = var2.getRealDataIndexStartM(); var10 <= var2.getRealDataIndexEndM(); var10++) {
               if (var2.OpenM[var10] == 0.0F) {
                  if (!(var3 < 0.0F)) {
                     var2.OpenM[var10] = var3;
                     var2.HighM[var10] = var4;
                     var2.LowM[var10] = var5;
                     var2.CloseM[var10] = var6;
                     var2.VolumeM[var10] = var7;
                  }
               } else {
                  var3 = var2.OpenM[var10];
                  var4 = var2.HighM[var10];
                  var5 = var2.LowM[var10];
                  var6 = var2.CloseM[var10];
                  var7 = var2.VolumeM[var10];
               }
            }
      }
   }
}
