package com.strategyquant.datalib.data.io.columns;

public class DateTimeCol extends DefaultCol {
   public DateTimeCol() {
      super("Date & Time", "DATETIME");
   }

   @Override
   public int getType() {
      return 10;
   }

   @Override
   public int getDataType() {
      return 9;
   }
}
