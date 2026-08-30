package SQ.Blocks.Indicators.WilliamsPR;

import SQ.Calculators.HighestCalculator;
import SQ.Calculators.LowestCalculator;
import SQ.Internal.IndicatorBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Indicator;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "(WILL%R) Williams Percent Range", display = "Williams %R(@Chart@#Period#)[#Shift#]", returnType = 1)
@Help("The Williams Percent Range is a momentum indicator that is designed to identify overbought and oversold areas in a nontrending market.")
@Indicator(oscillator = true, middleValue = -50.0, min = -100.0, max = 0.0, step = 1.0)
@ParameterSets({@ParameterSet(set = "Period=14"), @ParameterSet(set = "Period=20"), @ParameterSet(set = "Period=30")})
public class WilliamsPR extends IndicatorBlock {
   @Parameter
   public ChartData Chart;
   @Parameter(defaultValue = "14")
   public int Period;
   @Output(name = "Williams %R", color = "#FF0000")
   public DataSeries Value;
   private HighestCalculator highestCalculator;
   private LowestCalculator lowestCalculator;

   protected void OnInit() throws TradingException {
      this.highestCalculator = new HighestCalculator(this.Period);
      this.lowestCalculator = new LowestCalculator(this.Period);
   }

   protected void OnBarUpdate() throws TradingException {
      this.highestCalculator.onBarUpdate(this.Chart.High.get(0), this.getCurrentBar());
      this.lowestCalculator.onBarUpdate(this.Chart.Low.get(0), this.getCurrentBar());
      double var1 = this.highestCalculator.getHighestValue();
      double var3 = this.lowestCalculator.getLowestValue();
      double var5 = -100.0 * (var1 - this.Chart.Close.get(0)) / (var1 - var3 == 0.0 ? 1.0 : var1 - var3);
      this.Value.set(0, var5);
   }
}
