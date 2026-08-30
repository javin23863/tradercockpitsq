package SQ.Blocks.StrategyControl;

import SQ.Functions.OrderFunctions;
import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.ILiveOrder;
import com.strategyquant.tradinglib.IgnoreInBuilder;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(display = "Pending order(#Symbol#, #MagicNumber#, #Direction#, \"#Comment#\") exists", returnType = 3)
@SortOrder(400)
@Help("Is triggered when there exists specified pending order")
@IgnoreInBuilder
@ForEngine("*,-MC,-TS,-SP,-SA")
public class PendingOrderExists extends ConditionBlock {
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

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      for (int var1 = 0; var1 < this.Strategy.Trader.getOpenOrdersCount(false); var1++) {
         ILiveOrder var2 = this.Strategy.Trader.getOpenOrder(var1, false);
         if (OrderFunctions.identify(var2, this.Strategy, this.Symbol, this.Direction, this.MagicNumber, this.Comment) && var2.isPendingOrder()) {
            return true;
         }
      }

      return false;
   }
}
