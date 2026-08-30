package com.strategyquant.tradinglib;

public class ReturnTypes {
   public static final int None = 0;
   public static final int Number = 1;
   public static final int Price = 2;
   public static final int Boolean = 3;
   public static final int Action = 4;
   public static final int String = 5;
   public static final int PriceOrNumber = 6;
   public static final int PriceRange = 7;
   public static final int Order = 8;

   public static String translateReturnType(int var0) throws BlockDefinitionException {
      switch (var0) {
         case 0:
            return "none";
         case 1:
            return "number";
         case 2:
            return "price";
         case 3:
            return "boolean";
         case 4:
            return "action";
         case 5:
            return "string";
         case 6:
            return "pricenumber";
         case 7:
            return "pricerange";
         case 8:
            return "order";
         default:
            throw new BlockDefinitionException("Unknown return type: " + var0);
      }
   }

   public static int translate(String var0) {
      if (var0 == null) {
         return -1;
      }

      switch (var0) {
         case "none":
            return 0;
         case "number":
            return 1;
         case "price":
            return 2;
         case "boolean":
            return 3;
         case "action":
            return 4;
         case "string":
            return 5;
         case "pricenumber":
            return 6;
         case "pricerange":
            return 7;
         case "order":
            return 8;
         default:
            return -1;
      }
   }
}
