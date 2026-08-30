package com.strategyquant.datalib.consts;

import com.strategyquant.lib.L;

public class Precisions {
   private String[] langs = new String[]{
      L.tsq("Selected timeframe only (fastest)"),
      L.tsq("1 minute data tick simulation (slow)"),
      L.tsq("Real Tick - custom spread (slowest)"),
      L.tsq("Real Tick - real spread (slowest)"),
      L.tsq("Trade On Bar Open")
   };
   public static final int SelectedTF = 1;
   public static final int BaseTF = 2;
   public static final int TickCustomSpread = 3;
   public static final int TickRealSpread = 4;
   public static final int OpenPrices = 5;
   public static final String PRECISION_SELECTED_TF = "Selected timeframe only (fastest)";
   public static final String PRECISION_BASE_TF = "1 minute data tick simulation (slow)";
   public static final String PRECISION_TICK_CUSTOM_SPREADS = "Real Tick - custom spread (slowest)";
   public static final String PRECISION_TICK_REAL_SPREADS = "Real Tick - real spread (slowest)";
   public static final String PRECISION_OPEN_PRICES = "Trade On Bar Open";

   public static String toString(int var0) {
      switch (var0) {
         case 1:
            return "Selected timeframe only (fastest)";
         case 2:
            return "1 minute data tick simulation (slow)";
         case 3:
            return "Real Tick - custom spread (slowest)";
         case 4:
            return "Real Tick - real spread (slowest)";
         case 5:
            return "Trade On Bar Open";
         default:
            return null;
      }
   }

   public static int getPrecision(String var0) throws Exception {
      switch (var0) {
         case "Selected timeframe only (fastest)":
            return 1;
         case "1 minute data tick simulation (slow)":
            return 2;
         case "Real Tick - custom spread (slowest)":
            return 3;
         case "Real Tick - real spread (slowest)":
            return 4;
         case "Trade On Bar Open":
            return 5;
         case "Real Tick precision (slowest)":
            return 4;
         default:
            throw new Exception("Unknown precision type: '" + var0 + "'");
      }
   }

   public static int getPrecision(int var0) throws Exception {
      switch (var0) {
         case 1:
            return 1;
         case 2:
            return 2;
         case 3:
            return 3;
         case 4:
            return 4;
         case 5:
            return 5;
         default:
            throw new Exception("Unknown precision type: '" + var0 + "'");
      }
   }
}
