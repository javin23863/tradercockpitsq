package com.strategyquant.tradinglib.engine;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.datalib.data.DataException;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.ChartSetup;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.Trader;
import com.strategyquant.tradinglib.connection.Connection;
import com.strategyquant.tradinglib.connection.ConnectionManager;
import com.strategyquant.tradinglib.execution.IExecutionEngine;
import com.strategyquant.tradinglib.setup.Portfolio;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TradingEngine {
   public static final Logger Log = LoggerFactory.getLogger("TradingEngine");
   public static final int TYPE_LIVETRADING = 0;
   public static final int TYPE_BACKTEST = 1;
   private static TradingEngine instance = null;
   private Portfolio portfolio = new Portfolio();
   private ConnectionManager connectionManager;

   public static void init(TradingEngine var0) throws Exception {
      if (instance != null) {
         throw new Exception("TradingEngine.init() called more than once!");
      }

      instance = var0;
   }

   public static TradingEngine getLiveEngine() {
      return instance;
   }

   public TradingEngine(ConnectionManager var1) {
      this.connectionManager = var1;
   }

   public TradingSetup addSetup(ChartSetup var1, SettingsMap var2, StrategyBase var3, String... var4) throws Exception {
      var1 = var1.getClone();
      if (!var1.isCreatedUsingBackload()) {
         throw new TradingException(
            "You have to create Chart Setup with constructor that has backloadType and backloadNumber, not with constructor that uses date from - to !"
         );
      }

      String var5;
      if (var4.length == 0) {
         var4 = new String[]{var1.getCharts().get(0).getConnectionName()};
         var5 = var4[0];
      } else {
         var5 = var4[0];
      }

      var3.setTradeControllers(var5, this.getTradeControllers(var4));
      TradingSetup var6 = new TradingSetup(this, -659455871, var1, var2, var3);
      this.portfolio.add(var6);
      return var6;
   }

   private Trader[] getTradeControllers(String[] var1) throws DataException {
      Trader[] var2 = new Trader[var1.length];

      for (int var3 = 0; var3 < var1.length; var3++) {
         String var4 = var1[var3];
         var2[var3] = new Trader(var4, this.getExecutionEngine(var4));
      }

      return var2;
   }

   private IExecutionEngine getExecutionEngine(String var1) throws DataException {
      Connection var2 = this.getConnectionManager().getConnection(var1);
      if (!var2.isExecutionEngine()) {
         throw new DataException(1, "Connection '" + var2.getConnectionName() + "' is not an execution engine !");
      } else if (!(var2 instanceof IExecutionEngine)) {
         throw new DataException(1, "Connection '" + var2.getConnectionName() + "' doesn't implement an execution engine !");
      } else {
         return (IExecutionEngine)var2;
      }
   }

   public void deinitialize() {
   }

   public ConnectionManager getConnectionManager() {
      return this.connectionManager;
   }

   public ArrayList<TradingSetup> getTradingSetups() {
      return this.portfolio.getTradingSetups();
   }

   public void remove(TradingSetup var1) throws Exception {
      var1.stop();
      this.portfolio.remove(var1);
   }

   public void removeAll() throws Exception {
      for (TradingSetup var2 : this.portfolio.getTradingSetups()) {
         var2.stop();
      }

      this.portfolio.clear();
   }

   public TradingSetup getSetupById(int var1) {
      for (TradingSetup var3 : this.portfolio.getTradingSetups()) {
         if (var3.getSetupId() == var1) {
            return var3;
         }
      }

      return null;
   }
}
