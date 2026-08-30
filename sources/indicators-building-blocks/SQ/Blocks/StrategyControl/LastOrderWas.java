package SQ.Blocks.StrategyControl;

import SQ.Functions.OrderFunctions;
import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.IgnoreInBuilder;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(display = "Last order(#Symbol#, #MagicNumber#, \"#Comment#\") was #Direction#", returnType = 3)
@Help("Returns true if direction of last order matches. Considers only executed orders, not pending orders that were closed.")
@SortOrder(600)
@IgnoreInBuilder
@ForEngine("*,-MC,-TS,-SP,-SA")
public class LastOrderWas extends ConditionBlock {
   @Parameter(defaultValue = "Current", category = "Order identification", showIfDefault = false, allowAny = true)
   public String Symbol;
   @Parameter(defaultValue = "MagicNumber", category = "Order identification", showIfDefault = false)
   @Help("Magic number that can identify the order.")
   @Editor(type = 90)
   public int MagicNumber;
   @Parameter(defaultValue = "", category = "Order identification", showIfDefault = false)
   @Help("Comment can be also used to identify the order. In case of Comment, order matches if the order comments contains the text specified here.")
   public String Comment;
   @Parameter(defaultValue = "1", category = "Direction")
   @Editor(type = 40, values = "Long=1,Short=-1")
   public int Direction;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      for (int var1 = this.Strategy.Trader.getHistoryOrdersCount() - 1; var1 >= 0; var1--) {
         Order var2 = this.Strategy.Trader.getHistoryOrder(var1);
         if (OrderFunctions.identify(var2, this.Strategy, this.Symbol, 0, this.MagicNumber, this.Comment) && var2.isFilledOrder()) {
            if ((this.Direction <= 0 || !var2.isLong()) && (this.Direction >= 0 || !var2.isShort())) {
               return false;
            }

            return true;
         }
      }

      return false;
   }
}
