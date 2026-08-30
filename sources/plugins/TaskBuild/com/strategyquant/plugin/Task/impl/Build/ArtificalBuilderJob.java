package com.strategyquant.plugin.Task.impl.Build;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.gridlib.client.GridMessage;
import com.strategyquant.lib.IRandomGenerator;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.random.MersenneTwisterRng;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.backtestrunner.BacktestResult;
import com.strategyquant.tradinglib.engine.ChartSetups;
import com.strategyquant.tradinglib.project.ILastEventListener;
import com.strategyquant.tradinglib.project.StopPauseEngine;
import com.strategyquant.tradinglib.strategy.OutOfSample;
import java.io.Serializable;
import java.util.Map;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArtificalBuilderJob extends BuilderJob {
   private static final Logger Log = LoggerFactory.getLogger("ArtificalBuilderJob");
   private int taskLength = 2000;
   private StopPauseEngine stopPauseEngine;
   private IRandomGenerator rng;

   public ArtificalBuilderJob(String var1, Map<String, Serializable> var2, ILastEventListener var3) throws Exception {
      super(var1, var2, var3, null);
      this.rng = new MersenneTwisterRng((Long)((Serializable)var2.get("RandomSeed")));
      this.stopPauseEngine = new StopPauseEngine();
   }

   @Override
   public BacktestResult call() throws Exception {
      if (Log.isInfoEnabled()) {
         Log.info(String.format("--- Job %s started", this.strategyName));
      }

      this.stopPauseEngine.reset();
      this.stopPauseEngine.start();
      int var1 = this.taskLength / 50;

      while (var1 > 0) {
         var1--;
         if (this.stopPauseEngine.isStopped()) {
            return new BacktestResult("Task stopped - Strategy test not finished", 10002, null);
         }

         Thread.sleep(50L);
      }

      try {
         return this.createArtificalStrategy();
      } catch (Exception var3) {
         Log.error("Exception ", var3);
         return new BacktestResult(String.format("%s exception in generation", this.strategyName, var3.getMessage()), 10001, null);
      }
   }

   private BacktestResult createArtificalStrategy() throws Exception {
      ChartSetups var1 = new ChartSetups("History", "TEST_GBPUSD_M1", "H1", SQTime.toLong(2008, 6, 1), SQTime.toLong(2010, 8, 31), 2.5, "No Session");
      OutOfSample var2 = new OutOfSample();
      var2.addRange(SQTime.toLong(2008, 9, 21), SQTime.toLong(2008, 11, 13), (byte)21);
      var2.addRange(SQTime.toLong(2009, 3, 14), SQTime.toLong(2009, 5, 16), (byte)21);
      var2.addRange(SQTime.toLong(2010, 1, 1), SQTime.toLong(2010, 3, 26), (byte)21);
      SettingsMap var3 = new SettingsMap();
      var3.set("ChartSetups", var1);
      var3.set("MoneyManagement.InitialCapital", 20000);
      var3.set("OutOfSample", var2);
      var3.set("StrategyClass", "TestStrategy1");
      String var4 = "ArtificalTest1";
      ResultsGroup var5 = new ResultsGroup(var4);
      OrdersList var6 = generateTestOrders(50 + this.rng.nextInt(3000));
      String var7 = "Setup 1";
      var5.addSubresult(var7, var3, null);

      for (ChartDef var9 : var1.getMainSetup().getCharts()) {
         var5.symbols().add(var9.getSymbol(), var9.getSymbolInfo().instrument, var9.getSymbolInfo());
         var5.addResultSymbol(var7, var9.getSymbol());
      }

      SettingsMap var11 = var5.portfolio().getSettings();
      var11.setIfNotExists("MoneyManagement.InitialCapital", 20000);
      var5.orders().addAll(var6);

      for (int var12 = 0; var12 < var5.orders().size(); var12++) {
         Order var10 = var5.orders().get(var12);
         var10.SetupName = var7;
         var10.SampleType = var2.getSampleType(var10);
      }

      var6.clear();
      var5.computeAllStats();
      return new BacktestResult(var5, null);
   }

   public static OrdersList generateTestOrders(int var0) {
      OrdersList var1 = new OrdersList("generateTestOrders");
      long var2 = SQTime.toLong(2008, 6, 10);
      long var4 = SQTime.toLong(2010, 8, 31);
      Random var6 = new Random();

      for (int var7 = 0; var7 < var0; var7++) {
         Order var8 = new Order();
         var8.Symbol = "TEST_GBPUSD_M1";
         var8.OpenTime = var2 + var6.nextInt((int)(var4 - var2));
         var8.CloseTime = var2 + var6.nextInt((int)(var4 - var2));
         if (var8.CloseTime > var8.OpenTime) {
            long var9 = var8.CloseTime;
            var8.CloseTime = var8.OpenTime;
            var8.OpenTime = var9;
         }

         var8.OpenPrice = (float)(1.2345 + var6.nextDouble());
         var8.ClosePrice = (float)(1.2345 + var6.nextDouble());
         var8.Size = 0.1F;
         var8.Type = (byte)(var6.nextInt(2) == 1 ? 1 : 2);
         var8.CloseType = 1;
         var8.SampleType = 127;
         var1.add(var8);
      }

      return var1;
   }

   @Override
   public void messageReceived(GridMessage var1) {
      this.stopPauseEngine.stop();
   }
}
