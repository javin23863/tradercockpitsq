package com.strategyquant.tradinglib.table;

import com.strategyquant.lib.utils.StringComparator;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.TradelistColumn;
import java.util.Comparator;

public class TradelistOrdersComparator implements Comparator<Order> {
   private TradelistColumn column;
   private boolean asc;

   public int compare(Order var1, Order var2) {
      Object var3 = "";
      Object var4 = "";

      try {
         var3 = this.column.getValue(var1);
         var4 = this.column.getValue(var2);
         switch (this.column.getType()) {
            case "Text":
               StringComparator var21 = new StringComparator();
               int var8 = var21.compare(String.valueOf(var3), String.valueOf(var4));
               if (this.asc) {
                  return var8;
               }

               return -1 * var8;
            case "Time":
               long var9 = (Long)var3;
               long var11 = (Long)var4;
               if (var9 < var11) {
                  if (this.asc) {
                     return -1;
                  }

                  return 1;
               } else {
                  if (var9 > var11) {
                     if (this.asc) {
                        return 1;
                     }

                     return -1;
                  }

                  return 0;
               }
            case "Integer":
               int var13 = (Integer)var3;
               int var14 = (Integer)var4;
               if (var13 < var14) {
                  if (this.asc) {
                     return -1;
                  }

                  return 1;
               } else {
                  if (var13 > var14) {
                     if (this.asc) {
                        return 1;
                     }

                     return -1;
                  }

                  return 0;
               }
            default:
               if (var3 instanceof Float) {
                  float var22 = (Float)var3;
                  float var16 = (Float)var4;
                  if (var22 < var16) {
                     return this.asc ? -1 : 1;
                  } else if (var22 > var16) {
                     return this.asc ? 1 : -1;
                  } else {
                     return 0;
                  }
               } else {
                  double var15 = (Double)var3;
                  double var17 = (Double)var4;
                  if (var15 < var17) {
                     return this.asc ? -1 : 1;
                  } else if (var15 > var17) {
                     return this.asc ? 1 : -1;
                  } else {
                     return 0;
                  }
               }
         }
      } catch (Exception var19) {
         StringComparator var6 = new StringComparator();
         int var7 = var6.compare(String.valueOf(var3), String.valueOf(var4));
         return this.asc ? var7 : -1 * var7;
      }
   }

   public TradelistColumn getColumn() {
      return this.column;
   }

   public void setColumn(TradelistColumn var1) {
      this.column = var1;
   }

   public boolean isAsc() {
      return this.asc;
   }

   public void setAsc(boolean var1) {
      this.asc = var1;
   }
}
