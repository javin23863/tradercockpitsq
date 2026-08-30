package com.strategyquant.plugin.App.impl.Builder;

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
@Name(name = "Builder")
@Category(name = "App")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class BuilderAppPlugin implements IAppPlugin {
   public static final Logger Log = LoggerFactory.getLogger(BuilderAppPlugin.class);

   public String getName() {
      return "Builder";
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
      return "/builder";
   }

   public String getAppCode() {
      return "BUILDER";
   }

   public String getTooltip() {
      return "Builder";
   }

   public String getProject() {
      return "Builder";
   }

   public String getDefaultTaskType() {
      return "Build";
   }

   public String getDefaultTaskName() {
      return "Build";
   }
}
