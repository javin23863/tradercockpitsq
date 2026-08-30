package SQ.Blocks.Order.Modify;

import SQ.Functions.OrderFunctions;
import SQ.Internal.ActionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.CategoryOrder;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.IFormula;
import com.strategyquant.tradinglib.ILiveOrder;
import com.strategyquant.tradinglib.IgnoreInBuilder;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(name = "(SL) Set Stop Loss", display = "Set Stop Loss", returnType = 4)
@Help("Sets Stop Loss to a specified level. If SL already exists it will be moved.")
@SortOrder(200)
@CategoryOrder(200)
@IgnoreInBuilder
@ForEngine("*,-MC,-TS,-SP,-SA")
public class SetStopLoss extends ActionBlock {
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
   @Parameter(category = "Stop Loss")
   @Editor(type = 200, formulaName = "RangeLevel")
   public IFormula StopLoss;

   @Override
   public void OnAction() throws TradingException {
      for (int var1 = 0; var1 < this.Strategy.Trader.getOpenOrdersCount(false); var1++) {
         ILiveOrder var2 = this.Strategy.Trader.getOpenOrder(var1, false);
         double var3 = var2.isNettingMode() ? var2.getLastOpenPrice() : var2.getOpenPrice();
         if (OrderFunctions.identify(var2, this.Strategy, this.Symbol, this.Direction, this.MagicNumber, this.Comment)) {
            double var5 = this.StopLoss.evaluateFormula(this.Strategy, this.Symbol, var3, this.Direction);
            var2.setSL(var5).Send();
         }
      }
   }
}
