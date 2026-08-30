package com.strategyquant.plugin.Results.impl.TradelistViews;

import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.servlet.IServletPlugin;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

@Author(name = "Tomas Brynda")
@Name(name = "Tradelist views Servlet Plugin - SQ")
@Category(name = "Server Extension")
@License(text = "")
@ShortDesc(text = "Server API for tradelist views management")
@PluginImplementation
public class TradelistViewsPlugin implements IServletPlugin {
   private ServletContextHandler dataContext;

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
         this.dataContext = new ServletContextHandler(1);
         this.dataContext.setContextPath("/tradelistviews/");
         this.dataContext.addServlet(new ServletHolder(new TradelistViewsServlet()), "/*");
      }

      return this.dataContext;
   }
}
