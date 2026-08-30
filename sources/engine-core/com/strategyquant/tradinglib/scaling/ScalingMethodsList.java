package com.strategyquant.tradinglib.scaling;

import com.strategyquant.lib.snippets.CustomClasses;
import com.strategyquant.lib.snippets.NonexistingCustomClassException;
import com.strategyquant.tradinglib.ScalingMethod;

public class ScalingMethodsList extends CustomClasses<ScalingMethod> {
   private static ScalingMethodsList instance;

   public static ScalingMethodsList get() {
      if (instance == null) {
         instance = new ScalingMethodsList();
      }

      return instance;
   }

   private ScalingMethodsList() {
      this.setDirName("Trading.Scaling");
      this.setExpectedClassType(ScalingMethod.class);
      this.loadAvailableClasses();
   }

   public static ScalingMethod create(String var0, Object... var1) throws NonexistingCustomClassException {
      return get()._create(var0, var1);
   }

   private ScalingMethod _create(String var1, Object[] var2) throws NonexistingCustomClassException {
      for (ScalingMethod var4 : this.getAvailableClasses()) {
         if (var4.getClass().getSimpleName().equals(var1)) {
            try {
               return (ScalingMethod)var4.newInstance(var2);
            } catch (Exception var6) {
               Log.error("Cannot create new instance", var6);
            }
         }
      }

      throw new NonexistingCustomClassException(var1);
   }
}
