package com.strategyquant.tradinglib.optimization;

public class OptimizationConst {
   public static final int OPTIMIZATION_SIMPLE = 1;
   public static final int OPTIMIZATION_WALKFORWARD = 2;
   public static final int OPTIMIZATION_MULTIWALKFORWARD = 3;
   public static final int OPTIMIZATION_SEQUENTIAL = 4;
   public static final int SOURCE_FILE = 1;
   public static final int SOURCE_DATABANK = 2;
   public static final int SIMPLE_STORE_ALL = 5;
   public static final int SIMPLE_STORE_BEST = 10;
   public static final int WF_TYPE_SIMIS_SIMOOS = 0;
   public static final int WF_TYPE_SIMIS_EXACTOOS = 1;
   public static final int WF_TYPE_EXACTIS_EXACTOOS = 2;
   public static final int WF_PERIOD_TYPE_PERCENT = 10;
   public static final int WF_PERIOD_TYPE_DAYS = 20;
   public static final int WF_PERIOD_TYPE_BARS = 30;
   public static final int WF_OPTIMIZATION_TYPE_FLOATING = 15;
   public static final int WF_OPTIMIZATION_TYPE_FIXED = 25;

   public static final String printWFByPeriodType(int var0, int var1, int var2) {
      switch (var0) {
         case 10:
            return String.format("WF: %d runs : %d %% OOS", var1, var2);
         case 20:
            return String.format("WF: %d OOS days : %d IS days", var1, var2);
         case 30:
            return String.format("WF: %d OOS bars : %d IS bars", var1, var2);
         default:
            return String.format("WF: %d unknown : %d unknown", var1, var2);
      }
   }

   public static final String printWFByPeriodType(int var0, int var1, int var2, int var3, int var4, int var5, int var6) {
      switch (var0) {
         case 10:
            return String.format("WF runs: %d,%d,%d OOS %%:  %d,%d,%d", var1, var3, var2, var4, var6, var5);
         case 20:
            return String.format("WF OOS days: %d,%d,%d  IS days: %d,%d,%d", var1, var3, var2, var4, var6, var5);
         case 30:
            return String.format("WF OOS bars: %d,%d,%d  IS bars: %d,%d,%d", var1, var3, var2, var4, var6, var5);
         default:
            return String.format("WF OOS unknown: %d,%d,%d  IS unknown: %d,%d,%d", var1, var3, var2, var4, var6, var5);
      }
   }

   public static final String wfTypeToString(int var0) {
      switch (var0) {
         case 0:
            return "Simulated IS, Simulated OOS (fastest)";
         case 1:
            return "Simulated IS, Exact OOS (slower)";
         case 2:
            return "Exact IS, Exact OOS (slow)";
         default:
            return "?(" + var0 + ")";
      }
   }

   public static String getPeriodTypeAsString(int var0) {
      switch (var0) {
         case 10:
            return "Percent";
         case 20:
            return "Days";
         case 30:
            return "Bars";
         default:
            return "Unknown";
      }
   }
}
