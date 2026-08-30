package com.strategyquant.tradinglib.taskImpl;

public class TaskException extends Exception {
   public static final int ConfigNotInitialized = 1;
   public static final int NumberFormatException = 2;
   public static final int ParseException = 3;
   public static final int TaskPluginNotSet = 4;
   public static final int TaskPluginDoesntExist = 5;
   public static final int RegistrationError = 6;
   public static final int TaskInstantiationError = 7;
   public static final int ProjectNotInitialized = 8;
   public static final int TaskConfigError = 9;
   public static final int DataException = 10;
   private int code;

   public TaskException(String var1) {
      super(var1);
      this.code = 0;
   }

   public TaskException(int var1, String var2) {
      super(var2);
      this.code = var1;
   }

   public TaskException(int var1, Exception var2) {
      super(var2);
      this.code = var1;
   }

   public TaskException(int var1, String var2, Exception var3) {
      super(var2, var3);
      this.code = var1;
   }

   public int getCode() {
      return this.code;
   }
}
