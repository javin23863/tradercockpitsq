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

public class StrategyStockpickerTemplate implements IStrategyTemplate {
   public static final Logger Log = LoggerFactory.getLogger("StrategyStockpickerTemplate");
   public static final String SGN_LONG_ENTRY_ID = "33333333-1111-1111-3333-333333333333";
   public static final String SGN_LONG_ENTRY_NAME = "LongEntrySignal";
   public static final String SGN_LONG_EXIT_ID = "33333333-1111-2222-3333-333333333333";
   public static final String SGN_LONG_EXIT_NAME = "LongExitSignal";
   public static final String SGN_SHORT_ENTRY_ID = "33333333-2222-1111-3333-333333333333";
   public static final String SGN_SHORT_ENTRY_NAME = "ShortEntrySignal";
   public static final String SGN_SHORT_EXIT_ID = "33333333-2222-2222-3333-333333333333";
   public static final String SGN_SHORT_EXIT_NAME = "ShortExitSignal";
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
   protected int probabilityExitRule;
   protected int probabilityEODRule;

   public StrategyStockpickerTemplate(Element var1, IRandomGenerator var2) {
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
      this.checkContainsExitEODRules(this.elBlocks);
      this.elCharts = ((Element)var1.getChild("Data").getChild("Setups").getChildren("Setup").get(0)).getChildren("Chart");
   }

   @Override
   public Element generate() throws Exception {
      Element var1 = XMLUtil.fileToXmlElement(new File(MainApp.getDataPath() + "internal/ctemplate/build/sp/template.xml"));
      Element var2 = (Element)var1.getChild("Strategy").getChild("Rules").getChild("Events").getChildren("Event").get(1);
      this.addRules(var2);
      this.addVariables(var1);
      this.setExitsProbability(var1);
      return var1;
   }

   protected void addRules(Element var1) throws Exception {
      this.addRules(var1, "entryexitrule.xml");
      this.addPositionScoreRule(var1, "positionscorerulelong.xml");
      this.addPositionScoreRule(var1, "positionscoreruleshort.xml");
   }

   private void addPositionScoreRule(Element var1, String var2) throws JDOMException, IOException, Exception {
      Element var3 = XMLUtil.fileToXmlElement(new File(MainApp.getDataPath() + "internal/ctemplate/build/sp/" + var2)).detach();
      var1.addContent(var3);
   }

   protected void addRules(Element var1, String var2) throws JDOMException, IOException, Exception {
      Element var3 = XMLUtil.fileToXmlElement(new File(MainApp.getDataPath() + "internal/ctemplate/build/sp/" + var2));
      Element var4 = var3.clone().detach();
      var4.setAttribute("name", "Long");
      var1.addContent(var4);
      Element var5 = this.createEntry("RandomConditionLong", null, false, 1);
      var4.addContent(var5);
      Element var6 = this.createOrder("RandomActionLong", null, 1, false, false);
      if (var6 != null) {
         var4.addContent(var6);
      }

      Element[] var7 = this.createExits("RandomConditionLongExit", null, false, 1);

      for (int var8 = 0; var8 < var7.length; var8++) {
         var4.addContent(var7[var8]);
      }

      Element var13 = var3.clone().detach();
      var13.setAttribute("name", "Short");
      var1.addContent(var13);
      var5 = this.createEntry("RandomConditionShort", "RandomConditionLong", this.entrySymmetry, -1);
      var13.addContent(var5);
      var6 = this.createOrder("RandomActionShort", "RandomActionLong", -1, this.entrySymmetry, this.exitSymmetry);
      if (var6 != null) {
         var13.addContent(var6);
      }

      var7 = this.createExits("RandomConditionShortExit", "RandomConditionLongExit", this.entrySymmetry, -1);

      for (int var9 = 0; var9 < var7.length; var9++) {
         var13.addContent(var7[var9]);
      }
   }

   private Element createEmptyEntry() {
      return new Element("Entry");
   }

