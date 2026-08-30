package SQ.Blocks.StrategyControl;

import SQ.Functions.OrderFunctions;
import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.ILiveOrder;
import com.strategyquant.tradinglib.IgnoreInBuilder;
import com.strategyquant.tradinglib.Parameter;

@BuildingBlock(display = "Market Position(#Symbol#, #MagicNumber#, \"#Comment#\") Is Flat", returnType = 3)
@IgnoreInBuilder
public class MarketPositionIsFlat extends ConditionBlock {
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
   public boolean OnBlockEvaluate() throws TradingException {
      for (int var1 = 0; var1 < this.Strategy.Trader.getOpenOrdersCount(true); var1++) {
         ILiveOrder var2 = this.Strategy.Trader.getOpenOrder(var1, true);
         if (!var2.isPendingOrder() && OrderFunctions.identify(var2, this.Strategy, this.Symbol, 0, this.MagicNumber, this.Comment)) {
            return false;
         }
      }

      return true;
   }
}
