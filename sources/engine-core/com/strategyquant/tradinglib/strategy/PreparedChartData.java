package com.strategyquant.tradinglib.strategy;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.datalib.bartype.BarTypeFactory;
import com.strategyquant.datalib.data.DataException;
import com.strategyquant.datalib.dataseries.PreparedDataSeries;
import com.strategyquant.datalib.dataseries.PreparedTimeDataSeries;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.engine.TradingSetup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreparedChartData extends ChartData {
   public static final Logger Log = LoggerFactory.getLogger("ChartData");

   public PreparedChartData(ChartData var1, MarketData var2, TradingSetup var3, ChartDef var4, int var5) throws DataException {
      this.serieIndex = var1.getSerieIndex();
      this.connectionHash = var1.getConnectionHash();
      this.symbolHash = var1.getSymbolHash();
      if (var2 != null || var4 != null) {
         this.chartDef = var4;
         this.MarketData = var2;
         this.Connection = var4.getConnectionName();
         this.Symbol = var4.getSymbol();
         this.Instrument = var4.getInstrument();
         this.connectionHash = var4.getConnectionHash();
         this.symbolHash = var4.getSymbolHash();
         this.Timeframe = var4.getTimeframe();
         this.session = var4.getSession();
         this.SymbolInfo = var4.getSymbolInfo();
         this.Time = new PreparedTimeDataSeries(var1.Time, var5);
         this.Open = new PreparedDataSeries(var1.Open, var5);
         this.High = new PreparedDataSeries(var1.High, var5);
         this.Low = new PreparedDataSeries(var1.Low, var5);
         this.Close = new PreparedDataSeries(var1.Close, var5);
         this.Volume = new PreparedDataSeries(var1.Volume, var5);
         if (this.usesSameDailyDataSeries()) {
            this.TimeD = this.Time;
            this.OpenD = this.Open;
            this.HighD = this.High;
            this.LowD = this.Low;
            this.CloseD = this.Close;
            this.TimeW = this.Time;
            this.OpenW = this.Open;
            this.HighW = this.High;
            this.LowW = this.Low;
            this.CloseW = this.Close;
            this.TimeM = this.Time;
            this.OpenM = this.Open;
            this.HighM = this.High;
            this.LowM = this.Low;
            this.CloseM = this.Close;
         } else {
            this.TimeD = null;
            this.OpenD = null;
            this.HighD = null;
            this.LowD = null;
            this.CloseD = null;
            this.TimeW = null;
            this.OpenW = null;
            this.HighW = null;
            this.LowW = null;
            this.CloseW = null;
            this.TimeM = null;
            this.OpenM = null;
            this.HighM = null;
            this.LowM = null;
            this.CloseM = null;
         }

         this.Time.setShift(0);
         int var6 = this.Time.size();
         this.Time.setShift(var6 - 1);
         this.initializeComputedDataSeries();
         this.barType = BarTypeFactory.getBarType(this.Timeframe, var4.getBarTimeType()).clone();
         if (var5 > 0) {
            this.setIndyStartingBar(var5);
         } else {
            this.setIndyStartingBar(var1.getIndyStartingBar());
         }
      }
   }

   @Override
   public void destroy() {
      if (!this.usesSameDailyDataSeries()) {
         if (this.TimeD != null) {
            this.TimeD.destroy();
            this.TimeD = null;
         }

         if (this.OpenD != null) {
            this.OpenD.destroy();
            this.OpenD = null;
         }

         if (this.HighD != null) {
            this.HighD.destroy();
            this.HighD = null;
         }

         if (this.LowD != null) {
            this.LowD.destroy();
            this.LowD = null;
         }

         if (this.CloseD != null) {
            this.CloseD.destroy();
            this.CloseD = null;
         }

         if (this.TimeW != null) {
            this.TimeW.destroy();
            this.TimeW = null;
         }

         if (this.OpenW != null) {
            this.OpenW.destroy();
            this.OpenW = null;
         }

         if (this.HighW != null) {
            this.HighW.destroy();
            this.HighW = null;
         }

         if (this.LowW != null) {
            this.LowW.destroy();
            this.LowW = null;
         }

         if (this.CloseW != null) {
            this.CloseW.destroy();
            this.CloseW = null;
         }

         if (this.TimeM != null) {
            this.TimeM.destroy();
            this.TimeM = null;
         }

         if (this.OpenM != null) {
            this.OpenM.destroy();
            this.OpenM = null;
         }

         if (this.HighM != null) {
            this.HighM.destroy();
            this.HighM = null;
         }

         if (this.LowM != null) {
            this.LowM.destroy();
            this.LowM = null;
         }

         if (this.CloseM != null) {
            this.CloseM.destroy();
            this.CloseM = null;
         }
      }

      if (this.Median != null) {
         this.Median.destroy();
         this.Median = null;
      }

      if (this.Typical != null) {
         this.Typical.destroy();
         this.Typical = null;
      }

      if (this.Weighted != null) {
         this.Weighted.destroy();
         this.Weighted = null;
      }
   }
}
