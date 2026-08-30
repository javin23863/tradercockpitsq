package com.strategyquant.plugin.App.impl.DebugConsole;

import com.strategyquant.lib.app.MainApp;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.pluginlib.program.IProgram;
import com.strategyquant.pluginlib.program.Program;
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
public class DebugConsoleAppPlugin implements IAppPlugin, IProgram {
   public static final Logger Log = LoggerFactory.getLogger(DebugConsoleAppPlugin.class);

   public String getName() {
      return "DebugConsole";
   }

   public String getProduct() {
      return "SQUANT";
   }

   public int getPreferredPosition() {
      return 1;
   }

   public void initPlugin() throws Exception {
      if (!MainApp.runInConsole()) {
      }

      Program.register("DebugConsole", this);
   }

   public String getContextPath() {
      return "/debugconsole";
   }

   public String getAppCode() {
      return "DEBUGCONSOLE";
   }

   public String getTooltip() {
      return "DebugConsole";
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

   public Object call(String var1, Object... var2) throws Exception {
      if (!var1.equals("open") && var1.equals("close")) {
      }

      return null;
   }
}
