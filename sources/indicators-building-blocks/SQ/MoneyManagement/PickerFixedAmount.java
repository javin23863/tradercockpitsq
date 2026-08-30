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

@ClassConfig(name = "Fixed amount (Stockpicker)", display = "Fixed amount: #RiskedMoney# $")
@Help("<b>Fixed - the method allocates fixed amount of capital and split into maximum positions allowed.</b>")
@SortOrder(400)
@Description("Fixed amount, #RiskedMoney# $")
@ForEngine("SP,SA")
public class PickerFixedAmount extends MoneyManagementMethod {
   @Parameter(defaultValue = "10000", minValue = 1.0, name = "RiskedMoney", maxValue = 1000000.0, step = 100.0, category = "Default")
   @Help("How much in $ (money) will be risked on this strategy?")
   public double RiskedMoney;
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
      if (this.RiskedMoney < 0.0) {
         throw new Exception("Money management wasn't properly initialized. Call init() method before computing trade size!");
      }

      if (var4 <= 0.0) {
         throw new Exception(String.format("PickerFixedAmount money management - invalid %s symbol price. It must be > 0, got %s.", var2, SQUtils.d2(var4)));
      }

      double var14 = this.RiskedMoney * this.weight / var4;
      if (this.maxPos > 0.0) {
         var14 /= this.maxPos;
      }

      if (this.AllowFractionalShares) {
         var14 = Math.round(var14 / this.FractionalStep) * this.FractionalStep;
         var14 = SQUtils.round(var14, this.FractionalDecimalNumbers);
      } else {
         if (var14 < 1.0) {
            throw new TradeSizeSmallerThanOneException(String.format("Calculated trade size %s < 1", SQUtils.d2(var14)));
         }

         var14 = SQUtils.roundDown(var14, 0);
      }

      return var14;
   }
}
