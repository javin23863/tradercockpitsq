package SQ.Blocks.StrategyControl;

import SQ.Functions.OrderFunctions;
import SQ.Internal.ValueBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.IgnoreInBuilder;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(name = "Closed P/L (in pips)", display = "Closed P/L in pips(#Symbol#, #MagicNumber#, #Direction#, \"#Comment#\")", returnType = 1)
@Help(
   "Returns last closed P/L in pips for order with given Magic Number. It will return 0 if the order hasn't closed yet. If Magic Number is 0, it will return closed P/L of last order."
)
@SortOrder(800)
@IgnoreInBuilder
@ForEngine("*,-MC,-TS,-SP,-SA")
public class ClosedPLInPips extends ValueBlock {
   @Parameter(defaultValue = "Current", category = "Order identification", showIfDefault = false, allowAny = true)
   public String Symbol;
   @Parameter(defaultValue = "0", category = "Order identification", showIfDefault = false)
   @Editor(type = 40, values = "Long=1,Short=-1,Any=0")
   public int Direction;
   @Parameter(defaultValue = "MagicNumber", category = "Order identification", showIfDefault = false)
   @Help("Magic number that can identify the order.")
   @Editor(type = 90)
   public int MagicNumber;
   @Parameter(defaultValue = "", category = "Order identification", showIfDefault = false)
   @Help("Comment can be also used to identify the order. In case of Comment, order matches if the order comments contains the text specified here.")
   public String Comment;
   @Parameter(defaultValue = "0", minValue = 0.0, maxValue = 10000.0, step = 1.0, category = "Other")
   @Help("Leave at 0 for very last trade. 1 means trade befor ethe last one, 2 means trade before that, etc.")
   public int TradesAgo;

   @Override
   public double OnBlockEvaluate(int var1) throws TradingException {
      int var2 = 0;

      for (int var3 = this.Strategy.Trader.getHistoryOrdersCount() - 1; var3 >= 0; var3--) {
         Order var4 = this.Strategy.Trader.getHistoryOrder(var3);
         if (var4.isRealOrder() && OrderFunctions.identify(var4, this.Strategy, this.Symbol, this.Direction, this.MagicNumber, this.Comment)) {
            if (var2 == this.TradesAgo) {
               return var4.isFilledOrder() ? this.getPLInPips(var4) : 0.0;
            }

            var2++;
         }
      }

      return 0.0;
   }

   private double getPLInPips(Order var1) {
      double var2 = 0.0;
      if (var1.isLong()) {
         var2 = var1.ClosePrice - var1.OpenPrice;
      } else {
         var2 = var1.OpenPrice - var1.ClosePrice;
      }

      return var2 / this.Strategy.getInstrumentInfo().tickSize;
   }
}
