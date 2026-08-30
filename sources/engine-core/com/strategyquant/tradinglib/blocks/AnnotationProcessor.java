package com.strategyquant.tradinglib.blocks;

import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.ExitMethod;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnnotationProcessor {
   public static final Logger Log = LoggerFactory.getLogger("AnnotationProcessor");

   public static void wizardParseXml(IBlock var0, Field var1, Element var2) throws BlockDefinitionException {
      int var3 = 0;

      for (Annotation var7 : var1.getAnnotations()) {
         if (var7 instanceof Parameter) {
            ParameterAnnotationParser.wizardParseXml((Parameter)var7, var0, var1, var2);
         } else if (var7 instanceof Output) {
            OutputAnnotationHandler.wizardParseXml((Output)var7, var0, var1, var2);
         }

         var3++;
      }
   }

   public static void wizardGenerateXml(Field var0, Element var1, IBlock var2, Class<?> var3, boolean var4) throws BlockDefinitionException {
      if (var0.getType().toString().equals("class [Lcom.strategyquant.tradinglib.ExitMethod;")) {
         try {
            ParameterAnnotationGenerator.wizardGenerateExitMethodParams(var0, var1, (ExitMethod[])var0.get(var2), var3, var4);
         } catch (IllegalArgumentException | IllegalAccessException var9) {
            Log.error("Parameter is not an ExitMethods array (2)!", var9);
            throw new BlockDefinitionException("Parameter is not an ExitMethods array (2)!");
         }
      } else {
         for (Annotation var8 : var0.getAnnotations()) {
            if (var8 instanceof Parameter) {
               ParameterAnnotationGenerator.wizardGenerateXml((Parameter)var8, var0, var1, var2, var3, var4, 0, null, null);
            }
         }
      }
   }

   public static void wizardGenerateXmlForFormula(Field var0, Element var1, IBlock var2, Class<?> var3, int var4, String var5) throws BlockDefinitionException {
      for (Annotation var9 : var0.getAnnotations()) {
         if (var9 instanceof Parameter) {
            ParameterAnnotationGenerator.wizardGenerateXml((Parameter)var9, var0, var1, var2, var3, false, var4, var5, null);
         }
      }
   }

   public static void wizardParseExitTypesXml(IBlock var0, Field var1, Element var2) throws BlockDefinitionException {
      ParameterAnnotationParser.wizardParseXml(null, var0, var1, var2);
   }
}
