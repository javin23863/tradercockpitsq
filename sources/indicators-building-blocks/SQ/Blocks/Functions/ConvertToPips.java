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

@BuildingBlock(name = "(2PIP) ConvertPriceToPips", display = "ConvertPriceToPips(#Value#)", returnType = 1)
@Help(
   "Converts price value (for example 0.002) to pips that can be used in stop loss or profit target. Example: converts 0.002 to 20. This is an opposite function to ConvertPipsToPrice()"
)
@SortOrder(1400)
@IgnoreInBuilder
@ForEngine("*,-SP,-SA")
public class ConvertToPips extends ValueBlock {
   @Parameter(defaultValue = "Current", showIfDefault = false)
   @Editor(type = 60)
   public String Symbol;
   @Parameter
   public IBlock Value;

   @Override
   public double OnBlockEvaluate(int var1) throws TradingException {
      return this.Strategy.convertRealPriceToPips(this.Symbol, this.Value.evaluateBlock(var1));
   }
}
