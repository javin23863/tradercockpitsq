package com.strategyquant.plugin.DataSource.impl.Mt5Api;

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

@Author(name = "Petr Somek")
@Name(name = "MT5 API Servlet Plugin - SQ")
@Category(name = "Server Extension")
@License(text = "")
@ShortDesc(text = "Server API for MT5 data management")
@PluginImplementation
public class DataSourceMt5ApiPlugin implements IServletPlugin, IProgram {
   private ServletContextHandler dataContext;
   private DataSourceMt5ApiServlet dataSourceFilesServlet;

   public String getProduct() {
      return "SQUANTQDM";
   }

   public int getPreferredPosition() {
      return 0;
   }

   public void initPlugin() throws Exception {
      this.dataSourceFilesServlet = new DataSourceMt5ApiServlet();
      Program.register("DataSourceMt5Api", this);
   }

   public Object call(String var1, Object... var2) throws Exception {
      return this.dataSourceFilesServlet.execute(var1, var2 == null ? null : (Map)var2[0], null);
   }

   public Handler getHandler() {
      if (this.dataContext == null) {
         this.dataContext = new ServletContextHandler(1);
         this.dataContext.setContextPath("/dataSourceMt5Api/");
         this.dataContext.addServlet(new ServletHolder(new DataSourceMt5ApiServlet()), "/*");
      }

      return this.dataContext;
   }
}
