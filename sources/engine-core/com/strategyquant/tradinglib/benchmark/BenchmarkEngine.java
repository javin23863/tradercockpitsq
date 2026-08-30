package com.strategyquant.tradinglib.benchmark;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.data.DataException;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.data.io.VersatileData;
import com.strategyquant.datalib.data.io.newDataFormat.DataBinWriterNew;
import com.strategyquant.datalib.instrument.InstrumentManager;
import com.strategyquant.gridlib.client.GridClient;
import com.strategyquant.gridlib.client.GridMessage;
import com.strategyquant.gridlib.client.IGridMessageListener;
import com.strategyquant.gridlib.client.JobDetails;
import com.strategyquant.gridlib.client.SQGrid;
import com.strategyquant.lib.IRandomGenerator;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.random.MersenneTwisterRng;
import com.strategyquant.lib.time.SQTimeOld;
import com.strategyquant.lib.utils.SQUUID;
import com.strategyquant.tradinglib.ChartSetup;
import com.strategyquant.tradinglib.MoneyManagementMethod;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.RiskManagementMethod;
import com.strategyquant.tradinglib.TradingOption;
import com.strategyquant.tradinglib.backtest.BacktestDataType;
import com.strategyquant.tradinglib.engine.ChartSetups;
import com.strategyquant.tradinglib.fitnessfunction.IFitnessFunction;
import com.strategyquant.tradinglib.moneymanagement.MoneyManagementMethodsList;
import com.strategyquant.tradinglib.riskmanagement.RiskManagementMethodsList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BenchmarkEngine {
   public static final Logger Log = LoggerFactory.getLogger("BenchmarkEngine");
   public static final long TOTAL_TICKS = 17000000L;
   private static final int TOTAL_SINGLETHREAD_TESTS = 10;
   private static final String DATA_NAME = "BENCHMARK_DATA_TICK";
   private BenchmarkProgressEngine progressEngine;
   private ChartSetups chartSetups;
   private MoneyManagementMethod moneyManagementMethod;
   private RiskManagementMethod riskManagementMethod;
   private IFitnessFunction fitnessFunction;
   private ArrayList<TradingOption> options;
   private Element elStrategy;
   private String jobGroupID;
   private GridClient gridClient;
   private int totalTrades;
   private int finishedJobsCount;
   private ArrayList<BenchmarkResult> benchmarkResults = new ArrayList<>();
   private ThreadPoolExecutor executor;
   private ObjectArrayList<VersatileData> bigArray;
   private long dataLoadTime;

   public ArrayList<BenchmarkResult> getBenchmarkResults() {
      return this.benchmarkResults;
   }

   public BenchmarkEngine(BenchmarkProgressEngine var1) throws Exception {
      this.progressEngine = var1;
      this.moneyManagementMethod = MoneyManagementMethodsList.create("FixedSize", 0.1);
      this.riskManagementMethod = RiskManagementMethodsList.create("AllowAllTrades");
      this.fitnessFunction = null;
      this.options = new ArrayList<>();
      this.elStrategy = XMLUtil.fileToXmlElement(new File(MainApp.getDataPath() + "internal/web/benchmark/benchmark_strategy1.sqw"));
      this.jobGroupID = SQUUID.randomUUID();
      this.gridClient = SQGrid.getGridClient();
      this.gridClient.registerMessageListener(this.jobGroupID, new IGridMessageListener() {
         public void messageReceived(GridMessage var1) {
            BenchmarkEngine.this.processMessage(var1);
         }
      });
      this.executor = (ThreadPoolExecutor)Executors.newFixedThreadPool(40);
   }

   public void initData() throws Exception {
      long var1 = System.currentTimeMillis();
      this.progressEngine.reset("Init data", 17000000L);
      String var3 = "History";
      String var4 = "TICK";
      byte var5 = 1;
      DataInfo var6 = DataManager.getDataInfo(var3, "BENCHMARK_DATA_TICK");
      if (var6 != null && var6.rows > 0) {
         String var7 = DataManager.getDataFileName(var3, "BENCHMARK_DATA_TICK", var4, "No Session");
         File var8 = new File(var7);
         if (var8.exists()) {
            this.progressEngine.setProgress(100L);
            this.initChartSetup();
            return;
         }

         DataManager.deleteData(var3, "BENCHMARK_DATA_TICK");
      }

      if (!InstrumentManager.checkInstrumentExists("BENCHMARK_DATA_TICK")) {
         InstrumentManager.addInstrument(
            "BENCHMARK_DATA_TICK", "BENCHMARK_DATA_TICK", 100000.0, 1.0E-4, 1.0E-5, 0.0, 0.0, "<Method type=\"None\" use=\"true\"><Params/></Method>", (byte)3
         );
      }

      DataManager.addData(var3, "BENCHMARK_DATA_TICK", "BENCHMARK_DATA_TICK", var5, 1);
      var6 = DataManager.getDataInfo(var3, "BENCHMARK_DATA_TICK");
      DataBinWriterNew var11 = DataBinWriterNew.getInstance(2, MainApp.getDataPath(), var6.symbolInfo);
      var11.setFileName(DataManager.getDataFileName(var3, "BENCHMARK_DATA_TICK", var4, "No Session"));
      var11.open();
      VersatileData var12 = new VersatileData();
      int var9 = this.generateBenchmarkData(var11, var6, var12);
      var11.close();
      DataManager.updateData(var3, var6.symbol, SQTime.toLong(2000, 1, 1), var12.time, var9, 0L, 1, var4, null);
      this.initChartSetup();
      this.dataLoadTime = System.currentTimeMillis() - var1;
   }

   public void deleteData() {
      DataManager.deleteData("History", "BENCHMARK_DATA_TICK");
   }

   private void initChartSetup() throws DataException {
      this.chartSetups = new ChartSetups();
      this.chartSetups
         .add(new ChartSetup("History", "BENCHMARK_DATA_TICK", "H1", SQTimeOld.toLong(2000, 6, 1), SQTimeOld.toLong(2001, 6, 30), 3.5, "No Session"));
      this.chartSetups.getMainSetup().setTestPrecision(3);
   }

   private int generateBenchmarkData(DataBinWriterNew var1, DataInfo var2, VersatileData var3) throws Exception {
      MersenneTwisterRng var4 = new MersenneTwisterRng(12345L);
      int var5 = 0;
      int var6 = 0;

      for (int var7 = 0; var7 < 17000000L; var7++) {
         var5 = this.generateTestData(var3, var4, var5);
         var1.writeData(var3);
         this.progressEngine.setProgress(var6);
         var6++;
      }

      return var6;
   }

   private int generateTestData(VersatileData var1, IRandomGenerator var2, int var3) {
      if (var1.time == Long.MIN_VALUE) {
         var1.time = SQTime.toLong(2000, 1, 1);
         var1.ask = 1.5;
         var1.bid = 1.5003;
         var1.volume = 1.0;
         return 1;
      }

      var1.time = var1.time + (100 + var2.nextInt(1500));
      boolean var4 = var2.nextInt(10) <= 4;
      if (var4) {
         var1.time = SQTime.addSeconds(var1.time, 1 + var2.nextInt(7));
      }

      int var5 = var2.nextInt(10);
      boolean var6 = var2.nextInt(10) <= 2;
      if (!var6) {
         if (var3 > 0) {
            var1.ask += var5 * 1.0E-4;
         } else {
            var1.ask -= var5 * 1.0E-4;
         }
      } else {
         var3 *= -1;
         if (var3 > 0) {
            var1.ask -= var5 * 1.0E-4;
         } else {
            var1.ask += var5 * 1.0E-4;
         }
      }

      int var7 = 1 + var2.nextInt(10);
      var1.bid = var1.ask + var7 * 1.0E-4;
      return var3;
   }

   public void startTest(boolean var1, int var2, int var3) throws Exception {
      String var4 = (var1 ? "Single threaded" : "Multi threaded") + " data in " + BacktestDataType.toString(var2) + ", tests count: " + var3;
      BenchmarkResult var5 = new BenchmarkResult(var4);

      try {
         if (var1) {
            this.progressEngine.reset(var4, var3);
            this.runSingleThreadTest(var3, true);
         } else {
            this.progressEngine.reset(var4, var3);
            this.runMultiThreadTest(var3, true);
         }
      } catch (Exception | Error var15) {
         var5.setError(var15);
         Log.info("ERROR - skipping this test", var15);
         var15.printStackTrace(System.out);
      }

      if (!var5.isError()) {
         try {
            long var6 = System.currentTimeMillis();
            if (var1) {
               this.progressEngine.reset(var4, var3);
               this.runSingleThreadTest(var3, false);
            } else {
               this.progressEngine.reset(var4, var3);
               this.runMultiThreadTest(var3, false);
            }

            long var8 = System.currentTimeMillis() - var6;
            double var10 = (double)var8 / var3;
            double var12 = 3600000.0 / var10;
            var5.setResult(var8, this.dataLoadTime, var10, var12, this.totalTrades);
         } catch (Exception | Error var14) {
            var5.setError(var14);
            Log.info("ERROR - skipping this test", var14);
            var14.printStackTrace(System.out);
         }
      }

      this.benchmarkResults.add(var5);
   }

   private void runSingleThreadTest(int var1, boolean var2) throws Exception {
      if (var2) {
         var1 /= 2;
         Log.info("--- Running SingleThreadTest Initialization, tests :" + var1);
      } else {
         Log.info("--- Running SingleThreadTest, tests :" + var1);
      }

      this.totalTrades = 0;

      for (int var3 = 0; var3 < var1 && !this.progressEngine.isStopped(); var3++) {
         String var4 = String.format("Strategy 0.%d", var3);
         BenchmarkJob var5 = new BenchmarkJob(var4, this.getJobParams(var4));
         ResultsGroup var6 = var5.call();
         int var7 = var6.portfolio().stats((byte)0, (byte)10, (byte)127).getInt("NumberOfTrades");
         this.totalTrades = var7;
         Log.info("------ Test " + var3 + " finished");
         this.progressEngine.setProgress(var3);
      }
   }

   private Map<String, Serializable> getJobParams(String var1) {
      HashMap var2 = new HashMap();
      var2.put("StrategyName", var1);
      var2.put("StrategyXml", this.elStrategy.clone());
      var2.put("MoneyManagement.Method", this.moneyManagementMethod);
      var2.put("RiskManagement", this.riskManagementMethod);
      var2.put("FitnessFunction", this.fitnessFunction);
      var2.put("TradingOptions", this.options);
      var2.put("MoneyManagement.InitialCapital", 10000.0);
      var2.put("OutOfSample", null);
      var2.put("ChartSetups", this.chartSetups.getClone());
      var2.put("UseRobustnessTests", false);
      var2.put("RTMethods", null);
      var2.put("RTNumberOfSimulations", 20);
      var2.put("RTForEverySetup", true);
      return var2;
   }

   private Map<String, Serializable> getJobParams3(String var1) {
      HashMap var2 = new HashMap();
      var2.put("StrategyName", var1);
      var2.put("StrategyXml", this.elStrategy.clone());
      var2.put("MoneyManagement.Method", this.moneyManagementMethod);
      var2.put("RiskManagement", this.riskManagementMethod);
      var2.put("FitnessFunction", this.fitnessFunction);
      var2.put("TradingOptions", this.options);
      var2.put("MoneyManagement.InitialCapital", 10000);
      var2.put("OutOfSample", null);
      var2.put("ChartSetups", this.chartSetups.getClone());
      var2.put("UseRobustnessTests", false);
      var2.put("RTMethods", null);
      var2.put("RTNumberOfSimulations", 20);
      var2.put("RTForEverySetup", true);
      var2.put("BigArray", this.bigArray);
      return var2;
   }

   private void runMultiThreadTest(int var1, boolean var2) throws Exception {
      if (var2) {
         var1 = 1;
         Log.info("--- Running MultiThreadTest Initialization, tests :" + var1);
      } else {
         Log.info("--- Running MultiThreadTest, tests :" + var1);
      }

      ArrayList var3 = new ArrayList();
      this.totalTrades = 0;
      this.finishedJobsCount = 0;

      for (int var4 = 0; var4 < var1; var4++) {
         String var5 = String.format("Strategy 0.%d", var4);
         BenchmarkJob var6 = new BenchmarkJob(var5, this.getJobParams(var5));
         var3.add(var6);
      }

      this.gridClient.executeOnGrid(this.jobGroupID, var3);

      do {
         Thread.sleep(100L);
      } while (
         (this.gridClient.countRunningJobs(this.jobGroupID) != 0L || this.gridClient.countWaitingJobs(this.jobGroupID) != 0L)
            && !this.progressEngine.isStopped()
      );

      this.gridClient.stop(this.jobGroupID);
   }

   protected void processMessage(GridMessage var1) {
      if (var1.getMessageID() == 1) {
         JobDetails var2 = var1.getJobDetails();
         if (var2.isSuccess()) {
            ResultsGroup var3 = (ResultsGroup)var1.getData();

            try {
               int var4 = var3.portfolio().stats((byte)0, (byte)10, (byte)127).getInt("NumberOfTrades");
               var3.clear();
               this.totalTrades = var4;
            } catch (Exception var6) {
               var6.printStackTrace();
            }

            this.progressEngine.setProgress(this.finishedJobsCount);
         } else {
            Log.error("Job finished unsuccessfully!!!");
         }
      }
   }

   public void printResults() {
      Log.info("----------------------------------------------------------");
      Log.info("--- BENCHMARK RESULTS ---");
      Log.info("----------------------------------------------------------");
      Log.info("TEST NAME                       \t|\tDURATION\t|\tSTR. AVG TIME\t|\tSTRATEGIES PER HOUR\t|\tCHECKSUM(TRADES)\t|\tTIME PER TICK");

      for (BenchmarkResult var2 : this.benchmarkResults) {
         Log.info(var2.toString());
      }
   }

   private void runMultiThreadTest2(int var1, boolean var2) throws Exception {
      if (var2) {
         var1 = 1;
         Log.info("--- Running MultiThreadTest Initialization, tests :" + var1);
      } else {
         Log.info("--- Running MultiThreadTest, tests :" + var1);
      }

      ArrayList var3 = new ArrayList();

      for (int var4 = 0; var4 < var1; var4++) {
         String var5 = String.format("Strategy 0.%d", var4);
         BenchmarkJob var6 = new BenchmarkJob(var5, this.getJobParams(var5));
         Future var7 = this.executor.submit(var6);
         var3.add(var7);
      }

      for (Future var12 : var3) {
         try {
            ResultsGroup var13 = (ResultsGroup)var12.get();

            try {
               int var14 = var13.portfolio().stats((byte)0, (byte)10, (byte)127).getInt("NumberOfTrades");
               if (var13 != null) {
                  var13.clear();
               }

               this.totalTrades = var14;
            } catch (Exception var9) {
               var9.printStackTrace();
            }

            this.progressEngine.setProgress(this.finishedJobsCount);
         } catch (InterruptedException | ExecutionException var10) {
            var10.printStackTrace();
         }
      }
   }

   private void runMultiThreadTest3(int var1, boolean var2) throws Exception {
      if (var2) {
         var1 = 1;
         Log.info("--- Running MultiThreadTest Initialization, tests :" + var1);
      } else {
         Log.info("--- Running MultiThreadTest, tests :" + var1);
      }

      ArrayList var3 = new ArrayList();

      for (int var4 = 0; var4 < var1; var4++) {
         String var5 = String.format("Strategy 0.%d", var4);
         BenchmarkJob2 var6 = new BenchmarkJob2(var5, this.getJobParams3(var5));
         Future var7 = this.executor.submit(var6);
         var3.add(var7);
      }

      for (Future var12 : var3) {
         try {
            ResultsGroup var13 = (ResultsGroup)var12.get();

            try {
               byte var14 = 0;
               if (var13 != null) {
                  var13.clear();
               }

               this.totalTrades = var14;
            } catch (Exception var9) {
               var9.printStackTrace();
            }

            this.progressEngine.setProgress(this.finishedJobsCount);
         } catch (InterruptedException | ExecutionException var10) {
            var10.printStackTrace();
         }
      }
   }
}
