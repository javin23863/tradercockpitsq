package com.strategyquant.tradinglib.moneymanagement;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.snippets.CustomClasses;
import com.strategyquant.lib.snippets.NonexistingCustomClassException;
import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.MoneyManagementMethod;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.blocks.AnnotationHandler;
import com.strategyquant.tradinglib.blocks.ParameterAnnotationGenerator;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import org.jdom2.Element;

public class MoneyManagementMethodsList extends CustomClasses<MoneyManagementMethod> {
   private static MoneyManagementMethodsList instance;
   private MMComparatorByOrder comparatorByOrder;

   public static MoneyManagementMethodsList get() {
      if (instance == null) {
         instance = new MoneyManagementMethodsList();
      }

      return instance;
   }

   private MoneyManagementMethodsList() {
      this.setDirName("MoneyManagement");
      this.setExpectedClassType(MoneyManagementMethod.class);
      this.comparatorByOrder = new MMComparatorByOrder();
      this.loadAvailableClasses();
   }

   public static MoneyManagementMethod create(String var0, Object... var1) throws NonexistingCustomClassException {
      return get()._create(var0, var1);
   }

   private MoneyManagementMethod _create(String var1, Object[] var2) throws NonexistingCustomClassException {
      for (MoneyManagementMethod var4 : this.getAvailableClasses()) {
         if (var4.getClass().getSimpleName().equals(var1)) {
            try {
               return (MoneyManagementMethod)var4.newInstance(var2);
            } catch (Exception var6) {
               Log.error("Cannot create new instance", var6);
            }
         }
      }

      throw new NonexistingCustomClassException(var1);
   }

   public static MoneyManagementMethod createFromXml(Element var0) throws NonexistingCustomClassException {
      return get()._createFromXml(var0);
   }

   private MoneyManagementMethod _createFromXml(Element var1) throws NonexistingCustomClassException {
      if (var1 == null) {
         return null;
      }

      String var2 = var1.getAttributeValue("type");
      if (var2 != null && !var2.equals("MMNone")) {
         for (MoneyManagementMethod var4 : this.getAvailableClasses()) {
            if (var4.getClass().getSimpleName().equals(var2)) {
               try {
                  MoneyManagementMethod var5 = var4.newInstance();
                  var5.setMethodFromXML(var1);
                  return var5;
               } catch (Exception var6) {
                  Log.error("Cannot create new instance", var6);
               }
            }
         }

         throw new NonexistingCustomClassException(var2);
      } else {
         return null;
      }
   }

   public static void generateMethodsXml(Element var0) throws BlockDefinitionException {
      get()._generateMethodsXml(var0);
   }

   private void _generateMethodsXml(Element var1) throws BlockDefinitionException {
      for (MoneyManagementMethod var3 : this.getAvailableClasses()) {
         Element var4 = new Element("Item");
         Class var5 = var3.getClass();
         var4.setAttribute("key", var5.getSimpleName());
         var4.setAttribute("name", var3.getName());
         Element var6 = new Element("Description");
         var6.addContent(var3.getNote());
         var4.addContent(var6);
         Element var7 = new Element("Params");
         var4.addContent(var7);

         for (Field var11 : var5.getFields()) {
            Parameter var12 = var11.getAnnotation(Parameter.class);
            if (var12 != null) {
               Element var13 = new Element("Param");
               var13.setAttribute("key", AnnotationHandler.decorateFieldName(var11.getName()));
               var13.setAttribute("name", AnnotationHandler.getFieldName(var12.name(), SQUtils.insertUppercaseSpaces(var11.getName())));
               String var14 = var11.getType().getName();
               if (!var14.equals("int") && !var14.equals("long") && !var14.equals("double") && !var14.equals("boolean") && !var14.equals("java.lang.String")) {
                  throw new BlockDefinitionException("Parameter '" + var11.getName() + "' has an unsupported type!");
               }

               ParameterAnnotationGenerator.wizardGenerateXml(var12, var11, null, null, var5, false, 0, null, var13);
               var7.addContent(var13);
            }
         }

         var1.addContent(var4);
      }
   }

   public void loadAvailableClasses() {
      super.loadAvailableClasses();
      List var1 = super.getAvailableClasses();
      Collections.sort(var1, this.comparatorByOrder);
   }

   public static MoneyManagementMethod loadMMMethodFromXML(Element var0) throws Exception {
      try {
         Element var1 = var0.getChild("Strategy").getChild("MoneyManagement");
         Element var2 = var1.getChild("Method");
         var2.setAttribute("type", var1.getAttributeValue("type"));
         MoneyManagementMethod var3 = createFromXml(var2);
         if (var3 == null) {
            throw new Exception("Cannot create MM method from XML");
         } else {
            return var3;
         }
      } catch (Exception var4) {
         Log.error("Cannot get strategy MoneyManagement settings. Using FixedSize 0.1...");
         return create("FixedSize", 0.1);
      }
   }
}
