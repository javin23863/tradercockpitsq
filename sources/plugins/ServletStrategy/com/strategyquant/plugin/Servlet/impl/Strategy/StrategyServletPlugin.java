package com.strategyquant.plugin.Servlet.impl.Strategy;

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
@Name(name = "Strategy Servlet Plugin")
@Category(name = "Server Extension")
@License(text = "")
@ShortDesc(text = "Server API for strategies management")
@PluginImplementation
public class StrategyServletPlugin implements IServletPlugin {
   private ServletContextHandler strategyContext;

   public String getProduct() {
      return "SQTRADER";
   }

   public int getPreferredPosition() {
      return 0;
   }

   public void initPlugin() throws Exception {
   }

   public Handler getHandler() {
      if (this.strategyContext == null) {
         this.strategyContext = new ServletContextHandler(1);
         this.strategyContext.setContextPath("/strategies/");
         this.strategyContext.addServlet(new ServletHolder(new StrategyServlet()), "/*");
      }

      return this.strategyContext;
   }
}
