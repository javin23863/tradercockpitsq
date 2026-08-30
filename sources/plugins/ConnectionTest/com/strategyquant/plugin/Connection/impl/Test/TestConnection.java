package com.strategyquant.plugin.Connection.impl.Test;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.datalib.TickEvent;
import com.strategyquant.datalib.data.IDataBuffer;
import com.strategyquant.lib.ValuesMap;
import com.strategyquant.tradinglib.ILiveOrder;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.connection.Connection;
import com.strategyquant.tradinglib.connection.ConnectionManager;
import com.strategyquant.tradinglib.connection.HistoryData;
import com.strategyquant.tradinglib.connection.IDataFeed;
import com.strategyquant.tradinglib.event.ITradingEventListener;
import com.strategyquant.tradinglib.execution.IExecutionEngine;
import com.strategyquant.tradinglib.plugindef.connection.IConnectionPlugin;
import com.strategyquant.tradinglib.strategy.LiveOrderObj;
import it.unimi.dsi.fastutil.longs.Long2FloatRBTreeMap;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestConnection extends Connection implements IDataFeed, IExecutionEngine {
   public static final Logger Log = LoggerFactory.getLogger("TestConnection");
   private HashMap<String, Integer> symbolsMap = new HashMap<>();

   public TestConnection(IConnectionPlugin var1, ConnectionManager var2, String var3, ValuesMap var4) throws Exception {
      super(var2, var3, var1, var4);
   }

   public void initializeEngines() {
      this.setDataFeed(this);
      this.setExecutionEngine(this);
   }

   public void connect() throws Exception {
   }

   public void disconnect() throws Exception {
   }

   public void registerSymbol(String var1) {
      if (this.checkSymbolIsSupported(var1)) {
         if (!this.symbolsMap.containsKey(var1)) {
            this.symbolsMap.put(var1, var1.hashCode());
            this.startSendingTestData(var1.hashCode());
         }
      }
   }

   public boolean checkSymbolIsSupported(String var1) {
      String[] var2 = (String[])this.connParams.get("TestSymbols");

      for (int var3 = 0; var3 < var2.length; var3++) {
         if (var2[var3].equals(var1)) {
            return true;
         }
      }

      return false;
   }

   private void startSendingTestData(final int var1) {
      final IDataBuffer var2 = this.connectionManager.getDataBuffer();
      final TickEvent var3 = new TickEvent();
      (new Thread() {
         private int count = 0;

         @Override
         public void run() {
            while (true) {
               this.count++;
               var3.setSymbolHash(var1);
               var3.setConnectionHash(TestConnection.this.getConnectionHash());
               var3.setAsk(this.count);
               var3.setBid(this.count);
               var3.setIsSet(true);
               var2.put(var3);

               try {
                  Thread.sleep(500L);
               } catch (InterruptedException var2x) {
                  var2x.printStackTrace();
               }
            }
         }
      }).start();
   }

   public boolean isConnected() {
      return true;
   }

   public String getStatus() {
      return "Connected";
   }

   public HistoryData getHistoryData(ChartDef var1, int var2, long var3, long var5) {
      return null;
   }

   public void initialize() {
   }

   public ILiveOrder orderOpen(StrategyBase var1, ILiveOrder var2, int var3, ITradingEventListener var4) {
      return null;
   }

   public ILiveOrder orderClose(StrategyBase var1, ILiveOrder var2, byte var3, int var4, ITradingEventListener var5) {
      return null;
   }

   public ILiveOrder orderModify(StrategyBase var1, ILiveOrder var2, int var3, ITradingEventListener var4) {
      return null;
   }

   public int getOpenOrdersCount(boolean var1) {
      return 0;
   }

   public double getAccountBalance() {
      return 0.0;
   }

   public double getTotalPL() {
      return 0.0;
   }

   public String getMainCurrency() {
      return null;
   }

   public OrdersList getHistoryOrders() {
      return null;
   }

   public ILiveOrder getOpenOrder(int var1, boolean var2) {
      return null;
   }

   public double getAccountEquity() {
      return 0.0;
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
