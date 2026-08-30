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
import com.strategyquant.tradinglib.strategy.LiveOrderObj;

@BuildingBlock(name = "Open P/L (in money)", display = "Open P/L in money(#Symbol#, #MagicNumber#, #Direction#, \"#Comment#\")", returnType = 1)
@Help("Returns open P/L for specified order in money. If you'll not specify any order, it will return sum of open P/L for all active orders.")
@SortOrder(700)
@IgnoreInBuilder
public class OpenPLInMoney extends ValueBlock {
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
      double var2 = 0.0;

      for (int var4 = 0; var4 < this.Strategy.Trader.getOpenOrdersCount(true); var4++) {
         ILiveOrder var5 = this.Strategy.Trader.getOpenOrder(var4, true);
         if (OrderFunctions.identify(var5, this.Strategy, this.Symbol, this.Direction, this.MagicNumber, this.Comment)) {
            ((LiveOrderObj)var5).setTickData(this.Strategy.Ask(), this.Strategy.Bid());
            var2 += var5.getPL();
         }
      }

      return var2;
   }
}
