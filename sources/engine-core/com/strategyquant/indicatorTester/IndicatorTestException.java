package com.strategyquant.indicatorTester;

public class IndicatorTestException extends Exception {
   public static final int FILE_NOT_FOUND = 0;
   public static final int INVALID_FILE = 1;
   public static final int INVALID_CONFIG = 2;
   public static final int DATA_MISMATCH = 3;
   public static final int TEST_ERROR = 4;
   public static final int EMPTY_FILE = 5;
   private int code;

   public IndicatorTestException(int var1) {
      this.code = var1;
   }

   public IndicatorTestException(int var1, Exception var2) {
      super(var2);
      this.code = var1;
   }

   public int getCode() {
      return this.code;
   }

   @Override
   public String getMessage() {
      return this.printCode() + " - " + super.getMessage();
   }

   private String printCode() {
      switch (this.code) {
         case 0:
            return "File not found";
         case 1:
            return "Invalid file";
         case 2:
            return "Invalid configuration";
         case 3:
            return "Data mismatch";
         case 4:
            return "Test error";
         case 5:
            return "Empty file";
         default:
            return "";
      }
   }
}
