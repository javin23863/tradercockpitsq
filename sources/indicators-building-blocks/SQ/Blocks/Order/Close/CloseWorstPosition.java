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

@BuildingBlock(name = "Close least profitable position", returnType = 4)
@Help("Close least profitable position that fits the criteria")
@SortOrder(400)
@IgnoreInBuilder
@ForEngine("*,-MC,-TS,-SP,-SA")
public class CloseWorstPosition extends ActionBlock {
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

   @Override
   public void OnAction() throws TradingException {
      double var1 = Double.MAX_VALUE;
      ILiveOrder var3 = null;

      for (int var4 = this.Strategy.Trader.getOpenOrdersCount(false) - 1; var4 >= 0; var4--) {
         ILiveOrder var5 = this.Strategy.Trader.getOpenOrder(var4, false);
         if (OrderFunctions.identify(var5, this.Strategy, this.Symbol, this.Direction, this.MagicNumber, this.Comment)) {
            double var6 = var5.getPL();
            if (var3 == null || var6 < var1) {
               var3 = var5;
               var1 = var6;
            }
         }
      }

      if (var3 != null) {
         var3.setExitIndex((byte)-1);
         var3.Close((byte)1);
      }
   }
}
