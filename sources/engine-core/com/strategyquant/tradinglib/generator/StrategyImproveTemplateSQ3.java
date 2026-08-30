package com.strategyquant.tradinglib.generator;

import com.strategyquant.lib.IRandomGenerator;
import com.strategyquant.lib.L;
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

public class StrategyImproveTemplateSQ3 extends StrategyStandardTemplateSQ3 {
   public static final Logger Log = LoggerFactory.getLogger("StrategyImproveTemplate");

   public StrategyImproveTemplateSQ3(Element var1, IRandomGenerator var2) {
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
         throw new Exception(L.t("Improver - Strategy '%s' doesn't exist!", new Object[]{var1}));
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
         throw new Exception(L.t("Improver - No strategy found in file '%s' !", new Object[]{var1}));
      }

      Element var7 = var3.getChild("Strategy");
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

         for (Element var9 : var6.getChildren("Rule")) {
            String var10 = var9.getAttributeValue("type");
            String var11 = var9.getAttributeValue("name");
            boolean var12 = XMLUtil.getBooleanAttr(var9, "everyTick", false);
            if (!var10.equals("IfThen")) {
               var7.addContent(var9.clone());
            } else {
               Element var13 = new Element("Rule");
               var13.setAttribute("type", var10);
               var13.setAttribute("name", var11);
               var13.setAttribute("everyTick", String.valueOf(var12));
               Element var14 = var9.getChild("Descriptions");
               if (var14 != null) {
                  var13.addContent(var14.detach());
               }

               int var15 = this.recognizeRuleType(var11, var10);
               Element var16 = this.getIf(var9, var15);
               var13.addContent(var16);
               Element var17 = this.getImproverThen(var9, var15);
               var13.addContent(var17);
               var7.addContent(var13);
            }
         }
      }
   }

   private Element getIf(Element var1, int var2) throws Exception {
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
            var3 = 0;
      }

      if (var3 == 0) {
         return var1.getChild("If").detach();
      }

      if (var3 == 1) {
         Element var11 = null;
         if (var2 != 1 && var2 != 2) {
            var11 = XMLUtil.fileToXmlElement(new File(MainApp.getDataPath() + "internal/ctemplate/build/sq3/exitcondition.xml"));
            if (var2 == 3) {
               this.configureRandomExitCondition(var11, "RandomConditionLongExit", null, 1, false);
            } else {
               this.configureRandomExitCondition(var11, "RandomConditionShortExit", "RandomConditionLongExit", -1, this.exitSymmetry);
            }

            if (var2 == 3 || var2 == 4) {
               String var13 = var2 == 3 ? this.longMagicNumberValue : this.shortMagicNumberValue;
               if (var13 != null && !var13.equals("")) {
                  Element var15 = var11.getChild("Block").getChild("Item");
                  if (var15 != null) {
                     Element var16 = XMLUtil.getItemParameter(var15, "#MagicNumber#", false);
                     var16.setText(var13);
                  }
               }
            }
         } else {
            var11 = XMLUtil.fileToXmlElement(new File(MainApp.getDataPath() + "internal/ctemplate/build/sq3/entrycondition.xml"));
            if (var2 == 1) {
               this.configureRandomEntryCondition(var11, "RandomConditionLong", null, false);
            } else {
               this.configureRandomEntryCondition(var11, "RandomConditionShort", "RandomConditionLong", this.entrySymmetry);
            }
         }

         Element var14 = new Element("If");
         var14.addContent(var11.detach());
         return var14;
      } else {
         if (var3 != 2 && var3 != 2) {
            throw new Exception("Incorrect improvement action!");
         }

         Element var4 = var1.getChild("If").detach();
         Element var5 = XMLUtil.fileToXmlElement(new File(MainApp.getDataPath() + "internal/ctemplate/build/sq3/conditiononly.xml"));
         if (var2 != 1 && var2 != 2) {
            if (var2 == 3) {
               this.configureRandomExitConditionOnly(var5, "RandomConditionLongExit", null, false);
            } else {
               this.configureRandomExitConditionOnly(var5, "RandomConditionShortExit", "RandomConditionLongExit", this.exitSymmetry);
            }
         } else if (var2 == 1) {
            this.configureRandomEntryCondition(var5, "RandomConditionLong", null, false);
         } else {
            this.configureRandomEntryCondition(var5, "RandomConditionShort", "RandomConditionLong", this.entrySymmetry);
         }

         Element var6 = new Element("Block");
         var6.addContent(var5.detach());
         Element var7 = var4.getChild("Item");
         if (var7 != null) {
            String var8 = var7.getAttributeValue("key");
            if (var8 != null && var8.equals("AND")) {
               Element var17 = var7;
               var17.addContent(var6.detach());
            } else {
               Element var9 = new Element("Item");
               var9.setAttribute("key", "AND");
               new Element("Block");
               var9.addContent(var7.detach());
               var9.addContent(var6.detach());
               var4.addContent(var9);
            }
         }

         return var4;
      }
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
         var6.setAttribute("display", "RandomCondition(@Chart@#Identification#)");
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

      if (var2 == 1 || var2 == 2) {
         Element var5 = this.getStandardActionChild(var1.getChild("Then"));
         if (var5 != null) {
            Element var6 = XMLUtil.getItemParameter(var5, "#MagicNumber#", false);
            if (var6 != null) {
               if (var2 == 1) {
                  this.longMagicNumberValue = var6.getText();
               } else {
                  this.shortMagicNumberValue = var6.getText();
               }
            }
         }
      }

      if (var3 == 0 && var4 == 0) {
         return var1.getChild("Then").detach();
      }

      Element var15;
      if (var2 == 1 || !this.entrySymmetry) {
         var15 = XMLUtil.fileToXmlElement(new File(MainApp.getDataPath() + "internal/ctemplate/build/sq3/randomaction.xml"));
      } else if (this.exitSymmetry) {
         var15 = XMLUtil.fileToXmlElement(new File(MainApp.getDataPath() + "internal/ctemplate/build/sq3/oppositeaction_exitsymmetry.xml"));
      } else {
         var15 = XMLUtil.fileToXmlElement(new File(MainApp.getDataPath() + "internal/ctemplate/build/sq3/oppositeaction_exitunsymmetry.xml"));
      }

      Element var14;
      if (var3 == 0) {
         var14 = this.getStandardActionChild(var1.getChild("Then"));
      } else {
         var14 = var15.clone();
      }

      var14.removeContent();
      if (var2 != 1 && this.entrySymmetry) {
         this.addIdentificationParameter(var14, "randomIdCombo", "RandomActionLong");
      } else {
         this.addIdentificationParameter(var14, "randomId", var2 == 1 ? "RandomActionLong" : "RandomActionShort");
      }

      Element var7 = this.getStandardActionChild(var1.getChild("Then"));
      if (var4 == 0) {
         for (Element var9 : var7.getChildren("Param")) {
            var14.addContent(var9.clone());
         }
      } else {
         List var16 = var15.getChildren("Param");

         for (int var18 = 0; var18 < var16.size(); var18++) {
            Element var10 = (Element)var16.get(var18);
            if (!var10.getAttributeValue("key").equals("#Identification#")) {
               String var11 = var10.getAttributeValue("key");
               Element var12 = XMLUtil.getItemParameter(var7, var11, false);
               if (var12 == null) {
                  if (var3 != 0) {
                     var14.addContent(var10.clone());
                  }
               } else if (var11.contains(".")) {
                  boolean var13 = this.isUsedParameter(var12);
                  if (var4 != 0 && (var4 != 1 || var13)) {
                     var14.addContent(var10.clone());
                  } else {
                     var14.addContent(var12.clone());
                  }
               } else {
                  Element var19;
                  if (var3 == 0) {
                     var19 = var12.clone();
                  } else {
                     var19 = var10.clone();
                  }

                  if (var19.getAttributeValue("key").equals("#MagicNumber#")) {
                  }

                  var14.addContent(var19);
               }
            }
         }
      }

      Element var17 = new Element("Then");
      var17.addContent(var14);
      return var17;
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
