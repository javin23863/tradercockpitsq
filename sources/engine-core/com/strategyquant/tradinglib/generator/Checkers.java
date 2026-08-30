package com.strategyquant.tradinglib.generator;

import com.strategyquant.lib.snippets.CustomClassesLoader;
import com.strategyquant.lib.snippets.CustomClassesReg;
import com.strategyquant.lib.snippets.ICustomClasses;
import com.strategyquant.tradinglib.Checker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Checkers implements ICustomClasses {
   public static final Logger Log = LoggerFactory.getLogger("Checkers");
   private static Checkers instance;
   private CheckersList checkers = new CheckersList();

   public Checkers() {
      CustomClassesReg.add(this);
      this.initClassesMap("Checker");
   }

   public static void init() throws Exception {
      if (instance != null) {
         throw new Exception("Checkers.init() called more than once!");
      }

      instance = new Checkers();
   }

   private void initClassesMap(String var1) {
      try {
         CustomClassesLoader var2 = new CustomClassesLoader(var1);

         while (var2.hasNext()) {
            String var3 = var2.getNext();
            Object var4 = var2.createInstance(var3);
            if (var4 instanceof Checker) {
               Checker var5 = (Checker)var4;
               this.checkers.add(var5);
            } else if (Log.isDebugEnabled()) {
               Log.debug("Class: " + var3 + " is not an instance of Checker");
            }
         }
      } catch (Exception var6) {
         Log.error("An error occured while loading custom classes.", var6);
      }
   }

   public static CheckersList list() throws GenerateException {
      if (instance == null) {
         throw new GenerateException("You have to call Checkers.init() on program startup!");
      } else {
         return instance.checkers;
      }
   }

   public void reload() throws Exception {
      this.initClassesMap("Checker");
   }
}
