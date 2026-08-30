package com.strategyquant.tradinglib;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.table.TableColumn;
import java.util.Locale;

public abstract class TradelistColumn extends TableColumn {
   public TradelistColumn(String var1) {
      super(var1, "Decimal2");
   }

   public TradelistColumn(String var1, String var2) {
      super(var1, var2);
   }

   public TradelistColumn(String var1, String var2, boolean var3) {
      super(var1, var2, var3);
   }

   public boolean setValue(Order var1, double var2) throws Exception {
      if (!this.isEditable()) {
         throw new Exception(L.t("Column '%s' is not editable, you cannot call setValue() !", new Object[]{this.getClass().getSimpleName()}));
      } else {
         throw new Exception(L.t("Column '%s' is editable, it has to implement setValue() method !", new Object[]{this.getClass().getSimpleName()}));
      }
   }

   public abstract Object getValue(Order var1) throws Exception;

   public String getFormattedValue(Object var1) throws Exception {
      if (var1 == null) {
         return "";
      }

      double var2 = 0.0;
      if (var1 instanceof Float) {
         var2 = ((Float)var1).floatValue();
      } else if (var1 instanceof Double) {
         var2 = (Double)var1;
      } else if (var1 instanceof Integer) {
         var2 = ((Integer)var1).intValue();
      } else if (var1 instanceof Short) {
         var2 = ((Short)var1).shortValue();
      } else if (var1 instanceof String) {
         return (String)var1;
      }

      switch (this.getType()) {
         case "Decimal2":
         case "Decimal2PL":
         case "Decimal2Pips":
         case "Decimal2Pct":
            return String.format(Locale.ROOT, "%.2f", var2);
         case "Decimal4":
            return String.format(Locale.ROOT, "%.4f", var2);
         case "Decimal5":
            return String.format(Locale.ROOT, "%.5f", var2);
         case "Integer":
            if (var1 instanceof Short) {
               return String.valueOf((short)var2);
            }

            return String.valueOf((int)var2);
         case "Text":
            return var1.toString();
         default:
            return "N/A";
      }
   }
}
