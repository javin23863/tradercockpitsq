package SQ.Blocks.Indicators.AvgVolume;

import SQ.Calculators.AverageCalculator;
import SQ.Internal.IndicatorBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Indicator;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "(AV) Average Volume", display = "AV(@Chart@#Period#)[#Shift#]", returnType = 1)
@Indicator(min = 1.0, max = 1.0E7, step = 1.0)
@ParameterSets({@ParameterSet(set = "Period=14"), @ParameterSet(set = "Period=20"), @ParameterSet(set = "Period=30")})
public class AvgVolume extends IndicatorBlock {
   @Parameter(defaultChartIndex = 0)
   public ChartData Chart;
   @Parameter(defaultValue = "14", minValue = 2.0, maxValue = 10000.0, step = 1.0)
   public int Period;
   @Output(name = "AvgVolume", color = "#FF0000")
   public DataSeries Value;
   private AverageCalculator averageCalculator;

   protected void OnInit() throws TradingException {
      this.averageCalculator = new AverageCalculator(0, this.Period);
   }

   protected void OnBarUpdate() throws TradingException {
      this.averageCalculator.onBarUpdate(this.Chart.Volume.get(0), this.getCurrentBar());
      this.Value.set(0, this.averageCalculator.getValue());
   }
}
