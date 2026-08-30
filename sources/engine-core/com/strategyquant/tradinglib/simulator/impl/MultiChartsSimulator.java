package com.strategyquant.tradinglib.simulator.impl;

import com.strategyquant.datalib.TickEvent;
import com.strategyquant.tradinglib.OrderTypes;
import com.strategyquant.tradinglib.setup.BarUpdateInfo;
import com.strategyquant.tradinglib.simulator.ITradingSimulator;
import com.strategyquant.tradinglib.strategy.LiveOrderObj;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiChartsSimulator extends TradestationSimulator {
   public static final Logger Log = LoggerFactory.getLogger("MultiChartsSimulator");
   public static final Logger BacktestLog = LoggerFactory.getLogger("BacktestLog");
   public static final int Engine = 938213070;

   @Override
   public ITradingSimulator clone() {
      MultiChartsSimulator var1 = new MultiChartsSimulator();
      var1.setTestPrecision(this.getTestPrecision());
      return var1;
   }

   @Override
   protected void checkSLPT(TickEvent var1, int var2, long var3, double var5, double var7, double var9, BarUpdateInfo var11) {
      LiveOrderObj var12 = this.findExistingMarketOrder(-1, -1);
      if (var12 != null) {
         boolean var13 = false;
         boolean var14 = false;
         double var15 = 0.0;
         double var17 = 0.0;

         for (int var19 = 0; var19 < this.exitOrdersCount; var19++) {
            LiveOrderObj var20 = this.exitOrders[var19];
            if (var20 != null && var20.getSymbolHash() == var2) {
               if (!var20.isExitOrder()) {
                  throw new IllegalArgumentException("This shouldn't happen");
               }

               if (var20.getBarsInTrade() >= 1) {
                  byte var21 = var20.getOrderType();
                  if (var21 == 101) {
                     var15 = var20.getOpenPrice();
                  } else if (var21 == 103) {
                     var17 = var20.getOpenPrice();
                  }

                  this.removeClosingTradeNoFix(var19);
                  var14 = true;
               } else {
                  int var29 = var20.getMagicNumber();
                  if (var29 == 0 || var29 == var12.getMagicNumber()) {
                     boolean var22 = var20.getOrderType() == 102 || var20.getOrderType() == 100;
                     boolean var23 = var20.getOrderType() == 103 || var20.getOrderType() == 101;
                     if (this.useInitialSLPT && (var22 || var23) || var20.getOpenTime() >= var12.getOpenTime()) {
                        var20.setTickData(this.tickData.getAsk(), this.tickData.getBid());
                        double var24 = this.priceLevelReached(var1, var2, var20.getOrderType(), var20.getOpenPrice(), var11);
                        if (var24 > 0.0
                           && !this.stopLimitShouldBeFilledFirst(var1, var2, var20.getOrderType(), var20.getOpenPrice(), var11)
                           && !this.otherExitStopShouldBeFilledFirst(var1, var19, var2, var20.getOrderType(), var20.getOpenPrice(), var11)) {
                           byte var26 = var20.getOrderType();
                           double var27 = var1.getAsk() - var1.getBid();
                           if (var26 != 100 && var26 != 101) {
                              var24 = this.fixByHighLow(var24, var5, var7);
                           } else {
                              var24 = this.fixByHighLow(var24, var5 + var27, var7 + var27);
                           }

                           var24 = this.getStopLimitFillPriceMC(var24, var11, var20, var12, var1, var15, var17);
                           if (var11.eventType != 2) {
                              this.tradestationBouncingTicks(
                                 var1, this.bouncingTicksPct, var5, var7, var9, var2, var20.getOrderType(), var20.getInstrumentInfo(), var20.getOpenPrice()
                              );
                           }

                           this.lastFilledPriceThisBar = var24;
                           this.closeOrder(var12, var24, this.getOrderCloseType(var20.getOrderType()));
                           this.removeOpenTrade(var12, false);
                           var13 = true;
                           this.removeClosingTradeNoFix(var19);
                           var14 = true;
                           break;
                        }
                     }
                  }
               }
            }
         }

         if (var13) {
            this.fixRemovedElements();
         }

         if (var14) {
            this.fixRemovedClosingElements();
         }
      }
   }

   private double priceLevelReached(TickEvent var1, int var2, byte var3, double var4, BarUpdateInfo var6) {
      if (var1.getTime() == -1L) {
         return 0.0;
      }

      if (var3 == 6 || var3 == 102) {
         if (var1.getBid() <= var4) {
            if (var6 != null && var6.eventType == 2) {
               return var1.getBid();
            }

            return var4;
         }
      } else if (var3 == 5 || var3 == 100) {
         if (var1.getAsk() >= var4) {
            if (var6 != null && var6.eventType == 2) {
               return var1.getAsk();
            }

            return var4;
         }
      } else if (var3 == 4 || var3 == 103) {
         if (var1.getBid() >= var4) {
            if (var6 != null && var6.eventType == 2) {
               return var1.getBid();
            }

            return var4;
         }
      } else if ((var3 == 3 || var3 == 101) && var1.getAsk() <= var4) {
         if (var6 != null && var6.eventType == 2) {
            return var1.getAsk();
         }

         return var4;
      }

      return 0.0;
   }

   protected double getStopLimitFillPriceMC(double var1, BarUpdateInfo var3, LiveOrderObj var4, LiveOrderObj var5, TickEvent var6, double var7, double var9) {
      byte var11 = var4.getOrderType();
      if (var11 == 101) {
         return (var7 == 0.0 || var4.getOpenPrice() != var7) && var3 != null && var3.eventType == 2 ? var6.getAsk() : var1;
      }

      if (var11 == 103) {
         return (var9 == 0.0 || var4.getOpenPrice() != var9) && var3 != null && var3.eventType == 2 ? var6.getBid() : var1;
      }

      int var12 = var4.getDirection();
      if (this.lastFilledPriceThisBar <= 0.0) {
         return var1;
      }

      if (var12 > 0) {
         if ((OrderTypes.isStopOrder(var11) || OrderTypes.isLimitOrder(var11)) && this.lastFilledPriceThisBar > var1) {
            return this.lastFilledPriceThisBar;
         }

         if (OrderTypes.isLimitOrder(var11) && this.lastFilledPriceThisBar < var1) {
            return this.lastFilledPriceThisBar;
         }
      } else if (var12 < 0) {
         if ((OrderTypes.isStopOrder(var11) || OrderTypes.isLimitOrder(var11)) && this.lastFilledPriceThisBar < var1) {
            return this.lastFilledPriceThisBar;
         }

         if (OrderTypes.isLimitOrder(var11) && this.lastFilledPriceThisBar > var1) {
            return this.lastFilledPriceThisBar;
         }
      }

      return var1;
   }

   @Override
   public int getEngineId() {
      return 938213070;
   }
}
