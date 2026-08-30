package com.strategyquant.tradinglib.simulator.impl;

import com.strategyquant.datalib.InstrumentInfo;
import com.strategyquant.datalib.TickEvent;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.datalib.ticksimulator.ITickSimulator;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.ILiveOrder;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.Trader;
import com.strategyquant.tradinglib.execution.ReservedBarsType;
import com.strategyquant.tradinglib.setup.BarUpdateInfo;
import com.strategyquant.tradinglib.simulator.ITradingSimulator;
import com.strategyquant.tradinglib.strategy.LiveOrderObj;
import com.strategyquant.tradinglib.strategy.LiveOrdersCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractTradingSimulator implements ITradingSimulator {
   public static final Logger Log = LoggerFactory.getLogger("AbstractTradingSimulator");
   protected ReservedBarsType reservedBarsType;
   protected int backtestPrecision = 1;
   protected ChartData mainChartData;
   protected TickEvent tickData;
   protected boolean storePendingOrders = false;
   protected int tradingSymbolHash;
   protected int ambiguousTrades;
   protected LiveOrderObj[] liveOrders = null;
   protected int liveOrdersCount = 0;
   protected boolean realisticGapsHandling = false;
   protected double closingPrice = Double.MIN_VALUE;
   protected boolean isNewBar = false;
   private ITickSimulator tickSimulator;
   private LiveOrdersCache liveOrdersCache = new LiveOrdersCache();

   public AbstractTradingSimulator(ReservedBarsType var1, ITickSimulator var2) {
      this.reservedBarsType = var1;
      this.tickSimulator = var2;
   }

   @Override
   public abstract ITradingSimulator clone();

   @Override
   public void initialize() {
   }

   public abstract void eventBarUpdated(int var1);

   protected abstract double getSLPTFilledPrice(double var1, double var3);

   @Override
   public ITickSimulator getTickSimulator() {
      return this.tickSimulator;
   }

   @Override
   public ReservedBarsType getReservedBarsType() {
      return this.reservedBarsType;
   }

   @Override
   public void setTestPrecision(int var1) {
      this.backtestPrecision = var1;
   }

   @Override
   public int getTestPrecision() {
      return this.backtestPrecision;
   }

   @Override
   public int getAmbiguousTrades() {
      return this.ambiguousTrades;
   }

   @Override
   public void eventNewTick(TickEvent var1, double var2, double var4, BarUpdateInfo var6) throws TradingException {
      this.eventNewTick(var1, var2, var4);
   }

   @Override
   public boolean getApplyExitsAtTheEndOfRule() {
      return false;
   }

   public LiveOrderObj createLiveOrder(Trader var1, InstrumentInfo var2, byte var3, String var4) {
      return this.liveOrdersCache.createLiveOrder(var1, var2, var3, var4);
   }

   public LiveOrderObj createLiveOrder(Trader var1, InstrumentInfo var2, byte var3, String var4, double var5) {
      return this.liveOrdersCache.createLiveOrder(var1, var2, var3, var4, var5);
   }

   protected boolean removeOpenTrade(LiveOrderObj var1) {
      return this.removeOpenTrade(var1, true);
   }

   protected boolean removeOpenTrade(LiveOrderObj var1, boolean var2) {
      boolean var3 = false;

      for (int var4 = 0; var4 < this.liveOrdersCount; var4++) {
         if (this.liveOrders[var4] == var1) {
            this.removeOpenTradeNoFix(var4);
            var3 = true;
            break;
         }
      }

      if (var2 && var3) {
         this.fixRemovedElements();
      }

      return var3;
   }

   protected void reuseTemporaryOrder(LiveOrderObj var1) {
      this.liveOrdersCache.returnObject(var1);
   }

   protected void removeOpenTradeNoFix(int var1) {
      if (var1 >= 0 && var1 < this.liveOrdersCount) {
         this.liveOrdersCache.returnObject(this.liveOrders[var1]);
         this.liveOrders[var1] = null;
      }
   }

   protected void fixRemovedElements() {
      int var1 = -1;

      for (int var2 = 0; var2 < this.liveOrdersCount; var2++) {
         if (this.liveOrders[var2] != null) {
            this.liveOrders[++var1] = this.liveOrders[var2];
         }
      }

      this.liveOrdersCount = var1 == -1 ? 0 : var1 + 1;
   }

   @Override
   public int getOpenOrdersCount(boolean var1) {
      return this.liveOrdersCount;
   }

   @Override
   public ILiveOrder getOpenOrder(int var1, boolean var2) {
      if (var1 >= 0 && var1 < this.liveOrdersCount) {
         return this.liveOrders[var1];
      } else {
         throw new IllegalArgumentException("Trade index is out of range!");
      }
   }

   @Override
   public void reuseOpenOrder(LiveOrderObj var1) {
      Log.info("Returning unsuccess trade");
      this.removeOpenTrade(var1);
   }

   protected double priceLevelReached(TickEvent var1, int var2, int var3, double var4) {
      if (var1.getTime() == -1L) {
         return 0.0;
      }

      if (var3 == 6) {
         if (var1.getBid() <= var4) {
            return var1.getBid();
         }
      } else if (var3 == 5) {
         if (var1.getAsk() >= var4) {
            return var1.getAsk();
         }
      } else if (var3 == 4) {
         if (var1.getBid() >= var4) {
            return var1.getBid();
         }
      } else if (var3 == 3 && var1.getAsk() <= var4) {
         return var1.getAsk();
      }

      return 0.0;
   }

   protected double getRealisticSLPTFilledPrice(double var1, double var3, TickEvent var5, int var6) {
      if (this.realisticGapsHandling && this.isNewBar) {
         double var7 = var5.getBid();
         if (var7 != this.closingPrice && this.isNewBarOnMainSymbolTF(this.tickData)) {
            if (var6 == 5 || var6 == 6) {
               Log.debug("GAP detected - prev Close:{}, new Open: {}, realistic fill for MT4 set", this.closingPrice, var7);
               return getPriceByType(var6, this.tickData.getBid(), this.tickData.getAsk());
            }

            if (var6 == 3 || var6 == 4) {
               Log.debug("GAP detected for Limit - prev Close:{}, new Open: {}, realistic fill for MT4 set", this.closingPrice, var7);
               double var9 = getPriceByType(var6, this.tickData.getBid(), this.tickData.getAsk());
               double var11 = this.getSLPTFilledPrice(var1, var3);
               if (var6 == 3) {
                  return Math.min(var9, var11);
               }

               if (var6 == 4) {
                  return Math.max(var9, var11);
               }
            }
         }
      }

      return this.getSLPTFilledPrice(var1, var3);
   }

   public static double getPriceByType(int var0, double var1, double var3) {
      return var0 != 1 && var0 != 5 && var0 != 3 && var0 != 7 ? var1 : var3;
   }

   protected boolean isNewBarOnMainSymbolTF(TickEvent var1) {
      return this.mainChartData.isNewBarOnMainSymbolTF(var1);
   }

   protected boolean thereWasAGap(byte var1, TickEvent var2) {
      if (this.realisticGapsHandling && this.isNewBar && (var1 == 5 || var1 == 6)) {
         double var3 = var2.getBid();
         if (var3 != this.closingPrice && this.isNewBarOnMainSymbolTF(this.tickData)) {
            return true;
         }
      }

      return false;
   }

   protected void fixMAEMFEByRealPrice(LiveOrderObj var1) {
      double var2;
      if (var1.isLong()) {
         var2 = var1.getClosePrice() - var1.getOpenPrice();
      } else {
         var2 = var1.getOpenPrice() - var1.getClosePrice();
      }

      boolean var4 = var2 > 0.0;
      if (var4 && var1.getMFE() < var2) {
         var1.setMFE((float)var2);
      }

      var2 *= -1.0;
      if (!var4 && var1.getMAE() < var2) {
         var1.setMAE((float)var2);
      }
   }

   protected double getATR(StrategyBase var1, String var2) throws TradingException {
      ChartData var3 = var1.MarketData.Chart(var2);
      return var1.getATRValue(var3, 14, 0);
   }
}
