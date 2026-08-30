package com.strategyquant.plugin.Settings.impl.WhatToRetest;

import com.strategyquant.lib.L;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.project.ProjectConfigHelper;
import com.strategyquant.tradinglib.project.ProjectEngine;
import com.strategyquant.tradinglib.task.settings.ISettingTabPlugin;
import com.strategyquant.tradinglib.task.settings.TaskSettingsData;
import com.strategyquant.tradinglib.taskImpl.ISQTask;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;
import org.jdom2.Element;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Author(name = "Tomas Brynda")
@Name(name = "What To Retest setting plugin")
@Category(name = "Settings")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class SettingsWhatToRetestPlugin implements ISettingTabPlugin {
   public static final Logger Log = LoggerFactory.getLogger(SettingsWhatToRetestPlugin.class);

   public String getProduct() {
      return "SQUANT";
   }

   public int getPreferredPosition() {
      return 0;
   }

   public void initPlugin() throws Exception {
   }

   private void fixSettings(Element var1) {
      XMLUtil.tryAddElement(var1, "Databanks");
   }

   public void readSettings(String var1, ISQTask var2, Element var3, TaskSettingsData var4) {
      this.fixSettings(var3);
      Databank var5 = null;

      try {
         var5 = (Databank)ProjectEngine.get(var1).getDatabanks().get("Results");
      } catch (Exception var18) {
         String var7 = "Project '" + var1 + "' doesn't exist in ProjectEngine";
         Log.error(var7);
         var4.addError(this.getSettingName(), null, var7);
      }

      Serializable var6 = null;

      try {
         var6 = ProjectConfigHelper.getInputDatabank(var1, var3);
      } catch (Exception var17) {
         var6 = var5;
      }

      Serializable var21 = null;

      try {
         var21 = ProjectConfigHelper.getOutputDatabank(var1, var3);
      } catch (Exception var16) {
         var21 = var5;
      }

      try {
         var4.addParam("DatabankSource", var6);
         var4.addParam("DatabankTarget", var21);
      } catch (Exception var15) {
         var4.addError(this.getSettingName(), null, var15.getMessage());
      }

      try {
         Element var8 = var3.getChild("Databanks");
         if (var8 == null) {
            return;
         }

         String var9 = var8.getAttributeValue("retestSelected", "false");
         if (!var9.equals("true")) {
            return;
         }

         ArrayList var10 = new ArrayList();
         Element var11 = var3.getChild("SelectedStrategies");
         if (var11 != null) {
            List var12 = var11.getChildren("Strategy");

            for (int var13 = 0; var13 < var12.size(); var13++) {
               Element var14 = (Element)var12.get(var13);
               var10.add(var14.getText());
            }
         }

         var4.addParam("StrategiesToRetest", var10);
      } catch (Exception var19) {
         Log.error("Cannot load strategies to retest", var19);
      }
   }

   public void getStrategyConfigSettings(Element var1, JSONArray var2) throws Exception {
   }

   public String getSettingName() {
      return "Databanks";
   }

   public String getName() {
      return L.tsq("What to retest");
   }

   public JSONObject getInitializationData() throws Exception {
      return null;
   }
}
