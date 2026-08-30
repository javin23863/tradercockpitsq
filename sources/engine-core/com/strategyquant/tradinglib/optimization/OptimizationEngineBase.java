package com.strategyquant.tradinglib.optimization;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.tradinglib.ATM;
import com.strategyquant.tradinglib.ChartSetup;
import com.strategyquant.tradinglib.CommissionsMethod;
import com.strategyquant.tradinglib.Result;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.SwapMethod;
import com.strategyquant.tradinglib.backtest.TradingOptionsModifier;
import com.strategyquant.tradinglib.backtestrunner.BacktestResult;
import com.strategyquant.tradinglib.backtestrunner.BacktestRunner;
import com.strategyquant.tradinglib.backtestrunner.BacktestSettings;
import com.strategyquant.tradinglib.engine.BacktestEngine;
import com.strategyquant.tradinglib.engine.ChartSetups;
import com.strategyquant.tradinglib.exception.StrategyStoppedException;
import com.strategyquant.tradinglib.fitnessfunction.IFitnessFunction;
import com.strategyquant.tradinglib.options.TradingOptions;
import com.strategyquant.tradinglib.project.ILastEventListener;
import com.strategyquant.tradinglib.project.ProjectConfigHelper;
import com.strategyquant.tradinglib.project.StopPauseEngine;
import com.strategyquant.tradinglib.results.stats.StatsComputer;
import com.strategyquant.tradinglib.simulator.impl.TraderSimulators;
import com.strategyquant.tradinglib.strategy.MarketData;
import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OptimizationEngineBase {
   public static final Logger Log = LoggerFactory.getLogger("OptimizationEngineBase");
   protected final String lastSettingsXml;
   protected SettingsMap settings;
   protected OptimizationSettings optimizationSettings;
   protected OptimizationParams parameters;
   protected String taskName;
   protected StopPauseEngine progressEngine;
   private int status = 0;
   protected IFitnessFunction fitnessFunction;
   private INewResultListener newResultListener = null;
   private IEngineLogListener engineLogListener = null;
   private IStepsListener stepsListener = null;
   public int generationNumber = 0;
   protected long totalDays;
   protected int actualStep;
   protected StatsComputer customStatsComputer = null;
   protected StatsComputer fitnessStatsComputer = null;
   protected String source;
   protected ILastEventListener lastEventListener;
   private final ReentrantLock lock = new ReentrantLock();

   public OptimizationEngineBase(String var1, StopPauseEngine var2, String var3, ILastEventListener var4, String var5) {
      this.taskName = var1;
      this.progressEngine = var2;
      this.source = var3;
      this.lastEventListener = var4;
      this.lastSettingsXml = var5;
   }

   public void initialize(SettingsMap var1) throws Exception {
      this.settings = var1;
      this.optimizationSettings = (OptimizationSettings)var1.get("OptimizationSettings");
      if (this.optimizationSettings.optimizationType == 2) {
         this.optimizationSettings.optimizationType = 1;
      }

      this.parameters = this.optimizationSettings.parameters;
      this.fitnessFunction = (IFitnessFunction)var1.get("FitnessFunction");
      if (!this.fitnessFunction.getClass().getSimpleName().equals("FitnessMethodStrategyResult")) {
         String var2 = "<Rankings type=\"never\">\r\n          <FitnessCriteria method=\"ComputeFromStrategyResult\">\r\n            <Settings>\r\n              <Ranking type=\"ReturnDDRatio\" />\r\n            </Settings>\r\n          </FitnessCriteria>\r\n          </Rankings>";
         Element var3 = XMLUtil.stringToElement(var2);
         this.fitnessFunction = ProjectConfigHelper.getFitnessFunction(var3);
      }

      this.actualStep = 0;
   }

   protected void initOptimization() {
      ChartSetups var1 = (ChartSetups)this.settings.get("ChartSetups");
      this.totalDays = SQTime.getDaysBetween(var1.getMainSetup().getMainChart().getHistoryFrom(), var1.getMainSetup().getMainChart().getHistoryTo());
   }

   public void increaseStep() {
      this.increaseStep(1);
   }

   public void increaseStep(int var1) {
      synchronized (this) {
         this.actualStep += var1;
         if (this.stepsListener != null) {
            this.stepsListener.step(this.actualStep);
         }
      }
   }

   protected short[] prepareIndexesArray(int var1) {
      short[] var2 = new short[var1];

      for (int var3 = 0; var3 < var2.length; var3++) {
         var2[var3] = -1;
      }

      return var2;
   }

   public void addResultToDatabank(ResultsGroup var1) throws Exception {
      boolean var2 = this.lock.tryLock(20L, TimeUnit.SECONDS);
      if (var2) {
         try {
            if (this.newResultListener != null) {
               try {
                  Element var3 = var1.getStrategyXml();
                  if (var3 != null) {
                     StrategyBase var4 = StrategyBase.createXmlStrategy(var3);
                     var4.transformToNumbers();
                     var1.portfolio().addStrategyXml(var4.getStrategyXml());
                  }

                  if (this.settings.containsKey("LogicalVein")) {
                     String var10 = this.settings.getString("LogicalVein");
                     if (var10 != null && !var10.isEmpty()) {
                        var1.portfolio().set("LogicalVein", var10);
                     }
                  }
               } catch (Exception var8) {
                  if (!var8.getMessage().contains("ResultsMap is empty, this ResultsGroup was destroyed before!")) {
                     throw var8;
                  }
               }
            }
         } finally {
            this.lock.unlock();
         }
      }

      this.newResultListener.newResult(var1);
   }

   private void copyStatsFromFirstResultToPortfolio(ResultsGroup var1) throws Exception {
      String var2 = var1.getMainResultKey();
      Result var3 = var1.subResult(var2);
      Result var4 = var1.portfolio();
      String[] var5 = var3.getAllKeys();

      for (String var9 : var5) {
         if (!var4.containsKey(var9)) {
            var4.set(var9, var3.get(var9));
         }
      }
   }

   protected MarketData getMarketData() {
      try {
         SettingsMap var1 = this.cloneBacktestSettings(0L, 0L);
         var1.set("StrategyName", "StrToGetData");
         var1.set("MainResultKey", "StrToGetData");
         var1.set("ResultGroupName", "StrToGetData");
         StrategyBase var2 = (StrategyBase)this.settings.get("StrategyObject");
         var1.set("StrategyObject", var2);
         BacktestSettings var3 = new BacktestSettings(var1);
         ChartSetups var4 = (ChartSetups)this.settings.get("ChartSetups");
         ChartSetup var5 = var4.getMainSetup();
         BacktestEngine var6 = new BacktestEngine(TraderSimulators.getSimulator(var5));
         var6.setSingleThreaded(true);
         var6.addSetup(var3.getSettingsMap(var5));
         return var6.getMarketData();
      } catch (Exception var8) {
         var8.printStackTrace();
         return null;
      }
   }

   public ResultsGroup testStrategy(short[] var1, String var2, String var3, long var4, long var6) throws Exception {
      long var8 = System.currentTimeMillis();
      SettingsMap var10 = this.cloneBacktestSettings(var4, var6);
      var10.set("StrategyName", var2);
      var10.set("MainResultKey", var3);
      var10.set("ResultGroupName", var2);
      StrategyBase var11 = (StrategyBase)this.settings.get("StrategyObject");
      StrategyParamData var12 = StrategyBase.createStrategyVariation(var11, this.optimizationSettings, this.optimizationSettings.transformToFullIndexes(var1));
      StrategyBase var13 = var12.getStrategy();
      String var14 = var12.getParams();
      Element var15 = var13.getStrategyXml();
      var10.set("StrategyObject", StrategyBase.createXmlStrategy(var15));
      TradingOptions var16 = (TradingOptions)var10.get("TradingOptions");
      Element var17 = (Element)this.settings.get("StrategyLastSettings");
      TradingOptionsModifier.modifyTradingOptions(var16, var15, var17);
      boolean var18 = this.optimizationSettings.periodType == 30;
      BacktestSettings var19 = new BacktestSettings(var10);
      var19.removeEndOfTestOrders = var18;
      BacktestRunner var20 = new BacktestRunner(var19, this.progressEngine, null, false, this.lastEventListener, this.lastSettingsXml);
      BacktestResult var21 = var20.execute();
      ResultsGroup var22 = var21.getResult();
      if (var22 == null) {
         if (var21.getDismissalReason() == 10002) {
            throw new StrategyStoppedException("Stopped");
         } else {
            throw new Exception(L.t("No results for backtest! Reason: %s", new Object[]{var21.getDismissalMessage()}));
         }
      } else {
         var22.specialValues().setString("OptimizationParameters", var14);
         var22.specialValues().set("OptimizationParametersArray", var1);
         if (var17 != null) {
            var22.setLastSettings(XMLUtil.elementToString(var17));
         }

         double var23 = this.computeFitness(var22);
         var22.portfolio().setFitness((byte)127, var23);
         return var22;
      }
   }

   protected double computeFitness(ResultsGroup var1) throws Exception {
      return this.computeFitness(var1, (byte)0, (byte)10);
   }

   private double computeFitness(ResultsGroup var1, byte var2, byte var3) throws Exception {
      double var4 = this.fitnessFunction.computeFitness(var1, var2, var3);
      var1.portfolio().setFitness(var3, var4);
      if (var2 == 0 && var3 == 127) {
         Log.debug("Fitness computed to: " + var4);
      }

      return var4;
   }

   protected SettingsMap cloneBacktestSettings(long var1, long var3) throws Exception {
      SettingsMap var5 = new SettingsMap();
      var5.set("ChartSetups", ((ChartSetups)this.settings.get("ChartSetups")).getClone(var1, var3));
      var5.set("MoneyManagement.InitialCapital", this.settings.getDouble("MoneyManagement.InitialCapital"));
      var5.set("TestPrecision", this.settings.getInt("TestPrecision"));
      var5.set("Slippage", this.settings.getDouble("Slippage"));
      var5.set("MinDistance", this.settings.getDouble("MinDistance"));
      var5.set("Commission", (CommissionsMethod)this.settings.get("Commission"));
      var5.set("Swap", (SwapMethod)this.settings.get("Swap"));
      var5.set("FitnessFunction", this.settings.get("FitnessFunction"));
      var5.set("MoneyManagement.Method", this.settings.get("MoneyManagement.Method"));
      var5.set("RiskManagement", this.settings.get("RiskManagement"));
      TradingOptions var6 = ((TradingOptions)this.settings.get("TradingOptions")).getClone();
      var5.set("TradingOptions", var6);
      ATM var7 = (ATM)this.settings.get("ATM");
      if (var7 != null) {
         var5.set("ATM", var7.getClone());
      }

      return var5;
   }

   protected void checkEnoughDiskSize(int var1, long var2) throws NotEnoughSpaceException {
      int var4 = (int)(var1 * 12 * 1.5F);
      long var5 = var2 * var4;
      File var7 = new File(MainApp.getDataPath());
      long var8 = var7.getUsableSpace();
      Log.debug("Free space : " + var8 / 1024L / 1024L + " MB");
      var8 = var8 / 1024L / 1024L;
      var5 = var5 / 1024L / 1024L;
      if (var5 > var8) {
         throw new NotEnoughSpaceException(
            2,
            "There is not enough space on disc to store simulation temporary files. Approximately "
               + var5
               + " MB is required, while only "
               + var8
               + " MB is available!\nPlease free some disc space or decrease number of tests."
         );
      }
   }

   public boolean isPaused() {
      return this.status == 2;
   }

   public boolean isStopped() {
      return this.status == 4;
   }

   public void setStatus(int var1) {
      this.status = var1;
   }

   public void setNewResultListener(INewResultListener var1) {
      this.newResultListener = var1;
   }

   public void setLogListener(IEngineLogListener var1) {
      this.engineLogListener = var1;
   }

   public void setStepsListener(IStepsListener var1) {
      this.stepsListener = var1;
   }

   public void printLog(String var1) {
      if (this.engineLogListener != null) {
         this.engineLogListener.processMessage(var1);
      }
   }

   public int getGenerationNumber() {
      return this.generationNumber;
   }

   protected StopPauseEngine getStopPauseEngine() {
      return this.progressEngine;
   }
}
