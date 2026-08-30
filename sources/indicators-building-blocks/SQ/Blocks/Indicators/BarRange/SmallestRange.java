package SQ.Blocks.Indicators.BarRange;

import SQ.Internal.IndicatorBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Indicator;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "Smallest Range", display = "SmallestRange(@Chart@#Period#)[#Shift#]", returnType = 7)
@Indicator(min = 0.0, max = 5000.0, step = 0.001)
@Help("returns value of smallest range (high - low of one candle) of candles in given period")
@OppositeBlock("SmallestRange")
@ParameterSets(
   {
         @ParameterSet(set = "Period=3"),
         @ParameterSet(set = "Period=5"),
         @ParameterSet(set = "Period=10"),
         @ParameterSet(set = "Period=14"),
         @ParameterSet(set = "Period=20"),
         @ParameterSet(set = "Period=25"),
         @ParameterSet(set = "Period=30"),
         @ParameterSet(set = "Period=40"),
         @ParameterSet(set = "Period=50")
   }
)
public class SmallestRange extends IndicatorBlock {
   @Parameter
   public ChartData Input;
   @Parameter(defaultValue = "14")
   public int Period;
   @Output
   public DataSeries Value;

   protected void OnBarUpdate() throws TradingException {
      double var1 = 1.0E8;

      for (int var3 = 0; var3 < this.Period && var3 <= this.CurrentBar; var3++) {
         double var4 = SQUtils.round(this.Input.High.get(var3) - this.Input.Low.get(var3), 8);
         if (var4 < var1) {
            var1 = var4;
         }
      }

      this.Value.set(var1);
   }
}
