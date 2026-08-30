package SQ.Columns.Trades;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.TradelistColumn;

public class Type extends TradelistColumn {
   public Type() {
      super(L.tsq("Type"), "Text");
   }

   public Object getValue(Order var1) {
      return var1.Type;
   }

   public String getFormattedValue(Object var1) {
      byte var2 = (Byte)var1;
      switch (var2) {
         case 0:
            return L.t("Any", new Object[0]);
         case 1:
            return L.t("Buy", new Object[0]);
         case 2:
            return L.t("Sell", new Object[0]);
         case 3:
            return L.t("BuyLimit", new Object[0]);
         case 4:
            return L.t("SellLimit", new Object[0]);
         case 5:
            return L.t("BuyStop", new Object[0]);
         case 6:
            return L.t("SellStop", new Object[0]);
         case 7:
            return L.t("BuyStopLimit", new Object[0]);
         case 8:
            return L.t("SellStopLimit", new Object[0]);
         case 9:
            return L.t("Deposit", new Object[0]);
         case 10:
            return L.t("Withdrawal", new Object[0]);
         case 11:
            return L.t("Balance", new Object[0]);
         default:
            return L.t("N/A", new Object[0]);
      }
   }
}
