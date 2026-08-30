package com.strategyquant.tradinglib.gp.strategies;

import com.strategyquant.lib.IRandomGenerator;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.generator.RulePart;
import com.strategyquant.tradinglib.generator.RuleType;
import com.strategyquant.tradinglib.gp.AbstractFactory;
import java.util.List;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeneratedObject {
   public static final Logger Log = LoggerFactory.getLogger("GeneratedObject");
   private static final int TypeItem = 1;
   private static final int TypeParam = 2;
   private int GID;
   private int parentGID;
   private Element el;
   private Element elParent;
   private int type;
   private static final String NumberParamElement = "<Param key=\"#Number#\" name=\"Number\" type=\"double\" defaultValue=\"0\" controlType=\"jspinner\" minValue=\"-999999999\" maxValue=\"999999999\" step=\"1\" builderStep=\"1\">XXXXX</Param>";

   public GeneratedObject(int var1, Element var2) {
      this(var1, var2, 0);
   }

   public GeneratedObject(int var1, Element var2, int var3) {
      this.GID = var1;
      this.parentGID = var3;
      this.el = var2;
      switch (var2.getName()) {
         case "Item":
            this.type = 1;
            break;
         case "Param":
            this.type = 2;
            break;
         default:
            throw new IllegalArgumentException("Element is not Item neither Param!");
      }

      if (this.type == 2) {
         Element var6 = var2.getParentElement();
         if (var6.getName().equals("Item")) {
            this.elParent = var6;
         } else {
            var6 = var6.getParentElement();
            if (!var6.getName().equals("Item")) {
               throw new IllegalArgumentException("Cannot find parent element of this Param!");
            }

            this.elParent = var6;
         }
      }
   }

   public Element mutate(IRandomGenerator var1, AbstractFactory<Node> var2) throws Exception {
      if (this.el == null) {
         Log.error("el is null, this shouldn't happen!");
         return null;
      }

      String var3 = this.el.getAttributeValue("type");
      if (var3 != null && var3.equals("data")) {
         return null;
      }

      String var4 = this.el.getAttributeValue("key");
      if (var4 == null) {
         Log.error("key is null, this shouldn't happen! XML: {}", XMLUtil.elementToString(this.el));
         return null;
      }

      if (!var4.equals("#Comment#") && !var4.equals("#Size#") && !var4.equals("#Symbol#")) {
         if (var4.equals("#Number#")) {
            Element var5 = this.el.getParentElement();
            if (var5 != null) {
               var5 = var5.getParentElement();
               if (var5 != null) {
                  Element var6 = var5.getParentElement();
                  String var7 = var6.getAttributeValue("key");
                  if (var7.equals("IsGreater")
                     || var7.equals("IsLower")
                     || var7.equals("IsGreaterOrEqual")
                     || var7.equals("IsLowerOrEqual")
                     || var7.equals("Equals")
                     || var7.equals("NotEquals")) {
                     Element var8 = this.getParameter(var6, "#Left#");
                     if (var8 != null) {
                        String var9 = var8.getAttributeValue("key");
                        if (this.isBarTimeBlock(var9)) {
                           int var10 = this.generateCorrectRandomNumber(var1, var9);
                           String var11 = "<Param key=\"#Number#\" name=\"Number\" type=\"double\" defaultValue=\"0\" controlType=\"jspinner\" minValue=\"-999999999\" maxValue=\"999999999\" step=\"1\" builderStep=\"1\">XXXXX</Param>"
                              .replace("XXXXX", Integer.toString(var10));
                           return XMLUtil.stringToElement(var11);
                        }
                     }
                  }
               }
            }
         }

         NodeFactory var13 = (NodeFactory)var2;
         return this.el.getName().equals("Param") ? this.mutateParam(var1, var13) : this.mutateItem(var1, var13);
      } else {
         return null;
      }
   }

   private int generateCorrectRandomNumber(IRandomGenerator var1, String var2) {
      if (var2.contains("Hour")) {
         return var1.nextInt(23);
      } else if (var2.contains("Minute")) {
         return var1.nextInt(59);
      } else {
         return var2.contains("DayOfWeek") ? var1.nextInt(6) : 0;
      }
   }

   private Element getParameter(Element var1, String var2) {
      for (Element var4 : var1.getChildren()) {
         String var5 = var4.getAttributeValue("key");
         if (var5 != null && var5.equals(var2)) {
            return var4.getChild("Item");
         }
      }

      return null;
   }

   private boolean isBarTimeBlock(String var1) {
      return var1.contains("Hour") || var1.contains("Minute") || var1.contains("DayOfWeek");
   }

   private Element mutateItem(IRandomGenerator var1, NodeFactory var2) {
      return null;
   }

   private Element mutateParam(IRandomGenerator var1, NodeFactory var2) throws Exception {
      String var3 = this.elParent.getAttributeValue("randomId");
      if (var3 == null) {
         var3 = this.findRandomIdInSiblings();
      }

      if (var3 == null) {
         throw new IllegalArgumentException("randomId attribute not found in Param parent item or siblings!");
      }

      Element var4 = XMLUtil.copyElement(this.elParent);
      int var5 = RulePart.recognizeRulePartFromParents(this.elParent);
      int var6 = RuleType.recognizeRuleTypeFromParents(this.elParent);
      Element var7 = var2.generateRandomBlock(var3, var4, var5, var6);
      if (var7 == null) {
         return null;
      }

      Element var8 = XMLUtil.getItemParameter(var7, this.el.getAttributeValue("key"), true);
      String var9;
      String var10;
      if (this.el.getAttributeValue("key").contains(".")) {
         var9 = this.getFormulaValues(this.el);
         var10 = this.getFormulaValues(var8);
      } else {
         var9 = this.el.getText();
         var10 = var8.getText();
      }

      return var10.equals(var9) && !var9.equals("") ? null : var8;
   }

   private String findRandomIdInSiblings() {
      List var1 = this.elParent.getChildren("Param");

      for (int var2 = 0; var2 < var1.size(); var2++) {
         Element var3 = (Element)var1.get(var2);
         String var4 = var3.getAttributeValue("controlType");
         if (var4 != null && var4.equals("randomId")) {
            return var3.getText();
         }
      }

      return null;
   }

   private String getFormulaValues(Element var1) {
      StringBuilder var2 = new StringBuilder();
      this.getFormulaValuesRecursive(var2, var1);
      return var2.toString();
   }

   private void getFormulaValuesRecursive(StringBuilder var1, Element var2) {
      if (var2.getName().equals("Formula")) {
         var1.append(var2.getAttributeValue("key"));
         var1.append(",");
      }

      if (var2.getName().equals("Param")) {
         String var3 = var2.getText();
         if (!var3.equals("")) {
            var1.append(var3);
            var1.append(",");
         }
      }

      List var6 = var2.getChildren();
      if (var6 != null && var6.size() > 0) {
         for (int var4 = 0; var4 < var6.size(); var4++) {
            Element var5 = (Element)var6.get(var4);
            this.getFormulaValuesRecursive(var1, var5);
         }
      }
   }

   public int getParentGID() {
      return this.parentGID;
   }

   public int getGID() {
      return this.GID;
   }

   public void print() {
      System.out.println(String.format("ID: %d, %s - %s, parent: %d", this.GID, this.el.getName(), this.el.getAttributeValue("key"), this.parentGID));
   }

   public Element getElement() {
      return this.el;
   }

   public Element getParentElement() {
      return this.elParent;
   }
}
