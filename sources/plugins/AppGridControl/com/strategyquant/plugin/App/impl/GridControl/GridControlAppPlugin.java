package com.strategyquant.plugin.App.impl.GridControl;

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
@Name(name = "Grid Control")
@Category(name = "App")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class GridControlAppPlugin implements IAppPlugin {
   public static final Logger Log = LoggerFactory.getLogger(GridControlAppPlugin.class);

   public String getName() {
      return "Grid Control";
   }

   public String getProduct() {
      return "SQUANT";
   }

   public int getPreferredPosition() {
      return 100;
   }

   public void initPlugin() throws Exception {
   }

   public String getContextPath() {
      return "/grid-control";
   }

   public String getAppCode() {
      return "GRIDCONTROL";
   }

   public String getTooltip() {
      return "Grid Control";
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
