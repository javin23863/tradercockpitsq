package com.strategyquant.plugin.App.impl.Retester;

import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.plugindef.app.IAppPlugin;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Author(name = "Tomas Brynda")
@Name(name = "Retester")
@Category(name = "App")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class RetesterAppPlugin implements IAppPlugin {
   public static final Logger Log = LoggerFactory.getLogger(RetesterAppPlugin.class);

   public String getName() {
      return "Retester";
   }

   public String getProduct() {
      return "SQUANT";
   }

   public int getPreferredPosition() {
      return 0;
   }

   public void initPlugin() throws Exception {
   }

   public String getContextPath() {
      return "/retester";
   }

   public String getAppCode() {
      return "RETESTER";
   }

   public String getTooltip() {
      return "Retester";
   }

   public String getProject() {
      return "Retester";
   }

   public String getDefaultTaskType() {
      return "Retest";
   }

   public String getDefaultTaskName() {
      return "Retest";
   }
}
