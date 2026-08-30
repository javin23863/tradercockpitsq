package com.strategyquant.tradinglib.backtest;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.basket.BasketDto;
import com.strategyquant.datalib.basket.BasketOfStocksManager;
import com.strategyquant.datalib.basket.StockDto;
import com.strategyquant.datalib.data.DataException;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.ChartSetup;
import com.strategyquant.tradinglib.crosscheck.ICrossCheck;
import com.strategyquant.tradinglib.engine.BacktestEngine;
import com.strategyquant.tradinglib.engine.ChartSetups;
import com.strategyquant.tradinglib.engine.stockpicker.StockpickerBacktestEngine;
import com.strategyquant.tradinglib.engine.stockpicker.data.PickerDataCache;
import com.strategyquant.tradinglib.project.ILastEventListener;
import com.strategyquant.tradinglib.simulator.impl.TraderSimulators;
import java.util.ArrayList;
import java.util.List;

public class BacktestDataInitializer {
   private ArrayList<ChartSetup> chartSetups;
   private LoadDataProgressEngine progressEngine;
   private ArrayList<ICrossCheck> crossChecks;
   private ILastEventListener lastEventListener;

   public BacktestDataInitializer(LoadDataProgressEngine var1, ArrayList<ChartSetup> var2, boolean var3, ArrayList<ICrossCheck> var4, ILastEventListener var5) {
      this.chartSetups = var2;
      this.progressEngine = var1;
      if (var3) {
         this.crossChecks = var4;
      }

      this.lastEventListener = var5;
   }

   public BacktestDataInitializer(LoadDataProgressEngine var1, ArrayList<ChartSetup> var2, boolean var3, ArrayList<ICrossCheck> var4) {
      this(var1, var2, var3, var4, null);
   }

   public long prepareData() throws Exception {
      long var1 = System.currentTimeMillis();
      if (this.chartSetups != null && this.chartSetups.size() >= 1) {
         this.progressEngine.printToLog(L.t("Initializing backtest data...", new Object[]{true}));
         if (this.lastEventListener != null) {
            this.lastEventListener.setLastEvent(L.t("Initializing backtest data...", new Object[]{true}));
         }

         ChartSetup var3 = this.chartSetups.get(0);
         int var4 = var3.getBacktestEngine();
         if (var4 != 1316847364 && var4 != -1816889229) {
            this.prepareDataFor("Main test", var3);
            if (this.crossChecks != null && this.crossChecks.size() > 0) {
               for (int var5 = 0; var5 < this.crossChecks.size(); var5++) {
                  ICrossCheck var6 = this.crossChecks.get(var5);
                  if (var6.doesRetest()) {
                     ChartSetups var7 = var6.getChartSetups(var3);
                     if (var7 != null) {
                        ChartSetups var8 = var7.getClone();

                        for (int var9 = 0; var9 < var8.size(); var9++) {
                           ChartSetup var10 = var8.get(var9);
                           this.prepareDataFor(var6.getName(), var10);
                        }
                     }
                  }
               }
            }
         } else {
            this.preparePickerData(var3, this.crossChecks);
         }

         this.progressEngine.printToLog(L.t("All backtest data prepared", new Object[]{true}));
         if (this.lastEventListener != null) {
            this.lastEventListener.setLastEvent(L.t("All backtest data prepared", new Object[]{true}));
         }

         this.progressEngine.printToLog("-----------------------------");
         return System.currentTimeMillis() - var1;
      } else {
         return 0L;
      }
   }

