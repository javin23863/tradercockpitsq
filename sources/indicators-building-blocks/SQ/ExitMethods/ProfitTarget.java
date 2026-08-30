package SQ.ExitMethods;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.ClassConfig;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.ExitMethod;
import com.strategyquant.tradinglib.ExitType;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.IFormula;
import com.strategyquant.tradinglib.ILiveOrder;
import com.strategyquant.tradinglib.OrderTypes;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;
import com.strategyquant.tradinglib.StrategyBase;

@ClassConfig(name = "Profit Target")
@SortOrder(200)
public class ProfitTarget extends ExitMethod {
   @Parameter(category = "Basic", showIfDefault = false)
   @Editor(type = 200, formulaName = "SLPT")
   @ExitType(3)
   public IFormula ProfitTarget;

   public void setForOrder(ILiveOrder var1, StrategyBase var2) throws TradingException {
      if (var1.isFilled()) {
         this.setOrderPTValue(var1, var2);
      } else {
         boolean var3 = var2.getEngine() == 56756755 || var2.getEngine() == 938213070;
         if (var3 && this.isInitialSLPTApplied(var2)) {
            if (var1.isMarketOrder()) {
               this.setOrderPTValue(var1, var2);
            } else {
               this.setInitialExit(var1, var2);
            }
         }
      }
   }

   private void setOrderPTValue(ILiveOrder var1, StrategyBase var2) throws TradingException {
      int var3 = var1.isLong() ? 1 : -1;
      double var4 = var1.isNettingMode() ? var1.getLastOpenPrice() : var1.getOpenPrice();
      double var6 = this.ProfitTarget.evaluateFormula(var2, var1.getSymbol(), var4, var3);
      if (var6 != var4 && var6 != -9.9999999E7) {
         var6 = this.correctSLPT(var1, var6, false);
         var1.setPT(var6).Send();
         if (!var1.isSuccessful()) {
            var1.Close((byte)8);
         }
      }
   }

   public double computeValue(byte var1, StrategyBase var2, String var3, double var4) throws TradingException {
      int var6 = OrderTypes.isLongOrder(var1) ? 1 : -1;
      double var7 = this.ProfitTarget.evaluateFormula(var2, var3, var4, var6);
      if (var7 == -9.9999999E7) {
         return -9.9999999E7;
      }

      double var9 = var2.getInstrumentInfo().tickSize;
      return this.correctSLPT(var4, var7, var6, var9, false);
   }

   public boolean setExit(ILiveOrder var1, StrategyBase var2) throws TradingException {
      int var3 = var1.isLong() ? 1 : -1;
      double var4 = this.ProfitTarget.evaluateFormula(var2, var1.getSymbol(), var1.getOpenPrice(), var3);
      if (var4 == -9.9999999E7) {
         return false;
      }

      var4 = SQUtils.fixPrice(this.Strategy.getInstrumentInfo().tickStep, var4);
      var4 = this.correctSLPT(var1, var4, false);
      var1.setPT(var4);
      int var6 = var3 < 0 ? 101 : 103;
      if (!this.shouldApplySLPTToOrder(var1, var2)) {
         return true;
      }

      ILiveOrder var7 = this.Strategy.Trader.Open((byte)var6, var1.getSymbol(), var4).setComment("ExitPT").setMagicNumber(var1.getMagicNumber()).Send();
      return false;
   }

   public boolean setInitialExit(ILiveOrder var1, StrategyBase var2) throws TradingException {
      int var3 = var1.isLong() ? 1 : -1;
      double var4 = this.ProfitTarget.evaluateFormula(var2, var1.getSymbol(), 0.0, -1);
      boolean var6 = var2.getEngine() == 56756755 || var2.getEngine() == 938213070;
      if (var6) {
         if (var4 == -9.9999999E7) {
            return false;
         }

         var4 = Math.abs(var4);
         var4 = SQUtils.fixPrice(this.Strategy.getInstrumentInfo().tickStep, var4);
         var4 = this.correctSLPT(var1, var4, true);
      } else {
         if (var4 == -9.9999999E7 || var4 <= 0.0) {
            return false;
         }

         var4 = SQUtils.fixPrice(this.Strategy.getInstrumentInfo().tickStep, var4);
         var4 = this.correctSLPT(var1, var4, false);
      }

      var1.setPT(var4).Send();
      if (!this.shouldApplySLPTToOrder(var1, var2)) {
         return true;
      }

      int var7 = var3 < 0 ? 101 : 103;
      ILiveOrder var8 = this.Strategy.Trader.Open((byte)var7, var1.getSymbol(), var4).setComment("ExitPT").setMagicNumber(var1.getMagicNumber()).Send();
      return true;
   }

   public IBlock clone(boolean var1, StrategyBase var2) throws BlockDefinitionException {
      return this;
   }
}
