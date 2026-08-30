package com.strategyquant.tradinglib.exit;

import com.strategyquant.datalib.InstrumentInfo;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.IFormula;
import com.strategyquant.tradinglib.ILiveOrder;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.simulator.Engines;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrailingStopHandler {
   public static final Logger Log = LoggerFactory.getLogger("TrailingStop");

   public static double checkTrailingStop(ILiveOrder var0, IFormula var1, IFormula var2, StrategyBase var3, double var4, boolean var6, byte var7) throws TradingException {
      if (var0.isClosedOrder()) {
         return var4;
      }

      if (!var0.isMarketOrder()) {
         return var4;
      }

      boolean var8 = var3.getEngine() == 56756755 || var3.getEngine() == 938213070;
      InstrumentInfo var9 = var3.MarketData.getInstrumentInfo(var0.getSymbol());
      double var10 = var0.isNettingMode() ? var0.getLastOpenPrice() : var0.getOpenPrice();
      double var12 = calculatePrice(var0, var1, var2, var3);
      if (var12 == -9.9999999E7) {
         return var4;
      }

      double var14 = 0.0;
      if (var2 != null) {
         var14 = var2.evaluateFormula(var3, var0.getSymbol(), 0.0, 1);
         var14 = var14 == -9.9999999E7 ? 0.0 : var14;
         if (var8) {
            var14 = SQUtils.adjustToTickSize(var14, var9.tickStep);
         } else {
            var14 = SQUtils.round(var14, var9.decimals);
         }
      }

      double var16 = var0.getSL() == -9.9999999E7 ? -9.9999999E7 : SQUtils.round(var0.getSL(), var9.decimals);
      if (var0.getDirection() == 1) {
         double var18 = SQUtils.round(var3.MarketData.Chart(var0.getSymbol()).Bid() - var10, var9.decimals);
         if (var18 >= var14 && (var16 == -9.9999999E7 || var16 < var12) && isTrailingStopValid(var3, var0, var12)) {
            var12 = SQUtils.fixPrice(var9.tickStep, var12, var9.decimals);
            if (Engines.isTradestationEngine(var3.getEngine())) {
               if (var12 > var4 || var4 == -9.9999999E7) {
                  if (var6) {
                     Log.info(
                        SQTime.toDateMinuteString(var3.Time(0)) + " - Order " + var0.getOrderId() + " - Moving Long Trailing stop to: {}, orig SL: {}",
                        var12,
                        var16
                     );
                  }

                  var0.setExitIndex(var7);
                  var0.setSL((byte)21, var12).Send();
                  var4 = var12;
               }
            } else if (var12 > var4 || var4 == -9.9999999E7) {
               if (var6) {
                  Log.info(
                     SQTime.toDateMinuteString(var3.Time(0)) + " - Order " + var0.getOrderId() + " - Moving Long Trailing stop to: {}, orig SL: {}",
                     var12,
                     var16
                  );
               }

               var0.setExitIndex(var7);
               var0.setSL((byte)21, var12).Send();
               var4 = var12;
            }
         }
      } else {
         double var24 = SQUtils.round(var10 - var3.MarketData.Chart(var0.getSymbol()).Ask(), var9.decimals);
         if (var24 >= var14 && (var16 == -9.9999999E7 || var16 > var12) && isTrailingStopValid(var3, var0, var12)) {
            var12 = SQUtils.fixPrice(var9.tickStep, var12);
            if (Engines.isTradestationEngine(var3.getEngine())) {
               if (var4 == -9.9999999E7 || var12 < var4) {
                  if (var6) {
                     Log.info(SQTime.toDateMinuteString(var3.Time(0)) + " - Order " + var0.getOrderId() + " - Moving Short Trailing stop to: {}", var12);
                  }

                  var0.setExitIndex(var7);
                  var0.setSL((byte)21, var12).Send();
                  var4 = var12;
               }
            } else if (var4 == -9.9999999E7 || var12 < var4) {
               if (var6) {
                  Log.info(SQTime.toDateMinuteString(var3.Time(0)) + " - Order " + var0.getOrderId() + " - Moving Short Trailing stop to: {}", var12);
               }

               var0.setExitIndex(var7);
               var0.setSL((byte)21, var12).Send();
            }
         }
      }

      return var4;
   }

   private static boolean isTrailingStopValid(StrategyBase var0, ILiveOrder var1, double var2) throws TradingException {
      double var4 = var0.getMinDistance() * var0.MarketData.getInstrumentInfo(var1.getSymbol()).tickSize;
      if (var1.isLong()) {
         if (var2 < var0.MarketData.Chart(var1.getSymbol()).Bid() - var4) {
            return true;
         }
      } else if (var2 > var0.MarketData.Chart(var1.getSymbol()).Ask() + var4) {
         return true;
      }

      return false;
   }

   public static boolean setExit(ILiveOrder var0, StrategyBase var1, double var2, boolean var4) throws TradingException {
      if (var2 == -9.9999999E7) {
         return false;
      }

      if (var4) {
         Log.info(SQTime.toDateMinuteString(var1.Time(0)) + " - Creating order for Trailing stop at: {}", var2);
      }

      int var5 = var0.isLong() ? -1 : 1;
      int var6 = var5 > 0 ? 100 : 102;
      ILiveOrder var7 = var1.Trader.Open((byte)var6, var0.getSymbol(), var2).setMagicNumber(var0.getMagicNumber()).setSLType((byte)21).Send();
      return true;
   }

   public static double calculatePrice(ILiveOrder var0, IFormula var1, IFormula var2, StrategyBase var3) throws TradingException {
      boolean var4 = var3.getEngine() == 56756755 || var3.getEngine() == 938213070;
      InstrumentInfo var5 = var3.MarketData.getInstrumentInfo(var0.getSymbol());
      double var6;
      if (var0.getDirection() == 1) {
         var6 = var3.MarketData.Chart(var0.getSymbol()).Bid();
      } else {
         var6 = var3.MarketData.Chart(var0.getSymbol()).Ask();
      }

      double var8 = var1.evaluateFormula(var3, var0.getSymbol(), var6, var0.getDirection());
      if (var8 == -9.9999999E7) {
         return -9.9999999E7;
      }

      double var10 = 0.0;
      if (var4) {
         var10 = SQUtils.adjustToTickSize(var8, var5.tickStep);
      } else {
         var10 = SQUtils.round(var8, var5.decimals);
      }

      return var10;
   }
}
