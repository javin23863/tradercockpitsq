package com.strategyquant.plugin.Databank.impl.FilterByCorrelation;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Author(name = "Tomas Brynda")
@Name(name = "Filter by correlation")
@Category(name = "DatabankActions")
@License(text = "")
@ShortDesc(text = "")
@PluginImplementation
public class DatabankFilterByCorrelationPlugin implements IServletPlugin {
   private static final Logger Log = LoggerFactory.getLogger(DatabankFilterByCorrelationPlugin.class);
   private ServletContextHandler connectionContext;

   public String getProduct() {
      return "SQUANT";
   }

   public int getPreferredPosition() {
      return 0;
   }

   public void initPlugin() throws Exception {
   }

   public Handler getHandler() {
      if (this.connectionContext == null) {
         this.connectionContext = new ServletContextHandler(1);
         this.connectionContext.setContextPath("/filterByCorrelation/");
         this.connectionContext.addServlet(new ServletHolder(new DatabankFilterByCorrelationServlet()), "/*");
      }

      return this.connectionContext;
   }
}
