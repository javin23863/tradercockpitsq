package com.strategyquant.tradinglib.wfo;

public class EvolutionException extends Exception {
   public static final int EX_CANCELLED = 0;
   public static final int EX_NODE_NOT_FOUND_ON_MAP = 1;
   public static final int EX_INITIAL_POPULATION_MAX_RETRIES_REACHED = 2;
   public static final int EX_CANNOT_GENERATE_GOOD_CANDIDATE = 3;
   public static final int EX_TASK_EXECUTION_FAILED = 4;
   public static final int EX_EVALUATION_ERROR = 5;
   public static final int EX_INDICATOR_ARRAY_OUT_OF_BOUNDS = 6;
   public static final int EX_ZERO_SUITABLE_NODES = 7;
   public static final int EX_INDICATOR_NOT_REGISTERED_WITH_STRATEGY = 8;
   public static final int EX_TOO_DEEP_RECURSION = 9;
   public static final int EX_NOT_CORRECT_NODE_STATUS = 10;
   public static final int EX_CANNOT_GENERATE_GOOD_STRATEGY = 11;
   public static final int EX_CANNOT_GENERATE_SLPT = 11;
   public static final int EX_CANNOT_GENERATE_CI = 12;
   public static final int EX_CANNOT_GET_CI_VALUE = 13;
   public static final int EX_MISSING_CI_VALUE = 14;
   public static final int EX_RESTARTED_BECAUSEOF_STAGNATION = 15;
   public static final int EX_RESTARTED_BECAUSEOF_CUSTOMCONDITION = 16;
   private int exceptionCode = 0;

   public EvolutionException(int var1) {
      this.exceptionCode = var1;
   }

   public EvolutionException(int var1, Exception var2) {
      super(var2);
      this.exceptionCode = var1;
   }

   public EvolutionException(int var1, String var2) {
      super(var2);
      this.exceptionCode = var1;
   }

   public int getExceptionCode() {
      return this.exceptionCode;
   }
}
