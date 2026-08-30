package com.strategyquant.tradinglib.engine.stockpicker.data;

import com.strategyquant.datalib.TickEvent;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.datalib.data.io.VersatileData;
import com.strategyquant.tradinglib.engine.stockpicker.data.symbols.LoadedSymbolData;
import com.strategyquant.tradinglib.robustnesstests.DataModifierCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PickerDataModifier {
   public static final Logger Log = LoggerFactory.getLogger(PickerDataModifier.class);

   public void modifyData(LoadedPickerData var1, DataModifierCallback var2, long var3, long var5) throws TradingException {
      if (var2 != null) {
         try {
            String[] var7 = var1.stockGroupData.getSymbols().toArray(new String[var1.stockGroupData.getSymbols().size()]);

            for (int var8 = 0; var8 < var7.length; var8++) {
               String var9 = var7[var8];
               LoadedSymbolData var10 = var1.stockGroupData.getSymbolData(var9);
               if (var10 != null) {
                  this.modifySymbolData(var1, var9, var10, var2, var3, var5);
               }
            }
         } catch (Exception var11) {
            Log.error("Error modifying data", var11);
            throw new TradingException("Error modifying data", var11);
         }
      }
   }

   private void modifySymbolData(LoadedPickerData var1, String var2, LoadedSymbolData var3, DataModifierCallback var4, long var5, long var7) throws TradingException {
      VersatileData var9 = new VersatileData();
      TickEvent var10 = new TickEvent();
      long var11 = var1.timeline.indexToTimeD(var3.getRealDataIndexStartD());
      long var13 = var1.timeline.indexToTimeD(var3.getRealDataIndexEndD());
      if (var11 > var5) {
         var5 = var11;
      }

      if (var13 < var7) {
         var7 = var13;
      }

      int var15 = var1.timeline.searchIndexByTimeD(var5, true);
      int var16 = var1.timeline.searchIndexByTimeD(var7, false);
      double var17 = 0.0;

      for (int var19 = var15; var19 <= var16; var19++) {
         long var20 = var1.timeline.indexToTimeD(var19);
         var17 = this.computeATROnBar(var3.OpenD, var3.HighD, var3.LowD, var3.CloseD, var19);
         this.modifyOHLCData(var3.OpenD, var3.HighD, var3.LowD, var3.CloseD, var3.VolumeD, var9, var19, var20, var4);
         this.modifyTickData(var3.OpenD, var10, var19, var20, var4, var17);
         this.modifyTickData(var3.HighD, var10, var19, var20, var4, var17);
         this.modifyTickData(var3.LowD, var10, var19, var20, var4, var17);
         this.modifyTickData(var3.CloseD, var10, var19, var20, var4, var17);
         this.modifyTickData(var3.VolumeD, var10, var19, var20, var4, var17);
      }
   }

   private void modifyOHLCData(
      float[] var1, float[] var2, float[] var3, float[] var4, float[] var5, VersatileData var6, int var7, long var8, DataModifierCallback var10
   ) throws TradingException {
      var6.time = var8;
      var6.open = var1[var7];
      var6.high = var2[var7];
      var6.low = var3[var7];
      var6.close = var4[var7];
      var6.volume = var5[var7];
      var10.modifyOHLCData(var6);
      var1[var7] = (float)var6.open;
      var2[var7] = (float)var6.high;
      var3[var7] = (float)var6.low;
      var4[var7] = (float)var6.close;
      var5[var7] = (float)var6.volume;
   }

   private void modifyTickData(float[] var1, TickEvent var2, int var3, long var4, DataModifierCallback var6, double var7) throws TradingException {
      var2.setTime(var4);
      var2.setAsk(var1[var3]);
      var2.setBid(var1[var3]);
      var6.modifyData(var2, var7);
      var1[var3] = (float)var2.getAsk();
   }

   private double computeATROnBar(float[] var1, float[] var2, float[] var3, float[] var4, int var5) throws TradingException {
      byte var6 = 14;
      if (var5 - var6 + 1 < 0) {
         return 0.0;
      }

      double var7 = 0.0;

      for (int var9 = var5 - var6 + 1; var9 <= var5; var9++) {
         double var10 = var2[var9] - var3[var9];
         double var12 = var9 - 1 >= 0 ? Math.abs(var2[var9] - var4[var9 - 1]) : 0.0;
         double var14 = var9 - 1 >= 0 ? Math.abs(var3[var9] - var4[var9 - 1]) : 0.0;
         double var16 = Math.max(var10, Math.max(var12, var14));
         var7 += var16;
      }

      return var7 / var6;
   }
}
