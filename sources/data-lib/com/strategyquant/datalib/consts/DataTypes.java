package com.strategyquant.datalib.consts;

public class DataTypes {
   public static final byte Stock = 1;
   public static final byte Futures = 2;
   public static final byte Forex = 3;
   public static final byte CFDs = 4;
   public static final byte ETF = 5;
   public static final byte Index = 6;
   public static final byte Crypto = 7;
   public static final byte Bond = 8;
   public static final String DATATYPE_STOCK = "Stock";
   public static final String DATATYPE_FUTURES = "Futures";
   public static final String DATATYPE_FOREX = "Forex";
   public static final String DATATYPE_CFDS = "CFDs";
   public static final String DATATYPE_ETF = "ETF";
   public static final String DATATYPE_INDEX = "Index";
   public static final String DATATYPE_CRYPTO = "Crypto";
   public static final String DATATYPE_BOND = "Bond";

   public static String toString(int var0) {
      switch (var0) {
         case 1:
            return "Stock";
         case 2:
            return "Futures";
         case 3:
            return "Forex";
         case 4:
            return "CFDs";
         case 5:
            return "ETF";
         case 6:
            return "Index";
         case 7:
            return "Crypto";
         case 8:
            return "Bond";
         default:
            return var0 + "";
      }
   }

   public static byte getDataType(String var0) throws Exception {
      switch (var0) {
         case "Stock":
            return 1;
         case "Futures":
            return 2;
         case "Forex":
            return 3;
         case "CFDs":
            return 4;
         case "ETF":
            return 5;
         case "Index":
            return 6;
         case "Crypto":
            return 7;
         case "Bond":
            return 8;
         default:
            throw new Exception("Unknown data type: '" + var0 + "'");
      }
   }
}
