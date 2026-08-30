package com.strategyquant.plugin.Settings.impl.GoToTask;

import com.strategyquant.lib.L;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.project.ProjectConditions;
import com.strategyquant.tradinglib.project.ProjectEngine;
import com.strategyquant.tradinglib.project.SQProject;
import com.strategyquant.tradinglib.task.settings.ISettingTabPlugin;
import com.strategyquant.tradinglib.task.settings.TaskSettingsData;
import com.strategyquant.tradinglib.taskImpl.ISQTask;
import java.util.ArrayList;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;
import org.jdom2.Element;
import org.json.JSONArray;
import org.json.JSONObject;

@Author(name = "Tomas Brynda")
@Name(name = "Settings GoToTask plugin")
@Category(name = "Settings")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class SettingsGoToTaskPlugin implements ISettingTabPlugin {
   private static final String tabTitle = "Go To Task";

   public String getProduct() {
      return "SQUANT";
   }

   public int getPreferredPosition() {
      return 50;
   }

   public void initPlugin() throws Exception {
   }

   public void readSettings(String var1, ISQTask var2, Element var3, TaskSettingsData var4) {
      Element var5 = XMLUtil.tryAddElement(var3, this.getSettingName());
      Element var6 = XMLUtil.tryAddElement(var5, "Conditions");

      try {
         var5 = XMLUtil.getChildElem(var3, this.getSettingName());
         var6 = XMLUtil.tryAddElement(var5, "Conditions");
      } catch (Exception var13) {
         var4.addError("Go To Task", null, var13.getMessage());
         return;
      }

      String var7 = var5.getAttributeValue("task");
      if (var7 == null) {
         var4.addError("Go To Task", "Task", L.t("Task not set.", new Object[0]));
      } else {
         try {
            SQProject var8 = ProjectEngine.get(var1);
            Element var9 = var8.getTaskConfigByName(var7);
            if (var9 == null) {
               var4.addError("Go To Task", "Task", L.t("Task with name '%s' not found.", new Object[]{var7}));
               return;
            }

            boolean var10 = Boolean.parseBoolean(XMLUtil.tryGetAttr(var9, "active", "true"));
            if (!var10) {
               var4.addError("Go To Task", "Task", L.t("Task '%s' is disabled.", new Object[]{var7}));
               return;
            }
         } catch (Exception var12) {
            var4.addError("Go To Task", null, var12.getMessage());
         }

         try {
            ArrayList var16 = ProjectConditions.loadFromConfig(var6);
            var4.addParam("ProjectConditions", var16);
            var4.addParam("GoToTaskName", var7);
            var4.addParam("GoToTaskResetEvaluatedCycles", XMLUtil.getBooleanAttr(var5, "resetEvaluatedCycles", false));
            var4.addParam("GoToTaskResetActivatedCycles", XMLUtil.getBooleanAttr(var5, "resetActivatedCycles", false));
         } catch (Exception var11) {
            var4.addError("Go To Task", null, var11.getMessage());
         }
      }
   }

   public void getStrategyConfigSettings(Element var1, JSONArray var2) throws Exception {
   }

   public String getSettingName() {
      return "GoToTask";
   }

   public String getName() {
      return L.tsq("Go To Task");
   }

   public JSONObject getInitializationData() throws Exception {
      return null;
   }
}
