package SQ.Blocks.StrategyControl;

import SQ.Functions.OrderFunctions;
import SQ.Internal.ValueBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.IgnoreInBuilder;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(display = "BarsSinceOrderClosed(#Symbol#, #MagicNumber#, #Direction#, \"#Comment#\")", returnType = 1)
@Help("Returns number of bars since the specified order was opened.")
@SortOrder(500)
@IgnoreInBuilder
public class BarsSinceOrderClosed extends ValueBlock {
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
   public double OnBlockEvaluate(int var1) throws TradingException {
      Order var2 = OrderFunctions.findLastHistoryOrder(this.Strategy, this.Symbol, this.Direction, this.MagicNumber, this.Comment);
      if (var2 != null) {
         long var3 = var2.CloseTime;
         int var5 = 0;

         for (int var6 = 0; var6 < 10000; var6++) {
            if (var3 < this.Strategy.Time(var6)) {
               var5++;
            }
         }

         return var5;
      } else {
         return -1.0;
      }
   }
}
