package SQ.Blocks.Functions;

import SQ.Internal.ValueBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.IgnoreInBuilder;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(name = "(CUST) Custom function", display = "#Function#", returnType = 0)
@Help(
   "Call your own function. You can specify any MQL command here or call a function defined in /user/extend/Code/{Platform}/CustomFunctions folder. You are responsible for using the function correctly"
)
@SortOrder(1600)
@IgnoreInBuilder
public class CustomFunction extends ValueBlock {
   @Parameter(defaultValue = "")
   @Editor(type = 30)
   public String Function;

   @Override
   public double OnBlockEvaluate(int var1) throws TradingException {
      throw new TradingException("Custom Function is not implemented in StrategyQuant!");
   }
}
