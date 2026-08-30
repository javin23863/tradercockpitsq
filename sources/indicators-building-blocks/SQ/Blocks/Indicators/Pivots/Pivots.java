package SQ.Blocks.Indicators.Pivots;

import SQ.Internal.IndicatorBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TimeframeManager;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;

@BuildingBlock(name = "(Pivots) Pivots", display = "Pivots(@Chart@#StartHour#:#StartMinute#).#Line#[#Shift#]", returnType = 2)
@Help("Pivots")
@ParameterSet(set = "StartHour=0,StartMinute=0")
public class Pivots extends IndicatorBlock {
   @Parameter
   public ChartData Chart;
   @Parameter(defaultValue = "0", minValue = 0.0, maxValue = 23.0, step = 1.0)
   public int StartHour;
   @Parameter(defaultValue = "0", minValue = 0.0, maxValue = 59.0, step = 1.0)
   public int StartMinute;
   @Output(name = "PP", color = "#FF0000")
   public DataSeries PP;
   @Output(name = "R1", color = "#FF0000")
   public DataSeries R1;
   @Output(name = "R2", color = "#FF0000")
   public DataSeries R2;
   @Output(name = "R3", color = "#FF0000")
   public DataSeries R3;
   @Output(name = "S1", color = "#FF0000")
   public DataSeries S1;
   @Output(name = "S2", color = "#FF0000")
   public DataSeries S2;
   @Output(name = "S3", color = "#FF0000")
   public DataSeries S3;
   private int Period;
   private int StartMinutesIntoDay;
   private int CloseMinutesIntoDay;
   private int PrevClosingBar = 1;
   private long PrevClosingTime = 0L;
   private double PrevHigh = 0.0;
   private double PrevLow = 0.0;
   private double PrevClose = 0.0;

   protected void OnBarUpdate() throws TradingException {
      if (this.getCurrentBar() == 0) {
         if (this.StartHour < 0 || this.StartHour > 23) {
            this.StartHour = 0;
         }

         if (this.StartMinute < 0 || this.StartMinute > 59) {
            this.StartMinute = 0;
         }

         this.Period = (int)(TimeframeManager.getMillis(this.Chart.Timeframe) / 60000L);
         if (this.Period != 0) {
            this.StartMinutesIntoDay = this.correctStartMinutes(this.StartHour * 60 + this.StartMinute);
            this.CloseMinutesIntoDay = this.StartMinutesIntoDay - this.Period;
            this.CloseMinutesIntoDay = this.CloseMinutesIntoDay < 0 ? this.CloseMinutesIntoDay + 1440 : this.CloseMinutesIntoDay;
         }
      } else if (this.Period != 0) {
         this.PrevClosingBar = this.findLastTimeMatch(this.CloseMinutesIntoDay, 1, this.PrevClosingBar, true);
         if (this.PrevClosingTime != this.Chart.Time(this.PrevClosingBar)) {
            this.PrevClosingTime = this.Chart.Time(this.PrevClosingBar);
            int var1 = this.findLastTimeMatch(this.StartMinutesIntoDay, this.PrevClosingBar + 1, 1000000, false);
            this.PrevHigh = this.Chart.High(this.PrevClosingBar);
            this.PrevLow = this.Chart.Low(this.PrevClosingBar);
            this.PrevClose = this.Chart.Close(this.PrevClosingBar);

            for (int var2 = this.PrevClosingBar; var2 < var1 + 1; var2++) {
               if (this.Chart.High(var2) > this.PrevHigh) {
                  this.PrevHigh = this.Chart.High(var2);
               }

               if (this.Chart.Low(var2) < this.PrevLow) {
                  this.PrevLow = this.Chart.Low(var2);
               }
            }
         }

         double var3 = (this.PrevHigh + this.PrevLow + this.PrevClose) / 3.0;
         this.PP.set(0, var3);
         this.R1.set(0, 2.0 * var3 - this.PrevLow);
         this.R2.set(0, var3 + (this.PrevHigh - this.PrevLow));
         this.R3.set(0, var3 + 2.0 * (this.PrevHigh - this.PrevLow));
         this.S1.set(0, 2.0 * var3 - this.PrevHigh);
         this.S2.set(0, var3 - (this.PrevHigh - this.PrevLow));
         this.S3.set(0, var3 - 2.0 * (this.PrevHigh - this.PrevLow));
      }
   }

   private int findLastTimeMatch(int var1, int var2, int var3, boolean var4) throws TradingException {
      int var5 = Math.min(this.CurrentBar - 1, 1440 / this.Period * 3);
      if (this.checkBarIsWhatWeLookFor(var1, var2, var4)) {
         return var2;
      }

      if (var3 < var5 && this.checkBarIsWhatWeLookFor(var1, var3, var4)) {
         return var3;
      }

      if (var3 < var5 && this.checkBarIsWhatWeLookFor(var1, var3 + 1, var4)) {
         return var3 + 1;
      }

      for (int var6 = var2 + 1; var6 < var5; var6++) {
         if (this.checkBarIsWhatWeLookFor(var1, var6, var4)) {
            return var6;
         }
      }

      return var5 + 1;
   }

   private boolean checkBarIsWhatWeLookFor(int var1, int var2, boolean var3) throws TradingException {
      int var4 = SQTime.getHour(this.Chart.Time(var2 - 1)) * 60 + SQTime.getMinute(this.Chart.Time(var2 - 1));
      int var5 = SQTime.getHour(this.Chart.Time(var2)) * 60 + SQTime.getMinute(this.Chart.Time(var2));
      int var6 = SQTime.getHour(this.Chart.Time(var2 + 1)) * 60 + SQTime.getMinute(this.Chart.Time(var2 + 1));
      if (var5 == var1) {
         return true;
      }

      int var7 = SQTime.getDayOfYear(this.Chart.Time(var2 - 1));
      int var8 = SQTime.getDayOfYear(this.Chart.Time(var2));
      int var9 = SQTime.getDayOfYear(this.Chart.Time(var2 + 1));
      if (var9 != var8) {
         var6 -= 1440;
      }

      if (var7 != var8) {
         if (var4 > var1 && var5 > var1) {
            return true;
         }

         var4 += 1440;
      }

      if (var4 > var1 && var6 < var1) {
         return var3 ? var5 < var1 : true;
      } else {
         return false;
      }
   }

   int correctStartMinutes(int var1) {
      int var2 = var1;

      while (var2 % this.Period != 0) {
         var2++;
      }

      return var2 >= 1440 ? var2 - 1440 : var2;
   }
}
