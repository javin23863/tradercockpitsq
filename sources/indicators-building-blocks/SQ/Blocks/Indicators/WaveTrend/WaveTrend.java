package SQ.Blocks.Indicators.WaveTrend;

import SQ.Internal.IndicatorBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.Buffer;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.Indicator;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "(WAVT) WaveTrend", display = "WaveTrend(@Chart@#ChannelLength#,#AverageLength#).#Line#[#Shift#]")
@Indicator(oscillator = true, middleValue = 0.0, min = -80.0, max = 80.0, step = 0.1)
@ForEngine("MT4,MT5,TS,MC")
@ParameterSets(
   {
         @ParameterSet(set = "ChannelLength=10,AverageLength=21"),
         @ParameterSet(set = "ChannelLength=9,AverageLength=12"),
         @ParameterSet(set = "ChannelLength=14,AverageLength=21"),
         @ParameterSet(set = "ChannelLength=10,AverageLength=21,ComputedFrom=0"),
         @ParameterSet(set = "ChannelLength=9,AverageLength=12,ComputedFrom=0"),
         @ParameterSet(set = "ChannelLength=14,AverageLength=21,ComputedFrom=0")
   }
)
public class WaveTrend extends IndicatorBlock {
   @Parameter(defaultChartIndex = 0)
   public ChartData Chart;
   @Parameter(category = "Default", name = "ChannelLength", minValue = 2.0, maxValue = 200.0, defaultValue = "10", step = 1.0, isPeriod = true)
   public int ChannelLength;
   @Parameter(category = "Default", name = "AverageLength", minValue = 2.0, maxValue = 200.0, defaultValue = "21", step = 1.0, isPeriod = true)
   public int AverageLength;
   @Output(name = "WT1", color = "#008000")
   public DataSeries WT1;
   @Output(name = "WT2", color = "#FF0000")
   public DataSeries WT2;
   @Buffer
   public DataSeries avgPrice;
   @Buffer
   public DataSeries esa;
   @Buffer
   public DataSeries d;
   @Buffer
   public DataSeries ci;
   private double esaMultiplier;
   private double dMultiplier;
   private double tciMultiplier;
   private double wt2Multiplier;

   protected void OnInit() throws TradingException {
      this.esaMultiplier = 2.0 / (this.ChannelLength + 1.0);
      this.dMultiplier = 2.0 / (this.ChannelLength + 1.0);
      this.tciMultiplier = 2.0 / (this.AverageLength + 1.0);
      this.wt2Multiplier = 0.4;
   }

   protected void OnBarUpdate() throws TradingException {
      double var1 = (this.Chart.High.get(0) + this.Chart.Low.get(0) + this.Chart.Close.get(0)) / 3.0;
      this.avgPrice.set(0, var1);
      if (this.getCurrentBar() == 0) {
         this.esa.set(0, var1);
         this.d.set(0, 0.0);
         this.ci.set(0, 0.0);
         this.WT1.set(0, 0.0);
         this.WT2.set(0, 0.0);
      } else {
         double var3 = this.esaMultiplier * var1 + (1.0 - this.esaMultiplier) * this.esa.get(1);
         this.esa.set(0, var3);
         double var5 = Math.abs(var1 - var3);
         double var7 = this.dMultiplier * var5 + (1.0 - this.dMultiplier) * this.d.get(1);
         this.d.set(0, var7);
         if (var7 != 0.0) {
            double var9 = (var1 - var3) / (0.015 * var7);
            this.ci.set(0, var9);
            double var11 = this.tciMultiplier * var9 + (1.0 - this.tciMultiplier) * this.WT1.get(1);
            this.WT1.set(0, var11);
            double var13 = this.wt2Multiplier * var11 + (1.0 - this.wt2Multiplier) * this.WT2.get(1);
            this.WT2.set(0, var13);
         } else {
            this.ci.set(0, 0.0);
            this.WT1.set(0, this.WT1.get(1));
            this.WT2.set(0, this.WT2.get(1));
         }
      }
   }
}
