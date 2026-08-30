package com.strategyquant.tradinglib.commissions;

import com.strategyquant.lib.snippets.CustomClasses;
import com.strategyquant.lib.snippets.NonexistingCustomClassException;
import com.strategyquant.tradinglib.CommissionsMethod;

public class CommissionsMethodsList extends CustomClasses<CommissionsMethod> {
   private static CommissionsMethodsList instance;

   public static CommissionsMethodsList get() {
      if (instance == null) {
         instance = new CommissionsMethodsList();
      }

      return instance;
   }

   private CommissionsMethodsList() {
      this.setDirName("Trading.Commissions");
      this.setExpectedClassType(CommissionsMethod.class);
      this.loadAvailableClasses();
   }

   public static CommissionsMethod create(String var0, Object... var1) throws NonexistingCustomClassException {
      return get()._create(var0, var1);
   }

   private CommissionsMethod _create(String var1, Object[] var2) throws NonexistingCustomClassException {
      if (!this.checkClassExists(var1)) {
         throw new NonexistingCustomClassException(var1);
      }

      try {
         return (CommissionsMethod)((CommissionsMethod)this.findClassByName(var1)).newInstance(var2);
      } catch (Exception var4) {
         Log.error("Cannot create new instance", var4);
         throw new NonexistingCustomClassException(var1);
      }
   }
}
