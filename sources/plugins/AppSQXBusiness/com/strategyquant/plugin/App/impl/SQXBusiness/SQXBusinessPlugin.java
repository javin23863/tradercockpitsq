package com.strategyquant.plugin.App.impl.SQXBusiness;

import com.strategyquant.lib.sqxbusiness.SQXBusinessMainSettings;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.plugindef.app.IAppPlugin;
import com.strategyquant.tradinglib.servlet.IServletPlugin;
import jakarta.servlet.MultipartConfigElement;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Author(name = "Tomas Brynda")
@Name(name = "SQX Business")
@Category(name = "App")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class SQXBusinessPlugin implements IAppPlugin, IServletPlugin {
   public static final Logger Log = LoggerFactory.getLogger(SQXBusinessPlugin.class);
   private ServletContextHandler dataContext;

   public String getName() {
      return "SQX Business";
   }

   public String getProduct() {
      return "SQUANT";
   }

   public int getPreferredPosition() {
      return 10;
   }

   public void initPlugin() throws Exception {
      MQLMarketBuildExecutor.init();
      SQXBusinessMainSettings.init();
   }

   public String getContextPath() {
      return "/sqxbusiness";
   }

   public String getAppCode() {
      return "SQXBUSINESS";
   }

   public String getTooltip() {
      return "SQX Business";
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

   public Handler getHandler() {
      if (this.dataContext == null) {
         this.dataContext = new ServletContextHandler(1);
         this.dataContext.setContextPath("/sqxbusiness/");
         ServletHolder var1 = new ServletHolder(new SQXBusinessServlet());
         var1.getRegistration().setMultipartConfig(new MultipartConfigElement("./tmp"));
         this.dataContext.addServlet(var1, "/*");
      }

      return this.dataContext;
   }
}
