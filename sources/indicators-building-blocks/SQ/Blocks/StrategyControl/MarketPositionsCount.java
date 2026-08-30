package SQ.Blocks.StrategyControl;

import SQ.Functions.OrderFunctions;
import SQ.Internal.ValueBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.ILiveOrder;
import com.strategyquant.tradinglib.IgnoreInBuilder;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(display = "MarketPositionsCount(#Symbol#, #MagicNumber#, #Direction#, \"#Comment#\")", returnType = 1)
@Help("Returns number of market positions that fit the criteria.")
@SortOrder(300)
@IgnoreInBuilder
public class MarketPositionsCount extends ValueBlock {
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
      int var2 = 0;

      for (int var3 = 0; var3 < this.Strategy.Trader.getOpenOrdersCount(true); var3++) {
         ILiveOrder var4 = this.Strategy.Trader.getOpenOrder(var3, true);
         if (!var4.isPendingOrder() && OrderFunctions.identify(var4, this.Strategy, this.Symbol, this.Direction, this.MagicNumber, this.Comment)) {
            var2++;
         }
      }

      return var2;
   }
}
