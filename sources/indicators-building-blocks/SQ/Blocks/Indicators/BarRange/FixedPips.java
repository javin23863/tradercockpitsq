package SQ.Blocks.Indicators.BarRange;

import SQ.Internal.ValueBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.NoShift;
import com.strategyquant.tradinglib.Parameter;

@BuildingBlock(name = "(FP) Fixed pips", display = "FixedPips(#Value# pips)", returnType = 7)
@NoShift
public class FixedPips extends ValueBlock {
   @Parameter(defaultValue = "50", minValue = 1.0, builderMinValue = 5.0, builderMaxValue = 500.0, maxValue = 9999999.0, step = 5.0, postfix = "pips")
   public double Value;

   @Override
   public double OnBlockEvaluate(int var1) throws TradingException {
      return this.Strategy.convertPipsToRealPrice(this.Strategy.getSymbol(), this.Value);
   }
}
