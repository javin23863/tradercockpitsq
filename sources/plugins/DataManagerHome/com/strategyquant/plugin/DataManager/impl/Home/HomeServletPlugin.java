package com.strategyquant.plugin.DataManager.impl.Home;

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
@Name(name = "DataManager Home tab")
@Category(name = "Server Extension")
@License(text = "")
@ShortDesc(text = "Server API for DataManager Home tab")
@PluginImplementation
public class HomeServletPlugin implements IServletPlugin, IProgram {
   private HomeServlet homeServlet;
   private ServletContextHandler dataContext;

   public String getProduct() {
      return "SQMANAGERQDM";
   }

   public int getPreferredPosition() {
      return 0;
   }

   public void initPlugin() throws Exception {
      this.homeServlet = new HomeServlet();
      Program.register("HomeServlet", this);
   }

   public Handler getHandler() {
      if (this.dataContext == null) {
         this.dataContext = new ServletContextHandler(1);
         this.dataContext.setContextPath("/home/");
         this.dataContext.addServlet(new ServletHolder(this.homeServlet), "/*");
      }

      return this.dataContext;
   }

   public Object call(String var1, Object... var2) throws Exception {
      return this.homeServlet.execute(var1, var2 == null ? null : (Map)var2[0], null);
   }
}
