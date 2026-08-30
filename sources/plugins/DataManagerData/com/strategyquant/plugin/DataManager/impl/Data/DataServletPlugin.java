package com.strategyquant.plugin.DataManager.impl.Data;

import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.pluginlib.program.IProgram;
import com.strategyquant.pluginlib.program.Program;
import com.strategyquant.tradinglib.servlet.IServletPlugin;
import java.util.Map;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

@Author(name = "Tomas Brynda")
@Name(name = "Data Servlet Plugin - SQ")
@Category(name = "Server Extension")
@License(text = "")
@ShortDesc(text = "Server API for data management")
@PluginImplementation
public class DataServletPlugin implements IServletPlugin, IProgram {
   private DataServlet dataServlet;
   private ServletContextHandler dataContext;

   public String getProduct() {
      return "SQUANTSQMANAGERQDM";
   }

   public int getPreferredPosition() {
      return 0;
   }

   public void initPlugin() throws Exception {
      this.dataServlet = new DataServlet();
      Program.register("DataManagerData", this);
   }

   public Handler getHandler() {
      if (this.dataContext == null) {
         this.dataContext = new ServletContextHandler(1);
         this.dataContext.setContextPath("/data/");
         this.dataContext.addServlet(new ServletHolder(this.dataServlet), "/*");
      }

      return this.dataContext;
   }

   public Object call(String var1, Object... var2) throws Exception {
      return this.dataServlet.execute(var1, var2 == null ? null : (Map)var2[0], null);
   }
}
