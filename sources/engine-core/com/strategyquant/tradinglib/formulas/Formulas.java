package com.strategyquant.tradinglib.formulas;

import com.strategyquant.lib.snippets.CustomClassesLoader;
import com.strategyquant.lib.snippets.CustomClassesReg;
import com.strategyquant.lib.snippets.ICustomClasses;
import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.Formula;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.IFormula;
import com.strategyquant.tradinglib.IgnoreInBuilder;
import com.strategyquant.tradinglib.blocks.AnnotationProcessor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Formulas implements ICustomClasses {
   public static final Logger Log = LoggerFactory.getLogger("Formulas");
   private static Formulas instance;
   private HashMap<String, ArrayList<IBlock>> mapClasses = new HashMap<>();
   private Comparator<IBlock> comparatorByOrder;

   public Formulas() {
      CustomClassesReg.add(this);
      this.comparatorByOrder = new FormulaComparatorByOrder();
      this.initClassesMap("Formulas");
   }

   public static void init() throws Exception {
      if (instance != null) {
         throw new Exception("Formulas.init() called more than once!");
      }

      instance = new Formulas();
   }

   public static IFormula get(String var0) throws BlockDefinitionException {
      if (instance == null) {
         throw new BlockDefinitionException("You have to call Formulas.init() on program startup!");
      } else {
         return instance._get(var0);
      }
   }

   private IFormula _get(String var1) throws BlockDefinitionException {
      for (String var3 : this.mapClasses.keySet()) {
         ArrayList var4 = this.mapClasses.get(var3);

         for (int var5 = 0; var5 < var4.size(); var5++) {
            IBlock var6 = (IBlock)var4.get(var5);
            if (var6.getClass().getName().equals(var1)) {
               return (IFormula)var6;
            }
         }
      }

      throw new BlockDefinitionException("Cannot find formula '" + var1 + "'");
   }

   private void initClassesMap(String var1) {
      try {
         this.mapClasses.clear();
         CustomClassesLoader var2 = new CustomClassesLoader(var1);

         while (var2.hasNext()) {
            String var3 = var2.getNext();
            Object var4 = var2.createInstance(var3);
            if (var4 instanceof IFormula && var4 instanceof IBlock) {
               IBlock var5 = (IBlock)var4;
               Class var6 = var5.getClass();
               Formula var7 = var6.getAnnotation(Formula.class);
               if (var7 == null) {
                  Log.error("Class " + var6.getName() + " doesn't have @Formula annotation, it will be ignored!");
               } else {
                  String var8 = var7.formula();
                  if (var8.equals("Null")) {
                     Log.error("Class " + var6.getName() + " - @Formula annotation is missing formula definition!");
                  } else {
                     if (!this.mapClasses.containsKey(var8)) {
                        this.mapClasses.put(var8, new ArrayList<>());
                     }

                     ArrayList var9 = this.mapClasses.get(var8);
                     var9.add(var5);
                  }
               }
            } else if (Log.isDebugEnabled()) {
               Log.debug("Class: " + var3 + " is not an instance of block");
            }
         }

         this.sortClasses();
      } catch (Exception var10) {
         Log.error("An error occured while loading custom classes.", var10);
      }
   }

   private void sortClasses() {
      for (String var2 : this.mapClasses.keySet()) {
         ArrayList var3 = this.mapClasses.get(var2);
         Collections.sort(var3, this.comparatorByOrder);
      }
   }

   public static void generateFormulasXml(Element var0) throws BlockDefinitionException {
      if (instance == null) {
         throw new BlockDefinitionException("You have to call Blocks.init() on program startup!");
      }

      instance._generateFormulasXml(var0);
   }

   private void _generateFormulasXml(Element var1) throws BlockDefinitionException {
      for (String var3 : this.mapClasses.keySet()) {
         Element var4 = new Element("Formula");
         var4.setAttribute("key", var3);

         for (IBlock var6 : this.mapClasses.get(var3)) {
            Element var7 = this.getFormulaItem(var6, 0, null);
            var4.addContent(var7);
         }

         var1.addContent(var4);
      }
   }

   private Element getFormulaItem(IBlock var1, int var2, String var3) throws BlockDefinitionException {
      Element var4 = new Element("Item");
      Class var5 = var1.getClass();
      Formula var6 = var5.getAnnotation(Formula.class);
      if (var6 == null) {
         throw new BlockDefinitionException("This shouldn't happen!!!");
      }

      var4.setAttribute("key", var5.getName());
      var4.setAttribute("name", var6.name().equals("Null") ? var5.getSimpleName() : var6.name());
      if (var6.noneValue()) {
         var4.setAttribute("noneValue", "true");
      }

      IgnoreInBuilder var7 = var5.getAnnotation(IgnoreInBuilder.class);
      if (var7 != null) {
         var4.setAttribute("ignoreInBuilder", "true");
      }

      this.addFormulaParameters(var1, var5, var4, var2, var3);
      return var4;
   }

   private void addFormulaParameters(IBlock var1, Class<?> var2, Element var3, int var4, String var5) throws BlockDefinitionException {
      for (Field var9 : var2.getFields()) {
         AnnotationProcessor.wizardGenerateXmlForFormula(var9, var3, var1, var2, var4, var5);
      }
   }

   public static void generateFormulaParameters(String var0, Element var1, int var2, String var3) throws BlockDefinitionException {
      if (instance == null) {
         throw new BlockDefinitionException("You have to call Formulas.init() on program startup!");
      }

      instance._generateFormulaParameters(var0, var1, var2, var3);
   }

   private void _generateFormulaParameters(String var1, Element var2, int var3, String var4) throws BlockDefinitionException {
      for (IBlock var6 : this.mapClasses.get(var1)) {
         Element var7 = this.getFormulaItem(var6, var3, var4);
         String var8 = var7.getAttributeValue("ignoreInBuilder");
         if (var8 == null || !var8.equals("true")) {
            String var9 = var7.getAttributeValue("noneValue");
            if (var9 == null || !var9.equals("true")) {
               var2.addContent(var7);
            }
         }
      }
   }

   public void reload() throws Exception {
      this.initClassesMap("Formulas");
   }
}
