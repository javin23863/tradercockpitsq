package com.strategyquant.tradinglib.strategy;

import com.strategyquant.lib.snippets.CustomClasses;
import com.strategyquant.lib.snippets.NonexistingCustomClassException;
import com.strategyquant.tradinglib.StrategyBase;

public class StrategiesList extends CustomClasses<StrategyBase> {
   private static StrategiesList instance;

   static StrategiesList get() {
      if (instance == null) {
         instance = new StrategiesList();
      }

      return instance;
   }

   private StrategiesList() {
      this.setDirName("Strategies");
      this.setExpectedClassType(StrategyBase.class);
      this.loadAvailableClasses();
   }

   public static StrategyBase create(String var0) throws NonexistingCustomClassException {
      return get()._create(var0);
   }

   private StrategyBase _create(String var1) throws NonexistingCustomClassException {
      for (StrategyBase var3 : this.getAvailableClasses()) {
         if (var3.getClass().getSimpleName().equals(var1)) {
            try {
               return var3.newInstance();
            } catch (Exception var5) {
               Log.error("Cannot create new instance of a strategy '" + var1 + "'", var5);
            }
         }
      }

      throw new NonexistingCustomClassException(var1);
   }
}
