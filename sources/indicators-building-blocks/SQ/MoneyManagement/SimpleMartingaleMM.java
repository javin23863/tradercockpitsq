package SQ.MoneyManagement;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.ClassConfig;
import com.strategyquant.tradinglib.Description;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.MoneyManagementMethod;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrderTypes;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;
import com.strategyquant.tradinglib.StrategyBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ClassConfig(name = "Simple Martingale MM", display = "Simple Martingale MM")
@Help(
   "<b>Simple Martingale MM</b><br/><span style=\"color: red\">Experimental feature</span><br/>Characteristics:<br/><ul><li>Optional separate logic for Buy and Sell</li><li>Multiplies the last trade size by multiplier after loss</li><li>Resets size to a starting size after profit</li><li>When computed size is bigger than reset value, it uses Starting lots again</li></ul>"
)
@SortOrder(900)
@Description("Simple Martingale MM")
@ForEngine("*,-SP,-SA")
public class SimpleMartingaleMM extends MoneyManagementMethod {
   public static final Logger Log = LoggerFactory.getLogger("SimpleMartingaleMM");
   @Parameter(name = "Starting lots", defaultValue = "0.1", minValue = 0.01)
   @Help("Starting size.")
   public double LotsStart;
   @Parameter(name = "Lots multiplier", defaultValue = "2.0", minValue = 0.01)
   @Help("Multiplier to multiply the previous order size by.")
   public double LotsMultiplier;
   @Parameter(name = "Maximum Lots (reset)", defaultValue = "1.0", minValue = 0.01)
   @Help("Resets to Starting Lots if computed lot size is bigger than this.")
   public double LotsReset;
   @Parameter(defaultValue = "1", minValue = 0.0, name = "Size Decimals", maxValue = 6.0, step = 1.0, category = "Default")
   @Help("Order size will be rounded to the selected number of decimal places. Use 2 for microlots, 1 for mini lots and 0 for stocks and futures.")
   public int Decimals;
   @Parameter(name = "Separate MM by direction?", defaultValue = "true")
   @Help("If set to true, it will use Martingale independently for buy and sell orders.")
   public boolean SeparateByDirection;

   public double computeTradeSize(StrategyBase var1, String var2, byte var3, double var4, double var6, double var8, double var10, double var12) throws Exception {
      if (this.LotsStart < 0.0) {
         throw new Exception("Money management wasn't properly initialized. Call init() method before computing trade size!");
      }

      int var14 = var1.getEngine();
      if (!var1.isEngineDefined() || var14 != 938213070 && var14 != 56756755) {
         byte var15 = 0;
         if (this.SeparateByDirection) {
            var15 = (byte)(OrderTypes.isLongOrder(var3) ? 1 : -1);
         }

         Order var16 = this.getLastClosedOrder(var15, var1);
         if (var16 == null) {
            return this.round(this.LotsStart, var12, this.Decimals);
         }

         double var17 = this.getPL(var16);
         if (var17 > 0.0) {
            return this.round(this.LotsStart, var12, this.Decimals);
         }

         double var19 = SQUtils.round(var16.Size, this.Decimals);
         double var21 = var19 * this.LotsMultiplier * this.weight;
         return var21 > this.LotsReset ? this.round(this.LotsStart, var12, this.Decimals) : this.round(var21, var12, this.Decimals);
      } else {
         return this.round(this.LotsStart, var12, this.Decimals);
      }
   }

   private double getPL(Order var1) {
      return var1.isLong() ? var1.ClosePrice - var1.OpenPrice : var1.OpenPrice - var1.ClosePrice;
   }

   protected Order getLastClosedOrder(int var1, StrategyBase var2) {
      String var3 = var2.getStrategyName();
      if (var2.Trader != null) {
         for (int var4 = var2.Trader.getHistoryOrdersCount() - 1; var4 >= 0; var4--) {
            Order var5 = var2.Trader.getHistoryOrder(var4);
            if (var5.StrategyName.equals(var3)
               && var5.Symbol.equals(var2.MarketData.Chart(0).Symbol)
               && (var1 == 0 || var5.getDirection() == var1)
               && var5.OpenPrice != var5.ClosePrice) {
               return var5;
            }
         }
      }

      return null;
   }
}
