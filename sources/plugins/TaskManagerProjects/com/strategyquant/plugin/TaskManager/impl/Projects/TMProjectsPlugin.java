package com.strategyquant.plugin.TaskManager.impl.Projects;

import com.strategyquant.pluginlib.ISQPlugin;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Author(name = "Tamas Takacs")
@Name(name = "Task Manager projects plugin")
@Category(name = "TaskManager")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class TMProjectsPlugin implements ISQPlugin, IServletPlugin, IProgram {
   public static final Logger Log = LoggerFactory.getLogger(TMProjectsPlugin.class);
   private ServletContextHandler dataContext;
   private TMProjectsServlet servlet;

   public Handler getHandler() {
      if (this.dataContext == null) {
         this.dataContext = new ServletContextHandler(1);
         this.dataContext.setContextPath("/taskmanager/");
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
      this.servlet = new TMProjectsServlet();
      Program.register("TaskManager", this);
   }

   public Object call(String var1, Object... var2) throws Exception {
      return this.servlet.execute(var1, var2 == null ? null : (Map)var2[0], null);
   }
}
