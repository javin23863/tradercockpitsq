package com.strategyquant.plugin.Settings.impl.Notes;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.task.settings.ISettingTabPlugin;
import com.strategyquant.tradinglib.task.settings.TaskSettingsData;
import com.strategyquant.tradinglib.taskImpl.ISQTask;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;
import org.jdom2.Element;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Author(name = "Tomas Brynda")
@Name(name = "Notes setting plugin")
@Category(name = "Settings")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class NotesSettingsPlugin implements ISettingTabPlugin {
   public static final Logger Log = LoggerFactory.getLogger(NotesSettingsPlugin.class);

   public String getProduct() {
      return "SQUANT";
   }

   public int getPreferredPosition() {
      return 100;
   }

   public void initPlugin() throws Exception {
   }

   public void readSettings(String var1, ISQTask var2, Element var3, TaskSettingsData var4) {
      XMLUtil.tryAddElement(var3, "Notes");

      try {
         Element var5 = XMLUtil.getChildElem(var3, "Notes");
         String var6 = var5.getTextNormalize();
         var6 = var6.replace("<div>", " ");
         var6 = var6.replace("&nbsp;", " ");
         var6 = SQUtils.removeHtmlTags(var6);
         var4.addParam("Notes", var6);
      } catch (Exception var7) {
      }
   }

   public void getStrategyConfigSettings(Element var1, JSONArray var2) throws Exception {
      Element var3 = XMLUtil.getChildElem(var1, "Notes");
      String var4 = var3.getTextNormalize();
      var4 = var4.replace("<div>", " ");
      var4 = var4.replace("&nbsp;", " ");
      var4 = SQUtils.removeHtmlTags(var4);
      var2.put(new JSONObject().put("key", "Notes").put("value", var4));
   }

   public String getSettingName() {
      return "Notes";
   }

   public String getName() {
      return L.tsq("Note");
   }

   public JSONObject getInitializationData() throws Exception {
      return null;
   }
}
