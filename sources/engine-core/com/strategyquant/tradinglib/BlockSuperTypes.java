package com.strategyquant.tradinglib;

public class BlockSuperTypes {
   public static final int Property = 1;
   public static final int Comparison = 2;
   public static final int Condition = 3;
   public static final int Value = 4;
   public static final int Action = 5;
   public static final int Formula = 6;
   public static final int ExitMethod = 7;

   public static int translate(String var0) {
      switch (var0) {
         case "condition":
            return 3;
         case "Conditions":
            return 3;
         case "Condition":
            return 3;
         case "comparison":
            return 2;
         case "Comparisons":
            return 2;
         case "Comparison":
            return 2;
         case "property":
            return 1;
         case "value":
            return 4;
         case "Values":
            return 4;
         case "Value":
            return 4;
         case "Price level":
            return 4;
         case "action":
            return 5;
         case "Actions":
            return 5;
         case "Action":
            return 5;
         case "formula":
            return 5;
         default:
            return -1;
      }
   }

   public static String translate(int var0) {
      switch (var0) {
         case 1:
            return "property";
         case 2:
            return "comparison";
         case 3:
            return "condition";
         case 4:
            return "value";
         case 5:
            return "action";
         default:
            return "unknown";
      }
   }

   public static boolean isBooleanBlock(int var0) {
      return var0 == 2 || var0 == 3 || var0 == 1;
   }
}
