package SQ.Blocks.StrategyControl;

import SQ.Functions.OrderFunctions;
import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.IgnoreInBuilder;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(display = "TradeRecentlyClosed(#Symbol#, #MagicNumber#)", returnType = 3)
@Help("Returns true if there was active trade that was closed recently - which means at the same bar or even the same minute.")
@SortOrder(400)
@IgnoreInBuilder
@ForEngine("*,-MC,-TS,-SP,-SA")
public class TradeRecentlyClosed extends ConditionBlock {
   @Parameter(defaultValue = "Current", category = "Order identification", showIfDefault = false, allowAny = true)
   public String Symbol;
   @Parameter(defaultValue = "MagicNumber", category = "Order identification", showIfDefault = false)
   @Help("Magic number that can identify the order.")
   @Editor(type = 90)
   public int MagicNumber;
   @Parameter(defaultValue = "true", category = "Close Condition")
   @Help("Check if trade closed this bar.")
   public boolean CheckThisBar;
   @Parameter(defaultValue = "true", category = "Close Condition")
   @Help("Check if trade closed in the last minute.")
   public boolean CheckThisMinute;
   private int lastIndexChecked = 0;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      return this.checkTradeClosedRecently();
   }

   protected boolean checkTradeClosedRecently() throws TradingException {
      int var1 = this.Strategy.Trader.getHistoryOrdersCount();
      if (var1 == this.lastIndexChecked) {
         return false;
      }

      long var2 = this.Strategy.Time(0);
      long var4 = 0L;
      if (this.CheckThisMinute) {
         var4 = SQTime.setSecond(var2, 0);
         var4 = SQTime.setMiliSeconds(var4, 0);
      }

      int var6 = 0;
      if (this.lastIndexChecked < 0) {
         this.lastIndexChecked = 0;
      }

      for (int var7 = var1 - 1; var7 >= this.lastIndexChecked; var7--) {
         Order var8 = this.Strategy.Trader.getHistoryOrder(var7);
         if (OrderFunctions.identify(var8, this.Strategy, this.Symbol, 0, this.MagicNumber, null) && var8.isFilledOrder()) {
            if (++var6 > 10) {
               break;
            }

            if (this.CheckThisBar && var8.CloseTime >= var2) {
               return true;
            }

            if (this.CheckThisMinute && var8.CloseTime >= var4) {
               return true;
            }
         }
      }

      this.lastIndexChecked = var1;
      return false;
   }
}