   public void preparePickerData(ChartSetup var1, ArrayList<ICrossCheck> var2) throws Exception {
      DataManager.get().refreshDataInfoCache();
      int var3 = var1.getBacktestEngine();
      String var4 = var1.getSymbol();
      if (var3 == -1816889229 && DataManager.isGroupAlias(var4)) {
         BasketDto var5 = BasketOfStocksManager.getInstance().getBasket(var4);
         if (var5 == null) {
            throw new Exception(L.t("Group with name '{0}' not found.", new Object[]{var4}));
         }

         List var6 = BasketOfStocksManager.getInstance().getStocks(var5.getId());

         for (int var7 = 0; var7 < var6.size(); var7++) {
            StockDto var8 = (StockDto)var6.get(var7);
            ChartSetup var11 = var1.getClone(var8.getTicker());
            var1 = StockpickerBacktestEngine.createTempStockGroup(var11);
            this.prepareStockpickerData(var1);
         }
      } else {
         if (var3 == -1816889229) {
            var1 = StockpickerBacktestEngine.createTempStockGroup(var1);
         }

         this.prepareStockpickerData(var1);
      }

      if (var3 == 1316847364 && var2 != null && var2.size() > 0) {
         for (int var12 = 0; var12 < var2.size(); var12++) {
            ICrossCheck var13 = (ICrossCheck)var2.get(var12);
            if (var13.getSettingName().equals("RetestOnAdditionalMarkets")) {
               ChartSetups var14 = var13.getChartSetups(var1);
               if (var14 != null) {
                  ChartSetups var15 = var14.getClone();

                  for (int var9 = 0; var9 < var15.size(); var9++) {
                     ChartSetup var10 = var15.get(var9);
                     this.prepareStockpickerData(var10);
                  }
               }
               break;
            }
         }
      }
   }

   public void prepareStockpickerData(ChartSetup var1) throws Exception {
      ChartDef var2 = var1.getMainChart();
      String var3 = var2.getSymbol();
      DataInfo var4 = DataManager.getDataInfo("History", var3, true);
      if (var4 == null) {
         throw new DataException(2, "Data for connection 'History' and symbol '" + var3 + "' cannot be found!");
      }

      int var5 = var4.basketId;
      BasketDto var6 = BasketOfStocksManager.getInstance().getBasket(var5);
      if (var6 == null) {
         throw new DataException(2, L.t("Group with id %d doesn't exist", new Object[]{var5}));
      }

      if (!DataManager.isGroupAlias(var3)) {
         throw new DataException(2, L.t("Main chart symbol '%s' must be a group alias.", new Object[]{var3}));
      }

      for (int var8 = 1; var8 < var1.getCharts().size(); var8++) {
         ChartDef var7 = var1.getCharts().get(var8);
         if (!var7.getTimeframe().equals("D1") && !var7.getTimeframe().equals("Weekly") && !var7.getTimeframe().equals("Monthly")) {
            throw new Exception(
               L.t(
                  "Invalid Additional chart %d - %s/%s definition. TF must be one of: %s, %s, %s.",
                  new Object[]{var8, var7.getSymbol(), var7.getTimeframe(), "D1", "Weekly", "Monthly"}
               )
            );
         }
      }

      PickerDataCache var9 = PickerDataCache.getInstance();
      var9.loadData(var1, var6, this.progressEngine.getProgressEngine(), this.lastEventListener);
   }

   private void prepareDataFor(String var1, ChartSetup var2) throws Exception {
      BacktestInitStrategy var3 = new BacktestInitStrategy();
      BacktestEngine var4 = new BacktestEngine(TraderSimulators.getSimulator(var2));
      var4.setSingleThreaded(true);
      var4.setStopPauseEngine(this.progressEngine.getProgressEngine());
      SettingsMap var5 = new SettingsMap();
      var5.set("BacktestChart", var2);
      var5.set("MoneyManagement.InitialCapital", 100000.0);
      var5.set("StrategyObject", var3);
      var5.set("MinDistance", 0);
      var4.addSetup(var5);
      var4.setLoadDataProgressEngine(this.progressEngine);
      this.progressEngine.setParams(var1, var2.getCharts().get(0).getSymbol(), var2.getCharts().get(0).getTimeframe());
      var4.runBacktest("LoadDataStartegy", "LoadDataStrategy", false);
   }
}
