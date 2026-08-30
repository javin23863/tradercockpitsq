package com.strategyquant.tradinglib.propertygrid;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.settings.IXMLAble;
import com.strategyquant.tradinglib.ClassConfig;
import com.strategyquant.tradinglib.Description;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.Help;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Iterator;
import org.jdom2.Element;

public abstract class ParametersTableItem<T extends ParametersTableItemProperties> extends ParametersTableItemProperties implements IXMLAble {
   private boolean used = false;
   public String _text = null;
   public boolean editable = true;
   public String name = null;

   public boolean isUsed() {
      return this.used;
   }

   public void setUsed(boolean var1) {
      this.used = var1;
   }

   public void setEditable(boolean var1) {
      this.editable = var1;
   }

   public boolean isEditable() {
      return this.editable;
   }

   public String getName() {
      ClassConfig var1 = this.getConfig();
      return var1 != null ? var1.name() : "Unknown";
   }

   public String getNote() {
      Help var1 = this.getClass().getAnnotation(Help.class);
      return var1 != null ? var1.value() : "";
   }

   public String getDescription() {
      Description var1 = this.getClass().getAnnotation(Description.class);
      return var1 != null ? var1.value() : "";
   }

   public String getFormatedName() {
      ClassConfig var1 = this.getConfig();
      return var1 != null ? var1.display() : "Unknown";
   }

   public String printFormatedName() {
      String var1 = this.getFormatedName();

      for (IPGParameter var4 : this.getParams()) {
         var1 = var1.replace("#" + var4.getKey() + "#", var4.print());
      }

      return var1;
   }

   public String getEngine() {
      ForEngine var1 = this.getClass().getAnnotation(ForEngine.class);
      return var1 != null ? var1.value() : "*";
   }

   public boolean checkEngine(String var1) {
      String var2 = this.getEngine();
      return var1 != null && !var2.isEmpty() ? SQUtils.checkEngine(var1, var2) : true;
   }

   private ClassConfig getConfig() {
      Annotation var1 = this.getClass().getAnnotation(ClassConfig.class);
      return (ClassConfig)var1;
   }

   public T newInstance(Object[] var1) throws Exception {
      Constructor var2 = this.getClass().getConstructor();
      ParametersTableItemProperties var3 = (ParametersTableItemProperties)SQUtils.invokeUnchecked(var2, new Object[0]);
      var3.applyParameters(var1);
      return (T)var3;
   }

   public T getClone() throws Exception {
      Constructor var1 = this.getClass().getConstructor();
      ParametersTableItemProperties var2 = (ParametersTableItemProperties)SQUtils.invokeUnchecked(var1, new Object[0]);
      var2.applyParameters(this.getParams());
      return (T)var2;
   }

   public Element getXML() {
      String var1 = this.getClass().getSimpleName();
      Element var2 = new Element("Params");
      ArrayList var3 = this.getParams();

      for (int var4 = 0; var4 < var3.size(); var4++) {
         IPGParameter var5 = (IPGParameter)var3.get(var4);

         try {
            String var6 = var5.getEngine();
            Element var7 = var5.getXML()
               .setAttribute("className", var1)
               .setAttribute("category", var5.getCategory())
               .setAttribute("engine", var6 == null ? "*" : var6);
            var2.addContent(var7);
         } catch (Exception var8) {
            Log.error("Parameter '" + var5.getKey() + "' wasn't found", var8);
         }
      }

      return var2;
   }

   public void setFromXML(Element var1) throws Exception {
      Element var2 = var1.getChild("Params");
      if (var2 != null) {
         for (Element var4 : var2.getChildren("Param")) {
            this.setFromParameterEl(var4);
         }
      }
   }

   public void setFromParameterEl(Element var1) throws Exception {
      String var2 = var1.getAttributeValue("key");
      String var3 = var1.getTextTrim();
      if (var3.isEmpty()) {
         var3 = var1.getAttributeValue("value");
      }

      this.setParameterValue(var2, var3);
   }

   @Override
   public boolean equals(Object var1) {
      try {
         ParametersTableItemProperties var2 = (ParametersTableItemProperties)var1;

         for (String var4 : this.getParametersList()) {
            boolean var5 = false;
            Iterator var6 = var2.getParametersList().iterator();

            while (true) {
               if (var6.hasNext()) {
                  String var7 = (String)var6.next();
                  if (!var4.equals(var7) || !this.getParameterValue(var4).equals(var2.getParameterValue(var4))) {
                     continue;
                  }

                  var5 = true;
               }

               if (!var5) {
                  return false;
               }
               break;
            }
         }
      } catch (Exception var8) {
      }

      return true;
   }

   protected Element getUsedSubelement(Element var1, String var2) {
      boolean var3 = false;
      int var4 = 0;

      for (Element var6 : var1.getChildren(var2)) {
         var4++;
         String var7 = var6.getAttributeValue("use");
         if (var7 != null) {
            var3 = true;
            if (var7.equals("true")) {
               return var6;
            }
         }
      }

      if (var4 == 1 && !var3) {
         Iterator var8 = var1.getChildren(var2).iterator();
         if (var8.hasNext()) {
            return (Element)var8.next();
         }
      }

      return null;
   }
}
