package com.strategyquant.tradinglib;

import com.strategyquant.datalib.InstrumentInfo;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.event.ITradingEventListener;
import com.strategyquant.tradinglib.execution.IExecutionEngine;
import com.strategyquant.tradinglib.simulator.ITradingSimulator;
import com.strategyquant.tradinglib.simulator.impl.AbstractTradingSimulator;
import com.strategyquant.tradinglib.strategy.LiveOrderObj;
import com.strategyquant.tradinglib.strategy.MarketData;
import it.unimi.dsi.fastutil.longs.Long2FloatRBTreeMap;

public class Trader {
   private IExecutionEngine ee;
   private String name;
   private AbstractTradingSimulator tradingSimulator;
   private StrategyBase strategy;
   private SettingsMap settings;
   private MoneyManagementMethod moneyManagementMethod;
   private Order mmOrder;
   private RiskManagementMethod riskManagementMethod;
   private MarketData marketData;

   public Trader(String var1, IExecutionEngine var2) {
      this.name = var1;
      this.ee = var2;
      if (this.ee instanceof AbstractTradingSimulator) {
         this.tradingSimulator = (AbstractTradingSimulator)this.ee;
      }
   }

   public void setStrategy(StrategyBase var1) {
      this.strategy = var1;
   }

   public StrategyBase getStrategy() {
      return this.strategy;
   }

   public String getName() {
      return this.name;
   }

   public void initialize(MarketData var1) {
      this.marketData = var1;
      if (this.ee instanceof ITradingSimulator) {
         ((ITradingSimulator)this.ee).initialize(var1);
      }
   }

   public void eventBarUpdated(int var1) {
      if (this.tradingSimulator != null) {
         this.tradingSimulator.eventBarUpdated(var1);
      }
   }

   public ILiveOrder Open(byte var1, String var2) throws TradingException {
      InstrumentInfo var3;
      if (var2.equals("Current")) {
         var2 = this.marketData.Chart(0).Symbol;
         var3 = this.marketData.Chart(0).getInstrumentInfo();
      } else {
         var3 = this.marketData.getInstrumentInfo(var2);
      }

      return this.tradingSimulator.createLiveOrder(this, var3, var1, var2);
   }

   public ILiveOrder Open(byte var1, String var2, double var3) throws TradingException {
      InstrumentInfo var5;
      if (var2.equals("Current")) {
         var2 = this.marketData.Chart(0).Symbol;
         var5 = this.marketData.Chart(0).getInstrumentInfo();
      } else {
         var5 = this.marketData.getInstrumentInfo(var2);
      }

      return this.tradingSimulator.createLiveOrder(this, var5, var1, var2, var3);
   }

   public ILiveOrder Buy(String var1) throws TradingException {
      return this.Open((byte)1, var1);
   }

   public ILiveOrder Sell(String var1) throws TradingException {
      return this.Open((byte)2, var1, 0.0);
   }

   public ILiveOrder BuyLimit(String var1, double var2) throws TradingException {
      return this.Open((byte)3, var1, var2);
   }

   public ILiveOrder SellLimit(String var1, double var2) throws TradingException {
      return this.Open((byte)1, var1, var2);
   }

   public ILiveOrder BuyStop(String var1, double var2) throws TradingException {
      return this.Open((byte)5, var1, var2);
   }

   public ILiveOrder SellStop(String var1, double var2) throws TradingException {
      return this.Open((byte)6, var1, var2);
   }

   public ILiveOrder BuyStopLimit(String var1, double var2) throws TradingException {
      return this.Open((byte)7, var1, var2);
   }

   public ILiveOrder SellStopLimit(String var1, double var2) throws TradingException {
      return this.Open((byte)8, var1, var2);
   }

   public ILiveOrder send(ILiveOrder var1, byte var2, int var3, ITradingEventListener var4) throws TradingException {
      return this.send(var1, var2, (byte)0, var3, var4);
   }

   public ILiveOrder send(ILiveOrder var1, byte var2, byte var3, int var4, ITradingEventListener var5) throws TradingException {
      if (var1.getWorkingStatus() == -20) {
         return var1;
      } else if (var2 == 1) {
         var1.computeSizeIfMissing();
         return this.processOpen(var1, var3, var4, var5);
      } else if (var2 == 3) {
         return this.processModify(var1, var3, var4, var5);
      } else {
         return var2 == 2 ? this.processClose(var1, var3, var4, var5) : var1;
      }
   }

   private ILiveOrder processClose(ILiveOrder var1, byte var2, int var3, ITradingEventListener var4) throws TradingException {
      if (var1.getWorkingStatus() == -10) {
         return var1;
      } else if (var1.getWorkingStatus() == -20) {
         return var1;
      } else {
         return var1.getPositionStatus() != 1 && var1.getPositionStatus() != 2 ? var1 : this.ee.orderClose(this.strategy, var1, var2, var3, var4);
      }
   }

