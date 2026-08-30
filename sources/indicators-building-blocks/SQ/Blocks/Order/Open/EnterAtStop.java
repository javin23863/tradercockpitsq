package SQ.Blocks.Order.Open;

import SQ.Functions.OrderFunctions;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.ATM;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.IFormula;
import com.strategyquant.tradinglib.ILiveOrder;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.Required;
import com.strategyquant.tradinglib.SortOrder;
import com.strategyquant.tradinglib.simulator.Engines;
import java.util.ArrayList;

@BuildingBlock(name = "(STOP) Enter at stop", display = "EnterAtStop", returnType = 8)
@Help("Opens stop order at given price")
@SortOrder(200)
public class EnterAtStop extends EnterAtMarket {
   @Parameter(category = "Basic")
   @Editor(type = 200, formulaName = "Price")
   @Required
   public IFormula Price;
   @Parameter(
      category = "Advanced",
      defaultValue = "0",
      minValue = 0.0,
      maxValue = 1000.0,
      step = 1.0,
      builderMinValue = 0.0,
      builderMaxValue = 200.0,
      showIfDefault = false
   )
   @Help("Number of bars this pending order will be valid, after then it expires. 0 means it never expires.")
   @ForEngine("*,-TS,-MC")
   public int BarsValid;
   @Parameter(defaultValue = "true", name = "Replace Existing Order", category = "Order identification", showIfDefault = false)
   @Help("If existing stop order with the same identification exist, should it be replaced?")
   public boolean ReplaceExisting;

   @Override
   public void OnAction() throws TradingException {
      if (this.AllowDuplicateTrades && this.engineSupportsDuplicateTrades() || this.checkLiveOrderExists(0, true) == null) {
         if (this.Strategy == null || this.Strategy.Trader == null || this.Strategy.Trader.IsMarketOpen()) {
            ArrayList var1 = this.checkPendingOrdersExists(this.Direction);
            if (var1 != null) {
               if (!this.ReplaceExisting && !Engines.isTradestationEngine(this.Strategy.getEngine())) {
                  return;
               }

               for (int var2 = 0; var2 < var1.size(); var2++) {
                  ((ILiveOrder)var1.get(var2)).Close((byte)9);
               }
            }

            int var12 = this.Direction > 0 ? 5 : 6;
            double var3 = this.Price.evaluateFormula(this.Strategy, this.Symbol, 0.0, this.Direction);
            if (var3 == -9.9999999E7) {
               throw new TradingException("Open price not defined");
            }

            var3 = SQUtils.fixPrice(this.Strategy.getInstrumentInfo().tickStep, var3);
            if (this.checkOpenPriceWithinRange(var3)) {
               double var5 = this.computeSL((byte)var12, var3);
               double var7 = this.computeSize((byte)var12, var3, var5);
               ATM var9 = this.Strategy.getATM();
               if (var9 != null && var9.isApplicable(this.Strategy, var7, var5, (byte)var12)) {
                  double var10 = this.computePT((byte)var12, var3);
                  this.openATMOrder(var9, var3, var7, var5, var10, (byte)var12, this.BarsValid);
               } else {
                  this.openNormalOrder(var3, var7, var5, (byte)var12, this.BarsValid);
               }
            }
         }
      }
   }

   protected ArrayList<ILiveOrder> checkPendingOrdersExists(int var1) {
      ArrayList var2 = null;
      boolean var3 = this.Strategy.getEngine() == 1441180233 || this.Strategy.getEngine() == 56756755 || this.Strategy.getEngine() == 938213070;

      for (int var4 = 0; var4 < this.Strategy.Trader.getOpenOrdersCount(false); var4++) {
         ILiveOrder var5 = this.Strategy.Trader.getOpenOrder(var4, false);
         if (OrderFunctions.identify(var5, this.Strategy, this.Symbol, var1, this.MagicNumber, this.Comment)
            && var5.isPendingOrder()
            && (!var3 || !var5.usesATM())) {
            if (var2 == null) {
               var2 = new ArrayList();
            }

            var2.add(var5);
         }
      }

      return var2;
   }
}
