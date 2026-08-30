package com.strategyquant.plugin.Settings.impl.PartsToImprove;

import com.strategyquant.lib.L;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.atm.ATMTypes;
import com.strategyquant.tradinglib.task.settings.ISettingTabPlugin;
import com.strategyquant.tradinglib.task.settings.TaskSettingsData;
import com.strategyquant.tradinglib.task.settings.partstoimprove.PartsToImprove;
import com.strategyquant.tradinglib.taskImpl.ISQTask;
import java.util.List;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;
import org.jdom2.Element;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Author(name = "Tomas Brynda")
@Name(name = "Parts to improve setting plugin")
@Category(name = "Settings")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class PartsToImproveSettingsPlugin implements ISettingTabPlugin {
   public static final Logger Log = LoggerFactory.getLogger(PartsToImproveSettingsPlugin.class);
   private static final String tabTitle = "Parts to improve";

   public String getProduct() {
      return "SQUANT";
   }

   public int getPreferredPosition() {
      return 6;
   }

   public void initPlugin() throws Exception {
   }

   private void fixSettings(Element var1) {
      Element var2 = var1.getChild("WhatToBuild");
      if (var2 != null) {
         Element var3 = var2.getChild("StrategyType");
         if (var3 != null) {
            String var4 = var3.getAttributeValue("type");
            if (var4.equals("improve")) {
               Element var5 = XMLUtil.tryAddElement(var1, this.getSettingName());
               Element var6 = XMLUtil.tryAddElement(var5, "EntryRules");
               if (var6.getAttributeValue("symmetry") == null) {
                  var6.setAttribute("symmetry", "true");
               }

               Element var7 = XMLUtil.tryAddElement(var6, "LongImprovement");
               if (var7.getAttributeValue("use") == null) {
                  var7.setAttribute("use", "true");
               }

               if (var7.getAttributeValue("action") == null) {
                  var7.setAttribute("action", "add-or-replace");
               }

               Element var8 = XMLUtil.tryAddElement(var6, "ShortImprovement");
               if (var8.getAttributeValue("use") == null) {
                  var8.setAttribute("use", "true");
               }

               if (var8.getAttributeValue("action") == null) {
                  var8.setAttribute("action", "add-or-replace");
               }

               Element var9 = XMLUtil.tryAddElement(var5, "OrderTypes");
               Element var10 = XMLUtil.tryAddElement(var9, "LongImprovement");
               if (var10.getAttributeValue("use") == null) {
                  var10.setAttribute("use", "true");
               }

               Element var11 = XMLUtil.tryAddElement(var9, "ShortImprovement");
               if (var11.getAttributeValue("use") == null) {
                  var11.setAttribute("use", "true");
               }

               Element var12 = XMLUtil.tryAddElement(var5, "ExitRules");
               if (var12.getAttributeValue("symmetry") == null) {
                  var12.setAttribute("symmetry", "true");
               }

               Element var13 = XMLUtil.tryAddElement(var12, "LongImprovement");
               if (var13.getAttributeValue("use") == null) {
                  var13.setAttribute("use", "true");
               }

               if (var13.getAttributeValue("action") == null) {
                  var13.setAttribute("action", "add-or-replace");
               }

               Element var14 = XMLUtil.tryAddElement(var12, "ShortImprovement");
               if (var14.getAttributeValue("use") == null) {
                  var14.setAttribute("use", "true");
               }

               if (var14.getAttributeValue("action") == null) {
                  var14.setAttribute("action", "add-or-replace");
               }
            }
         }
      }
   }

   public void readSettings(String var1, ISQTask var2, Element var3, TaskSettingsData var4) {
      this.fixSettings(var3);
      Element var5 = var3.getChild("WhatToBuild");
      if (var5 != null) {
         Element var6 = var5.getChild("StrategyType");
         if (var6 != null) {
            String var7 = var6.getAttributeValue("type");
            if (var7.equals("improve")) {
               Element var8 = null;

               try {
                  var8 = XMLUtil.getChildElem(var3, this.getSettingName());
               } catch (Exception var12) {
                  var4.addError(this.getSettingName(), null, var12.getMessage());
                  return;
               }

               PartsToImprove var9 = new PartsToImprove();
               if (this.checkRules(var8, "EntryRules", var9, var4)
                  && this.checkRules(var8, "ExitRules", var9, var4)
                  && this.checkRules(var8, "OrderTypes", var9, var4)) {
                  try {
                     var4.addParam("PartsToImprove", var9);
                  } catch (Exception var11) {
                     var4.addError(this.getSettingName(), null, "Cannot load Parts to improve - " + var11.getMessage());
                  }
               }

               this.verifyConfig(var8, var3, var4);
            }
         }
      }
   }

   private void verifyConfig(Element var1, Element var2, TaskSettingsData var3) {
      boolean var4 = Boolean.parseBoolean(XMLUtil.getAttr(var1, "improveATM", "false"));
      boolean var5 = false;
      List var6 = var1.getChildren();

      for (int var7 = 0; var7 < var6.size(); var7++) {
         Element var8 = (Element)var6.get(var7);
         List var9 = var8.getChildren();

         for (int var10 = 0; var10 < var9.size(); var10++) {
            Element var11 = (Element)var9.get(var10);
            boolean var12 = Boolean.parseBoolean(XMLUtil.getAttr(var11, "use", "false"));
            if (var12) {
               var5 = true;
               break;
            }
         }

         if (var5) {
            break;
         }
      }

      if (!var5 && !var4) {
         var3.addError(this.getSettingName(), null, "You didn't select to improve any part of the strategy!");
      }

      if (var4) {
         Element var13 = var2.getChild("ATMs");
         if (var13 == null) {
            return;
         }

         boolean var14 = XMLUtil.getBooleanAttr(var13, "enable", false);
         if (!var14) {
            var3.addError(this.getSettingName(), null, "You have chosen to improve ATM, but you didn't enabled ATM in ATM settings tab!");
         } else {
            int var15 = ATMTypes.recognize(var13.getAttributeValue("scaleOutType"), 2);
            if (var15 != 3) {
               var3.addError(this.getSettingName(), null, "You have chosen to improve ATM, but you didn't set ATM config to 'Generate' in ATM settings tab!");
            }
         }
      }
   }

   private boolean checkRules(Element var1, String var2, PartsToImprove var3, TaskSettingsData var4) {
      Element var5 = var1.getChild(var2);
      if (var5 == null) {
         var4.addError(this.getSettingName(), null, "Element '" + var2 + "' not found in config");
         return false;
      }

      Element var6 = var5.getChild("LongImprovement");
      if (var6 == null) {
         var4.addError(this.getSettingName(), null, var2 + " - Element 'LongImprovement' not found in config");
         return false;
      }

      Element var7 = var5.getChild("ShortImprovement");
      if (var7 == null) {
         var4.addError(this.getSettingName(), null, var2 + " - Element 'ShortImprovement' not found in config");
         return false;
      }

      boolean var8 = true;
      boolean var9 = var2.equals("EntryRules");
      boolean var10 = var2.equals("ExitRules");
      boolean var11 = XMLUtil.elementIs(var6, "use");
      boolean var12 = XMLUtil.elementIs(var7, "use");
      if (!var9 && !var10) {
         var3.useOrderLongImprovement = var11;
         var3.useOrderShortImprovement = var12;
      } else {
         String var13 = var6.getAttributeValue("action");

         try {
            this.checkActionType(var13);
         } catch (Exception var17) {
            var4.addError(this.getSettingName(), null, var2 + " - Invalid long improvement action type '" + var13 + "'");
            var8 = false;
         }

         String var14 = var7.getAttributeValue("action");

         try {
            this.checkActionType(var14);
         } catch (Exception var16) {
            var4.addError(this.getSettingName(), null, var2 + " - Invalid short improvement action type '" + var14 + "'");
            var8 = false;
         }

         if (var9) {
            var3.useEntryLongImprovement = var11;
            var3.useEntryShortImprovement = var12;
            var3.entryLongAction = var13;
            var3.entryShortAction = var14;
         } else {
            var3.useExitLongImprovement = var11;
            var3.useExitShortImprovement = var12;
            var3.exitLongAction = var13;
            var3.exitShortAction = var14;
         }
      }

      var3.improveATM = XMLUtil.getBooleanAttr(var1, "improveATM", false);
      return var8;
   }

   private void checkActionType(String var1) throws Exception {
      switch (var1) {
         case "add-or-replace":
         case "replace":
         case "add":
            return;
         default:
            throw new Exception("");
      }
   }

   public void getStrategyConfigSettings(Element var1, JSONArray var2) throws Exception {
   }

   public String getSettingName() {
      return "PartsToImprove";
   }

   public String getName() {
      return L.tsq("Parts to improve");
   }

   public JSONObject getInitializationData() throws Exception {
      return null;
   }
}
