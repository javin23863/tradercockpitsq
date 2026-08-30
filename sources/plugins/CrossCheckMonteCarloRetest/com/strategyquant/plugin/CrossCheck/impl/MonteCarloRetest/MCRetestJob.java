package com.strategyquant.plugin.CrossCheck.impl.MonteCarloRetest;

import com.strategyquant.datalib.TickEvent;
import com.strategyquant.datalib.data.io.VersatileData;
import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.random.MersenneTwisterRng;
import com.strategyquant.tradinglib.BadStrategyException;
import com.strategyquant.tradinglib.ChartSetup;
import com.strategyquant.tradinglib.MonteCarloRetest;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.backtestrunner.DurationStats;
import com.strategyquant.tradinglib.backtestrunner.IBacktestProgressListener;
import com.strategyquant.tradinglib.crosscheck.MCResultsComputer;
import com.strategyquant.tradinglib.engine.BacktestEngine;
import com.strategyquant.tradinglib.engine.stockpicker.StockpickerBacktestEngine;
import com.strategyquant.tradinglib.options.TradingOptions;
import com.strategyquant.tradinglib.project.ILastEventListener;
import com.strategyquant.tradinglib.project.StopPauseEngine;
import com.strategyquant.tradinglib.results.SymbolsMap;
import com.strategyquant.tradinglib.robustnesstests.DataModifierCallback;
import com.strategyquant.tradinglib.robustnesstests.RobustnessResults;
import com.strategyquant.tradinglib.simulator.Engines;
import com.strategyquant.tradinglib.simulator.ITradingSimulator;
import com.strategyquant.tradinglib.simulator.impl.TraderSimulators;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MCRetestJob extends GridJob<MCJobResult> {
   public static final Logger Log = LoggerFactory.getLogger("MCRetestJob");
   private ILastEventListener lastEventListener;
   private long index;
   private String strategyName;
   private StopPauseEngine parentStopPauseEngine;
   private SettingsMap resultSettings;
   private SettingsMap mcSettings;
   private int mainBacktestPrecision;
   private double globalATR;
   private SymbolsMap symbolsMap;
   private boolean useFullSample;
   private int backtestPrecision;

   public MCRetestJob(
      long var1,
      String var3,
      String var4,
      Map<String, Serializable> var5,
      StopPauseEngine var6,
      ILastEventListener var7,
      int var8,
      double var9,
      SymbolsMap var11
   ) {
      super(var4, 1, var5);
      this.lastEventListener = var7;
      this.index = var1;
      this.strategyName = var3;
      this.parentStopPauseEngine = var6;
      this.resultSettings = (SettingsMap)var5.get("resultSettings");
      this.mcSettings = (SettingsMap)var5.get("mcSettings");
      this.mainBacktestPrecision = var8;
      this.useFullSample = (Boolean)((Serializable)var5.get("useFullSample"));
      this.backtestPrecision = (Integer)((Serializable)var5.get("backtestPrecision"));
      this.globalATR = var9;
      this.symbolsMap = var11;
   }

   public MCJobResult call() throws Exception {
      DurationStats var1 = new DurationStats();
      long var2 = System.currentTimeMillis();

      try {
         if (this.lastEventListener != null) {
            String var4 = L.t("Running MC simulation %d for %s", new Object[]{this.index, this.strategyName});
            this.lastEventListener.setLastEvent(var4);
         }

         ArrayList var16 = (ArrayList)this.mcSettings.get("MonteCarloManipulation");
         ArrayList var17 = new ArrayList();

         for (MonteCarloRetest var7 : var16) {
            var17.add(var7.getClone());
         }

         SettingsMap var18 = this.cloneSettings(this.resultSettings);
         ChartSetup var19 = (ChartSetup)var18.get("BacktestChart");
         if (this.backtestPrecision == -1) {
            var19.setTestPrecision(this.mainBacktestPrecision);
         } else {
            var19.setTestPrecision(this.backtestPrecision);
         }

         Object var8 = null;
         if (var19.getBacktestEngine() != 1316847364 && var19.getBacktestEngine() != -1816889229) {
            ITradingSimulator var9 = TraderSimulators.getSimulator(var19);
            var8 = new BacktestEngine(var9);
            boolean var10 = !this.isRandomizeHistoryUsed(var19, var17);
            if (Engines.isTradestationEngine(var9.getEngineId())) {
               var10 = true;
            }

            var8.setUsePreparedData(var10);
         } else {
            var8 = new StockpickerBacktestEngine();
         }

         var8.setSingleThreaded(true);
         var8.setStopPauseEngine(this.parentStopPauseEngine);
         long var21 = System.currentTimeMillis() + 109L * this.index;
         final MersenneTwisterRng var11 = new MersenneTwisterRng(var21);
         var8.registerProgressListener(new IBacktestProgressListener() {
            public void setProgress(int var1) {
            }

            public void increaseProgressStep() {
            }
         });

         for (final MonteCarloRetest var13 : var17) {
            if (this.parentStopPauseEngine.isStopped()) {
               break;
            }

            if (var13.getType() == 1) {
               var13.modifySettings(var11, var18);
            } else if (var13.getType() == 2) {
               var13.initSettings(var18);
               var8.setDataModifierCallback(new DataModifierCallback() {
                  public void modifyData(TickEvent var1) {
                     var13.modifyData(var11, var1, MCRetestJob.this.globalATR);
                  }

                  public void modifyOHLCData(VersatileData var1) {
                     var13.modifyOHLCData(var11, var1, MCRetestJob.this.globalATR);
                  }

                  public void modifyData(TickEvent var1, double var2x) {
                     var13.modifyData(var11, var1, var2x);
                  }
               });
            }
         }

         var8.addSetup(var18);
         ResultsGroup var22 = var8.runBacktest().getResults();
         OrdersList var23 = var22.orders();
         if (!this.useFullSample) {
            var23 = this.filterInSampleOnly(var23, var18);
         }

         RobustnessResults var14 = MCResultsComputer.computeResults(var23, var18, this.symbolsMap, null);
         var1.addDuration(DurationStats.MainTest, System.currentTimeMillis() - var2);
         return new MCJobResult(var14, var1);
      } catch (Exception var15) {
         if (var15 instanceof BadStrategyException) {
            BadStrategyException var5 = (BadStrategyException)var15;
            if (!var15.getMessage().contains("Automatic filter") && !var15.getMessage().contains(L.t("Automatic filter", new Object[0]))) {
               Log.error(String.format("Exception in Cross check - %s : ", this.getJobId(), BadStrategyException.getReasonAsString(var5.getReason())), var15);
               return new MCJobResult(L.t("Automatic filter", new Object[0]), 10004, var1);
            }
         }

         return new MCJobResult(L.t("Exception in backtest: %s", new Object[]{var15.getMessage()}), 10004, var1);
      }
   }

   private boolean isRandomizeHistoryUsed(ChartSetup var1, ArrayList<MonteCarloRetest> var2) {
      if (var1.getMainChart().getSymbolInfo().dataType != 2) {
         for (MonteCarloRetest var4 : var2) {
            if (var4.getType() == 2) {
               return true;
            }
         }
      }

      return false;
   }

   private OrdersList filterInSampleOnly(OrdersList var1, SettingsMap var2) {
      OrdersList var3 = var1.filterWithClone("*", (byte)0, (byte)10);
      var1.clear();
      return var3;
   }

   protected SettingsMap cloneSettings(SettingsMap var1) throws CloneNotSupportedException {
      SettingsMap var2 = new SettingsMap();
      String[] var3 = var1.getAllKeys();

      for (int var4 = 0; var4 < var3.length; var4++) {
         Object var5 = this.cloneValue(var3[var4], var1.get(var3[var4]));
         var2.set(var3[var4], var5);
      }

      var2.set("StrategyCheckDate", -1L);
      return var2;
   }

   private Object cloneValue(String var1, Object var2) throws CloneNotSupportedException {
      if (var1.equals("TradingOptions")) {
         return ((TradingOptions)var2).getClone();
      } else if (var1.equals("StoreChartData")) {
         return false;
      } else if (var2 instanceof StrategyBase) {
         return ((StrategyBase)var2).clone();
      } else {
         return var1.equals("BacktestChart") ? ((ChartSetup)var2).getClone() : var2;
      }
   }
}
