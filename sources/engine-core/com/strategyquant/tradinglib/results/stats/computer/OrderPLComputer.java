package com.strategyquant.tradinglib.results.stats.computer;

import com.strategyquant.datalib.InstrumentInfo;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.results.SymbolInfo;
import com.strategyquant.tradinglib.results.SymbolsMap;
import java.io.Serializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrderPLComputer implements IOrderComputer, Serializable {
   public static final Logger Log = LoggerFactory.getLogger("OrderPLComputer");

   @Override
   public void compute(OrdersList var1, SettingsMap var2, SymbolsMap var3) throws Exception {
      double var4 = var2.getDouble("MoneyManagement.InitialCapital", 10000.0);
      double var6 = var4;
      boolean var8 = var2.getBoolean("AddCommissionSwapToPL", false);
      float var9 = 0.0F;
      InstrumentInfo var10 = var3.getDefaultInstrumentInfo();

      for (int var13 = 0; var13 < var1.size(); var13++) {
         Order var14 = var1.get(var13);
         InstrumentInfo var11;
         if (var10 != null) {
            var11 = var10;
         } else {
            SymbolInfo var12 = var3.get(var14.Symbol);
            if (var12 == null) {
               throw new Exception("Symbol info data not found for symbol: '" + var14.Symbol + "' for order " + var14.toString());
            }

            var11 = var12.InstrumentInfo();
         }

         if (var14.Type != 9 && var14.Type != 10) {
            var14.PipsPL = (float)this.computeOrderPLInTicks(var14, var11.tickSize);
         } else {
            var14.PipsPL = 0.0F;
         }

         if (var14.Type != 9 && var14.Type != 10) {
            var14.PL = (float)this.computeOrderPLInMoney(var14, var11.tickSize, var11.pointValue);
            var14.CommSwapApplied = false;
         }

         if (var14.PL == -1.0E8F) {
            var14.PL = 0.0F;
         }

         if (var8 && !var14.CommSwapApplied) {
            var14.PL = var14.PL + var14.CommSwap;
            var14.PL = var14.PL - var14.SlippageInMoney;
            var14.CommSwapApplied = true;
         }

         var14.PctPL = (float)getPercentagePL(var14.PL, var4);
         if (var14.PL > 0.0F && var14.PctPL < 0.0F) {
            var14.PctPL = -var14.PctPL;
         }

         if (var14.PL < 0.0F && var14.PctPL > 0.0F) {
            var14.PctPL = -var14.PctPL;
         }

         var6 += var14.PL;
         var14.AccountBalance = (float)var6;
         var9 += var14.PctPL;
         var14.PctAccountBalance = var9;
         long var15 = (var14.CloseTime - var14.OpenTime) / 1000L;
         var14.Duration = (int)(var15 < 2147483647L ? var15 : 2147483647L);
         if (var11 != null) {
            if (var14.PipsMAE != 0.0F) {
               var14.MAE = (float)(var14.Size * var14.PipsMAE * var11.pointValue * var11.tickSize);
            }

            if (var14.PipsMFE != 0.0F) {
               var14.MFE = (float)(var14.Size * var14.PipsMFE * var11.pointValue * var11.tickSize);
            }
         }
      }

      this.endCompute(var1, var2, var3);
   }

   public double computeOrderPLInMoney(Order var1, double var2, double var4) {
      return var1.PipsPL * var1.Size * var4 * var2;
   }

   public double computeOrderPLInTicks(Order var1, double var2) {
      if (var1.Type == 1) {
         return SQUtils.round2((var1.ClosePrice - var1.OpenPrice) / var2);
      } else {
         return var1.Type == 2 ? SQUtils.round2((var1.OpenPrice - var1.ClosePrice) / var2) : 0.0;
      }
   }

   public static double getPercentagePL(double var0, double var2) {
      return var2 == 0.0 ? 0.0 : 100.0 * var0 / var2;
   }

   private void endCompute(OrdersList var1, SettingsMap var2, SymbolsMap var3) throws Exception {
      float var4 = 1.0F;
      float var5 = 0.0F;
      float var6 = 0.0F;

      for (int var7 = 0; var7 < var1.size(); var7++) {
         Order var8 = var1.get(var7);
         if (var8.Type != 9 && var8.Type != 10) {
            float var9 = 1.0F + var8.PctPL / 100.0F;
            float var10 = var9 * var4;
            var4 = var10;
            var6 = (var10 - 1.0F) * 100.0F;
            var8.PctPL_TWR = var6 - var5;
            var5 = var6;
            var8.PL = (float)SQUtils.round2(var8.PL);
            var8.PctPL_TWR = var8.PctPL_TWR;
            var8.PipsPL = (float)SQUtils.round2(var8.PipsPL);
         }
      }
   }
}
