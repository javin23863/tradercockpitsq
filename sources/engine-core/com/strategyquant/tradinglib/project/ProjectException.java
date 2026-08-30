package com.strategyquant.tradinglib.project;

public class ProjectException extends Exception {
   public static final int NotDirectory = 1;
   public static final int NotEmptyDirectory = 2;
   public static final int ProjectDoesntExist = 3;
   public static final int ProjectConfigError = 4;
   public static final int DuplicateProject = 5;
   public static final int AlreadyLoaded = 6;
   private int code;

   public ProjectException(int var1, String var2) {
      super(var2);
      this.code = var1;
   }

   public ProjectException(int var1, Exception var2) {
      super(var2);
      this.code = var1;
   }

   public ProjectException(int var1, String var2, Exception var3) {
      super(var2, var3);
      this.code = var1;
   }

   public int getCode() {
      return this.code;
   }
}
