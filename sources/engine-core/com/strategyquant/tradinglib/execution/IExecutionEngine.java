package com.strategyquant.tradinglib.execution;

import com.strategyquant.datalib.TickEvent;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.ILiveOrder;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.event.ITradingEventListener;
import com.strategyquant.tradinglib.strategy.LiveOrderObj;
import it.unimi.dsi.fastutil.longs.Long2FloatRBTreeMap;

public interface IExecutionEngine {
   void initialize();

   ILiveOrder orderOpen(StrategyBase var1, ILiveOrder var2, int var3, ITradingEventListener var4) throws TradingException;

   ILiveOrder orderClose(StrategyBase var1, ILiveOrder var2, byte var3, int var4, ITradingEventListener var5) throws TradingException;

   ILiveOrder orderModify(StrategyBase var1, ILiveOrder var2, int var3, ITradingEventListener var4) throws TradingException;

   OrdersList getHistoryOrders();

   int getOpenOrdersCount(boolean var1);

   ILiveOrder getOpenOrder(int var1, boolean var2);

   int getHistoryOrdersCount();

   Order getHistoryOrder(int var1);

   double getAccountBalance();

   double getAccountEquity();

   double getInitialBalance();

   double getTotalPL();

   String getMainCurrency();

   TickEvent getTickData();

   boolean fillAtRealAskBidPrice();

   Long2FloatRBTreeMap getWorstDailyEquity();

   int getAmbiguousTrades();

   void reuseOpenOrder(LiveOrderObj var1);

   void evaluateActionListeners(int var1, StrategyBase var2) throws TradingException;

   boolean supportsDuplicateTrades();

   boolean IsMarketOpen();
}
