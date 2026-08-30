package com.strategyquant.plugin.App.impl.DataManager;

import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.plugindef.app.IAppPlugin;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Author(name = "Mark Fric")
@Name(name = "DataManager")
@Category(name = "App")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class DataManagerAppPlugin implements IAppPlugin {
   public static final Logger Log = LoggerFactory.getLogger(DataManagerAppPlugin.class);

   public String getName() {
      return "DataManager";
   }

   public String getProduct() {
      return "SQUANTQDM";
   }

   public int getPreferredPosition() {
      return 1;
   }

   public void initPlugin() throws Exception {
   }

   public String getContextPath() {
      return "/manager";
   }

   public String getAppCode() {
      return "SQMANAGER";
   }

   public String getTooltip() {
      return "DataManager";
   }

   public String getProject() {
      return null;
   }

   public String getDefaultTaskType() {
      return null;
   }

   public String getDefaultTaskName() {
      return null;
   }
}
