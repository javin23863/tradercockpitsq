package SQ.Trading.Commissions;

import com.strategyquant.tradinglib.ClassConfig;
import com.strategyquant.tradinglib.CommissionsMethod;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.ILiveOrder;
import com.strategyquant.tradinglib.Parameter;

@ClassConfig(name = "Stockpicker", display = "#Commission# / #CommissionType#")
@Help("<b>Stockpicker commissions</b>")
public class Stockpicker extends CommissionsMethod {
   @Parameter(defaultValue = "0.0035", minValue = 0.0, name = "Commission", maxValue = 100000.0, step = 0.001, decimals = 5, category = "Default")
   @Help("Commission")
   public double Commission;
   @Parameter(defaultValue = "share", name = "Commission type")
   @Editor(type = 40, values = "$ per share=share,$ per order=order,% of trade value=equity")
   @Help("Commission type")
   public String CommissionType;
   @Parameter(defaultValue = "0.35", minValue = 0.0, name = "Min per order", maxValue = 100000.0, step = 0.1, category = "Default")
   @Help("Minimum commission per order. Zero means no minimum")
   public double MinPerOrder;
   @Parameter(defaultValue = "money", name = "Min per order type")
   @Editor(type = 40, values = "$=money,% of trade value=equity")
   @Help("Minimum commission per order type")
   public String MinPerOrderType;
   @Parameter(defaultValue = "1", minValue = 0.0, name = "Max per order", maxValue = 100000.0, step = 1.0, category = "Default")
   @Help("Maximum commission per order. Zero means no maximum")
   public double MaxPerOrder;
   @Parameter(defaultValue = "equity", name = "Max per order type")
   @Editor(type = 40, values = "$=money,% of trade value=equity")
   @Help("Maximum commission per order type")
   public String MaxPerOrderType;

   public double computeCommissionsOnOpen(ILiveOrder var1, double var2, double var4) throws Exception {
      double var14 = var1.getSize() * var1.getOpenPrice();

      double var6 = switch (this.CommissionType) {
         case "share" -> this.Commission * var1.getSize();
         case "order" -> this.Commission;
         case "equity" -> {
            double var12 = this.Commission / 100.0;
            yield var12 * var14;
         }
         default -> throw new Exception("Invalid commision type: " + this.CommissionType);
      };

      double var8 = switch (this.MinPerOrderType) {
         case "money" -> this.MinPerOrder;
         case "equity" -> {
            double var18 = this.MinPerOrder / 100.0;
            yield var18 * var14;
         }
         default -> throw new Exception("Invalid min per order type: " + this.MinPerOrderType);
      };

      double var10 = switch (this.MaxPerOrderType) {
         case "money" -> this.MaxPerOrder;
         case "equity" -> {
            double var19 = this.MaxPerOrder / 100.0;
            yield var19 * var14;
         }
         default -> throw new Exception("Invalid max per order type: " + this.MaxPerOrderType);
      };
      if (var8 > 0.0 && var6 < var8) {
         var6 = var8;
      }

      if (var10 > 0.0 && var6 > var10) {
         var6 = var10;
      }

      return var6;
   }

   public double computeCommissionsOnClose(ILiveOrder var1, double var2, double var4) throws Exception {
      return this.computeCommissionsOnOpen(var1, var2, var4);
   }
}