   private Element createEntry(String var1, String var2, boolean var3, int var4) throws Exception {
      if (var4 == 1) {
         if (!this.marketSides.equals("both") && !this.marketSides.equals("long")) {
            return this.createEmptyEntry();
         }
      } else if (!this.marketSides.equals("short") && !this.marketSides.equals("both")) {
         return this.createEmptyEntry();
      }

      Element var5 = XMLUtil.fileToXmlElement(new File(MainApp.getDataPath() + "internal/ctemplate/build/sp/entry.xml"));
      Element var6 = var5.getChild("Item");
      if (var6 == null) {
         throw new Exception("Cannot find RandomCondition Item for Entry!");
      }

      Element var7 = var6.getChild("Param");
      if (var7 == null) {
         throw new Exception("Cannot find Identification param for Entry!");
      }

      if (!var3) {
         var6.setAttribute("key", "RandomCondition");
         var6.setAttribute("name", "RandomCondition");
         var6.setAttribute("display", "RandomCondition(@Chart@#Identification#)");
         var6.setAttribute("generate", "random");
         var6.setAttribute("generated", "random");
         var7.setText(var1);
         var7.setAttribute("ownRandomKey", "true");
      } else {
         var6.setAttribute("key", "NegatedCondition");
         var6.setAttribute("name", "NegatedCondition");
         var6.setAttribute("display", "NegatedCondition(#Identification#)");
         var6.setAttribute("generate", "opposite");
         var6.removeAttribute("generated");
         var7.setAttribute("controlType", "randomIdCombo");
         var7.setText(var2);
      }

      return var5.detach();
   }

