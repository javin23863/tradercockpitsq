package SQ.Blocks.Order.Open;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.ATM;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.ILiveOrder;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(name = "(MKT) Enter/reverse at market", display = "Enter/ReverseAtMarket", returnType = 8)
@Help("Opens order at current market price<br/> If there already is existing order to an opposite direction, it closes it first.")
@SortOrder(110)
@ForEngine("*,-SP,-SA")
public class EnterReverseAtMarket extends EnterAtMarket {
   @Override
   public void OnAction() throws TradingException {
      int var1 = this.Direction > 0 ? 1 : 2;
      ATM var2 = this.Strategy.getATM();
      boolean var3 = var2 != null && var2.isApplicable(this.Strategy, 0.0, 0.0, (byte)var1);
      if (var3) {
         for (ILiveOrder var4 = this.checkLiveOrderExists(this.Direction > 0 ? -1 : 1, false);
            var4 != null;
            var4 = this.checkLiveOrderExists(this.Direction > 0 ? -1 : 1, false)
         ) {
            var4.Close((byte)9);
         }
      } else {
         ILiveOrder var10 = this.checkLiveOrderExists(this.Direction > 0 ? -1 : 1, false);
         if (var10 != null) {
            var10.Close((byte)9);
         }
      }

      if (this.AllowDuplicateTrades && this.engineSupportsDuplicateTrades() || this.checkLiveOrderExists(this.Direction, false) == null) {
         double var11 = this.computeSL(
            (byte)var1, this.Direction > 0 ? this.Strategy.MarketData.Chart(this.Symbol).Ask() : this.Strategy.MarketData.Chart(this.Symbol).Bid()
         );
         double var6 = this.computeSize((byte)var1, 0.0, var11);
         if (var3) {
            double var8 = this.computePT(
               (byte)var1, this.Direction > 0 ? this.Strategy.MarketData.Chart(this.Symbol).Ask() : this.Strategy.MarketData.Chart(this.Symbol).Bid()
            );
            this.openATMOrder(var2, -1.0, var6, var11, var8, (byte)var1, 0);
         } else {
            this.openNormalOrder(-1.0, var6, var11, (byte)var1, 0);
         }
      }
   }
}
