package com.strategyquant.plugin.App.impl.PortfolioMaster;

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
@Name(name = "PortfolioMaster")
@Category(name = "App")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class PortfolioMasterAppPlugin implements IAppPlugin {
   public static final Logger Log = LoggerFactory.getLogger(PortfolioMasterAppPlugin.class);

   public String getName() {
      return "Portfolio Master";
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
      return "/portfoliomaster";
   }

   public String getAppCode() {
      return "PORTFOLIOMASTER";
   }

   public String getTooltip() {
      return "Portfolio Master";
   }

   public String getProject() {
      return "PortfolioMaster";
   }

   public String getDefaultTaskType() {
      return "AutomaticPortfolioBuilder";
   }

   public String getDefaultTaskName() {
      return "AutomaticPortfolioBuilder";
   }

   public boolean disabledForSpecialTrial() {
      return true;
   }
}
