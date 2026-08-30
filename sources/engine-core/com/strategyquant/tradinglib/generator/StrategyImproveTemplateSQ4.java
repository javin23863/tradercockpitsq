package com.strategyquant.tradinglib.generator;

import com.strategyquant.lib.IRandomGenerator;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.pluginlib.program.IProgram;
import com.strategyquant.pluginlib.program.Program;
import com.strategyquant.tradinglib.ResultsGroup;
import java.io.File;
import java.util.List;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StrategyImproveTemplateSQ4 extends StrategyStandardTemplateSQ4 {
   public static final Logger Log = LoggerFactory.getLogger("StrategyImproveTemplate");

   public StrategyImproveTemplateSQ4(Element var1, IRandomGenerator var2) {
      super(var1, var2);
   }

   @Override
   public Element generate() throws Exception {
      String var1 = this.elWhatToBuild.getChild("StrategyType").getAttributeValue("strategyFile");
      if (!var1.contains("/") && !var1.contains("\\")) {
         var1 = MainApp.getDataPath() + "user/strategies/" + var1;
      }

      File var2 = new File(var1);
      if (!var2.exists()) {
         throw new Exception(String.format("Improver - Strategy '%s' doesn't exist!", var1));
      }

      Element var3;
      if (var1.endsWith(".xml")) {
         var3 = XMLUtil.fileToXmlElement(var2);
      } else {
         IProgram var4 = Program.get("Loader");
         ResultsGroup var5 = (ResultsGroup)var4.call("loadFile", new Object[]{var1, false});
         var3 = var5.getStrategyXml();
      }

      if (var3 == null) {
         throw new Exception(String.format("Improver - No strategy found in file '%s' !", var1));
      }

      Element var7 = var3.getChild("Strategy");
      XMLUtil.removeAttribute(var7, "generate");
      XMLUtil.removeAttribute(var7, "generated");
      Element var8 = new Element("StrategyFile");
      Element var6 = new Element("Strategy");
      var8.addContent(var6);
      var6.setAttribute("name", var7.getAttributeValue("name"));
      var6.setAttribute("engine", var7.getAttributeValue("engine"));
      this.copyElement("Note", var7, var6);
      this.copyElement("Description", var7, var6);
      this.copyElement("MoneyManagement", var7, var6);
      this.copyElement("GlobalSLPT", var7, var6);
      this.createTemplateRules(var7, var6);
      this.copyElement("Variables", var7, var6);
      this.copyElement("Datas", var7, var6);
      return var8;
   }

   private void createTemplateRules(Element var1, Element var2) throws Exception {
      this.loadImproverSettings();
      Element var3 = new Element("Rules");
      var2.addContent(var3);
      Element var4 = new Element("Events");
      var3.addContent(var4);

      for (Element var6 : var1.getChild("Rules").getChild("Events").getChildren("Event")) {
         Element var7 = new Element("Event");
         var7.setAttribute("key", var6.getAttributeValue("key"));
         var4.addContent(var7);
         List var8 = var6.getChildren("Rule");

         for (int var9 = 0; var9 < var8.size(); var9++) {
            Element var10 = (Element)var8.get(var9);
            String var11 = var10.getAttributeValue("type");
            String var12 = var10.getAttributeValue("name");
            boolean var13 = XMLUtil.getBooleanAttr(var10, "everyTick", false);
            if (var11.equals("Signal")) {
               Element var14 = new Element("Rule");
               var14.setAttribute("type", var11);
               var14.setAttribute("name", var12);
               var14.setAttribute("everyTick", String.valueOf(var13));
               Element var15 = var10.getChild("Descriptions");
               if (var15 != null) {
                  var14.addContent(var15.detach());
               }

               Element var16 = this.getSignals(var10);
               var14.addContent(var16);
               var7.addContent(var14);
            } else if (var11.equals("IfThen")) {
               Element var19 = new Element("Rule");
               var19.setAttribute("type", var11);
               var19.setAttribute("name", var12);
               var19.setAttribute("everyTick", String.valueOf(var13));
               Element var20 = var10.getChild("Descriptions");
               if (var20 != null) {
                  var19.addContent(var20.detach());
               }

               int var21 = this.recognizeRuleType(var12, var11);
               Element var17 = var10.getChild("If").clone();
               var19.addContent(var17);
               Element var18 = this.getImproverThen(var10, var21);
               var19.addContent(var18);
               var7.addContent(var19);
            } else {
               var7.addContent(var10.clone());
            }
         }
      }
   }

   private int recognizeRuleType(String var1, String var2) {
      if (!var2.equals("IfThen")) {
         return 0;
      } else {
         var1 = var1.toUpperCase();
         if (var1.contains("LONG")) {
            return var1.contains("EXIT") ? 3 : 1;
         } else if (var1.contains("SHORT")) {
            return var1.contains("EXIT") ? 4 : 2;
         } else {
            return 0;
         }
      }
   }

   private Element getSignals(Element var1) throws Exception {
      Element var2 = new Element("signals");
      Element var3 = this.getSignal(var1, 1);
      if (var3 != null) {
         var2.addContent(var3);
      }

      Element var4 = this.getSignal(var1, 2);
      if (var4 != null) {
         var2.addContent(var4);
      }

      Element var5 = this.getSignal(var1, 3);
      if (var5 != null) {
         var2.addContent(var5);
      }

      Element var6 = this.getSignal(var1, 4);
      if (var6 != null) {
         var2.addContent(var6);
      }

      return var2;
   }

   private Element getSignal(Element var1, int var2) throws Exception {
      int var3;
      switch (var2) {
         case 1:
            var3 = this.entryLongImprovement;
            break;
         case 2:
            var3 = this.entryShortImprovement;
            break;
         case 3:
            var3 = this.exitLongImprovement;
            break;
         case 4:
            var3 = this.exitShortImprovement;
            break;
         default:
            throw new Exception("No signal type detected");
      }

      Element var4 = this.getSignalByType(var1, var2);
      if (var4 == null) {
         var4 = this.getSignalByType(var1, var2);
         return null;
      }

      if (var3 == 0) {
         return var4.detach();
      }

      if (var3 == 1) {
         Element var12 = null;
         if (var2 == 1) {
            var12 = this.getEntrySignalCondition(1, "RandomConditionLong", null);
         } else if (var2 == 2) {
            var12 = this.getEntrySignalCondition(-1, "RandomConditionShort", "RandomConditionLong");
         } else if (var2 == 3) {
            var12 = this.getExitSignalCondition(1, "RandomConditionLongExit", null);
         } else if (var2 == 4) {
            var12 = this.getExitSignalCondition(-1, "RandomConditionShortExit", "RandomConditionLongExit");
         }

         return var12;
      } else {
         if (var3 != 2 && var3 != 2) {
            throw new Exception("Incorrect improvement action!");
         }

         Element var5;
         if (var2 == 1) {
            var5 = this.getSignalRandomCondition(1, "RandomConditionLong", null);
         } else if (var2 == 2) {
            var5 = this.getSignalRandomCondition(-1, "RandomConditionShort", "RandomConditionLong");
         } else if (var2 == 3) {
            var5 = this.getSignalRandomCondition(1, "RandomConditionLongExit", null);
         } else {
            if (var2 != 4) {
               throw new Exception("No signal type detected");
            }

            var5 = this.getSignalRandomCondition(-1, "RandomConditionShortExit", "RandomConditionLongExit");
         }

         List var7 = var4.getChildren();
         Element var6;
         if (var7.size() <= 0) {
            var6 = var5;
         } else {
            Element var8 = (Element)var4.getChildren().get(0);
            Element var9 = new Element("Item");
            var9.setAttribute("key", "AND");
            Element var10 = new Element("Block");
            var9.addContent(var10);
            var10.addContent(var8.detach());
            var10 = new Element("Block");
            var9.addContent(var10);
            var10.addContent(var5.detach());
            var6 = var9;
         }

         var4.addContent(var6);
         return var4.detach();
      }
   }

   private Element getSignalRandomCondition(int var1, String var2, String var3) {
      Element var4 = new Element("Item");
      Element var5 = new Element("Param");
      var4.addContent(var5);
      var5.setAttribute("key", "#Identification#");
      var5.setAttribute("name", "Identification");
      var5.setAttribute("type", "string");
      if (var1 != 1 && this.entrySymmetry) {
         var4.setAttribute("key", "NegatedCondition");
         var4.setAttribute("name", "NegatedCondition");
         var4.setAttribute("display", "NegatedCondition(#Identification#)");
         var4.setAttribute("generated", "negated");
         var5.addContent(var3);
         var5.setAttribute("controlType", "randomIdCombo");
      } else {
         var4.setAttribute("key", "RandomCondition");
         var4.setAttribute("name", "RandomCondition");
         var4.setAttribute("display", "RandomCondition(@Chart@#Identification#)");
         var4.setAttribute("generated", "random");
         var5.addContent(var2);
         var5.setAttribute("controlType", "randomId");
         var5.setAttribute("ownRandomKey", "true");
      }

      return var4;
   }

   private Element getSignalByType(Element var1, int var2) throws Exception {
      List var3 = var1.getChild("signals").getChildren("signal");
      String var4 = null;
      switch (var2) {
         case 1:
            var4 = "33333333-1111-1111-3333-333333333333";
            break;
         case 2:
            var4 = "33333333-2222-1111-3333-333333333333";
            break;
         case 3:
            var4 = "33333333-1111-2222-3333-333333333333";
            break;
         case 4:
            var4 = "33333333-2222-2222-3333-333333333333";
      }

      if (var4 == null) {
         throw new Exception("Incorrect signalType");
      }

      for (int var5 = 0; var5 < var3.size(); var5++) {
         Element var6 = (Element)var3.get(var5);
         if (var6.getAttributeValue("variable").equals(var4)) {
            return var6;
         }
      }

      switch (var2) {
         case 1:
            if (var3.size() == 4) {
               return (Element)var3.get(0);
            }
            break;
         case 2:
            if (var3.size() == 3) {
               return (Element)var3.get(0);
            }
            break;
         case 3:
            if (var3.size() == 2) {
               return (Element)var3.get(0);
            }
            break;
         case 4:
            if (var3.size() == 1) {
               return (Element)var3.get(0);
            }
      }

      return null;
   }

   private void configureRandomEntryCondition(Element var1, String var2, String var3, boolean var4) {
      Element var5 = var1.getChild("Param");
      Element var6 = var1;
      if (var5 == null) {
         var6 = ((Element)var1.getChildren("Block").get(1)).getChild("Item");
         var5 = var6.getChild("Param");
      }

      if (!var4) {
         var6.setAttribute("key", "RandomCondition");
         var6.setAttribute("name", "RandomCondition");
         var6.setAttribute("display", "RandomCondition(#Identification#)");
         var6.setAttribute("generated", "random");
         var5.addContent(var2);
         var5.setAttribute("ownRandomKey", "true");
      } else {
         var6.setAttribute("key", "NegatedCondition");
         var6.setAttribute("name", "NegatedCondition");
         var6.setAttribute("display", "NegatedCondition(#Identification#)");
         var6.setAttribute("generated", "negated");
         var5.addContent(var3);
      }
   }

   private Element getThenOld(Element var1, int var2) throws Exception {
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

      Element var6;
      if (var2 == 1 || !this.entrySymmetry) {
         var6 = XMLUtil.fileToXmlElement(new File(MainApp.getDataPath() + "internal/ctemplate/build/sq3/randomaction.xml"));
      } else if (this.exitSymmetry) {
         var6 = XMLUtil.fileToXmlElement(new File(MainApp.getDataPath() + "internal/ctemplate/build/sq3/oppositeaction_exitsymmetry.xml"));
      } else {
         var6 = XMLUtil.fileToXmlElement(new File(MainApp.getDataPath() + "internal/ctemplate/build/sq3/oppositeaction_exitunsymmetry.xml"));
      }

      Element var5;
      if (var3 == 0) {
         var5 = this.getStandardActionChild(var1.getChild("Then"));
      } else {
         var5 = var6.clone();
      }

      var5.removeContent();
      if (var2 == 1 || !this.entrySymmetry) {
         this.addIdentificationParameter(var5, "randomId", var2 == 1 ? "RandomActionLong" : "RandomActionShort");
      } else if (this.entrySymmetry && !this.exitSymmetry && var2 == 2) {
         this.addIdentificationParameter(var5, "randomIdCombo", "RandomActionShort");
      } else {
         this.addIdentificationParameter(var5, "randomIdCombo", "RandomActionLong");
      }

      Element var7 = this.getStandardActionChild(var1.getChild("Then"));
      if (var4 == 0) {
         for (Element var9 : var7.getChildren("Param")) {
            var5.addContent(var9.clone());
         }
      } else {
         List var15 = var6.getChildren("Param");

         for (int var17 = 0; var17 < var15.size(); var17++) {
            Element var10 = (Element)var15.get(var17);
            if (!var10.getAttributeValue("key").equals("#Identification#")) {
               String var11 = var10.getAttributeValue("key");
               Element var12 = XMLUtil.getItemParameter(var7, var11, false);
               if (var12 == null) {
                  if (var3 != 0) {
                     var5.addContent(var10.clone());
                  }
               } else if (var11.contains(".")) {
                  boolean var13 = this.isUsedParameter(var12);
                  if (var4 != 0 && (var4 != 1 || var13)) {
                     Element var14 = var10.clone();
                     var5.addContent(var14);
                  } else {
                     var5.addContent(var12.clone());
                  }
               } else {
                  Element var18;
                  if (var3 == 0) {
                     var18 = var12.clone();
                  } else {
                     var18 = var10.clone();
                  }

                  if (var18.getAttributeValue("key").equals("#MagicNumber#")) {
                  }

                  var5.addContent(var18);
               }
            }
         }
      }

      Element var16 = new Element("Then");
      var16.addContent(var5);
      return var16;
   }

   private void loadImproverSettings() {
      Element var1 = this.elSettings.getChild("PartsToImprove");
      Element var2 = var1.getChild("EntryRules");
      Element var3 = var1.getChild("OrderTypes");
      Element var4 = var1.getChild("ExitRules");
      this.entryLongImprovement = this.recognizeImproveType(var2.getChild("LongImprovement"));
      if (this.entrySymmetry) {
         this.entryShortImprovement = this.entryLongImprovement;
      } else {
         this.entryShortImprovement = this.recognizeImproveType(var2.getChild("ShortImprovement"));
      }

      this.orderLongImprovement = this.recognizeImproveType(var3.getChild("LongImprovement"));
      this.orderShortImprovement = this.recognizeImproveType(var3.getChild("ShortImprovement"));
      this.exitLongImprovement = this.recognizeImproveType(var4.getChild("LongImprovement"));
      this.exitShortImprovement = this.recognizeImproveType(var4.getChild("ShortImprovement"));
   }

   private int recognizeImproveType(Element var1) {
      byte var2 = 1;
      String var3 = var1.getAttributeValue("use");
      if (var3.equals("false")) {
         return 0;
      }

      String var4 = var1.getAttributeValue("action");
      if (var4 != null) {
         switch (var4) {
            case "add":
               return 2;
            case "replace":
               return 1;
            case "add-or-replace":
               return 2;
         }
      }

      return var2;
   }

   private void copyElement(String var1, Element var2, Element var3) {
      Element var4 = var2.getChild(var1);
      if (var4 != null) {
         var3.addContent(var4.detach());
      }
   }
}
