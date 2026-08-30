package SQ.Blocks.Indicators.AnchoredVWAP;

import SQ.Internal.IndicatorBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.Buffer;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "(AVWAP) Anchored VWAP", display = "AnchoredVWAP(@Chart@#SessionType#,#StdDevMult#)[#Shift#]", returnType = 2)
@Help("Anchored VWAP with standard deviation bands. Resets at session start (Day/Week/Month).")
@ForEngine("MT4,MT5,TS,MC")
@ParameterSets(
   {
         @ParameterSet(set = "SessionType=1,StdDevMult=1.0"),
         @ParameterSet(set = "SessionType=1,StdDevMult=2.0"),
         @ParameterSet(set = "SessionType=2,StdDevMult=1.0"),
         @ParameterSet(set = "SessionType=2,StdDevMult=2.0"),
         @ParameterSet(set = "SessionType=3,StdDevMult=1.0"),
         @ParameterSet(set = "SessionType=3,StdDevMult=2.0")
   }
)
public class AnchoredVWAP extends IndicatorBlock {
   @Parameter(defaultChartIndex = 0)
   public ChartData Chart;
   @Parameter(defaultValue = "1", name = "SessionType")
   @Help("Anchor point for VWAP calculation")
   @Editor(type = 40, values = "Daily=1,Weekly=2,Monthly=3")
   public int SessionType;
   @Parameter(defaultValue = "1.0", name = "StdDevMult", minValue = 0.5, maxValue = 4.0, step = 0.1)
   @Help("Standard deviation multiplier for bands")
   public double StdDevMult;
   @Output(name = "VWAP", color = "#000000")
   public DataSeries VWAP;
   @Buffer
   public DataSeries cumPV;
   @Buffer
   public DataSeries cumVolume;
   @Buffer
   public DataSeries cumPV2;
   private long currentSessionEnd = 0L;

   protected void OnInit() throws TradingException {
   }

   protected void OnBarUpdate() throws TradingException {
      this.SessionType = SQUtils.fixAllowedRange(this.SessionType, 1, 3, 1);
      long var1 = this.Chart.Time(0);
      double var3 = (this.Chart.High(0) + this.Chart.Low(0) + this.Chart.Close(0)) / 3.0;
      double var5 = this.Chart.Volume(0);
      if (var5 <= 0.0) {
         var5 = 1.0;
      }

      boolean var7 = this.currentSessionEnd == 0L || var1 >= this.currentSessionEnd;
      if (var7) {
         this.calculateSessionBoundaries(var1);
         this.cumPV.set(0, var3 * var5);
         this.cumVolume.set(0, var5);
         this.cumPV2.set(0, var3 * var3 * var5);
      } else {
         double var8 = this.cumPV.get(1);
         double var10 = this.cumVolume.get(1);
         double var12 = this.cumPV2.get(1);
         this.cumPV.set(0, var8 + var3 * var5);
         this.cumVolume.set(0, var10 + var5);
         this.cumPV2.set(0, var12 + var3 * var3 * var5);
      }

      double var20 = this.cumPV.get(0);
      double var21 = this.cumVolume.get(0);
      double var22 = this.cumPV2.get(0);
      double var14 = var20 / var21;
      double var16 = var22 / var21 - var14 * var14;
      if (var16 < 0.0) {
         var16 = 0.0;
      }

      double var18 = Math.sqrt(var16);
      this.VWAP.set(0, var14);
   }

   private void calculateSessionBoundaries(long var1) throws TradingException {
      long var3 = SQTime.setTime(var1, 0, 0, 0, 0);
      switch (this.SessionType) {
         case 1:
            this.currentSessionEnd = SQTime.addDays(var3, 1);
            break;
         case 2:
            long var5 = SQTime.setDayOfWeek(var3, 1);
            if (var5 > var3) {
               var5 = SQTime.addDays(var5, -7);
            }

            this.currentSessionEnd = SQTime.addDays(var5, 7);
            break;
         case 3:
            long var7 = SQTime.setDayOfMonth(var3, 1);
            this.currentSessionEnd = SQTime.addMonths(var7, 1);
            break;
         default:
            this.currentSessionEnd = SQTime.addDays(var3, 1);
      }
   }
}
