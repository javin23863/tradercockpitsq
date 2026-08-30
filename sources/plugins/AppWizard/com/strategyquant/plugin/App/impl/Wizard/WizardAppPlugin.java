package com.strategyquant.plugin.App.impl.Wizard;

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
@Name(name = "Wizard")
@Category(name = "App")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class WizardAppPlugin implements IAppPlugin {
   public static final Logger Log = LoggerFactory.getLogger(WizardAppPlugin.class);

   public String getName() {
      return "Wizard";
   }

   public String getProduct() {
      return "SQUANT";
   }

   public int getPreferredPosition() {
      return 10;
   }

   public void initPlugin() throws Exception {
   }

   public String getContextPath() {
      return "/wizard";
   }

   public String getAppCode() {
      return "SQWIZARD";
   }

   public String getTooltip() {
      return "Wizard";
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
