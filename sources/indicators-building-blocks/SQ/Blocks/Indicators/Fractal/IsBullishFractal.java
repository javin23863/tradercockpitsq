package SQ.Blocks.Indicators.Fractal;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.SortOrder;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "Is Bullish Fractal", display = "Is Bullish Fractal(#Fractal#)[#Shift#]", returnType = 3)
@Help("Is triggered in case of Bullish Fractal (trend reversal)")
@OppositeBlock("IsBearishFractal")
@SortOrder(200)
@ParameterSets({@ParameterSet(set = "Fractal=3"), @ParameterSet(set = "Fractal=5")})
public class IsBullishFractal extends ConditionBlock {
   @Parameter
   public ChartData Input;
   @Parameter(name = "Fractal", defaultValue = "3")
   @Editor(type = 40, values = "3-bar=3,5-bar=5")
   public int Fractal;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      Fractal var1 = this.Strategy.Indicators.Fractal(this.Input, this.Fractal);
      return var1.Down.get(this.Shift) > 0.0;
   }
}
