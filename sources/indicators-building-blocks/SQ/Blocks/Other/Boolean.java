package SQ.Blocks.Other;

import SQ.Internal.ValueBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.CategoryOrder;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.IgnoreInBuilder;
import com.strategyquant.tradinglib.NotFirstValue;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(name = "(BOOL) Boolean", display = "#Value#", returnType = 3)
@Help("Number constant")
@SortOrder(100)
@CategoryOrder(700)
@NotFirstValue
@IgnoreInBuilder
public class Boolean extends ValueBlock {
   @Parameter(defaultValue = "false")
   public boolean Value;

   public Boolean() {
   }

   public Boolean(boolean var1) {
      this.Value = var1;
   }

   @Override
   public double OnBlockEvaluate(int var1) throws TradingException {
      return this.Value ? 1 : 0;
   }
}
