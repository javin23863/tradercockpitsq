package com.strategyquant.datalib.data.io.columns;

public class TimeCol extends DefaultCol {
   public static final String NAME = "Time";

   public TimeCol() {
      super("Time", "TIME");
   }

   @Override
   public int getType() {
      return 7;
   }

   @Override
   public int getDataType() {
      return 10;
   }
}
