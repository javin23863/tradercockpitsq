package SQ.TradingOptions;

import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.TradingOption;
import org.jdom2.Element;

public class MinMaxSLPT extends TradingOption {
   @Parameter(name = "Minimum SL", defaultValue = "0", minValue = 0.0, maxValue = 1000000.0, category = "Trading options")
   @Help("Minimum SL in pips, it will cut the SL value so that it is not smaller than this setting. If 0, no limit is used.")
   @ForEngine("*,-SP,-SA")
   public int MinimumSL;
   @Parameter(name = "Maximum SL", defaultValue = "0", minValue = 0.0, maxValue = 1000000.0, category = "Trading options")
   @Help("Maximum SL in pips, it will cut the SL value so that it is not bigger than this setting. If 0, no limit is used.")
   @ForEngine("*,-SP,-SA")
   public int MaximumSL;
   @Parameter(name = "Minimum PT", defaultValue = "0", minValue = 0.0, maxValue = 1000000.0, category = "Trading options")
   @Help("Minimum PT in pips, it will cut the PT value so that it is not smaller than this setting. If 0, no limit is used.")
   @ForEngine("*,-SP,-SA")
   public int MinimumPT;
   @Parameter(name = "Maximum PT", defaultValue = "0", minValue = 0.0, maxValue = 1000000.0, category = "Trading options")
   @Help("Maximum PT in pips, it will cut the PT value so that it is not bigger than this setting. If 0, no limit is used.")
   @ForEngine("*,-SP,-SA")
   public int MaximumPT;

   public boolean OnBarUpdate(StrategyBase var1) throws Exception {
      return true;
   }

   public boolean isUsedInTrading() {
      return false;
   }

   public TradingOption getClone() {
      MinMaxSLPT var1 = new MinMaxSLPT();
      var1.MinimumSL = this.MinimumSL;
      var1.MaximumSL = this.MaximumSL;
      var1.MinimumPT = this.MinimumPT;
      var1.MaximumPT = this.MaximumPT;
      return var1;
   }

   public void setFromParameterEl(Element var1) throws Exception {
      String var2 = var1.getAttributeValue("key");
      String var3 = var1.getTextTrim();
      if (var3.isEmpty()) {
         var3 = var1.getAttributeValue("value");
      }

      if (var2.equals("MinimumSLPT")) {
         this.setParameterValue("MinimumSL", var3);
         this.setParameterValue("MinimumPT", var3);
      } else if (var2.equals("MaximumSLPT")) {
         this.setParameterValue("MaximumSL", var3);
         this.setParameterValue("MaximumPT", var3);
      } else {
         this.setParameterValue(var2, var3);
      }
   }
}
