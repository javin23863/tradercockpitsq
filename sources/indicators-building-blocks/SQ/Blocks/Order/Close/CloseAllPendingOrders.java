package SQ.Blocks.Order.Close;

import SQ.Functions.OrderFunctions;
import SQ.Internal.ActionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.ILiveOrder;
import com.strategyquant.tradinglib.IgnoreInBuilder;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(returnType = 4)
@Help("Close all pending order that fit the criteria")
@SortOrder(100)
@IgnoreInBuilder
@ForEngine("*,-MC,-TS,-SP,-SA")
public class CloseAllPendingOrders extends ActionBlock {
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
   public void OnAction() throws TradingException {
      int var1 = this.Strategy.Trader.getOpenOrdersCount(false);

      for (int var2 = var1 - 1; var2 >= 0; var2--) {
         ILiveOrder var3 = this.Strategy.Trader.getOpenOrder(var2, false);
         if (var3.isPendingOrder() && OrderFunctions.identify(var3, this.Strategy, this.Symbol, this.Direction, this.MagicNumber, this.Comment)) {
            var3.Close((byte)22);
         }
      }
   }
}
