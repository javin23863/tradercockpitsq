package SQ.Blocks.StrategyControl;

import SQ.Functions.OrderFunctions;
import SQ.Internal.ValueBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.CategoryOrder;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.ILiveOrder;
import com.strategyquant.tradinglib.IgnoreInBuilder;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(name = "(MKT) MarketPosition", display = "MarketPosition(#Symbol#, #MagicNumber#, \"#Comment#\")", returnType = 1)
@Help(
   "Returns current market position, it will search for all orders that fit the criteria. If there are orders to both long and short, it will return direction of first order found."
)
@SortOrder(100)
@CategoryOrder(500)
@IgnoreInBuilder
public class MarketPosition extends ValueBlock {
   @Parameter(defaultValue = "Current", category = "Order identification", showIfDefault = false, allowAny = true)
   public String Symbol;
   @Parameter(defaultValue = "MagicNumber", category = "Order identification", showIfDefault = false)
   @Help("Magic number that can identify the order.")
   @Editor(type = 90)
   public int MagicNumber;
   @Parameter(defaultValue = "", category = "Order identification", showIfDefault = false)
   @Help("Comment can be also used to identify the order. In case of Comment, order matches if the order comments contains the text specified here.")
   public String Comment;

   @Override
   public double OnBlockEvaluate(int var1) throws TradingException {
      for (int var2 = 0; var2 < this.Strategy.Trader.getOpenOrdersCount(true); var2++) {
         ILiveOrder var3 = this.Strategy.Trader.getOpenOrder(var2, true);
         if (!var3.isPendingOrder() && OrderFunctions.identify(var3, this.Strategy, this.Symbol, 0, this.MagicNumber, this.Comment)) {
            if (var3.isLong()) {
               return 1.0;
            }

            return -1.0;
         }
      }

      return 0.0;
   }
}
