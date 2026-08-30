package com.strategyquant.tradinglib.conditions;

import com.strategyquant.lib.XMLUtil;
import java.util.ArrayList;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Conditions {
   private static final Logger Log = LoggerFactory.getLogger(Conditions.class);
   protected ArrayList<Condition> conditions = new ArrayList<>();
   protected boolean conditionsFilled = false;
   protected boolean omit = false;

   public int count() {
      return this.conditions.size();
   }

   public void checkConditions(Element var1) {
      Element var2 = var1.getChild("Conditions");
      if (var2 == null) {
         this.conditionsFilled = false;
      } else {
         String var3 = var2.getAttributeValue("conditionsType");
         if (var3 == null || !var3.equals("use") && !var3.equals("omit")) {
            var3 = "use";
         }

         this.omit = var3.equals("omit");

         for (Element var5 : var2.getChildren("Condition")) {
            boolean var6 = XMLUtil.tryGetBoolAttr(var5, "use");
            if (var6) {
               Element var7 = var5.getChild("Left-Side");
               Element var8 = var5.getChild("Right-Side");
               Element var9 = var5.getChild("Comparator");
               if (var7 != null && var8 != null && var9 != null) {
                  IConditionValue var10 = this.loadConditionValue(var7);
                  IConditionValue var11 = this.loadConditionValue(var8);
                  String var12 = var9.getAttributeValue("value");
                  if (var10 != null && var11 != null) {
                     Condition var13 = new Condition(var6, var10, var11, var12);
                     this.conditions.add(var13);
                     this.conditionsFilled = true;
                  }
               }
            }
         }
      }
   }

   private IConditionValue loadConditionValue(Element var1) {
      String var2 = var1.getAttributeValue("valueType");
      IConditionValue var3 = null;
      if (var2.equals("numeric")) {
         Element var4 = var1.getChild("Numeric-Value");
         if (var4 == null) {
            return null;
         }

         double var5 = XMLUtil.tryGetDoubleAttr(var4, "value");
         var3 = new NumericValue(var5);
      } else {
         var3 = this.getConditionValue(var2, var1);
      }

      return var3;
   }

   public boolean fitsConditions(Object var1) {
      if (this.conditionsFilled) {
         for (Condition var3 : this.conditions) {
            try {
               boolean var4 = var3.isMet(var1);
               if (!this.omit && !var4 || this.omit && var4) {
                  return false;
               }
            } catch (Exception var5) {
               Log.error("Error while checking condition", var5);
            }
         }
      }

      return true;
   }

   public boolean filled() {
      return this.conditionsFilled;
   }

   protected abstract IConditionValue getConditionValue(String var1, Element var2);
}
