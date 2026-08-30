package com.strategyquant.plugin.Servlet.impl.Project;

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
@Name(name = "Project Servlet Plugin - SQ")
@Category(name = "Server Extension")
@License(text = "")
@ShortDesc(text = "Server API for projects management")
@PluginImplementation
public class ProjectServletPlugin implements IServletPlugin, IProgram {
   private ServletContextHandler dataContext;
   private ProjectServlet servlet;

   public String getProduct() {
      return "SQUANTAlgoWizard";
   }

   public int getPreferredPosition() {
      return 0;
   }

   public void initPlugin() throws Exception {
      this.servlet = new ProjectServlet();
      Program.register("Project", this);
   }

   public Handler getHandler() {
      if (this.dataContext == null) {
         this.dataContext = new ServletContextHandler(1);
         this.dataContext.setContextPath("/project/");
         this.dataContext.addServlet(new ServletHolder(this.servlet), "/*");
      }

      return this.dataContext;
   }

   public Object call(String var1, Object... var2) throws Exception {
      return this.servlet.execute(var1, var2 == null ? null : (Map)var2[0], null);
   }
}
