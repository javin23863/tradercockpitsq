package SQ.Blocks.Order.Open;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.ATM;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.ILiveOrder;
import com.strategyquant.tradinglib.SortOrder;
import com.strategyquant.tradinglib.simulator.Engines;
import java.util.ArrayList;

@BuildingBlock(name = "(LMT) Enter at limit", display = "EnterAtLimit", returnType = 8)
@Help("Opens limit order at given price")
@SortOrder(300)
public class EnterAtLimit extends EnterAtStop {
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

            int var12 = this.Direction > 0 ? 3 : 4;
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
}
