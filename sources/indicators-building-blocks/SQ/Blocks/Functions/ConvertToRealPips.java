package SQ.Blocks.Functions;

import SQ.Internal.ValueBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.IgnoreInBuilder;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(name = "(2RPIP) ConvertPipsToPrice", display = "ConvertPipsToPrice(#Value#)", returnType = 7)
@Help(
   "Converts pips to real price that can be used as price level for price, stop loss or profit target. Example: converts 20 to 0.002. This is an opposite function to ConvertPriceToPips()"
)
@SortOrder(1300)
@IgnoreInBuilder
@ForEngine("*,-SP,-SA")
public class ConvertToRealPips extends ValueBlock {
   @Parameter(defaultValue = "Current", showIfDefault = false)
   @Editor(type = 60)
   public String Symbol;
   @Parameter
   public IBlock Value;

   @Override
   public double OnBlockEvaluate(int var1) throws TradingException {
      return this.Strategy.convertPipsToRealPrice(this.Symbol, this.Value.evaluateBlock(var1));
   }
}
