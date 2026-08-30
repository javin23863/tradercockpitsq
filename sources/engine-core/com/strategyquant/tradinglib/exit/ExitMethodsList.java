package com.strategyquant.tradinglib.exit;

import com.strategyquant.lib.snippets.CustomClasses;
import com.strategyquant.lib.snippets.NonexistingCustomClassException;
import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.ExitMethod;
import com.strategyquant.tradinglib.StrategyBase;
import java.util.ArrayList;
import org.jdom2.Element;

public class ExitMethodsList extends CustomClasses<ExitMethod> {
   private static ExitMethodsList instance;

   public static ExitMethodsList get() {
      if (instance == null) {
         instance = new ExitMethodsList();
      }

      return instance;
   }

   private ExitMethodsList() {
      this.setDirName("ExitMethod");
      this.setExpectedClassType(ExitMethod.class);
      this.loadAvailableClasses();
   }

   private static ExitMethod getObject(String var0, Object... var1) throws NonexistingCustomClassException {
      return get()._getObject(var0, var1);
   }

   private ExitMethod _getObject(String var1, Object[] var2) throws NonexistingCustomClassException {
      for (ExitMethod var4 : this.getAvailableClasses()) {
         if (var4.getClass().getSimpleName().equals(var1)) {
            try {
               return var4;
            } catch (Exception var6) {
               Log.error("Cannot create new instance", var6);
            }
         }
      }

      throw new NonexistingCustomClassException(var1);
   }

   public static ExitMethod getExitMethodObject(String var0, StrategyBase var1, ArrayList<Element> var2) throws BlockDefinitionException {
      ExitMethod var3;
      try {
         var3 = getObject(var0);
      } catch (NonexistingCustomClassException var5) {
         throw new BlockDefinitionException(String.format("Cannot create ExitMethod for snippet '%s'", var0), var5);
      }

      return var3.newInstance(var1, var2);
   }
}