   private Element createOrder(String var1, String var2, int var3, boolean var4, boolean var5) throws Exception {
      if (var3 == 1) {
         if (!this.marketSides.equals("both") && !this.marketSides.equals("long")) {
            return null;
         }
      } else if (!this.marketSides.equals("short") && !this.marketSides.equals("both")) {
         return null;
      }

      Element var6;
      Element var7;
      if ((var4 || var3 != 1) && !this.marketSides.equals("short")) {
         if (var5) {
            var6 = XMLUtil.fileToXmlElement(new File(MainApp.getDataPath() + "internal/ctemplate/build/sp/oppositeaction_exitsymmetry.xml"));
         } else {
            var6 = XMLUtil.fileToXmlElement(new File(MainApp.getDataPath() + "internal/ctemplate/build/sp/oppositeaction_exitunsymmetry.xml"));
         }

         var7 = var6.getChild("Item");
         if (var3 == -1 && var4 && var5) {
            Element var12 = XMLUtil.getItemParameter(var7, "#Identification#", true);
            var12.setText(var2);
            var7.setAttribute("key", "OppositeAction");
            var7.setAttribute("name", "OppositeAction");
            var7.setAttribute("display", "OppositeAction(#Identification#)");
            var7.setAttribute("generate", "opposite");
         } else {
            Element var11 = XMLUtil.getItemParameter(var7, "#Identification#", true);
            var11.setText(var1);
         }

         for (Element var17 : var7.getChildren("Param")) {
            String var10 = var17.getAttributeValue("generate");
            if (var10 != null && var10.equals("opposite")) {
               var17.setAttribute("identification", var2);
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
         var6 = XMLUtil.fileToXmlElement(new File(MainApp.getDataPath() + "internal/ctemplate/build/sp/randomaction.xml"));
         var7 = var6.getChild("Item");
         Element var8 = XMLUtil.getItemParameter(var7, "#Identification#", true);
         var8.setText(var1);
         Element var9 = XMLUtil.getItemParameter(var7, "#MagicNumber#", true);
         if (this.useSameIdForLongShort) {
            var9.setAttribute("defaultValue", "MagicNumber");
            var9.setText("11111111-1111-1111-1111-111111111111");
         } else {
            var9.setAttribute("defaultValue", "MagicLong");
            var9.setText("11111111-1111-1111-1111-111111111111");
         }
      }

      if (var3 == -1 && !var4) {
         Element var15 = XMLUtil.getItemParameter(var7, "#Price#", false);
         if (var15 != null) {
            var15.setAttribute("generate", "random");
            var15.setAttribute("identification", var1);
         }
      }

      Element var16 = XMLUtil.getItemParameter(var7, "#Direction#", true);
      var16.setText(Integer.toString(var3));
      if (this.replacePendingOrders) {
         Element var18 = XMLUtil.getItemParameter(var7, "#ReplaceExisting#", true);
         var18.setText("true");
      }

      return var6.detach();
   }

   private Element[] createExits(String var1, String var2, boolean var3, int var4) throws Exception {
      return new Element[]{this.createExitEOD(), this.createExit(var1, var2, var3, var4)};
   }

   private Element createExitEOD() {
      Element var1 = new Element("Exit");
      if (this.probabilityEODRule > 0) {
         var1.setAttribute("atEndOfDay", "DayClose");
      } else {
         var1.setAttribute("atEndOfDay", "None");
      }

      return var1;
   }

   private Element createExit(String var1, String var2, boolean var3, int var4) throws Exception {
      Element var5 = new Element("Exit");
      if (this.probabilityExitRule == 0) {
         return var5;
      }

      if (var4 == 1 && (this.marketSides.equals("both") || this.marketSides.equals("long"))) {
         Element var6 = this.createExitCondition(var1, var2, var4, false);
         var5.addContent(var6);
      }

      if (var4 == -1 && (this.marketSides.equals("short") || this.marketSides.equals("both"))) {
         Element var7 = this.createExitCondition(var1, var2, var4, this.exitSymmetry);
         var5.addContent(var7);
      }

      return var5;
   }

   private Element createExitCondition(String var1, String var2, int var3, boolean var4) {
      Element var5 = new Element("Item");
      var5.setAttribute("returnType", "boolean");
      var5.setAttribute("ignoreInBuilder", "true");
      var5.setAttribute("randomTemplate", "true");
      var5.setAttribute("superType", "condition");
      Element var6 = new Element("Param");
      var6.setAttribute("key", "#Identification#");
      var6.setAttribute("name", "Identification");
      var6.setAttribute("type", "string");
      var6.setAttribute("controlType", "randomId");
      var6.setAttribute("ownRandomKey", "true");
      var5.addContent(var6);
      if (!var4) {
         var5.setAttribute("key", "RandomCondition");
         var5.setAttribute("name", "RandomCondition");
         var5.setAttribute("display", "RandomCondition(@Chart@#Identification#)");
         var5.setAttribute("generated", "random");
         var6.addContent(var1);
         var6.setAttribute("ownRandomKey", "true");
      } else {
         var5.setAttribute("key", "NegatedCondition");
         var5.setAttribute("name", "NegatedCondition");
         var5.setAttribute("display", "NegatedCondition(#Identification#)");
         var5.setAttribute("generated", "negated");
         var6.addContent(var2);
      }

      return var5;
   }

   @Override
   public void setUseSameIdForLongShort(boolean var1) {
      this.useSameIdForLongShort = var1;
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

   private void checkContainsExitEODRules(Element var1) {
      this.probabilityExitRule = 0;
      this.probabilityEODRule = 0;
      Element var2 = var1.getChild("ExitTypes");

      for (Element var4 : var2.getChildren()) {
         if (var4.getAttributeValue("key").equals("_ExitRule_") && var4.getAttributeValue("use").equals("true")) {
            this.probabilityExitRule = Integer.parseInt(var4.getAttributeValue("probability"));
         }

         if (var4.getAttributeValue("key").equals("_ExitEOD_") && var4.getAttributeValue("use").equals("true")) {
            this.probabilityEODRule = Integer.parseInt(var4.getAttributeValue("probability"));
         }
      }
   }

   protected void setExitsProbability(Element var1) {
      Element var2 = var1.getChild("Strategy");
      var2.setAttribute("probabilityExitRule", Integer.toString(this.probabilityExitRule));
      var2.setAttribute("probabilityEODRule", Integer.toString(this.probabilityEODRule));
   }

   public static void setExitsProbability(Element var0, Element var1, IRandomGenerator var2) {
      StrategyStockpickerTemplate var3 = new StrategyStockpickerTemplate(var1, var2);
      var3.setExitsProbability(var0);
   }
}
