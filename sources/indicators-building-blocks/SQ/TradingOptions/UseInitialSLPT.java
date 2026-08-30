package SQ.TradingOptions;

import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.TradingOption;
import org.jdom2.Element;

public class UseInitialSLPT extends TradingOption {
   @Parameter(name = "Use initial SL & PT", defaultValue = "false", category = "Trading options")
   @Help("Sets StopLoss and ProfitTarget immediately after the order gets filled. Otherwise SL/PT is set on next bar")
   @ForEngine("MC,TS")
   public boolean UseInitialSLPT;

   public boolean OnBarUpdate(StrategyBase var1) throws Exception {
      return true;
   }

   public boolean isUsedInTrading() {
      return false;
   }

   public TradingOption getClone() {
      UseInitialSLPT var1 = new UseInitialSLPT();
      var1.UseInitialSLPT = this.UseInitialSLPT;
      return var1;
   }

   public void setFromParameterEl(Element var1) throws Exception {
      String var2 = var1.getAttributeValue("key");
      String var3 = var1.getTextTrim();
      if (var3.isEmpty()) {
         var3 = var1.getAttributeValue("value");
      }

      if (var2.equals("UseInitialSLPT")) {
         this.setParameterValue("UseInitialSLPT", var3);
      }
   }
}
