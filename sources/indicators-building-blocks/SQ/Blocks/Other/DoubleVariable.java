package SQ.Blocks.Other;

import SQ.Internal.ValueBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.IgnoreInBuilder;
import com.strategyquant.tradinglib.NotFirstValue;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(name = "(DVAR) Double Variable", display = "#Variable#", returnType = 1)
@Help("Double Variable")
@SortOrder(300)
@NotFirstValue
@IgnoreInBuilder
public class DoubleVariable extends ValueBlock {
   @Parameter
   @Editor(type = 80)
   public double Variable;

   @Override
   public double OnBlockEvaluate(int var1) throws TradingException {
      return this.Variable;
   }
}
