package SQ.Blocks.Order.Close;

import SQ.Functions.OrderFunctions;
import SQ.Internal.ActionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.CategoryOrder;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.IFormula;
import com.strategyquant.tradinglib.ILiveOrder;
import com.strategyquant.tradinglib.IgnoreInBuilder;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.Required;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(returnType = 4)
@Help("Close specified position or its part")
@SortOrder(100)
@CategoryOrder(300)
@IgnoreInBuilder
public class ClosePosition extends ActionBlock {
   @Parameter(defaultValue = "Current", category = "Order identification", showIfDefault = false, allowAny = true)
   public String Symbol;
   @Parameter(defaultValue = "0", category = "Order identification")
   @Editor(type = 40, values = "Long=1,Short=-1,Any=0")
   public int Direction;
   @Parameter(defaultValue = "MagicNumber", category = "Order identification", showIfDefault = false)
   @Help("Magic number that can identify the order.")
   @Editor(type = 90)
   public int MagicNumber;
   @Parameter(defaultValue = "", category = "Order identification", showIfDefault = false)
   @Help("Comment can be also used to identify the order. In case of Comment, order matches if the order comments contains the text specified here.")
   public String Comment;
   @Parameter(category = "Order size")
   @Editor(type = 200, formulaName = "CloseSize")
   @Required
   public IFormula Size;

   @Override
   public void OnAction() throws TradingException {
      for (int var1 = 0; var1 < this.Strategy.Trader.getOpenOrdersCount(false); var1++) {
         ILiveOrder var2 = this.Strategy.Trader.getOpenOrder(var1, false);
         if (OrderFunctions.identify(var2, this.Strategy, this.Symbol, this.Direction, this.MagicNumber, this.Comment)) {
            var2.setExitIndex((byte)-1);
            var2.Close((byte)22);
            return;
         }
      }
   }
}
