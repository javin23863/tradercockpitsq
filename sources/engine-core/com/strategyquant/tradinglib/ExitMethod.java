package com.strategyquant.tradinglib;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.blocks.AnnotationHandler;
import com.strategyquant.tradinglib.blocks.AnnotationProcessor;
import com.strategyquant.tradinglib.blocks.ParameterAnnotationGenerator;
import com.strategyquant.tradinglib.debug.Debugger;
import com.strategyquant.tradinglib.exit.ExitMethodsList;
import com.strategyquant.tradinglib.formulas.Formulas;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ExitMethod extends Debugger implements IBlock {
   private static final int UseInitialSLPTHash = "UseInitialSLPT".hashCode();
   public static final Logger Log = LoggerFactory.getLogger("ExitMethod");
   public StrategyBase Strategy;
   private StrategyBase nonXmlStrategy = null;
   private Int2IntOpenHashMap customData = null;
   private int exitType = 0;
   private int returnType = 0;
   private int minSLPips = -1;
   private int maxSLPips = -1;
   private int minPTPips = -1;
   private int maxPTPips = -1;

   public abstract double computeValue(byte var1, StrategyBase var2, String var3, double var4) throws TradingException;

   public abstract void setForOrder(ILiveOrder var1, StrategyBase var2) throws TradingException;

   public ExitMethod newInstance(StrategyBase var1, ArrayList<Element> var2) throws BlockDefinitionException {
      try {
         Constructor var3 = this.getClass().getConstructor();
         ExitMethod var4 = (ExitMethod)SQUtils.invokeUnchecked(var3, new Object[0]);
         var4.initialize(var1, var2);
         return var4;
      } catch (NoSuchMethodException | SecurityException var5) {
         var5.printStackTrace();
         throw new BlockDefinitionException("Cannot create ExitMethod", var5);
      }
   }

   private void initialize(StrategyBase var1, ArrayList<Element> var2) throws BlockDefinitionException {
      this.Strategy = var1;
      if (var2 != null) {
         this.parseXml(var2);
      }
   }

   private void parseXml(ArrayList<Element> var1) throws BlockDefinitionException {
      Class var2 = this.getClass();
      Element var3 = new Element("Item");

      for (Element var5 : var1) {
         Element var6 = var5.clone();
         this.fixKey(var6);
         var3.addContent(var6);
      }

      for (Field var7 : var2.getFields()) {
         AnnotationProcessor.wizardParseXml(this, var7, var3);
         int var8 = this.getFieldExitType(var7);
         if (var8 != 0) {
            this.exitType = var8;
         }
      }
   }

   private int getFieldExitType(Field var1) {
      for (Annotation var5 : var1.getAnnotations()) {
         if (var5 instanceof ExitType) {
            return ((ExitType)var5).value();
         }
      }

      return 0;
   }

   private void fixKey(Element var1) {
      String var2 = var1.getAttributeValue("key");
      String[] var3 = var2.split("\\.");
      StringBuilder var4 = new StringBuilder("#");
      var4.append(var3[1]);
      var1.setAttribute("key", var4.toString());
   }

   public void generateXmlParams(Element var1, Field var2, Element var3, ExitMethod[] var4, boolean var5) throws BlockDefinitionException {
      Class var6 = this.getClass();
      String var7 = var6.getSimpleName();
      String var8 = null;

      for (Field var12 : var6.getFields()) {
         Parameter var13 = var12.getAnnotation(Parameter.class);
         if (var13 != null) {
            Element var14 = new Element("Param");
            String var15 = var12.getName();
            String var16 = String.format("%s.%s", var7, var15);
            String var17 = var12.getType().getName();
            var14.setAttribute("key", AnnotationHandler.decorateFieldName(var16));
            var14.setAttribute("name", AnnotationHandler.getFieldName(var13.name(), SQUtils.insertUppercaseSpaces(var12.getName())));
            if (var17.equals("java.lang.String")) {
               var14.setAttribute("type", "string");
            } else if (var17.equals("java.lang.Object")) {
               var14.setAttribute("type", "none");
            } else {
               var14.setAttribute("type", var17);
            }

            int var18 = 1;
            ExitType var19 = var12.getAnnotation(ExitType.class);
            if (var19 != null) {
               var18 = var19.value();
            }

            if (var17.equals("int")
               || var17.equals("long")
               || var17.equals("double")
               || var17.equals("boolean")
               || var17.equals("java.lang.String")
               || var17.equals("java.lang.Object")) {
               ExitMethod var21 = this.findThisExitMethod(var5, var4);
               if (var21 != null) {
                  ParameterAnnotationGenerator.wizardGenerateXml(var13, var12, var3, var21, var6, var5, var18, var8, var14);
               }
            } else if (var17.equals("com.strategyquant.tradinglib.IFormula")) {
               var14.setAttribute("type", "formula");
               ExitMethod var20 = this.findThisExitMethod(var5, var4);
               if (var20 != null) {
                  ParameterAnnotationGenerator.wizardGenerateFormulaXml(var13, var12, var3, var20, var6, var17, var5, var18, var8, var14);
               }
            }

            var1.addContent(var14);
            var8 = var16;
         }
      }
   }

   private ExitMethod findThisExitMethod(boolean var1, ExitMethod[] var2) {
      if (var2 != null) {
         for (int var3 = 0; var3 < var2.length; var3++) {
            if (this.getClass().getCanonicalName().hashCode() == var2[var3].getClass().getCanonicalName().hashCode()) {
               return var2[var3];
            }
         }
      }

      if (!var1 && var2 == null) {
         for (ExitMethod var4 : ExitMethodsList.get().getAvailableClasses()) {
            if (this.getClass().getCanonicalName().hashCode() == var4.getClass().getCanonicalName().hashCode()) {
               return var4;
            }
         }
      }

      return null;
   }

   public void createExitMethodXml(Element var1) throws BlockDefinitionException {
      Class var2 = this.getClass();
      String var3 = var2.getSimpleName();
      String var4 = null;

      for (Field var8 : var2.getFields()) {
         Parameter var9 = var8.getAnnotation(Parameter.class);
         if (var9 != null) {
            Element var10 = new Element("ExitRule");
            String var11 = var8.getName();
            String var12 = String.format("%s.%s", var3, var11);
            var10.setAttribute("key", var12);
            var10.setAttribute("name", SQUtils.insertUppercaseSpaces(var11));
            var10.setAttribute("probability", "50");
            if (var4 != null) {
               var10.setAttribute("dependentOn", var4);
            }

            String var13 = var8.getType().getName();
            int var14 = 1;
            ExitType var15 = var8.getAnnotation(ExitType.class);
            if (var15 != null) {
               var14 = var15.value();
            }

            if (var13.equals("int")
               || var13.equals("long")
               || var13.equals("double")
               || var13.equals("boolean")
               || var13.equals("java.lang.String")
               || var13.equals("java.lang.Object")) {
               var10.setAttribute("type", var13);
               ParameterAnnotationGenerator.wizardGenerateXml(var9, var8, var10, null, var2, false, var14, var4, null);
            } else if (var13.equals("com.strategyquant.tradinglib.IFormula")) {
               var10.setAttribute("type", "formula");
               Editor var16 = var8.getAnnotation(Editor.class);
               if (var16 == null) {
                  throw new BlockDefinitionException("Parameter '" + var11 + "' has type formula, but doesn't have Formula editor specified!");
               }

               Formulas.generateFormulaParameters(var16.formulaName(), var10, var14, var4);
            }

            ForEngine var18 = var2.getAnnotation(ForEngine.class);
            if (var18 != null && !var18.value().equals("Null")) {
               String var17 = var18.value();
               var10.setAttribute("forEngine", var17);
            }

            var1.addContent(var10);
            var4 = var12;
         }
      }
   }

   @Override
   public double evaluateBlock() throws TradingException {
      throw new TradingException("Not used here!");
   }

   @Override
   public double evaluateBlock(int var1) throws TradingException {
      throw new TradingException("Not used here!");
   }

   @Override
   public IBlock newInstance(StrategyBase var1, Element var2) throws BlockDefinitionException {
      throw new BlockDefinitionException("Not used here!");
   }

   @Override
   public StrategyBase getStrategy() {
      return this.Strategy != null ? this.Strategy : this.nonXmlStrategy;
   }

   @Override
   public IBlock clone(boolean var1, StrategyBase var2) throws BlockDefinitionException {
      throw new BlockDefinitionException("Not used here!");
   }

   @Override
   public void setCustomData(String var1, int var2) {
      if (this.customData == null) {
         this.customData = new Int2IntOpenHashMap();
      }

      this.customData.put(var1.hashCode(), var2);
   }

   @Override
   public int getCustomData(String var1) {
      return this.customData != null && this.customData.containsKey(var1.hashCode()) ? this.customData.get(var1.hashCode()) : Integer.MIN_VALUE;
   }

   @Override
   public void copyCustomData(IBlock var1) {
      if (var1 instanceof ExitMethod) {
         ExitMethod var2 = (ExitMethod)var1;
         if (var2.customData != null) {
            if (this.customData == null) {
               this.customData = new Int2IntOpenHashMap();
            }

            this.customData.putAll(var2.customData);
         }
      }
   }

   public int getExitType() {
      return this.exitType;
   }

   @Override
   public int getReturnType() {
      return this.returnType;
   }

   @Override
   public void setReturnType(int var1) {
      this.returnType = var1;
   }

   public double correctSLPT(ILiveOrder var1, double var2, boolean var4) {
      return this.correctSLPT(var1.getOpenPrice(), var2, var1.getDirection(), var1.getInstrumentInfo().tickSize, var4);
   }

   public double correctSLPT(double var1, double var3, int var5, double var6, boolean var8) {
      if (this.minSLPips < 0) {
         SettingsMap var9 = this.Strategy.getSettings();
         this.minSLPips = var9.getInt("MinMaxSLPT.MinimumSL", 0);
         this.maxSLPips = var9.getInt("MinMaxSLPT.MaximumSL", 0);
      }

      if (this.minPTPips < 0) {
         SettingsMap var12 = this.Strategy.getSettings();
         this.minPTPips = var12.getInt("MinMaxSLPT.MinimumPT", 0);
         this.maxPTPips = var12.getInt("MinMaxSLPT.MaximumPT", 0);
      }

      int var13 = (var5 <= 0 || var8) && (var5 >= 0 || !var8) ? -1 : 1;
      if (var8) {
         if (this.minSLPips > 0) {
            double var10 = var1 + var13 * this.minSLPips * var6;
            var3 = var13 > 0 ? Math.max(var10, var3) : Math.min(var10, var3);
         }

         if (this.maxSLPips > 0) {
            double var14 = var1 + var13 * this.maxSLPips * var6;
            var3 = var13 > 0 ? Math.min(var14, var3) : Math.max(var14, var3);
         }
      } else {
         if (this.minPTPips > 0) {
            double var15 = var1 + var13 * this.minPTPips * var6;
            var3 = var13 > 0 ? Math.max(var15, var3) : Math.min(var15, var3);
         }

         if (this.maxPTPips > 0) {
            double var16 = var1 + var13 * this.maxPTPips * var6;
            var3 = var13 > 0 ? Math.min(var16, var3) : Math.max(var16, var3);
         }
      }

      return var3;
   }

   public boolean setExit(ILiveOrder var1, StrategyBase var2) throws TradingException {
      return false;
   }

   @Override
   public Element getCustomBlockXml(int var1) throws BlockDefinitionException {
      throw new BlockDefinitionException("Not implemented for this!");
   }

   protected boolean isInitialSLPTApplied(StrategyBase var1) {
      SettingsMap var2 = var1.getSettings();
      return var2.containsKey(UseInitialSLPTHash) && (Boolean)var2.get(UseInitialSLPTHash);
   }

   protected boolean shouldApplySLPTToOrder(ILiveOrder var1, StrategyBase var2) throws TradingException {
      boolean var3 = var2.getEngine() == 56756755 || var2.getEngine() == 938213070;
      return var3 ? this.isInitialSLPTApplied(var2) || var2.Time() >= var1.getOpenTime() : true;
   }
}
