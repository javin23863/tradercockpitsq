package com.strategyquant.datalib.data.io.columns;

public class DateCol extends DefaultCol {
   public static final String NAME = "Date";

   public DateCol() {
      super("Date", "DATE");
   }

   @Override
   public int getType() {
      return 6;
   }

   @Override
   public int getDataType() {
      return 9;
   }
}
