package com.strategyquant.tradinglib.riskmanagement;

import com.strategyquant.lib.snippets.CustomClasses;
import com.strategyquant.lib.snippets.NonexistingCustomClassException;
import com.strategyquant.tradinglib.RiskManagementMethod;

public class RiskManagementMethodsList extends CustomClasses<RiskManagementMethod> {
   private static RiskManagementMethodsList instance;

   public static RiskManagementMethodsList get() {
      if (instance == null) {
         instance = new RiskManagementMethodsList();
      }

      return instance;
   }

   private RiskManagementMethodsList() {
      this.setDirName("RiskManagement");
      this.setExpectedClassType(RiskManagementMethod.class);
      this.loadAvailableClasses();
   }

   public static RiskManagementMethod create(String var0, Object... var1) throws NonexistingCustomClassException {
      return get()._create(var0, var1);
   }

   private RiskManagementMethod _create(String var1, Object[] var2) throws NonexistingCustomClassException {
      for (RiskManagementMethod var4 : this.getAvailableClasses()) {
         if (var4.getClass().getSimpleName().equals(var1)) {
            try {
               return (RiskManagementMethod)var4.newInstance(var2);
            } catch (Exception var6) {
               Log.error("Cannot create new instance", var6);
            }
         }
      }

      throw new NonexistingCustomClassException(var1);
   }
}
