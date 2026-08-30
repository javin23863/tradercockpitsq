package com.strategyquant.tradinglib.robustnesstests;

import com.strategyquant.tradinglib.DatabankColumn;
import java.util.Comparator;

public class RobustnessResultComparator implements Comparator<RobustnessResults> {
   private static final int TypeDouble = 1;
   private static final int TypeInt = 2;
   private static final int TypeUncomparable = 3;
   private DatabankColumn column;
   private byte valueType;
   private int columnType;
   private String statsValue;

   public RobustnessResultComparator(DatabankColumn var1) {
      this.column = var1;
      this.valueType = var1.getValueType();
      if (this.valueType == 3) {
         this.valueType = 1;
      }

      this.columnType = this.recognizeColumnType(var1.getType());
      this.statsValue = var1.getStatsValueName();
   }

   private int recognizeColumnType(String var1) {
      if (var1.contains("Decimal")) {
         return 1;
      } else {
         return var1.contains("Integer") ? 2 : 3;
      }
   }

   public int compare(RobustnessResults var1, RobustnessResults var2) {
      if (this.columnType == 3) {
         return 0;
      }

      if (this.statsValue == null) {
         return 0;
      }

      double var3;
      double var5;
      if (this.columnType == 2) {
         var3 = var1.getOriginalStats().getInt(this.statsValue);
         var5 = var2.getOriginalStats().getInt(this.statsValue);
      } else {
         var3 = var1.getOriginalStats().getDouble(this.statsValue);
         var5 = var2.getOriginalStats().getDouble(this.statsValue);
      }

      if (this.valueType == 2) {
         if (var3 > var5) {
            return 1;
         } else {
            return var3 < var5 ? -1 : 0;
         }
      } else if (var3 > var5) {
         return -1;
      } else {
         return var3 < var5 ? 1 : 0;
      }
   }
}
