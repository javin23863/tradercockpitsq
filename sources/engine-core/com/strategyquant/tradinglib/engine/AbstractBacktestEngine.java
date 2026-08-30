package com.strategyquant.tradinglib.engine;

import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.backtest.LoadDataProgressEngine;
import com.strategyquant.tradinglib.backtestrunner.IBacktestProgressListener;
import com.strategyquant.tradinglib.project.StopPauseEngine;
import com.strategyquant.tradinglib.robustnesstests.DataModifierCallback;

public abstract class AbstractBacktestEngine {
   protected boolean singleThreaded = true;
   protected DataModifierCallback dataModifierCallback;
   protected StopPauseEngine stopPauseEngine;
   protected IBacktestProgressListener backtestProgressListener;
   protected LoadDataProgressEngine loadDataProgressEngine = null;
   protected String lastSettingsXml = null;
   protected boolean usePreparedData = true;
   protected double globalATR;

   public void setSingleThreaded(boolean var1) {
      this.singleThreaded = var1;
   }

   public void setDataModifierCallback(DataModifierCallback var1) {
      this.dataModifierCallback = var1;
   }

   public void setStopPauseEngine(StopPauseEngine var1) {
      this.stopPauseEngine = var1;
   }

   public void registerProgressListener(IBacktestProgressListener var1) {
      this.backtestProgressListener = var1;
   }

   public void setLoadDataProgressEngine(LoadDataProgressEngine var1) {
      this.loadDataProgressEngine = var1;
   }

   public void setLastSettings(String var1) {
      this.lastSettingsXml = var1;
   }

   public void setUsePreparedData(boolean var1) {
      this.usePreparedData = var1;
   }

   public abstract int getEngineId();

   public abstract BacktestEngine runBacktest() throws Exception;

   public abstract BacktestEngine runBacktest(String var1, String var2, boolean var3) throws Exception;

   public abstract void addSetup(SettingsMap var1) throws Exception;

   public abstract double getGlobalATR();
}
