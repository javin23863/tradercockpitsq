package com.strategyquant.tradinglib.strategy;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.datalib.InstrumentInfo;
import com.strategyquant.datalib.TickEvent;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.datalib.bartype.BarType;
import com.strategyquant.datalib.bartype.BarTypeFactory;
import com.strategyquant.datalib.data.DataException;
import com.strategyquant.datalib.data.io.VersatileData;
import com.strategyquant.datalib.dataseries.TimeDataSeries;
import com.strategyquant.datalib.ticksimulator.DefaultTickSimulator;
import com.strategyquant.lib.time.SQTimeOld;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.ChartSetup;
import com.strategyquant.tradinglib.connection.Connection;
import com.strategyquant.tradinglib.connection.ConnectionManager;
import com.strategyquant.tradinglib.connection.HistoryData;
import com.strategyquant.tradinglib.engine.TradingSetup;
import com.strategyquant.tradinglib.setup.BarUpdateInfo;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MarketData {
   public static final Logger Log = LoggerFactory.getLogger("MarketData");
   protected ChartData[] chartData;
   protected TradingSetup tradingSetup;
   protected ChartSetup chartSetup;
   private Int2ObjectOpenHashMap<ChartData> chartDatasBySymbol = new Int2ObjectOpenHashMap();
   private int indyStartingBar;

   public MarketData(TradingSetup var1, ChartSetup var2) throws Exception {
      this(var1, var2, 0);
   }

   public MarketData(TradingSetup var1, ChartSetup var2, int var3) throws Exception {
      this.tradingSetup = var1;
      this.chartSetup = var2;
      this.indyStartingBar = var3;
      this.initializeChartData();
   }

   protected MarketData() {
      boolean var1 = true;
   }

   private void initializeChartData() throws Exception {
      ArrayList var1 = this.chartSetup.getCharts();
      this.chartData = new ChartData[var1.size()];
      int var2 = 0;

      for (ChartDef var4 : var1) {
         this.chartData[var2] = new ChartData(this, var4, var2, this.chartSetup.getBacktestEngine());
         this.chartData[var2].setIndyStartingBar(this.getIndyStartingBar());
         var2++;
      }
   }

   public ChartData Chart(int var1) {
      return this.chartData[var1];
   }

   public ChartData Chart(String var1) throws TradingException {
      if (var1.equals("Current")) {
         return this.chartData[0];
      }

      int var2 = var1.hashCode();

      for (ChartData var6 : this.chartData) {
         if (var6.getSymbolHash() == var2) {
            return var6;
         }
      }

      throw new TradingException(String.format("Chart for symbol '%s' was not found!", var1));
   }

   public void addChart(String var1) throws Exception {
      this.addChart(this.chartData[0].Connection, this.chartData[0].Symbol, var1);
   }

   public void addChart(String var1, String var2) throws Exception {
      this.addChart(this.chartData[0].Connection, var1, var2);
   }

   public void addChart(String var1, String var2, String var3) throws Exception {
      if (!this.tradingSetup.isInInit()) {
         throw new TradingException("You cannot call MarketData.addChart() outside the Strategy.onInit() method !");
      }

      ChartDef var4 = this.chartSetup.addChart(var1, var2, var3);
      this.chartData = Arrays.copyOf(this.chartData, this.chartData.length + 1);
      this.chartData[this.chartData.length - 1] = new ChartData(this, var4, this.chartData.length - 1, this.chartSetup.getBacktestEngine());
   }

   public int getChartsCount() {
      return this.chartSetup.getChartsCount();
   }

   public boolean processTick(TickEvent var1, BarUpdateInfo var2, int var3) throws Exception {
      if (this.chartData.length == 0) {
         return false;
      }

      int var4 = 0;
      boolean var5 = false;

      for (int var6 = 0; var6 < this.chartData.length; var6++) {
         ChartData var7 = this.chartData[var6];
         var7.processTick(var1, var3, this.chartData.length);
         if (var7.EventType != 1) {
            var4++;
            if (var6 == 0) {
               var2.updatedMainChart = true;
            }

            if (var2.updatedChart < 0) {
               var2.updatedChart = var6;
               var2.eventType = var7.EventType;
            }

            if (var7.EventType == var3) {
               var5 = true;
            }
         }
      }

      if (var4 > 1) {
         var2.multipleChartsUpdated = true;
         var2.updatedChart = -1;
         var2.eventType = 5;
      }

      return var5;
   }

   public boolean findTickInData(TickEvent var1, BarUpdateInfo var2, int var3, boolean var4) throws Exception {
      return this.findTickInData(var1, var2, var3, var4, true);
   }

   public boolean findTickInData(TickEvent var1, BarUpdateInfo var2, int var3, boolean var4, boolean var5) throws Exception {
      if (this.chartData.length == 0) {
         return false;
      }

      int var6 = 0;
      boolean var7 = false;
      var2.mainChartEventType = -1;
      if (this.chartData.length == 1) {
         ChartData var8 = this.chartData[0];
         var8.findTickInData(var1, var3, var4, var5, 0);
         if (var8.EventType != 1) {
            var6++;
            var2.updatedMainChart = true;
            if (var2.updatedChart <= 0) {
               var2.updatedChart = 0;
               var2.eventType = var8.EventType;
            }

            if (var8.EventType == var3) {
               var7 = true;
            }
         }
      } else {
         for (int var11 = 0; var11 < this.chartData.length; var11++) {
            ChartData var9 = this.chartData[var11];
            var9.findTickInData(var1, var3, var4, var5, var11);
            if (var9.EventType != 1) {
               var6++;
               if (var11 == 0) {
                  var2.updatedMainChart = true;
                  var2.mainChartEventType = var9.EventType;
               }

               if (var2.updatedChart <= 0) {
                  var2.updatedChart = var11;
                  var2.eventType = var9.EventType;
               }

               if (var9.EventType == var3) {
                  var7 = true;
               }
            }
         }

         if (var6 > 1) {
            var2.multipleChartsUpdated = true;
            var2.updatedChart = -1;
            var2.eventType = 5;
         }
      }

      return var7;
   }

   public void requestHistoryData(ConnectionManager var1, int var2) throws Exception {
      SQTimeOld var3 = new SQTimeOld();
      ChartDef var4 = this.chartSetup.getCharts().get(0);
      long var5 = this.recognizeStartTime(var4, var3);
      HashMap var7 = this.loadHistoryData(var1, var5, var3);
      this.fillMarketData(var7);
      Log.debug("Load history data finished");
   }

   private void fillMarketData(HashMap<String, HistoryData> var1) throws Exception {
      DefaultTickSimulator var2 = new DefaultTickSimulator();
      TickEvent var3 = new TickEvent();

      for (HistoryData var5 : var1.values()) {
         if (var5 != null) {
            for (VersatileData var7 : var5.getData()) {
               var2.init(var7);

               while (var2.getNextTick(var3)) {
                  for (int var8 = 0; var8 < this.chartData.length; var8++) {
                     ChartData var9 = this.chartData[var8];
                     var9.processTickSimplified(var3, this.chartData.length);
                  }
               }
            }
         }
      }
   }

   private HashMap<String, HistoryData> loadHistoryData(ConnectionManager var1, long var2, SQTimeOld var4) throws DataException {
      HashMap var5 = new HashMap();

      for (ChartDef var7 : this.chartSetup.getCharts()) {
         String var8 = var7.getConnectionName() + "_" + var7.getSymbol();
         if (!var5.containsKey(var8)) {
            Connection var9 = var1.getConnection(var7.getConnectionName());
            if (!var9.isDataFeed()) {
               throw new DataException(1, "Connection '" + var9.getConnectionName() + "' is not a Data Feed !");
            }

            HistoryData var10 = var9.getDataFeed().getHistoryData(var7, 1, var2, var4.getMilis());
            var5.put(var8, var10);
         }
      }

      return var5;
   }

   long recognizeStartTime(ChartDef var1, SQTimeOld var2) throws DataException {
      if (var1.getBackloadType() == 1) {
         SQTimeOld var7 = var2.addDaysReturnDate((int)((int)var1.getBackloadNumber() * 1.3));
         return var7.getMilis();
      }

      if (var1.getBackloadType() == 2) {
         BarType var3 = BarTypeFactory.getBarType(var1.getTimeframe(), var1.getBarTimeType());
         long var4 = var3.estimateStartDate((int)var1.getBackloadNumber() + 200, var2.getMilis());
         if (var4 == 0L) {
            SQTimeOld var6 = var2.addDaysReturnDate(-100);
            var4 = var6.getMilis();
         }

         return var4;
      } else {
         return var1.getBackloadType() == 3 ? SQTimeOld.correctDayStart(var1.getBackloadNumber()) : 0L;
      }
   }

   public void destroy() {
      if (this.chartData != null) {
         for (ChartData var4 : this.chartData) {
            var4.destroy();
         }

         this.chartData = null;
      }
   }

   public ChartData[] getChartData() {
      return this.chartData;
   }

   public InstrumentInfo getInstrumentInfo(String var1) {
      if (var1.equals("Current")) {
         return this.chartData[0].getInstrumentInfo();
      }

      int var2 = var1.hashCode();
      if (this.chartDatasBySymbol.containsKey(var2)) {
         return ((ChartData)this.chartDatasBySymbol.get(var2)).getInstrumentInfo();
      }

      for (ChartData var6 : this.chartData) {
         if (var6.getSymbolHash() == var2) {
            this.chartDatasBySymbol.put(var2, var6);
            return var6.getInstrumentInfo();
         }
      }

      return null;
   }

   public long Time(int var1) throws TradingException {
      return this.chartData[0].Time(var1);
   }

   public double Open(int var1) throws TradingException {
      return this.chartData[0].Open(var1);
   }

   public double High(int var1) throws TradingException {
      return this.chartData[0].High(var1);
   }

   public double Low(int var1) throws TradingException {
      return this.chartData[0].Low(var1);
   }

   public double Close(int var1) throws TradingException {
      return this.chartData[0].Close(var1);
   }

   public double Volume(int var1) throws TradingException {
      return this.chartData[0].Volume(var1);
   }

   public long TimeCurrent() throws TradingException {
      return this.chartData[0].TimeCurrent();
   }

   public MarketData cloneToPrepared(TradingSetup var1, ChartSetup var2, int var3) throws Exception {
      MarketData var4 = new MarketData();
      var4.tradingSetup = var1;
      var4.chartSetup = var2;
      if (var3 > 0) {
         var4.indyStartingBar = var3;
      }

      var4.chartData = new ChartData[this.chartData.length];
      ArrayList var5 = var2.getCharts();
      if (var5.size() != this.chartData.length) {
         throw new Exception("ChartDefs size is not as same as prepared chart data size !");
      }

      for (int var6 = 0; var6 < this.chartData.length; var6++) {
         var4.chartData[var6] = new PreparedChartData(this.chartData[var6], var4, var1, (ChartDef)var5.get(var6), var3);
      }

      return var4;
   }

   public void setPerformanceParams(boolean var1, boolean var2, boolean var3, boolean var4) {
      for (int var5 = 0; var5 < this.chartData.length; var5++) {
         this.chartData[var5].setPerformanceParams(var1, var2, var3, var4);
      }
   }

   public boolean isNextDay(TickEvent var1) {
      if (this.chartData.length == 0) {
         return false;
      }

      ChartData var2 = this.chartData[0];
      return var2.isNextDay(var1);
   }

   public int getIndyStartingBar() {
      return this.indyStartingBar;
   }

   public long getDateBarsFromDate(long var1, int var3) {
      TimeDataSeries var4 = this.chartData[0].Time;
      return var4 == null ? 0L : var4.getDateBarsFromDate(var1, var3);
   }
}
