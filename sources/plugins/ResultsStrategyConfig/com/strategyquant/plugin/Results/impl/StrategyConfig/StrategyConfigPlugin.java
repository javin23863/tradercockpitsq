package com.strategyquant.plugin.Results.impl.StrategyConfig;

import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.results.AbstractResultsPlugin;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.json.JSONObject;

@Author(name = "Tamas Takacs")
@Name(name = "Strategy config Servlet Plugin - SQ")
@Category(name = "Server Extension")
@License(text = "")
@ShortDesc(text = "Server API for results Strategy config tab")
@PluginImplementation
public class StrategyConfigPlugin extends AbstractResultsPlugin {
   private ServletContextHandler dataContext;
   private StrategyConfigServlet servlet;

   public String getProduct() {
      return "SQUANTAlgoWizardBACKTESTNODE";
   }

   public int getPreferredPosition() {
      return 0;
   }

   public void initPlugin() throws Exception {
   }

   public Handler getHandler() {
      if (this.dataContext == null) {
         this.servlet = new StrategyConfigServlet(this.rgProvider);
         this.dataContext = new ServletContextHandler(1);
         this.dataContext.setContextPath("/strategyconfig/");
         this.dataContext.addServlet(new ServletHolder(this.servlet), "/*");
      }

      return this.dataContext;
   }

   public boolean containsResult(ResultsGroup var1) {
      return true;
   }

   public String getKey() {
      return "strategyConfig";
   }

   public JSONObject getInitializationData() throws Exception {
      return null;
   }
}
