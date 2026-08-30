package com.strategyquant.tradinglib.generator;

import com.strategyquant.lib.IRandomGenerator;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StrategyStandardTemplateSQ3 implements IStrategyTemplate {
   public static final Logger Log = LoggerFactory.getLogger("StrategyStandardTemplateSQ3");
   protected static final String MN_NAME = "MagicNumber";
   protected static final String MN_LONG_NAME = "MagicLong";
   protected static final String MN_SHORT_NAME = "MagicShort";
   protected static final String MN_LONG_ID = "11111111-1111-1111-1111-111111111111";
   protected static final String MN_SHORT_ID = "22222222-2222-2222-2222-222222222222";
   protected Element elSettings;
   protected Element elWhatToBuild;
   protected IRandomGenerator rng;
   protected boolean entrySymmetry;
   protected boolean exitSymmetry;
   protected String marketSides;
   protected boolean containsExitRule;
   protected boolean useSameIdForLongShort;
   protected List<Element> elCharts;
   protected String strategyType;
   protected Element elBlocks;
   protected boolean replacePendingOrders = false;
   protected static final int KeepExisting = 0;
   protected static final int Replace = 1;
   protected static final int Add = 2;
   protected static final int AddOrReplace = 2;
   protected static final int UnknownRule = 0;
   protected static final int LongEntry = 1;
   protected static final int ShortEntry = 2;
   protected static final int LongExit = 3;
   protected static final int ShortExit = 4;
   protected int entryLongImprovement;
   protected int entryShortImprovement;
   protected int orderLongImprovement;
   protected int orderShortImprovement;
   protected int exitLongImprovement;
   protected int exitShortImprovement;
   protected String longMagicNumberValue = "";
   protected String shortMagicNumberValue = "";

   public StrategyStandardTemplateSQ3(Element var1, IRandomGenerator var2) {
      this.rng = var2;
      this.elSettings = var1;
      this.elBlocks = var1.getChild("Blocks");
      this.elWhatToBuild = var1.getChild("WhatToBuild");
      Element var3 = this.elWhatToBuild.getChild("MarketSides");
      this.marketSides = var3.getAttributeValue("type");
      this.entrySymmetry = false;
      this.exitSymmetry = false;
      if (this.marketSides.equals("both")) {
         this.entrySymmetry = Boolean.parseBoolean(var3.getChildText("EntrySymmetry"));
         this.exitSymmetry = Boolean.parseBoolean(var3.getChildText("ExitSymmetry"));
      }

      this.strategyType = this.elWhatToBuild.getChild("StrategyType").getAttributeValue("type");
      this.useSameIdForLongShort = true;
      this.containsExitRule = this.checkContainsExitRule(this.elBlocks);
      Element var4 = var1.getChild("Options");
      if (var4 != null) {
         Element var5 = var4.getChild("BuildTradingOptions");
         if (var5 != null) {
            Element var6 = XMLUtil.getOptionParameter(var5, "ReplacePendingOrders");
            if (var6 != null) {
               this.replacePendingOrders = Boolean.parseBoolean(var6.getText());
            }
         }
      }

      this.elCharts = ((Element)var1.getChild("Data").getChild("Setups").getChildren("Setup").get(0)).getChildren("Chart");
   }

   @Override
   public Element generate() throws Exception {
      Element var1 = XMLUtil.fileToXmlElement(new File(MainApp.getDataPath() + "internal/ctemplate/build/sq3/template.xml"));
      this.addEntryRules(var1);
      this.addExitRules(var1);
      this.addDatas(var1);
      this.addVariables(var1);
      return var1;
   }

   protected void addVariables(Element var1) {
      Element var2 = var1.getChild("Strategy").getChild("Variables");
      if (this.useSameIdForLongShort) {
         this.createVariable(var2, "11111111-1111-1111-1111-111111111111", "MagicNumber", 11111);
      } else {
         this.createVariable(var2, "11111111-1111-1111-1111-111111111111", "MagicLong", 11111);
         this.createVariable(var2, "22222222-2222-2222-2222-222222222222", "MagicShort", 22222);
      }
   }

   protected void createVariable(Element var1, String var2, String var3, int var4) {
      Element var5 = new Element("variable");
      var5.addContent(new Element("id").addContent(var2));
      var5.addContent(new Element("name").addContent(var3));
      var5.addContent(new Element("type").addContent("int"));
      var5.addContent(new Element("value").addContent(Integer.toString(var4)));
      var5.addContent(new Element("makeExternal").addContent("true"));
      var1.addContent(var5);
   }

   protected void addDatas(Element var1) {
      Element var2 = var1.getChild("Strategy").getChild("Datas");
      String var3 = null;

      for (int var4 = 0; var4 < this.elCharts.size(); var4++) {
         Element var5 = this.elCharts.get(var4);
         Element var6 = new Element("data");
         var6.addContent(new Element("id").setText(Integer.toString(var4)));
         if (var4 == 0) {
            var6.addContent(new Element("chart").setText("Main chart"));
            var3 = var5.getAttributeValue("symbol");
            var6.addContent(new Element("symbol").setText("NULL"));
            var6.addContent(new Element("timeFrame").setText("0"));
            StrategyTemplateGenerator.addTickSizeToDataElem(var6, var3, var3);
         } else {
            var6.addContent(new Element("chart").setText(String.format("Subchart %d", var4)));
            String var7 = var3;
            if (var5.getAttributeValue("symbol").equals(var3)) {
               var6.addContent(new Element("symbol").setText("NULL"));
            } else {
               var7 = var5.getAttributeValue("symbol");
               var6.addContent(new Element("symbol").setText(var7));
            }

            var6.addContent(new Element("timeFrame").setText(convertTimeframeToWizard(var5.getAttributeValue("timeframe"))));
            StrategyTemplateGenerator.addTickSizeToDataElem(var6, var7, var3);
         }

         var2.addContent(var6);
      }
   }

   public static String convertTimeframeToWizard(String var0) {
      switch (var0) {
         case "M1":
            return "1";
         case "M5":
            return "5";
         case "M15":
            return "15";
         case "M30":
            return "30";
         case "H1":
            return "60";
         case "H4":
            return "240";
         case "D1":
            return "1440";
         case "Weekly":
            return "10080";
         case "Monthly":
            return "43200";
         default:
            return "0";
      }
   }

   private void addEntryRules(Element var1) throws Exception {
      if (this.marketSides.equals("both") || this.marketSides.equals("long")) {
         Element var2 = this.createEntryRule("Long entry", "RandomConditionLong", null, false, 1);
         Element var3 = this.createRandomAction(var2.getChild("Then").getChild("Item"), "RandomActionLong", 1, null, false, false);
         var2.getChild("Then").addContent(var3.detach());
         this.addRule(var1, var2, 1);
      }

      if (this.marketSides.equals("short") || this.marketSides.equals("both")) {
         Element var4 = this.createEntryRule("Short entry", "RandomConditionShort", "RandomConditionLong", this.entrySymmetry, -1);
         Element var5 = this.createRandomAction(
            var4.getChild("Then").getChild("Item"), "RandomActionShort", -1, "RandomActionLong", this.entrySymmetry, this.exitSymmetry
         );
         var4.getChild("Then").addContent(var5.detach());
         this.addRule(var1, var4, -1);
      }
   }

   private void addExitRules(Element var1) throws Exception {
      if (this.containsExitRule) {
         if (this.marketSides.equals("both") || this.marketSides.equals("long")) {
            Element var2 = this.createExitRule("Long exit", "RandomConditionLongExit", 1, null, false);
            this.addRule(var1, var2, 1);
         }

         if (this.marketSides.equals("short") || this.marketSides.equals("both")) {
            Element var3 = this.createExitRule("Short exit", "RandomConditionShortExit", -1, "RandomConditionLongExit", this.exitSymmetry);
            this.addRule(var1, var3, -1);
         }
      }
   }

   private Element createExitRule(String var1, String var2, int var3, String var4, boolean var5) throws Exception {
      Element var6 = XMLUtil.fileToXmlElement(new File(MainApp.getDataPath() + "internal/ctemplate/build/sq3/exitrule.xml"));
      var6.setAttribute("name", var1);
      this.configureRandomExitCondition(var6.getChild("If").getChild("Item"), var2, var4, var3, var5);
      Element var7 = (Element)var6.getChild("Then").getChild("Item").getChildren("Param").get(1);
      if (!var7.getAttributeValue("key").equals("#Direction#")) {
         throw new Exception("Second parameter in exit action is not direction!");
      }

      var7.addContent(Integer.toString(var3));
      Element var8 = (Element)var6.getChild("Then").getChild("Item").getChildren("Param").get(2);
      if (!var8.getAttributeValue("key").equals("#MagicNumber#")) {
         throw new Exception("Third parameter in exit action is not MagicNumber!");
      }

      if (var3 == 1) {
         var8.setText("11111111-1111-1111-1111-111111111111");
      } else if (this.useSameIdForLongShort) {
         var8.setText("11111111-1111-1111-1111-111111111111");
      } else {
         var8.setText("22222222-2222-2222-2222-222222222222");
      }

      return var6;
   }

   protected void configureRandomExitConditionOnly(Element var1, String var2, String var3, boolean var4) {
      Element var5 = var1.getChild("Param");
      if (!var4) {
         var1.setAttribute("key", "RandomCondition");
         var1.setAttribute("name", "RandomCondition");
         var1.setAttribute("display", "RandomCondition(@Chart@#Identification#)");
         var1.setAttribute("generated", "random");
         var5.addContent(var2);
         var5.setAttribute("ownRandomKey", "true");
      } else {
         var1.setAttribute("key", "NegatedCondition");
         var1.setAttribute("name", "NegatedCondition");
         var1.setAttribute("display", "NegatedCondition(#Identification#)");
         var1.setAttribute("generated", "negated");
         var5.addContent(var3);
      }
   }

   protected void configureRandomExitCondition(Element var1, String var2, String var3, int var4, boolean var5) throws Exception {
      Element var6 = ((Element)var1.getChildren("Block").get(0)).getChild("Item");
      if (var4 == 1) {
         var6.setAttribute("key", "MarketPositionIsLong");
         var6.setAttribute("name", "Market Position Is Long");
         var6.setAttribute("display", "Market Position(#Symbol#, #MagicNumber#, &amp;quot;#Comment#&amp;quot;) is Long");
         Element var7 = (Element)var6.getChildren("Param").get(1);
         if (!var7.getAttributeValue("key").equals("#MagicNumber#")) {
            throw new Exception("Missing MagicNumber parameetr!");
         }

         var7.setText("11111111-1111-1111-1111-111111111111");
      } else {
         var6.setAttribute("key", "MarketPositionIsShort");
         var6.setAttribute("name", "Market Position Is Short");
         var6.setAttribute("display", "Market Position(#Symbol#, #MagicNumber#, &amp;quot;#Comment#&amp;quot;) is Short");
         Element var8 = (Element)var6.getChildren("Param").get(1);
         if (!var8.getAttributeValue("key").equals("#MagicNumber#")) {
            throw new Exception("Second parameter in exit action is not MagicNumber!");
         }

         if (this.useSameIdForLongShort) {
            var8.setText("11111111-1111-1111-1111-111111111111");
         } else {
            var8.setText("22222222-2222-2222-2222-222222222222");
         }
      }

      Element var9 = ((Element)var1.getChildren("Block").get(1)).getChild("Item");
      this.configureRandomExitConditionOnly(var9, var2, var3, var5);
   }

   private Element createEntryRule(String var1, String var2, String var3, boolean var4, int var5) throws Exception {
      Element var6 = XMLUtil.fileToXmlElement(new File(MainApp.getDataPath() + "internal/ctemplate/build/sq3/entryrule.xml"));
      var6.setAttribute("name", var1);
      Element var7 = ((Element)var6.getChild("If").getChild("Item").getChildren("Block").get(0)).getChild("Item");
      Element var8 = XMLUtil.getItemParameter(var7, "#MagicNumber#", true);
      if (var5 != 1 && !this.useSameIdForLongShort) {
         var8.setText("22222222-2222-2222-2222-222222222222");
      } else {
         var8.setText("11111111-1111-1111-1111-111111111111");
      }

      Element var9 = ((Element)var6.getChild("If").getChild("Item").getChildren("Block").get(1)).getChild("Item");
      Element var10 = var9.getChild("Param");
      if (!var4) {
         var9.setAttribute("key", "RandomCondition");
         var9.setAttribute("name", "RandomCondition");
         var9.setAttribute("display", "RandomCondition(@Chart@#Identification#)");
         var9.setAttribute("generated", "random");
         var10.addContent(var2);
         var10.setAttribute("ownRandomKey", "true");
      } else {
         var9.setAttribute("key", "NegatedCondition");
         var9.setAttribute("name", "NegatedCondition");
         var9.setAttribute("display", "NegatedCondition(#Identification#)");
         var9.setAttribute("generated", "negated");
         var10.addContent(var3);
      }

      return var6;
   }

   protected Element createRandomAction(Element var1, String var2, int var3, String var4, boolean var5, boolean var6) throws JDOMException, IOException, Exception {
      Element var7;
      if ((var5 || var3 != 1) && !this.marketSides.equals("short")) {
         if (var6) {
            var7 = XMLUtil.fileToXmlElement(new File(MainApp.getDataPath() + "internal/ctemplate/build/sq3/oppositeaction_exitsymmetry.xml"));
         } else {
            var7 = XMLUtil.fileToXmlElement(new File(MainApp.getDataPath() + "internal/ctemplate/build/sq3/oppositeaction_exitunsymmetry.xml"));
         }

         if (var3 == -1 && var5 && var6) {
            Element var12 = XMLUtil.getItemParameter(var7, "#Identification#", true);
            var12.setText(var4);
            var7.setAttribute("key", "OppositeAction");
            var7.setAttribute("name", "OppositeAction");
            var7.setAttribute("display", "OppositeAction(#Identification#)");
            var7.setAttribute("generate", "opposite");
         } else {
            Element var11 = XMLUtil.getItemParameter(var7, "#Identification#", true);
            var11.setText(var2);
         }

         for (Element var17 : var7.getChildren("Param")) {
            String var10 = var17.getAttributeValue("generate");
            if (var10 != null && var10.equals("opposite")) {
               var17.setAttribute("identification", var4);
            }
         }

         Element var14 = XMLUtil.getItemParameter(var7, "#MagicNumber#", true);
         if (this.useSameIdForLongShort) {
            var14.setAttribute("defaultValue", "MagicNumber");
            var14.setText("11111111-1111-1111-1111-111111111111");
         } else {
            var14.setAttribute("defaultValue", "MagicShort");
            var14.setText("22222222-2222-2222-2222-222222222222");
         }
      } else {
         var7 = XMLUtil.fileToXmlElement(new File(MainApp.getDataPath() + "internal/ctemplate/build/sq3/randomaction.xml"));
         Element var8 = XMLUtil.getItemParameter(var7, "#Identification#", true);
         var8.setText(var2);
         Element var9 = XMLUtil.getItemParameter(var7, "#MagicNumber#", true);
         if (this.useSameIdForLongShort) {
            var9.setAttribute("defaultValue", "MagicNumber");
            var9.setText("11111111-1111-1111-1111-111111111111");
         } else {
            var9.setAttribute("defaultValue", "MagicLong");
            var9.setText("11111111-1111-1111-1111-111111111111");
         }
      }

      if (var3 == -1 && !var5) {
         Element var15 = XMLUtil.getItemParameter(var7, "#Price#", false);
         if (var15 != null) {
            var15.setAttribute("generate", "random");
            var15.setAttribute("identification", var2);
         }
      }

      Element var16 = XMLUtil.getItemParameter(var7, "#Direction#", true);
      var16.setText(Integer.toString(var3));
      if (this.replacePendingOrders) {
         Element var18 = XMLUtil.getItemParameter(var7, "#ReplaceExisting#", true);
         var18.setText("true");
      }

      return var7;
   }

   private void addRule(Element var1, Element var2, int var3) {
      Element var4 = (Element)var1.getChild("Strategy").getChild("Rules").getChild("Events").getChildren("Event").get(1);

      try {
         Element var5 = XMLUtil.getItemParameter(var2, "#Direction#", false);
         if (var5 != null) {
            var5.setText(Integer.toString(var3));
         }
      } catch (Exception var6) {
      }

      var2.detach();
      var4.addContent(var2);
   }

   private boolean checkContainsExitRule(Element var1) {
      Element var2 = var1.getChild("ExitTypes");

      for (Element var4 : var2.getChildren()) {
         if (var4.getAttributeValue("key").equals("_ExitRule_")) {
            return true;
         }
      }

      return false;
   }

   @Override
   public void setUseSameIdForLongShort(boolean var1) {
      this.useSameIdForLongShort = var1;
   }

   protected Element getImproverThen(Element var1, int var2) throws Exception {
      int var3;
      int var4;
      switch (var2) {
         case 1:
            var3 = this.orderLongImprovement;
            var4 = this.exitLongImprovement;
            break;
         case 2:
            var3 = this.orderShortImprovement;
            var4 = this.exitShortImprovement;
            break;
         default:
            var3 = 0;
            var4 = 0;
      }

      if (var3 == 0 && var4 == 0) {
         return var1.getChild("Then").detach();
      }

      Element var5 = this.getStandardActionChild(var1.getChild("Then"));
      if (var5 == null) {
         return new Element("Then");
      }

      this.getCurrentMagicNumberSetting(var5, var2);
      Element var6 = this.getImproverActionElement(var3, var5, var2);
      this.addPriceParameter(var6, var5, var3, var2);
      Element var7 = XMLUtil.fileToXmlElement(new File(MainApp.getDataPath() + "internal/ctemplate/build/sq3/randomaction.xml"));
      List var8 = var7.getChildren("Param");

      for (int var9 = 0; var9 < var8.size(); var9++) {
         Element var10 = (Element)var8.get(var9);
         String var11 = var10.getAttributeValue("key");
         if (!var11.equals("#Identification#") && !var11.equals("#Price#")) {
            boolean var12 = var11.contains(".");
            if (var12 && var4 != 0) {
               Element var16 = XMLUtil.getItemParameter(var5, var11, false);
               if (var16 != null) {
                  boolean var14 = this.isUsedParameter(var16);
                  if (var4 == 1 && !var14) {
                     var6.addContent(var16.clone());
                  } else if (var4 == 2 && var14) {
                     var6.addContent(var16.clone());
                  } else if (var2 != 1 && this.exitSymmetry) {
                     var10.setAttribute("generate", "opposite");
                     var10.setAttribute("identification", "RandomActionLong");
                     var6.addContent(var10.clone());
                  } else {
                     var10.setAttribute("generate", "random");
                     var6.addContent(var10.clone());
                  }
               }
            } else {
               Element var13 = XMLUtil.getItemParameter(var5, var11, false);
               if (var13 != null) {
                  var6.addContent(var13.clone());
               } else if (var2 != 1 && this.exitSymmetry) {
                  var10.setAttribute("generate", "opposite");
                  var10.setAttribute("identification", "RandomActionLong");
                  var6.addContent(var10.clone());
               } else {
                  var10.setAttribute("generate", "random");
                  var6.addContent(var10.clone());
               }
            }
         }
      }

      Element var15 = new Element("Then");
      var15.addContent(var6);
      return var15;
   }

   protected boolean isUsedParameter(Element var1) throws Exception {
      Element var2 = var1.getChild("Formula");
      if (var2 != null) {
         String var3 = var2.getAttributeValue("key");
         if (var3.contains(".None")) {
            return false;
         }
      } else {
         String var5 = var1.getText();
         String var4 = var1.getAttributeValue("defaultValue");
         if (var4 != null && var4.equals(var5)) {
            return false;
         }
      }

      return true;
   }

   private void addPriceParameter(Element var1, Element var2, int var3, int var4) throws Exception {
      if (var3 == 0) {
         Element var5 = XMLUtil.getItemParameter(var2, "#Price#", false);
         if (var5 != null) {
            var1.addContent(var5.clone());
            return;
         }
      }

      Element var6 = XMLUtil.fileToXmlElement(new File(MainApp.getDataPath() + "internal/ctemplate/build/sq3/entry_priceparam.xml"));
      if (var4 != 1 && this.entrySymmetry) {
         var6.setAttribute("generate", "opposite");
      } else {
         var6.setAttribute("generate", "random");
      }

      var1.addContent(var6.detach());
   }

   private Element getImproverActionElement(int var1, Element var2, int var3) {
      Element var4;
      if (var1 == 0) {
         var4 = var2.clone();
         var4.removeContent();
         this.addIdentificationParameter(var4, "randomId", var3 == 1 ? "RandomActionLong" : "RandomActionShort");
      } else {
         var4 = new Element("Item");
         if (var3 != 1 && this.entrySymmetry) {
            var4.setAttribute("key", "OppositeAction");
            var4.setAttribute("name", "OppositeAction");
            var4.setAttribute("returnType", "none");
            var4.setAttribute("display", "OppositeAction(#Identification#)");
            var4.setAttribute("generate", "opposite");
            var4.setAttribute("superType", "action");
            var4.setAttribute("categoryType", "randomBlock");
            this.addIdentificationParameter(var4, "randomIdCombo", "RandomActionLong");
         } else {
            var4.setAttribute("key", "RandomAction");
            var4.setAttribute("name", "RandomAction");
            var4.setAttribute("returnType", "none");
            var4.setAttribute("display", "RandomAction(#Identification#)");
            var4.setAttribute("generate", "random");
            var4.setAttribute("superType", "action");
            var4.setAttribute("categoryType", "randomBlock");
            this.addIdentificationParameter(var4, "randomId", var3 == 1 ? "RandomActionLong" : "RandomActionShort");
         }
      }

      return var4;
   }

   protected void addIdentificationParameter(Element var1, String var2, String var3) {
      Element var4 = new Element("Param");
      var4.setAttribute("key", "#Identification#");
      var4.setAttribute("name", "Identification");
      var4.setAttribute("type", "string");
      var4.setAttribute("controlType", var2);
      if (var2.equals("randomId")) {
         var4.setAttribute("ownRandomKey", "true");
      }

      var4.addContent(var3);
      var1.addContent(var4);
   }

   private void getCurrentMagicNumberSetting(Element var1, int var2) throws Exception {
      Element var3 = XMLUtil.getItemParameter(var1, "#MagicNumber#", false);
      if (var3 != null) {
         if (var2 == 1) {
            this.longMagicNumberValue = var3.getText();
         } else {
            this.shortMagicNumberValue = var3.getText();
         }
      }
   }

   protected Element getStandardActionChild(Element var1) throws Exception {
      List var2 = var1.getChildren();
      if (var2.size() == 0) {
         return null;
      } else if (var2.size() > 1) {
         throw new Exception("Strategy doesn't have standard format - THEN part contains more than one action!");
      } else {
         Element var3 = (Element)var2.get(0);
         String var4 = var3.getAttributeValue("returnType");
         if ((var4 == null || !var4.equals("order")) && !this.checkIsOpenOrderAction(var3)) {
            throw new Exception("Strategy doesn't have standard format - THEN action is not an order!");
         } else {
            return var3.clone();
         }
      }
   }

   private boolean checkIsOpenOrderAction(Element var1) {
      String var2 = var1.getAttributeValue("key");
      return var2.startsWith("EnterAt") || var2.startsWith("EnterReverseAt");
   }
}
