package com.strategyquant.tradinglib.backtest;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.datalib.data.DataException;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.tradinglib.connection.Connection;
import com.strategyquant.tradinglib.connection.HistoryData;
import com.strategyquant.tradinglib.connection.IDataFeed;

public class BacktestConnection extends Connection implements IDataFeed {
   private BacktestDataFeed backtestDataFeed;

   public BacktestConnection(String var1, BacktestDataFeed var2) throws DataException {
      super(null, var1, null, null);
      this.backtestDataFeed = var2;
      if (!DataManager.checkConnectionExists(var1)) {
         throw new DataException(1, "Connection with name '" + var1 + "' doesn't exist!");
      }
   }

   @Override
   public void initializeEngines() {
      this.setDataFeed(this);
   }

   @Override
   public void connect() {
   }

   @Override
   public void disconnect() {
   }

   @Override
   public void registerSymbol(String var1) {
   }

   @Override
   public boolean checkSymbolIsSupported(String var1) {
      return DataManager.checkDataExists(this.connectionName, var1);
   }

   @Override
   public boolean isConnected() {
      return true;
   }

   @Override
   public String getType() {
      return "Backtest";
   }

   @Override
   public String getStatus() {
      return "Connected";
   }

   @Override
   public String getPluginName() {
      return "BacktestConnection";
   }

   @Override
   public HistoryData getHistoryData(ChartDef var1, int var2, long var3, long var5) {
      return null;
   }
}
