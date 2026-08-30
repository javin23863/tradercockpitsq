package com.strategyquant.plugin.Connection.impl.MT4;

import com.strategyquant.datalib.TickEvent;
import com.strategyquant.tradinglib.ILiveOrder;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.event.ITradingEventListener;
import com.strategyquant.tradinglib.event.TradingEventObj;
import com.strategyquant.tradinglib.event.TradingEventsArray;
import com.strategyquant.tradinglib.execution.IExecutionEngine;
import com.strategyquant.tradinglib.strategy.LiveOrderObj;
import it.unimi.dsi.fastutil.longs.Long2FloatRBTreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MT4ExecutionEngine implements IExecutionEngine {
   public static final Logger Log = LoggerFactory.getLogger("MT4ExecutionEngine");
   private MT4Connection mt4Connection;
   private MT4Bridge mt4Bridge;
   private TradingEventsArray tradingEventsArray = new TradingEventsArray();

   public MT4ExecutionEngine(MT4Connection var1, MT4Bridge var2) {
      this.mt4Connection = var1;
      this.mt4Bridge = var2;
   }

   public void initialize() {
   }

   public ILiveOrder orderOpen(StrategyBase var1, ILiveOrder var2, int var3, ITradingEventListener var4) {
      LiveOrderObj var5 = (LiveOrderObj)var2;
      var5.setWorkingStatus((byte)-20);
      if (var3 == 0) {
         var5.setWorkingStatus((byte)-30);
         this.sendEvent(var1, var4, 1, var2);
         return var2;
      } else {
         this.sendEvent(var1, var4, 2, var2);
         return var2;
      }
   }

   public ILiveOrder orderClose(StrategyBase var1, ILiveOrder var2, byte var3, int var4, ITradingEventListener var5) {
      return null;
   }

   public ILiveOrder orderModify(StrategyBase var1, ILiveOrder var2, int var3, ITradingEventListener var4) {
      return null;
   }

   private void sendEvent(StrategyBase var1, ITradingEventListener var2, int var3, ILiveOrder var4) {
      TradingEventObj var5 = this.tradingEventsArray.getAvailableEventObject();
      var5.setEventType(var3);
      var5.setOrder(var4);

      try {
         var1.OnEvent(var5);
      } catch (Exception var8) {
         Log.error("Exception during strategy.onEvent()", var8);
      }

      if (var2 != null) {
         try {
            var2.OnTradingEvent(var5);
         } catch (Exception var7) {
            Log.error("Exception during OrderEventListener.onTradingEvent()", var7);
         }
      }

      var5.releaseEvent();
   }

   public int getOpenOrdersCount(boolean var1) {
      return 0;
   }

   public double getAccountBalance() {
      return 0.0;
   }

   public double getAccountEquity() {
      return 0.0;
   }

   public double getTotalPL() {
      return 0.0;
   }

   public String getMainCurrency() {
      return "USD";
   }

   public OrdersList getHistoryOrders() {
      return null;
   }

   public ILiveOrder getOpenOrder(int var1, boolean var2) {
      return null;
   }

   public double getInitialBalance() {
      return 0.0;
   }

   public TickEvent getTickData() {
      return null;
   }

   public int getHistoryOrdersCount() {
      return 0;
   }

   public Order getHistoryOrder(int var1) {
      return null;
   }

   public boolean fillAtRealAskBidPrice() {
      return false;
   }

   public Long2FloatRBTreeMap getWorstDailyEquity() {
      return null;
   }

   public int getAmbiguousTrades() {
      return 0;
   }

   public void reuseOpenOrder(LiveOrderObj var1) {
   }

   public void evaluateActionListeners(int var1, StrategyBase var2) {
   }

   public boolean supportsDuplicateTrades() {
      return true;
   }

   public boolean IsMarketOpen() {
      return true;
   }
}
