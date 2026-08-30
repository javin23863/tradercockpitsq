package com.strategyquant.tradinglib;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParametersHelper {
   public static final Logger Log = LoggerFactory.getLogger("ParametersHelper");

   public static void setParameterValue(IBlock var0, String var1, Object var2) throws BlockDefinitionException {
      Class var3 = var0.getClass();

      for (Field var7 : var3.getFields()) {
         for (Annotation var11 : var7.getAnnotations()) {
            if (var11 instanceof Parameter && var7.getName().equals(var1)) {
               try {
                  var7.set(var0, var2);
               } catch (IllegalArgumentException | IllegalAccessException var13) {
                  throw new BlockDefinitionException("Exception setting value of parameter '" + var1 + "' found!", var13);
               }
            }
         }
      }
   }

   public static Object getParameterValue(IBlock var0, String var1) throws BlockDefinitionException {
      Class var2 = var0.getClass();

      for (Field var6 : var2.getFields()) {
         for (Annotation var10 : var6.getAnnotations()) {
            if (var10 instanceof Parameter && var6.getName().equals(var1)) {
               try {
                  return var6.get(var0);
               } catch (IllegalArgumentException | IllegalAccessException var12) {
                  throw new BlockDefinitionException("Exception getting value of parameter '" + var1 + "' found!", var12);
               }
            }
         }
      }

      throw new BlockDefinitionException("Exception - No parameter '" + var1 + "' found in block " + var2.getSimpleName() + " !");
   }

   public static ArrayList<String> getParameters(IBlock var0) {
      ArrayList var1 = new ArrayList();
      Class var2 = var0.getClass();

      for (Field var6 : var2.getFields()) {
         for (Annotation var10 : var6.getAnnotations()) {
            if (var10 instanceof Parameter) {
               var1.add(var6.getName());
            }
         }
      }

      return var1;
   }

   public static void setFieldValue(IBlock var0, String var1, Object var2) throws BlockDefinitionException {
      Class var3 = var0.getClass();

      for (Field var7 : var3.getFields()) {
         if (var7.getName().equals(var1)) {
            try {
               var7.set(var0, var2);
            } catch (IllegalArgumentException | IllegalAccessException var9) {
               throw new BlockDefinitionException("Exception setting value of parameter '" + var1 + "' found!", var9);
            }
         }
      }
   }

   public static Object getFieldValue(IBlock var0, String var1) throws BlockDefinitionException {
      Class var2 = var0.getClass();

      for (Field var6 : var2.getFields()) {
         if (var6.getName().equals(var1)) {
            try {
               return var6.get(var0);
            } catch (IllegalArgumentException | IllegalAccessException var8) {
               throw new BlockDefinitionException("Exception getting value of parameter '" + var1 + "' found!", var8);
            }
         }
      }

      throw new BlockDefinitionException("Exception - No parameter '" + var1 + "' found!");
   }

   public static void copyParametersFromBlockToBlock(IBlock var0, IBlock var1, IParametersHelperModifier var2) throws BlockDefinitionException {
      ArrayList var3 = new ArrayList();
      Class var4 = var1.getClass();

      for (Field var8 : var4.getFields()) {
         for (Annotation var12 : var8.getAnnotations()) {
            if (var12 instanceof Parameter) {
               var3.add(var8);
            }
         }
      }

      var4 = var0.getClass();

      for (Field var25 : var4.getFields()) {
         String var26 = var25.getName();

         for (Annotation var13 : var25.getAnnotations()) {
            if (var13 instanceof Parameter) {
               try {
                  Object var14 = var2.modifyParameterValue(var25.getName(), var25.get(var0));

                  for (int var15 = 0; var15 < var3.size(); var15++) {
                     Field var16 = (Field)var3.get(var15);
                     if (var16.getName().equals(var26)) {
                        var16.set(var1, var14);
                        StrategyBase var17 = var0.getStrategy();
                        if (var17 != null) {
                           Variables var18 = var17.variables();
                           if (var18 != null) {
                              Variable var19 = var18.getByField(var0, var26);
                              if (var19 != null) {
                                 var19.registerAttachedField(var1, var25);
                              }
                           }
                        }
                        break;
                     }
                  }
               } catch (IllegalArgumentException | IllegalAccessException var20) {
                  throw new BlockDefinitionException("Exception getting value of parameter '" + var25.getName() + "' found!", var20);
               }
            }
         }
      }
   }

   public static void negateParametersInClonedBlock(IBlock var0, IBlock var1, final NegatersList var2, final StrategyBase var3) throws BlockDefinitionException {
      copyParametersFromBlockToBlock(var0, var1, new IParametersHelperModifier() {
         @Override
         public Object modifyParameterValue(String var1, Object var2x) throws BlockDefinitionException {
            return var2x instanceof IBlock ? var2.negate((IBlock)var2x, var3) : var2x;
         }
      });
   }

   public static void negateDataSeries(IBlock var0, IBlock var1) {
      int var2 = getField(var0, "ComputedFrom");
      byte var3 = -1;
      if (var2 == 2) {
         var3 = 3;
      }

      if (var2 == 3) {
         var3 = 2;
      }

      if (var2 > 0) {
         if (var3 != -1) {
            setField(var1, "ComputedFrom", var3);
         }
      } else {
         var2 = var0.getCustomData("InputComputedFrom");
         var3 = -1;
         if (var2 == 2) {
            var3 = 3;
         }

         if (var2 == 3) {
            var3 = 2;
         }

         if (var3 != -1) {
            var1.setCustomData("InputComputedFrom", var3);
         } else {
            var2 = var0.getCustomData("ChartComputedFrom");
            if (var2 == 2) {
               var3 = 3;
            }

            if (var2 == 3) {
               var3 = 2;
            }

            if (var3 != -1) {
               var1.setCustomData("ChartComputedFrom", var3);
            }
         }
      }
   }

   public static int getField(Object var0, String var1) {
      Class var2 = var0.getClass();

      try {
         Field var3 = var2.getDeclaredField(var1);
         Object var4 = var3.get(var0);
         return (Integer)var4;
      } catch (Exception var5) {
         return Integer.MIN_VALUE;
      }
   }

   public static void setField(Object var0, String var1, int var2) {
      Class var3 = var0.getClass();

      try {
         Field var4 = var3.getDeclaredField(var1);
         var4.set(var0, var2);
      } catch (Exception var5) {
      }
   }
}
