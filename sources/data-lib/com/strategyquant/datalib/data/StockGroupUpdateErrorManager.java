package com.strategyquant.datalib.data;

import java.util.HashMap;
import java.util.Map;

public class StockGroupUpdateErrorManager {
   private static final StockGroupUpdateErrorManager instance = new StockGroupUpdateErrorManager();
   private Map<String, String> errors;

   public static StockGroupUpdateErrorManager getInstance() {
      return instance;
   }

   public synchronized void start() {
      if (this.errors == null) {
         this.errors = new HashMap<>();
      }
   }

   public synchronized void logError(String var1, String var2) {
      if (this.errors != null) {
         this.errors.put(var1, var2);
      }
   }

   public synchronized Map<String, String> finished() {
      Map var1 = this.errors;
      this.errors = null;
      return var1;
   }
}
