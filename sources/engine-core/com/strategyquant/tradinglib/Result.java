package com.strategyquant.tradinglib;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.TimePeriod;
import com.strategyquant.lib.ValuesMap;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.tradinglib.engine.TradingSetup;
import com.strategyquant.tradinglib.engine.stockpicker.Stockpicker;
import com.strategyquant.tradinglib.engine.stockpicker.data.LoadedPickerData;
import com.strategyquant.tradinglib.equitychart.Periods;
import com.strategyquant.tradinglib.results.stats.StatsComputer;
import com.strategyquant.tradinglib.robustnesstests.MCSimulations;
import com.strategyquant.tradinglib.robustnesstests.RobustnessResults;
import com.strategyquant.tradinglib.robustnesstests.SequentialOptimizationResults;
import com.strategyquant.tradinglib.stockchart.StockSaverForStockPicking;
import com.strategyquant.tradinglib.stockchart.StockSaverOriginalFormat;
import com.strategyquant.tradinglib.strategy.OutOfSample;
import com.strategyquant.tradinglib.util.TradingLibUtil;
import it.unimi.dsi.fastutil.longs.Long2FloatRBTreeMap;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.jar.Attributes.Name;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Result extends ValuesMap {
   public static final Logger Log = LoggerFactory.getLogger("Result");
   private static final int[] ignoredSettings = new int[]{
      "Commission".hashCode(),
      "BacktestChart".hashCode(),
      "MoneyManagement.Method".hashCode(),
      "RiskManagement".hashCode(),
      "OutOfSample".hashCode(),
      "StrategyXml".hashCode()
   };
   private ResultsGroup resultsGroup;
   private String resultKey;
   private transient SettingsMap settings;
   private transient StrategyBase strategyObj;
   private Element strategyXml = null;
   private ReentrantLock chartsDataLock = new ReentrantLock();
   private ReentrantLock dailyEquityLock = new ReentrantLock();
   private ReentrantLock maxDailyDDLock = new ReentrantLock();
   private ReentrantLock maxDailyDDPctLock = new ReentrantLock();
   private String stockChartPath;
   private Long2FloatRBTreeMap worstDailyEquity;
   private Long2FloatRBTreeMap maxDailyDD;
   private Long2FloatRBTreeMap maxDailyDDPct;
   private boolean specialResult = false;
   private MCSimulations mcManipulationSimulations;
   private MCSimulations mcRetestSimulations;
   private FitnessCollection fitnessCollection = new FitnessCollection();
   private SequentialOptimizationResults sequentialOptimizationResults;

   public Result(ResultsGroup var1) {
      this.resultsGroup = var1;
   }

   public Result(String var1, ResultsGroup var2, SettingsMap var3) {
      this.resultKey = var1;
      if (!var1.equals("Portfolio")) {
         String[] var4 = var1.split("/");
      }

      this.resultsGroup = var2;
      this.settings = var3;
   }

   public Result clone(ResultsGroup var1) {
      Result var2 = new Result(var1);
      var2.resultKey = this.resultKey;
      var2.strategyObj = this.strategyObj;
      var2.strategyXml = this.strategyXml != null ? this.strategyXml.clone() : this.strategyXml;
      var2.stockChartPath = this.cloneStockChart(this.stockChartPath);
      if (this.fitnessCollection != null) {
         var2.fitnessCollection = this.fitnessCollection.clone();
      }

      if (this.getWorstDailyEquity() != null) {
         var2.setWorstDailyEquity(this.getWorstDailyEquity().clone());
      }

      if (this.getMaxDailyDD() != null) {
         var2.setMaxDailyDD(this.getMaxDailyDD().clone());
      }

      if (this.getMaxDailyDDPct() != null) {
         var2.setMaxDailyDDPct(this.getMaxDailyDDPct().clone());
      }

      if (this.settings != null) {
         var2.settings = this.settings.clone();
      }

      var2.values = SQUtils.cloneValuesMap(this.values);

      try {
         if (this.mcManipulationSimulations != null) {
            var2.mcManipulationSimulations = this.mcManipulationSimulations.getClone();
         }

         if (this.mcRetestSimulations != null) {
            var2.mcRetestSimulations = this.mcRetestSimulations.getClone();
         }

         if (this.sequentialOptimizationResults != null) {
            var2.sequentialOptimizationResults = this.sequentialOptimizationResults.getClone();
         }
      } catch (Exception var4) {
         Log.error("Cannot clone MC simulation results!", var4);
      }

      return var2;
   }

   private String cloneStockChart(String var1) {
      if (var1 == null) {
         return null;
      }

      File var2 = new File(var1);
      if (var2 != null && var2.exists()) {
         BufferedInputStream var3 = null;
         BufferedOutputStream var4 = null;

         try {
            var3 = new BufferedInputStream(new FileInputStream(var2));
            File var5 = new File(var2.getParent());
            File var6 = File.createTempFile("sq4-stock-", ".jar", var5);
            var4 = new BufferedOutputStream(new FileOutputStream(var6));
            var6.deleteOnExit();
            byte[] var7 = new byte[1024];

            int var8;
            while ((var8 = var3.read(var7)) > 0) {
               var4.write(var7, 0, var8);
               var4.flush();
            }

            return var6.getAbsolutePath();
         } catch (Exception var23) {
            Log.error("Cannot copy chart file " + var1, var23);
         } finally {
            if (var3 != null) {
               try {
                  var3.close();
               } catch (Exception var22) {
                  Log.error("Failed to close InputStream", var22);
               }
            }

            if (var4 != null) {
               try {
                  var4.close();
               } catch (Exception var21) {
                  Log.error("Failed to close OutputStream", var21);
               }
            }
         }
      }

      return null;
   }

   public void setFrom(Result var1, String var2) {
      this.resultKey = var2;
      this.strategyObj = var1.strategyObj;
      this.strategyXml = var1.strategyXml;
      this.stockChartPath = var1.stockChartPath;
      if (var1.settings != null) {
         this.settings = var1.settings.clone();
      }

      this.mcManipulationSimulations = var1.mcManipulationSimulations;
      this.mcRetestSimulations = var1.mcRetestSimulations;
      this.values = var1.values.clone();
   }

   public SQStats stats(StatsTypeCombination var1) throws Exception {
      return this.stats(var1.getDirection(), var1.getPLType(), var1.getSampleType());
   }

   public SQStats stats(byte var1, byte var2, byte var3) throws StatsDontExistException {
      int var4 = StatsTypeCombination.createKey(var1, var2, var3);
      if (!this.containsKey(var4)) {
         throw new StatsDontExistException(
            "Result doesn't contain stats for '"
               + var4
               + "', call rg.computeAllStats() before calling this method! D: "
               + var1
               + ", PL: "
               + var2
               + ", S: "
               + var3
         );
      } else {
         return (SQStats)this.get(var4);
      }
   }

   public SQStats statsOrNull(byte var1, byte var2, byte var3) {
      int var4 = StatsTypeCombination.createKey(var1, var2, var3);
      return !this.containsKey(var4) ? null : (SQStats)this.get(var4);
   }

   public void addStrategy(StrategyBase var1) {
      this.strategyObj = var1;
   }

   public StrategyBase getStrategy() {
      return this.strategyObj;
   }

   public void addStrategyXml(Element var1) {
      this.strategyXml = var1;
   }

   public Element getStrategyXml() {
      return this.strategyXml;
   }

   public SettingsMap getSettings() {
      return this.settings;
   }

   public void setSettings(SettingsMap var1) {
      this.settings = var1;
   }

   public String getResultKey() {
      return this.resultKey;
   }

   public Element getXML() {
      Element var1 = new Element("Result");
      XMLUtil.trySetAttr(var1, "resultKey", this.resultKey);
      XMLUtil.trySetAttr(var1, "special", Boolean.toString(this.isSpecial()));
      var1.addContent(this.fitnessCollection.getXML());
      Element var2 = super.getXML();
      var1.addContent(var2);
      if (this.settings != null) {
         Element var3 = this.settings.getXML();
         var1.addContent(var3);
      }

      return var1;
   }

   public void setFromXML(Element var1) {
      Element var2 = var1.getChild("SettingsMap");
      Element var3 = var1.getChild("ValuesMap");
      this.fitnessCollection.setFromXML(var1);
      this.values.clear();

      for (Element var5 : var3.getChildren()) {
         String var6 = SQUtils.decodeXmlKey(var5.getName());
         Object var7 = null;
         if (var5.getContentSize() > 0) {
            var7 = TradingLibUtil.elementToValue(var5);
         }

         if (var6.startsWith("stats")) {
            int var8 = ValuesMap.getStatsIntKeyFromString(var6);
            this.set(var8, var7);
         } else {
            this.set(var6, var7);
         }
      }

      this.resultKey = var1.getAttributeValue("resultKey").intern();
      this.settings = new SettingsMap();
      if (var2 != null) {
         this.settings.setFromXML(var2, ignoredSettings);
      }

      String var9 = var1.getAttributeValue("special");
      if (var9 != null) {
         this.specialResult = Boolean.parseBoolean(var9);
      } else {
         this.specialResult = false;
      }
   }

   public void computeAllStats(SettingsMap var1, OutOfSample var2) throws Exception {
      OrdersList var3;
      try {
         var3 = this.resultsGroup.orders().filter(this.resultKey, (byte)0, (byte)127);
         this.settings.set("AddCommissionSwapToPL", true);
      } catch (Exception var11) {
         throw var11;
      }

      if (var2 == null) {
         var2 = this.saveChartOOSSettings(var3);
      }

      StatsComputer var4 = new StatsComputer();
      var4.setRGSpecialValues(var1);
      var4.initializeWithOrders(var3);
      StatsTypeCombination var5 = new StatsTypeCombination((byte)1, (byte)10, (byte)127);
      boolean var6 = MainApp.settings().getBoolean("ComputePipsMetrics", false);
      boolean var7 = MainApp.settings().getBoolean("ComputePctsMetrics", false);
      boolean var8 = MainApp.settings().getBoolean("ComputeSeparateMetrics", true);
      this.computeSubStats(var4, var5, (byte)10, var6, var7, var8, var2);
      if (var2 != null && !var2.isEmpty()) {
         this.computeSubStats(var4, var5, (byte)20, var6, var7, var8, var2);
         this.computeSubStats(var4, var5, (byte)127, var6, var7, var8, var2);
      } else {
         this.computeNoOOSStats(var4, var5, var6, var7, var8);
      }

      if (!this.resultKey.equals("Portfolio") && !this.specialResult) {
         int var9 = var4.checkStrategyProblems(this.resultKey, this.getStrategyProblems());
         int var10 = this.getSettings().getInt("StrategyDismissSettings", 0);
         if ((var10 & var9) != 0) {
            throw new BadStrategyException(512);
         }
      }
   }

   private void computeNoOOSStats(StatsComputer var1, StatsTypeCombination var2, boolean var3, boolean var4, boolean var5) {
      this.copyStats(var2, (byte)10, (byte)127, var3, var4);
      SQStats var6 = var1.computeZeroStats();
      this.setZeroStats(var6, var2.set((byte)1, (byte)10, (byte)20));
      if (var4) {
         this.setZeroStats(var6, var2.set((byte)1, (byte)20, (byte)20));
      }

      if (var3) {
         this.setZeroStats(var6, var2.set((byte)1, (byte)30, (byte)20));
      }

      this.setZeroStats(var6, var2.set((byte)-1, (byte)10, (byte)20));
      if (var4) {
         this.setZeroStats(var6, var2.set((byte)-1, (byte)20, (byte)20));
      }

      if (var3) {
         this.setZeroStats(var6, var2.set((byte)-1, (byte)30, (byte)20));
      }

      this.setZeroStats(var6, var2.set((byte)0, (byte)10, (byte)20));
      if (var4) {
         this.setZeroStats(var6, var2.set((byte)0, (byte)20, (byte)20));
      }

      if (var3) {
         this.setZeroStats(var6, var2.set((byte)0, (byte)30, (byte)20));
      }
   }

   private void setZeroStats(SQStats var1, StatsTypeCombination var2) {
      int var3 = var2.getKey();
      this.set(var3, var1);
   }

   private OutOfSample saveChartOOSSettings(OrdersList var1) {
      try {
         Periods var2 = new Periods();
         List var3 = var2.getAvailablePeriods(var1, "time");
         OutOfSample var4 = new OutOfSample();

         for (int var5 = 0; var5 < var3.size(); var5++) {
            TimePeriod var6 = (TimePeriod)var3.get(var5);
            if (var6.type != 11) {
               var4.addRange(var6.from, var6.to, var6.type);
            }
         }

         this.settings.set("ChartOOS", var4);
         return var4;
      } catch (Exception var7) {
         Log.error("Cannot save chart oos time periods", var7);
         return null;
      }
   }

   private void computeSubStats(StatsComputer var1, StatsTypeCombination var2, byte var3, boolean var4, boolean var5, boolean var6, OutOfSample var7) throws Exception {
      if (var7 == null) {
         this.computeSubSubStats(var1, var2, var3, var4, var5);
      } else {
         if (var3 == 20) {
            for (int var8 = 0; var8 < 10; var8++) {
               this.cleanStats(var2, (byte)20, var8, var4, var5);
            }

            int var12 = var7.getRangesCount();
            int var9 = 0;
            if (var6) {
               for (int var10 = 0; var10 < var12; var10++) {
                  byte var11 = var7.getPartType(var10);
                  if (SampleTypes.isOOS(var11)) {
                     var9++;
                     this.computeSubSubStats(var1, var2, var11, var4, var5);
                  }
               }
            }

            if (var9 == 1) {
               this.copyStats(var2, (byte)21, (byte)20, var4, var5);
            } else {
               this.computeSubSubStats(var1, var2, (byte)20, var4, var5);
            }
         } else if (var3 == 10) {
            for (int var13 = 0; var13 < 10; var13++) {
               this.cleanStats(var2, (byte)10, var13, var4, var5);
            }

            int var14 = var7.getRangesCount();
            int var15 = 0;
            if (var6) {
               for (int var16 = 0; var16 < var14; var16++) {
                  byte var17 = var7.getPartType(var16);
                  if (SampleTypes.isISV(var17)) {
                     var15++;
                     this.computeSubSubStats(var1, var2, var17, var4, var5);
                  }
               }
            }

            if (var15 <= 0 && var6) {
               this.computeSubSubStats(var1, var2, (byte)10, var4, var5);
               this.copyStats(var2, (byte)10, (byte)11, var4, var5);
            } else {
               this.computeSubSubStats(var1, var2, (byte)10, var4, var5);
               this.computeSubSubStats(var1, var2, (byte)11, var4, var5);
               this.computeSubSubStats(var1, var2, (byte)40, var4, var5);
            }
         } else {
            if (var3 != 127) {
               throw new Exception("Trying to compute stats for strange sample type: " + var3);
            }

            this.computeSubSubStats(var1, var2, (byte)127, var4, var5);
         }
      }
   }

   private void cleanStats(StatsTypeCombination var1, byte var2, int var3, boolean var4, boolean var5) {
      if (var2 == 20) {
         this.remove(StatsTypeCombination.createKey((byte)1, (byte)10, OutOfSample.getOOSPart(var3 + 1)));
         this.remove(StatsTypeCombination.createKey((byte)1, (byte)20, OutOfSample.getOOSPart(var3 + 1)));
         this.remove(StatsTypeCombination.createKey((byte)1, (byte)30, OutOfSample.getOOSPart(var3 + 1)));
      } else if (var2 == 10) {
         this.remove(StatsTypeCombination.createKey((byte)1, (byte)10, OutOfSample.getISVPart(var3 + 1)));
         this.remove(StatsTypeCombination.createKey((byte)1, (byte)20, OutOfSample.getISVPart(var3 + 1)));
         this.remove(StatsTypeCombination.createKey((byte)1, (byte)30, OutOfSample.getISVPart(var3 + 1)));
      }
   }

   private void copyStats(StatsTypeCombination var1, byte var2, byte var3, boolean var4, boolean var5) {
      this.set(StatsTypeCombination.createKey((byte)1, (byte)10, var3), this.get(StatsTypeCombination.createKey((byte)1, (byte)10, var2)));
      if (var5) {
         this.set(StatsTypeCombination.createKey((byte)1, (byte)20, var3), this.get(StatsTypeCombination.createKey((byte)1, (byte)20, var2)));
      }

      if (var4) {
         this.set(StatsTypeCombination.createKey((byte)1, (byte)30, var3), this.get(StatsTypeCombination.createKey((byte)1, (byte)30, var2)));
      }

      this.set(StatsTypeCombination.createKey((byte)-1, (byte)10, var3), this.get(StatsTypeCombination.createKey((byte)-1, (byte)10, var2)));
      if (var5) {
         this.set(StatsTypeCombination.createKey((byte)-1, (byte)20, var3), this.get(StatsTypeCombination.createKey((byte)-1, (byte)20, var2)));
      }

      if (var4) {
         this.set(StatsTypeCombination.createKey((byte)-1, (byte)30, var3), this.get(StatsTypeCombination.createKey((byte)-1, (byte)30, var2)));
      }

      this.set(StatsTypeCombination.createKey((byte)0, (byte)10, var3), this.get(StatsTypeCombination.createKey((byte)0, (byte)10, var2)));
      if (var5) {
         this.set(StatsTypeCombination.createKey((byte)0, (byte)20, var3), this.get(StatsTypeCombination.createKey((byte)0, (byte)20, var2)));
      }

      if (var4) {
         this.set(StatsTypeCombination.createKey((byte)0, (byte)30, var3), this.get(StatsTypeCombination.createKey((byte)0, (byte)30, var2)));
      }
   }

   private void computeSubSubStats(StatsComputer var1, StatsTypeCombination var2, byte var3, boolean var4, boolean var5) throws Exception {
      var1.filterBy(this.resultKey, (byte)1, var3);
      var1.sortAndComputeOrderValues(this.settings, this.resultsGroup.symbols());
      this.computeComputeStats(var1, var2.set((byte)1, (byte)10, var3));
      if (var5) {
         this.computeComputeStats(var1, var2.set((byte)1, (byte)20, var3));
      }

      if (var4) {
         this.computeComputeStats(var1, var2.set((byte)1, (byte)30, var3));
      }

      var1.filterBy(this.resultKey, (byte)-1, var3);
      var1.sortAndComputeOrderValues(this.settings, this.resultsGroup.symbols());
      this.computeComputeStats(var1, var2.set((byte)-1, (byte)10, var3));
      if (var5) {
         this.computeComputeStats(var1, var2.set((byte)-1, (byte)20, var3));
      }

      if (var4) {
         this.computeComputeStats(var1, var2.set((byte)-1, (byte)30, var3));
      }

      var1.filterBy(this.resultKey, (byte)0, var3);
      var1.sortAndComputeOrderValues(this.settings, this.resultsGroup.symbols());
      this.computeComputeStats(var1, var2.set((byte)0, (byte)10, var3));
      if (var5) {
         this.computeComputeStats(var1, var2.set((byte)0, (byte)20, var3));
      }

      if (var4) {
         this.computeComputeStats(var1, var2.set((byte)0, (byte)30, var3));
      }
   }

   private void computeComputeStats(StatsComputer var1, StatsTypeCombination var2) throws Exception {
      SQStats var3 = var1.computeStats(this, var2, this.settings);
      int var4 = var2.getKey();
      this.set(var4, var3);
   }

   public void addTradingChartsData(TradingSetup var1, OrdersList var2, boolean var3) throws Exception {
      this.chartsDataLock.lock();

      try {
         File var4 = this.getTempFile(var3);
         StockSaverOriginalFormat var5 = new StockSaverOriginalFormat();
         Manifest var6 = new Manifest();
         var6.getMainAttributes().put(Name.MANIFEST_VERSION, "1.0");
         JarOutputStream var7 = new JarOutputStream(new FileOutputStream(var4), var6);

         try {
            var7.setLevel(0);
            var5.save(var7, var1, var2, this.resultKey);
         } catch (Throwable var15) {
            try {
               var7.close();
            } catch (Throwable var14) {
               var15.addSuppressed(var14);
            }

            throw var15;
         }

         var7.close();
         this.stockChartPath = var4.getAbsolutePath();
         this.resultsGroup.specialValues().set("ContainsChart", true);
      } finally {
         this.chartsDataLock.unlock();
      }
   }

   private File getTempFile(boolean var1) throws IOException {
      String var2 = "/internal/tmp/";
      String var3 = MainApp.getDataPath() + var2 + (var1 ? "algo" : "stock");
      SQUtils.ensureDirExists(var3);
      return File.createTempFile("sq4-stock-", ".jar", new File(var3));
   }

   public void addTradingChartsData(LoadedPickerData var1, Stockpicker var2, OrdersList var3, boolean var4) throws Exception {
      this.chartsDataLock.lock();

      try {
         File var5 = this.getTempFile(var4);
         StockSaverForStockPicking var6 = new StockSaverForStockPicking();
         Manifest var7 = new Manifest();
         var7.getMainAttributes().put(Name.MANIFEST_VERSION, "1.0");
         JarOutputStream var8 = new JarOutputStream(new FileOutputStream(var5), var7);

         try {
            var8.setLevel(0);
            var6.save(var8, var1, var2, var3, this.resultKey);
         } catch (Throwable var16) {
            try {
               var8.close();
            } catch (Throwable var15) {
               var16.addSuppressed(var15);
            }

            throw var16;
         }

         var8.close();
         this.stockChartPath = var5.getAbsolutePath();
         this.resultsGroup.specialValues().set("ContainsChart", true);
      } finally {
         this.chartsDataLock.unlock();
      }
   }

   public void clear(boolean var1, boolean var2) {
      String[] var3 = this.getAllKeys();

      for (int var4 = 0; var4 < var3.length; var4++) {
         if (var3[var4] != null) {
            if (var3[var4].contains("RobustnessSimulation")) {
               if (var3[var4].contains("Orders")) {
                  OrdersList var5 = (OrdersList)this.get(var3[var4]);
                  if (var5 != null) {
                     var5.clear();
                  }

                  this.values.remove(this.getKeyHash(var3[var4]));
               } else if (var3[var4].contains("Results")) {
                  this.values.remove(this.getKeyHash(var3[var4]));
               }
            } else if (var3[var4].startsWith("stats")) {
               Object var6 = this.get(ValuesMap.getStatsIntKeyFromString(var3[var4]));
               if (var6 instanceof SQStats) {
                  ((SQStats)var6).clearData();
               }
            }
         }
      }

      if (this.mcManipulationSimulations != null) {
         this.mcManipulationSimulations.clear();
      }

      if (this.mcRetestSimulations != null) {
         this.mcRetestSimulations.clear();
      }

      this.values.remove(this.getKeyHash("RobustnessOriginalResults"));
      this.fitnessCollection.clear();
      if (var2) {
         this.removeStockChartTempFile();
      }
   }

   private void removeStockChartTempFile() {
      this.chartsDataLock.lock();

      try {
         if (this.stockChartPath != null) {
            File var1 = new File(this.stockChartPath);

            try {
               Files.deleteIfExists(var1.toPath());
            } catch (IOException var6) {
               Log.error("Error while deleting chart file", var6);
            }

            this.stockChartPath = null;
         }
      } finally {
         this.chartsDataLock.unlock();
      }
   }

   public void freeMemory2() {
      this.clear();
   }

   public void setStrategyProblems(int var1) {
      this.resultsGroup.specialValues().set("StrategyProblems", var1);
   }

   public int getStrategyProblems() {
      return this.resultsGroup.specialValues().getInt("StrategyProblems", 0);
   }

   public void copyStats(Result var1) {
      String[] var2 = var1.getAllKeys();

      for (int var3 = 0; var3 < var2.length; var3++) {
         Object var4 = var1.get(var2[var3]);
         if (var4 instanceof SQStats) {
            try {
               this.set(var2[var3], ((SQStats)var4).getClone());
            } catch (Exception var6) {
               Log.error("Error while cloning Result's SQStats", var6);
            }
         }
      }
   }

   public String getStockChartPath() {
      return this.stockChartPath;
   }

   public void setStockChartPath(String var1) {
      this.stockChartPath = var1;
   }

   public Long2FloatRBTreeMap getWorstDailyEquity() {
      this.dailyEquityLock.lock();

      try {
         return this.worstDailyEquity;
      } finally {
         this.dailyEquityLock.unlock();
      }
   }

   public Long2FloatRBTreeMap getMaxDailyDD() {
      this.maxDailyDDLock.lock();

      try {
         return this.maxDailyDD;
      } finally {
         this.maxDailyDDLock.unlock();
      }
   }

   public Long2FloatRBTreeMap getMaxDailyDDPct() {
      this.maxDailyDDPctLock.lock();

      try {
         return this.maxDailyDDPct;
      } finally {
         this.maxDailyDDPctLock.unlock();
      }
   }

   public void setWorstDailyEquity(Long2FloatRBTreeMap var1) {
      this.dailyEquityLock.lock();

      try {
         this.worstDailyEquity = var1;
      } finally {
         this.dailyEquityLock.unlock();
      }
   }

   public void setMaxDailyDD(Long2FloatRBTreeMap var1) {
      this.maxDailyDDLock.lock();

      try {
         this.maxDailyDD = var1;
      } finally {
         this.maxDailyDDLock.unlock();
      }
   }

   public void setMaxDailyDDPct(Long2FloatRBTreeMap var1) {
      this.maxDailyDDPctLock.lock();

      try {
         this.maxDailyDDPct = var1;
      } finally {
         this.maxDailyDDPctLock.unlock();
      }
   }

   public boolean isSpecial() {
      return this.specialResult;
   }

   public void setSpecial(boolean var1) {
      this.specialResult = var1;
   }

   public void removeUnsavableSettings() {
      if (this.settings != null) {
         this.settings.removeUnsavableValues();
         this.settings.removeIgnoredKeys(ignoredSettings);
      }
   }

   public void addMCSimulation(String var1, RobustnessResults var2) {
      MCSimulations var3 = null;
      if (var1.contains("Manipul")) {
         if (this.mcManipulationSimulations == null) {
            this.mcManipulationSimulations = new MCSimulations();
         }

         var3 = this.mcManipulationSimulations;
      } else {
         if (this.mcRetestSimulations == null) {
            this.mcRetestSimulations = new MCSimulations();
         }

         var3 = this.mcRetestSimulations;
      }

      var3.add(var2);
   }

   public RobustnessResults getMCSimulation(String var1, int var2) {
      MCSimulations var3 = null;
      if (var1.contains("Manipul")) {
         var3 = this.mcManipulationSimulations;
      } else {
         var3 = this.mcRetestSimulations;
      }

      return var3 != null && var2 < var3.size() ? (RobustnessResults)var3.get(var2) : null;
   }

   public double getFitness(byte var1) {
      return this.fitnessCollection.getFitness(var1);
   }

   public void setFitness(byte var1, double var2) {
      this.fitnessCollection.setFitness(var1, var2);
   }

   public SequentialOptimizationResults getSequentialOptimizaionResults() {
      return this.sequentialOptimizationResults;
   }

   public void setSequentialOptimizationResults(SequentialOptimizationResults var1) {
      this.sequentialOptimizationResults = var1;
   }

   public ResultsGroup getResultsGroup() {
      return this.resultsGroup;
   }
}
