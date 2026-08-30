package com.strategyquant.tradinglib.simulator.impl;

import com.strategyquant.datalib.TickEvent;
import com.strategyquant.tradinglib.ChartData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MarketPriceHolder {
   public static final Logger Log = LoggerFactory.getLogger("AbstractTradingSimulator");
   private MarketPriceHolder.TwoTickEvents[] tickData = new MarketPriceHolder.TwoTickEvents[4];
   private TickEvent oneSymbolCurrentTick = new TickEvent();
   private int oneSymbolHash;
   private ChartData oneSymbolChartData;
   private int symbolsCount = 0;

   public void registerNewSymbol(ChartData var1) {
      int var2 = var1.getSymbolHash();
      if (!this.containsSymbol(var2)) {
         if (this.symbolsCount == this.tickData.length - 1) {
            this.resizeArray();
         }

         this.tickData[this.symbolsCount] = new MarketPriceHolder.TwoTickEvents(var2, var1);
         this.symbolsCount++;
         if (this.symbolsCount == 1) {
            this.oneSymbolHash = var2;
            this.oneSymbolChartData = var1;
         }
      }
   }

   private void resizeArray() {
      MarketPriceHolder.TwoTickEvents[] var1 = new MarketPriceHolder.TwoTickEvents[this.tickData.length + 5];

      for (int var2 = 0; var2 < this.tickData.length; var2++) {
         var1[var2] = this.tickData[var2];
      }

      this.tickData = var1;
   }

   public boolean containsSymbol(int var1) {
      if (this.symbolsCount == 0) {
         return false;
      }

      if (this.symbolsCount == 1) {
         return this.oneSymbolHash == var1;
      }

      for (int var2 = 0; var2 < this.symbolsCount; var2++) {
         if (this.tickData[var2].symbolHash == var1) {
            return true;
         }
      }

      return false;
   }

   public TickEvent setNewTick(int var1, TickEvent var2) {
      if (this.symbolsCount == 0) {
         return null;
      }

      if (this.symbolsCount == 1) {
         if (this.oneSymbolHash != var1) {
            return null;
         }

         this.oneSymbolCurrentTick.copyValues(var2);
         return this.oneSymbolCurrentTick;
      } else {
         for (int var3 = 0; var3 < this.symbolsCount; var3++) {
            if (this.tickData[var3].symbolHash == var1) {
               this.tickData[var3].setNewTick(var2);
               return this.tickData[var3].current;
            }
         }

         return null;
      }
   }

   public TickEvent getCurrentTick(int var1) {
      if (this.symbolsCount == 1 && this.oneSymbolHash == var1) {
         return this.oneSymbolCurrentTick;
      }

      MarketPriceHolder.TwoTickEvents var2 = this.getTickData(var1);
      return var2.current;
   }

   private MarketPriceHolder.TwoTickEvents getTickData(int var1) {
      if (this.symbolsCount == 0) {
         throw new IllegalArgumentException("Symbol with this hash doesn't exist (1)!");
      }

      if (this.symbolsCount == 1) {
         if (this.tickData[0].symbolHash != var1) {
            throw new IllegalArgumentException("Symbol with this hash doesn't exist (2)!");
         } else {
            return this.tickData[0];
         }
      } else {
         for (int var2 = 0; var2 < this.symbolsCount; var2++) {
            if (this.tickData[var2].symbolHash == var1) {
               return this.tickData[var2];
            }
         }

         throw new IllegalArgumentException("Symbol with this hash doesn't exist (2)!");
      }
   }

   private boolean notInitialized(TickEvent var1) {
      return var1.getTime() == -1L;
   }

   public ChartData getChartData(int var1) {
      if (this.symbolsCount == 1 && this.oneSymbolHash == var1) {
         return this.oneSymbolChartData;
      }

      MarketPriceHolder.TwoTickEvents var2 = this.getTickData(var1);
      return var2.chartData;
   }

   class TwoTickEvents {
      ChartData chartData;
      TickEvent current = new TickEvent();
      int symbolHash;

      public TwoTickEvents(int nullx, ChartData nullxx) {
         this.chartData = nullxx;
         this.symbolHash = nullx;
      }

      public void setNewTick(TickEvent var1) {
         this.current.copyValues(var1);
      }
   }
}
