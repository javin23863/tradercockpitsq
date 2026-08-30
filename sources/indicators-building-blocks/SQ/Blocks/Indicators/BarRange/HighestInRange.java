package SQ.Blocks.Indicators.BarRange;

import SQ.Internal.IndicatorBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Indicator;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;

@BuildingBlock(name = "Highest in range", display = "HighestInRange(@Chart@#TimeFrom#, #TimeTo#)[#Shift#]", returnType = 2)
@Help("returns highest high of candles in given time range")
@Indicator
@OppositeBlock("LowestInRange")
public class HighestInRange extends IndicatorBlock {
   @Parameter
   public ChartData Input;
   @Parameter(defaultValue = "0", minValue = 0.0, maxValue = 2359.0, step = 30.0)
   @Help("Time in format HHMM, for example 945 or 1430")
   public int TimeFrom;
   @Parameter(defaultValue = "0", minValue = 0.0, maxValue = 2359.0, step = 30.0)
   @Help("Time in format HHMM, for example 945 or 1430")
   public int TimeTo;
   @Output
   public DataSeries Value;
   private final int DayMillis = 86400000;
   private final int HourMillis = 3600000;
   private final int MinuteMillis = 60000;
   private long timeFrom = -1L;
   private long timeTo = -1L;
   private double highestValue = 0.0;
   private double lastValue;
   private double lastUsableValue;

   protected void OnBarUpdate() throws TradingException {
      if (this.getCurrentBar() == 0) {
         long var1 = SQTime.correctDayStart(this.Input.Time(0));
         this.timeFrom = var1 + this.TimeFrom / 100 * 3600000 + this.TimeFrom % 100 * 60000;
         this.timeTo = var1 + this.TimeTo / 100 * 3600000 + this.TimeTo % 100 * 60000;
         if (this.timeTo < this.timeFrom) {
            this.timeTo += 86400000L;
         }
      }

      long var5 = this.Input.Time(0);
      if (var5 >= this.timeTo) {
         long var3 = SQTime.correctDayStart(var5);
         this.timeFrom = var3 + this.TimeFrom / 100 * 3600000 + this.TimeFrom % 100 * 60000;
         this.timeTo = var3 + this.TimeTo / 100 * 3600000 + this.TimeTo % 100 * 60000;
         this.lastValue = this.highestValue;
         this.highestValue = 0.0;
         if (this.timeTo <= var5) {
            if (this.TimeTo < this.TimeFrom) {
               this.timeTo += 86400000L;
            } else {
               this.timeFrom += 86400000L;
               this.timeTo += 86400000L;
            }
         }

         if (this.timeFrom <= var5) {
            this.highestValue = this.Input.High(0);
         }
      } else if (var5 >= this.timeFrom) {
         this.highestValue = Math.max(this.highestValue, this.Input.High(0));
      } else {
         this.highestValue = 0.0;
      }

      if (this.lastValue > 0.0) {
         this.lastUsableValue = this.lastValue;
         this.Value.set(0, this.lastValue);
      } else {
         this.Value.set(0, this.lastUsableValue);
      }
   }
}
