package SQ.Blocks.Indicators.GannHiLo;

import SQ.Calculators.AverageCalculator;
import SQ.Internal.IndicatorBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.Buffer;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "(GHL) GannHiLo", display = "GannHiLo(@Chart@#Period#)[#Shift#]", returnType = 2)
@Help("GannHiLo help text")
@ParameterSets(
   {
         @ParameterSet(set = "Period=12"),
         @ParameterSet(set = "Period=24"),
         @ParameterSet(set = "Period=48"),
         @ParameterSet(set = "Period=120"),
         @ParameterSet(set = "Period=5"),
         @ParameterSet(set = "Period=10")
   }
)
public class GannHiLo extends IndicatorBlock {
   @Parameter
   public ChartData Chart;
   @Parameter(defaultValue = "10", isPeriod = true, minValue = 2.0, maxValue = 1000.0, step = 1.0)
   public int Period;
   @Output
   public DataSeries GHA;
   @Buffer
   public DataSeries GHHigh;
   @Buffer
   public DataSeries GHLow;
   private AverageCalculator highAvgCalculator;
   private AverageCalculator lowAvgCalculator;
   private int hld;
   private int hlv;

   protected void OnInit() throws TradingException {
      this.highAvgCalculator = new AverageCalculator(0, this.Period);
      this.lowAvgCalculator = new AverageCalculator(0, this.Period);
   }

   protected void OnBarUpdate() throws TradingException {
      if (this.getCurrentBar() < this.Period) {
         this.GHA.set(0, 0.0);
      } else if (this.getCurrentBar() > this.Period) {
         this.highAvgCalculator.onBarUpdate(this.Chart.High.get(0), this.getCurrentBar());
         this.lowAvgCalculator.onBarUpdate(this.Chart.Low.get(0), this.getCurrentBar());
         this.GHHigh.set(0, this.highAvgCalculator.getValue());
         this.GHLow.set(0, this.lowAvgCalculator.getValue());
         if (this.Chart.Close.get(0) > this.GHHigh.get(1)) {
            this.hld = 1;
         } else if (this.Chart.Close.get(0) < this.GHLow.get(1)) {
            this.hld = -1;
         } else {
            this.hld = 0;
         }

         if (this.hld != 0) {
            this.hlv = this.hld;
         }

         if (this.hlv == -1) {
            this.GHA.set(0, this.GHHigh.get(0));
         } else {
            this.GHA.set(0, this.GHLow.get(0));
         }
      }
   }
}
