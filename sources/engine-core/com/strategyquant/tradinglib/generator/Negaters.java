package com.strategyquant.tradinglib.generator;

import com.strategyquant.lib.snippets.CustomClassesLoader;
import com.strategyquant.lib.snippets.CustomClassesReg;
import com.strategyquant.lib.snippets.ICustomClasses;
import com.strategyquant.tradinglib.Negater;
import com.strategyquant.tradinglib.NegatersList;
import java.util.Collections;
import java.util.Comparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Negaters implements ICustomClasses {
   public static final Logger Log = LoggerFactory.getLogger("Negaters");
   private static Negaters instance;
   private NegatersList negaters = new NegatersList();
   private Comparator<Negater> comparatorByOrder;

   public Negaters() {
      CustomClassesReg.add(this);
      this.comparatorByOrder = new NegaterComparatorByOrder();
      this.initClassesMap("Negater");
   }

   public static void init() throws Exception {
      if (instance != null) {
         throw new Exception("Negaters.init() called more than once!");
      }

      instance = new Negaters();
   }

   private void initClassesMap(String var1) {
      try {
         this.negaters.clear();
         CustomClassesLoader var2 = new CustomClassesLoader(var1);

         while (var2.hasNext()) {
            String var3 = var2.getNext();
            Object var4 = var2.createInstance(var3);
            if (var4 instanceof Negater) {
               Negater var5 = (Negater)var4;
               this.negaters.add(var5);
            } else if (Log.isDebugEnabled()) {
               Log.debug("Class: " + var3 + " is not an instance of Negater");
            }
         }

         this.sortClasses();
      } catch (Exception var6) {
         Log.error("An error occured while loading custom classes.", var6);
      }
   }

   private void sortClasses() {
      Collections.sort(this.negaters, this.comparatorByOrder);
   }

   public static NegatersList list() throws GenerateException {
      if (instance == null) {
         throw new GenerateException("You have to call Negaters.init() on program startup!");
      } else {
         return instance.negaters;
      }
   }

   public void reload() throws Exception {
      this.initClassesMap("Negater");
   }
}
