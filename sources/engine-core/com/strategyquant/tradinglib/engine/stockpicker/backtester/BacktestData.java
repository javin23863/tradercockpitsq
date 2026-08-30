package com.strategyquant.tradinglib.engine.stockpicker.backtester;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.ChartSetup;
import com.strategyquant.tradinglib.engine.stockpicker.data.LoadedPickerData;
import com.strategyquant.tradinglib.engine.stockpicker.data.stockGroups.LoadedStockGroupData;
import com.strategyquant.tradinglib.engine.stockpicker.data.symbols.LoadedSymbolData;
import com.strategyquant.tradinglib.engine.stockpicker.signals.CollectedSignals;
import com.strategyquant.tradinglib.engine.stockpicker.signals.Signals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BacktestData {
   public static final Logger Log = LoggerFactory.getLogger("BacktestData");
   private String symbol;
   private LoadedPickerData pickerData;
   private CollectedSignals signals;
   private ChartSetup chartSetup;
   private int currentBarD;
   private int currentBarW;
   private int currentBarM;

   public BacktestData(ChartSetup var1, LoadedPickerData var2, CollectedSignals var3) {
      this.chartSetup = var1;
      this.pickerData = var2;
      this.signals = var3;
      this.currentBarD = 0;
      this.currentBarW = 0;
      this.currentBarM = 0;
   }

   public double Open(int var1, int var2) throws TradingException {
      LoadedSymbolData var3 = this.getChartData(var1);
      switch (this.getChartTimeframe(var1)) {
         case "D1":
            return var3.OpenD(this.currentBarD - var2);
         case "Weekly":
            return var3.OpenW(this.currentBarW - var2);
         case "Monthly":
            return var3.OpenM(this.currentBarM - var2);
         default:
            return 0.0;
      }
   }

   public double OpenD(int var1, int var2) throws TradingException {
      return this.getChartData(var1).OpenD(this.currentBarD - var2);
   }

   public double OpenW(int var1, int var2) throws TradingException {
      return this.getChartData(var1).OpenW(this.currentBarW - var2);
   }

   public double OpenM(int var1, int var2) throws TradingException {
      return this.getChartData(var1).OpenM(this.currentBarM - var2);
   }

   public double Close(int var1, int var2) throws TradingException {
      LoadedSymbolData var3 = this.getChartData(var1);
      switch (this.getChartTimeframe(var1)) {
         case "D1":
            return var3.CloseD(this.currentBarD - var2);
         case "Weekly":
            return var3.CloseW(this.currentBarW - var2);
         case "Monthly":
            return var3.CloseM(this.currentBarM - var2);
         default:
            return 0.0;
      }
   }

   public double CloseD(int var1, int var2) throws TradingException {
      return this.getChartData(var1).CloseD(this.currentBarD - var2);
   }

   public double CloseW(int var1, int var2) throws TradingException {
      return this.getChartData(var1).CloseW(this.currentBarW - var2);
   }

   public double CloseM(int var1, int var2) throws TradingException {
      return this.getChartData(var1).CloseM(this.currentBarM - var2);
   }

   public double High(int var1, int var2) throws TradingException {
      LoadedSymbolData var3 = this.getChartData(var1);
      switch (this.getChartTimeframe(var1)) {
         case "D1":
            return var3.HighD(this.currentBarD - var2);
         case "Weekly":
            return var3.HighW(this.currentBarW - var2);
         case "Monthly":
            return var3.HighM(this.currentBarM - var2);
         default:
            return 0.0;
      }
   }

   public double HighD(int var1, int var2) throws TradingException {
      return this.getChartData(var1).HighD(this.currentBarD - var2);
   }

   public double HighW(int var1, int var2) throws TradingException {
      return this.getChartData(var1).HighW(this.currentBarW - var2);
   }

   public double HighM(int var1, int var2) throws TradingException {
      return this.getChartData(var1).HighM(this.currentBarM - var2);
   }

   public double Low(int var1, int var2) throws TradingException {
      LoadedSymbolData var3 = this.getChartData(var1);
      switch (this.getChartTimeframe(var1)) {
         case "D1":
            return var3.LowD(this.currentBarD - var2);
         case "Weekly":
            return var3.LowW(this.currentBarW - var2);
         case "Monthly":
            return var3.LowM(this.currentBarM - var2);
         default:
            return 0.0;
      }
   }

   public double LowD(int var1, int var2) throws TradingException {
      return this.getChartData(var1).LowD(this.currentBarD - var2);
   }

   public double LowW(int var1, int var2) throws TradingException {
      return this.getChartData(var1).LowW(this.currentBarW - var2);
   }

   public double LowM(int var1, int var2) throws TradingException {
      return this.getChartData(var1).LowM(this.currentBarM - var2);
   }

   public double VolumeD(int var1, int var2) throws TradingException {
      return this.getChartData(var1).VolumeD(this.currentBarD - var2);
   }

   public double VolumeW(int var1, int var2) throws TradingException {
      return this.getChartData(var1).VolumeW(this.currentBarW - var2);
   }

   public double VolumeM(int var1, int var2) throws TradingException {
      return this.getChartData(var1).VolumeM(this.currentBarM - var2);
   }

   public long TimeD(int var1) throws TradingException {
      return this.pickerData.timeline.indexToTimeD(this.currentBarD - var1);
   }

   public long TimeW(int var1) throws TradingException {
      return this.pickerData.timeline.indexToTimeW(this.currentBarW - var1);
   }

   public long TimeM(int var1) throws TradingException {
      return this.pickerData.timeline.indexToTimeM(this.currentBarM - var1);
   }

   public LoadedSymbolData getChartData(int var1) {
      return var1 != 0 && this.pickerData.additionalData.get(var1 - 1) != null
         ? (LoadedSymbolData)this.pickerData.additionalData.get(var1 - 1)
         : this.pickerData.stockGroupData.getSymbolData(this.symbol);
   }

   public LoadedSymbolData getChartSymbolData(int var1, String var2) {
      return var1 != 0 && this.pickerData.additionalData.get(var1 - 1) != null
         ? (LoadedSymbolData)this.pickerData.additionalData.get(var1 - 1)
         : this.pickerData.stockGroupData.getSymbolData(var2);
   }

   public String getChartTimeframe(int var1) {
      ChartDef var2 = this.chartSetup.getCharts().get(var1);
      return var2.getTimeframe();
   }

   public void setCurrentBar(int var1) throws TradingException {
      this.currentBarD = var1;
      long var2 = this.pickerData.timeline.indexToTimeD(var1);
      long var4 = SQTime.setFirstDayOfWeek(var2);
      long var6 = SQTime.setDayOfMonth(var2, 1);
      this.currentBarW = this.pickerData.timeline.timeToIndexW(var4);
      this.currentBarM = this.pickerData.timeline.timeToIndexM(var6);
   }

   public double Volume(int var1, int var2) throws TradingException {
      LoadedSymbolData var3 = this.getChartData(var1);
      switch (this.getChartTimeframe(var1)) {
         case "D1":
            return var3.VolumeD(this.currentBarD - var2);
         case "Weekly":
            return var3.VolumeW(this.currentBarW - var2);
         case "Monthly":
            return var3.VolumeM(this.currentBarM - var2);
         default:
            return 0.0;
      }
   }

   public int getCurrentBar(int var1) {
      switch (this.getChartTimeframe(var1)) {
         case "D1":
            return this.currentBarD;
         case "Weekly":
            return this.currentBarW;
         case "Monthly":
            return this.currentBarM;
         default:
            return 0;
      }
   }

   public long getCurrentTime() throws TradingException {
      return this.pickerData.timeline.indexToTimeD(this.currentBarD);
   }

   public long getTime(int var1) throws TradingException {
      return this.pickerData.timeline.indexToTimeD(this.currentBarD - var1);
   }

   public int getCurrentBar() {
      return this.currentBarD;
   }

   public LoadedPickerData getPickerData() {
      return this.pickerData;
   }

   public LoadedStockGroupData getStockGroupData() {
      return this.pickerData.stockGroupData;
   }

   public Signals Signals(long var1, boolean var3) {
      if (!this.signals.containsKey(var1) && var3) {
         this.signals.put(var1, new Signals());
      }

      return (Signals)this.signals.get(var1);
   }

   public String getSymbol(int var1) {
      if (var1 == 0) {
         return this.symbol;
      }

      ChartDef var2 = this.chartSetup.getCharts().get(var1);
      String var3 = var2.getSymbol();
      String var4 = this.chartSetup.getMainChart().getSymbol();
      return var3.equals(var4) ? this.symbol : var3;
   }

   public void init(String var1) {
      this.symbol = var1;
      this.currentBarD = 0;
      this.currentBarW = 0;
      this.currentBarM = 0;
   }

   public boolean exists(int var1) throws TradingException {
      return this.OpenD(0, var1) > 0.0;
   }
}
