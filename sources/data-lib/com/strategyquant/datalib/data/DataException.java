package com.strategyquant.datalib.data;

public class DataException extends Exception {
   public static final int CONNECTION = 1;
   public static final int DATA = 2;
   public static final int INSTRUMENT = 3;
   public static final int BARTYPE = 4;
   public static final int SERIES = 5;
   public static final int TIMEFRAME = 6;
   private int type;

   public DataException(int var1, String var2) {
      super(var2);
      this.type = var1;
   }
}