   private ILiveOrder processModify(ILiveOrder var1, byte var2, int var3, ITradingEventListener var4) throws TradingException {
      if (var1.getWorkingStatus() == -10) {
         return var1;
      } else {
         return !var1.isModified()
            ? ((LiveOrderObj)var1).setError(4, "You cannot call Modify when you don't set new SL or PT !")
            : this.ee.orderModify(this.strategy, var1, var3, var4);
      }
   }

   private ILiveOrder processOpen(ILiveOrder var1, byte var2, int var3, ITradingEventListener var4) throws TradingException {
      byte var5 = 100;
      byte var6 = var1.getOrderType();
      if (var1.getSize() == 0.0 && var6 != 100 && var6 != 102 && var6 != 101 && var6 != 103) {
         return ((LiveOrderObj)var1).setError(3, "Order has a zero size!");
      } else if (this.ee.getOpenOrdersCount(false) > var5) {
         return ((LiveOrderObj)var1)
            .setError(7, String.format("Order refused - there are too many open orders, maximum number of open orders allowed is %d", Integer.valueOf(var5)));
      } else if (var1.getWorkingStatus() != -10) {
         return ((LiveOrderObj)var1).setError(2, "You are using Send() on an order that was already placed or is no longer active!");
      } else if (!this.getStrategy().isTradestationEngine() && !OrderTypes.isMarketOrder(var1.getOrderType()) && var1.getOpenPrice() < 0.0) {
         return ((LiveOrderObj)var1).setError(3, "Pending order doesn't have price filled!");
      } else if (var1.isRefused()) {
         LiveOrderObj var7 = (LiveOrderObj)var1;
         var7.setWorkingStatus((byte)-40);
         this.ee.reuseOpenOrder(var7);
         return var7;
      } else {
         return this.ee.orderOpen(this.strategy, var1, var3, var4);
      }
   }

   private void checkRiskManagement(ILiveOrder var1) {
      if (this.riskManagementMethod != null) {
         this.riskManagementMethod.verifyOrder(this, var1);
      }
   }

   public ILiveOrder refuseOrder(ILiveOrder var1, String var2) {
      return var1.getWorkingStatus() == -10 ? ((LiveOrderObj)var1).setError(5, var2) : var1;
   }

   public OrdersList getHistoryOrders() {
      return this.ee.getHistoryOrders();
   }

   public Long2FloatRBTreeMap getWorstDailyEquity() {
      return this.ee.getWorstDailyEquity();
   }

   public int getHistoryOrdersCount() {
      return this.ee.getHistoryOrdersCount();
   }

   public Order getHistoryOrder(int var1) {
      return this.ee.getHistoryOrder(var1);
   }

   public double getAccountBalance() {
      return this.ee.getAccountBalance();
   }

   public double getAccountEquity() {
      return this.ee.getAccountEquity();
   }

   public double getInitialBalance() {
      return this.ee.getInitialBalance();
   }

   public String getStrategyName() {
      return this.strategy.getStrategyName();
   }

   public String getSetupName() {
      return this.strategy.getSetupName();
   }

   public void setSettings(SettingsMap var1) {
      this.settings = var1;
      if (var1.containsKey("MoneyManagement.Method")) {
         this.moneyManagementMethod = (MoneyManagementMethod)var1.get("MoneyManagement.Method");
         this.riskManagementMethod = (RiskManagementMethod)var1.get("RiskManagement");
      }
   }

   public MoneyManagementMethod getMoneyManagementMethod() {
      return this.moneyManagementMethod;
   }

   public int getOpenOrdersCount(boolean var1) {
      return this.ee.getOpenOrdersCount(var1);
   }

   public ILiveOrder getOpenOrder(int var1, boolean var2) {
      return this.ee.getOpenOrder(var1, var2);
   }

   public void destroy() {
      if (this.ee != null && this.ee instanceof ITradingSimulator) {
         ((ITradingSimulator)this.ee).destroy();
         this.ee = null;
      }
   }

   public boolean fillAtRealAskBidPrice() {
      return this.ee.fillAtRealAskBidPrice();
   }

   public int getAmbiguousTrades() {
      return this.ee.getAmbiguousTrades();
   }

   public void evaluateActionListeners(int var1, StrategyBase var2) throws TradingException {
      this.ee.evaluateActionListeners(var1, var2);
   }

   public boolean supportsDuplicateTrades() {
      return this.ee.supportsDuplicateTrades();
   }

   public boolean IsMarketOpen() {
      return this.ee.IsMarketOpen();
   }
}
