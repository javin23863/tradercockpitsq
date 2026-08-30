package SQ.MoneyManagement;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.ClassConfig;
import com.strategyquant.tradinglib.Description;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.MoneyManagementMethod;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.TradeSizeSmallerThanOneException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ClassConfig(name = "Risk fixed % of account (Stockpicker)", display = "Risk fixed % of account: #Risk#%")
@Help(
   "<b>Risk % - the method allocates defined percent amount of the account capital to a selected strategy.<br>The amount will be split into maximum positions allowed."
)
@Description("Risk #Risk#% of account")
@SortOrder(300)
@ForEngine("SP,SA")
public class PickerRiskFixedPctOfAccount extends MoneyManagementMethod {
   public static final Logger Log = LoggerFactory.getLogger("PickerRiskFixedPctOfAccount");
   @Parameter(name = "Risk in %", category = "Risk", defaultValue = "10", minValue = 0.1, maxValue = 100.0, step = 0.1)
   @Help("How big percentage of your account will be risked on this strategy?")
   public double Risk;
   @Parameter(name = "Allow fractional shares", defaultValue = "false")
   @Help(
      "Allow fractional shares - if true it will allow trading fractions of stocks. Not avaiable for all brokers and all stocks - check documentation for more info."
   )
   public boolean AllowFractionalShares;
   @Parameter(defaultValue = "4", minValue = 0.0, name = "Fractional decimal numbers", maxValue = 12.0, step = 1.0, category = "Default")
   @Help("Fractional decimal numbers.")
   public int FractionalDecimalNumbers;
   @Parameter(name = "Fractional step", defaultValue = "0.01", minValue = 0.0, maxValue = 1.0, step = 0.01)
   @Help("Fractional step")
   public double FractionalStep;

   public double computeTradeSize(StrategyBase var1, String var2, byte var3, double var4, double var6, double var8, double var10, double var12) throws Exception {
      if (this.Risk < 0.0) {
         throw new Exception("Money management wasn't properly initialized. Call init() method before computing trade size!");
      }

      if (var4 <= 0.0) {
         throw new Exception(
            String.format("PickerRiskFixedPctOfAccount money management - invalid %s symbol price. It must be > 0, got %s.", var2, SQUtils.d2(var4))
         );
      }

      double var14 = this.Risk * this.weight;
      if (var14 > 100.0) {
         var14 = 100.0;
      }

      double var16 = SQUtils.round7(var1.getAccountEquity() * (var14 / 100.0));
      double var18 = var16 / var4;
      if (this.maxPos > 0.0) {
         var18 /= this.maxPos;
      }

      if (this.AllowFractionalShares) {
         var18 = Math.round(var18 / this.FractionalStep) * this.FractionalStep;
         var18 = SQUtils.round(var18, this.FractionalDecimalNumbers);
      } else {
         if (var18 < 1.0) {
            throw new TradeSizeSmallerThanOneException(String.format("Calculated trade size %s < 1", SQUtils.d2(var18)));
         }

         var18 = SQUtils.roundDown(var18, 0);
      }

      return var18;
   }
}
