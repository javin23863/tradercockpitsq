package SQ.Blocks.Other;

import SQ.Internal.ValueBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.CategoryOrder;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.NotFirstValue;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(name = "(NUM) Number", display = "#Number#", returnType = 1)
@Help("Number constant")
@SortOrder(100)
@CategoryOrder(700)
@NotFirstValue
public class Number extends ValueBlock {
   @Parameter(defaultValue = "0", minValue = -9.99999999E8, maxValue = 9.99999999E8)
   @Editor(type = 10)
   public double Number;

   @Override
   public double OnBlockEvaluate(int var1) throws TradingException {
      return this.Number;
   }
}
