package SQ.Blocks.Indicators.Fibo;

import SQ.Internal.IndicatorBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.NoShift;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "(FIB) Fibo", display = "Fibo(@Chart@)", returnType = 2)
@NoShift
@ParameterSets(
   {
         @ParameterSet(set = "FiboRange=1,FiboLevel=23.6"),
         @ParameterSet(set = "FiboRange=1,FiboLevel=-23.6"),
         @ParameterSet(set = "FiboRange=1,FiboLevel=38.2"),
         @ParameterSet(set = "FiboRange=1,FiboLevel=-38.2"),
         @ParameterSet(set = "FiboRange=1,FiboLevel=61.8"),
         @ParameterSet(set = "FiboRange=1,FiboLevel=-61.8"),
         @ParameterSet(set = "FiboRange=2,FiboLevel=23.6"),
         @ParameterSet(set = "FiboRange=2,FiboLevel=-23.6"),
         @ParameterSet(set = "FiboRange=2,FiboLevel=38.2"),
         @ParameterSet(set = "FiboRange=2,FiboLevel=-38.2"),
         @ParameterSet(set = "FiboRange=2,FiboLevel=61.8"),
         @ParameterSet(set = "FiboRange=2,FiboLevel=-61.8"),
         @ParameterSet(set = "FiboRange=5,FiboLevel=23.6"),
         @ParameterSet(set = "FiboRange=5,FiboLevel=-23.6"),
         @ParameterSet(set = "FiboRange=5,FiboLevel=38.2"),
         @ParameterSet(set = "FiboRange=5,FiboLevel=-38.2"),
         @ParameterSet(set = "FiboRange=5,FiboLevel=61.8"),
         @ParameterSet(set = "FiboRange=5,FiboLevel=-61.8"),
         @ParameterSet(set = "FiboRange=6,FiboLevel=23.6"),
         @ParameterSet(set = "FiboRange=6,FiboLevel=-23.6"),
         @ParameterSet(set = "FiboRange=6,FiboLevel=38.2"),
         @ParameterSet(set = "FiboRange=6,FiboLevel=-38.2"),
         @ParameterSet(set = "FiboRange=6,FiboLevel=61.8"),
         @ParameterSet(set = "FiboRange=6,FiboLevel=-61.8")
   }
)
public class Fibo extends IndicatorBlock {
   @Parameter(defaultChartIndex = 0)
   public ChartData Chart;
   @Parameter(defaultValue = "1", name = "FiboRange")
   @Help("Choose range of Fibo indicator")
   @Editor(
      type = 40,
      values = "High-Low previous day=1,High-low previous week=2,High-low previous month=3,Open-Close previous day=5,Open-Close previous week=6,Open-Close previous month=7"
   )
   public int FiboRange;
   @Parameter(name = "FiboLevel", defaultValue = "76.4")
   @Help("Only for some of Fibo ranges - select value of X here")
   @Editor(
      type = 40,
      values = "-423.6=-423.6,-261.8=-261.8,-161.8=-161.8,-127.2=-127.2,-100.0=-100.0,-78.6=-78.6,-61.8=-61.8,-50.0=-50.0,-38.2=-38.2,-23.6=-23.6,0.0=0.0,23.6=23.6,38.2=38.2,50.0=50.0,61.8=61.8,78.6=78.6,100.0=100.0,127.2=127.2,161.8=161.8,261.8=261.8,423.6=423.6"
   )
   public double FiboLevel;
   @Output(name = "Value", color = "#008000")
   public DataSeries Value;
   private long tfEndTime = 0L;
   private int barsUsed = -1;
   private double prevTFOpen;
   private double prevTFHigh;
   private double prevTFLow;
   private double prevTFClose;
   private double fiboLevel;

   protected void OnBarUpdate() throws TradingException {
      this.FiboRange = SQUtils.fixAllowedRange(this.FiboRange, 1, 7, 1);
      if (this.isNewTFStart()) {
         double var1 = 0.0;
         double var3 = 0.0;
         switch (this.FiboRange) {
            case 0:
            case 1:
            case 2:
            case 3:
               var1 = this.prevTFHigh;
               var3 = this.prevTFLow;
            case 4:
            default:
               break;
            case 5:
            case 6:
            case 7:
               var1 = Math.max(this.prevTFOpen, this.prevTFClose);
               var3 = Math.min(this.prevTFOpen, this.prevTFClose);
         }

         double var5 = this.FiboLevel;
         double var7 = (var1 - var3) / 100.0;
         double var9 = var5 * var7;
         boolean var11 = this.prevTFClose > this.prevTFOpen;
         this.fiboLevel = var11 ? var1 - var9 : var3 + var9;
         this.prevTFOpen = this.Chart.Open(0);
         this.prevTFHigh = this.Chart.High(0);
         this.prevTFLow = this.Chart.Low(0);
         this.prevTFClose = this.Chart.Close(0);
         this.barsUsed = 1;
      } else {
         this.prevTFHigh = Math.max(this.prevTFHigh, this.Chart.High(0));
         this.prevTFLow = Math.min(this.prevTFLow, this.Chart.Low(0));
         this.prevTFClose = this.Chart.Close(0);
         this.barsUsed++;
      }

      this.Value.set(0, this.fiboLevel);
   }

   private boolean isNewTFStart() throws TradingException {
      long var1 = this.Chart.Time(0);
      switch (this.FiboRange) {
         case 0:
         case 1:
         case 2:
         case 3:
         case 4:
         case 5:
         case 6:
         case 7:
         case 8:
         default:
            if (this.tfEndTime != 0L && this.tfEndTime > var1) {
               return false;
            }

            this.setEndTime();
            return true;
         case 9:
         case 10:
            return this.barsUsed == -1;
      }
   }

   private void setEndTime() throws TradingException {
      long var1 = SQTime.setTime(this.Chart.Time(0), 0, 0, 0, 0);
      switch (this.FiboRange) {
         case 0:
         case 1:
         case 5:
            this.tfEndTime = SQTime.addDays(var1, 1);
            break;
         case 2:
         case 6:
            this.tfEndTime = SQTime.setDayOfWeek(var1, 1);
            this.tfEndTime = SQTime.addDays(this.tfEndTime, 7);
            break;
         case 3:
         case 7:
            this.tfEndTime = SQTime.setDayOfMonth(var1, 1);
            this.tfEndTime = SQTime.addMonths(this.tfEndTime, 1);
         case 4:
      }
   }
}
