package SQ.MoneyManagement;

import com.strategyquant.tradinglib.ClassConfig;
import com.strategyquant.tradinglib.Description;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.MoneyManagementMethod;
import com.strategyquant.tradinglib.OrderTypes;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;
import com.strategyquant.tradinglib.StrategyBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ClassConfig(name = "Stocks size by price", display = "Stocks size by price")
@Help("<b>Size computed as Balance / StockSize.</b> Position sizing method specifically for stocks.")
@Description("Stocks size by price, max #MaxSize# lots")
@SortOrder(500)
@ForEngine("*,-SP,-SA")
public class StocksSizeByPrice extends MoneyManagementMethod {
   public static final Logger Log = LoggerFactory.getLogger("StocksSizeByPrice");
   @Parameter(name = "Use account balance", defaultValue = "true")
   @Help("If set to true, it will use current account balance. Otherwise it will use initial capital.")
   public boolean UseAccountBalance;
   @Parameter(name = "Maximum size", defaultValue = "100", minValue = 0.01, maxValue = 1.0E9, step = 0.1)
   @Help("The biggest size allowed")
   public double MaxSize;

   public double computeTradeSize(StrategyBase var1, String var2, byte var3, double var4, double var6, double var8, double var10, double var12) throws Exception {
      if (this.MaxSize < 0.0) {
         throw new Exception("Money management wasn't properly initialized. Call init() method before computing trade size!");
      }

      double var14 = var4 > 0.0 ? var4 : (OrderTypes.isLongOrder(var3) ? var1.MarketData.Chart(var2).Ask() : var1.MarketData.Chart(var2).Bid());
      double var16 = 0.0;
      if (this.UseAccountBalance) {
         var16 = var1.getAccountBalance() / var14 * this.weight;
      } else {
         var16 = var1.getInitialBalance() / var14 * this.weight;
      }

      var16 = this.round(var16, var12, 0);
      if (var16 <= 0.0) {
         var16 = 1.0;
      }

      if (var16 > this.MaxSize) {
         var16 = this.MaxSize;
      }

      if (Log.isDebugEnabled()) {
         Log.debug("Final trade size : " + var16);
      }

      return var16;
   }
}
