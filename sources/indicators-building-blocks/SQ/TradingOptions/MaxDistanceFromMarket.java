package SQ.TradingOptions;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.Activator;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.TradingOption;

public class MaxDistanceFromMarket extends TradingOption {
   @Parameter(name = "Max distance from market", defaultValue = "false", category = "Trading options")
   @Help("Limits pending orders maximal distance from current market price")
   @ForEngine("*,-SP,-SA")
   public boolean MaxDistanceFromMarket;
   @Parameter(name = "Max distance percent", defaultValue = "6", minValue = 0.0, maxValue = 1000.0, step = 0.1, category = "Trading options")
   @Help("Orders with price exceeding this level will be canceled")
   @Editor(type = 10)
   @Activator(param = "MaxDistanceFromMarket")
   @ForEngine("*,-SP,-SA")
   public double MaxDistancePct;

   public boolean isUsedInTrading() {
      return false;
   }

   public boolean OnBarUpdate(StrategyBase var1) throws TradingException {
      return true;
   }

   public TradingOption getClone() {
      MaxDistanceFromMarket var1 = new MaxDistanceFromMarket();
      var1.MaxDistanceFromMarket = this.MaxDistanceFromMarket;
      var1.MaxDistancePct = this.MaxDistancePct;
      return var1;
   }
}
