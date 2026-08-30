package SQ.ExitMethods;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.ClassConfig;
import com.strategyquant.tradinglib.ExitMethod;
import com.strategyquant.tradinglib.ExitType;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.IActionEventListener;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.ILiveOrder;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;
import com.strategyquant.tradinglib.StrategyBase;

@ClassConfig(name = "Exit After Bars")
@SortOrder(500)
public class ExitAfterBars extends ExitMethod {
   @Parameter(
      category = "Advanced",
      defaultValue = "0",
      minValue = 0.0,
      maxValue = 99999.0,
      builderMinValue = 5.0,
      builderMaxValue = 500.0,
      step = 1.0,
      showIfDefault = false
   )
   @Help("Number of bars after which this trade will automatically be closed")
   @ExitType(4)
   public int ExitAfterBars;

   public void setForOrder(final ILiveOrder var1, StrategyBase var2) throws TradingException {
      if (this.ExitAfterBars > 0) {
         var1.registerEvent(2, new IActionEventListener() {
            public void OnActionEvent(StrategyBase var1x) throws TradingException {
               if (var1.isMarketOpen()) {
                  ExitAfterBars.this.checkExitAfterBars(var1);
               }
            }
         });
      }
   }

   private void checkExitAfterBars(ILiveOrder var1) throws TradingException {
      if (!var1.isClosedOrder()) {
         if (var1.isMarketOrder() && var1.getBarsInTrade() >= this.ExitAfterBars) {
            var1.Close((byte)19);
         }
      }
   }

   public double computeValue(byte var1, StrategyBase var2, String var3, double var4) throws TradingException {
      throw new TradingException("This method shouldn't be called!");
   }

   public IBlock clone(boolean var1, StrategyBase var2) throws BlockDefinitionException {
      return this;
   }
}
