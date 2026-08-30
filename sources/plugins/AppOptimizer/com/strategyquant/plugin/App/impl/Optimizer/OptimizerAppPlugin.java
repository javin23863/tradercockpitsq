package com.strategyquant.plugin.App.impl.Optimizer;

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
@Name(name = "Optimizer")
@Category(name = "App")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class OptimizerAppPlugin implements IAppPlugin {
   public static final Logger Log = LoggerFactory.getLogger(OptimizerAppPlugin.class);

   public String getName() {
      return "Optimizer";
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
      return "/optimizer";
   }

   public String getAppCode() {
      return "OPTIMIZER";
   }

   public String getTooltip() {
      return "Optimizer";
   }

   public String getProject() {
      return "Optimizer";
   }

   public String getDefaultTaskType() {
      return "Optimize";
   }

   public String getDefaultTaskName() {
      return "Optimize";
   }

   public boolean disabledForSpecialTrial() {
      return true;
   }
}
