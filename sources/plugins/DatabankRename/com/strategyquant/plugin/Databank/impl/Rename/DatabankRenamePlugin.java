package com.strategyquant.plugin.Databank.impl.Rename;

import com.strategyquant.pluginlib.ISQPlugin;
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

@Author(name = "Tamas Takacs")
@Name(name = "Databank rename")
@Category(name = "DatabankActions")
@License(text = "")
@ShortDesc(text = "Renames selected strategies in databank")
@PluginImplementation
public class DatabankRenamePlugin implements ISQPlugin, IServletPlugin {
   public static final Logger Log = LoggerFactory.getLogger(DatabankRenamePlugin.class);
   private ServletContextHandler dataContext;
   private DatabankRenameServlet servlet;

   public Handler getHandler() {
      if (this.dataContext == null) {
         this.dataContext = new ServletContextHandler(1);
         this.dataContext.setContextPath("/databank/");
         this.dataContext.addServlet(new ServletHolder(this.servlet), "/*");
      }

      return this.dataContext;
   }

   public String getProduct() {
      return "SQUANT";
   }

   public int getPreferredPosition() {
      return 0;
   }

   public void initPlugin() throws Exception {
      this.servlet = new DatabankRenameServlet();
   }
}
