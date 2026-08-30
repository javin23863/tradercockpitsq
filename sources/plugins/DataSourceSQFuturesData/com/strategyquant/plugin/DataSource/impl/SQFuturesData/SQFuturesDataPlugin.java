package com.strategyquant.plugin.DataSource.impl.SQFuturesData;

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

@Author(name = "Tomas Takacs")
@Name(name = "SQFuturesDataPlugin")
@Category(name = "Server Extension")
@License(text = "")
@ShortDesc(text = "Server API for managing SQ Futures Data")
@PluginImplementation
public class SQFuturesDataPlugin implements IServletPlugin, IProgram {
   private SQFuturesDataServlet servlet;
   private ServletContextHandler dataContext;

   public String getProduct() {
      return "SQUANTQDM";
   }

   public int getPreferredPosition() {
      return 0;
   }

   public void initPlugin() throws Exception {
      this.servlet = new SQFuturesDataServlet();
      Program.register("DataSourceSQFuturesData", this);
   }

   public Handler getHandler() {
      if (this.dataContext == null) {
         this.dataContext = new ServletContextHandler(1);
         this.dataContext.setContextPath("/sqFuturesData/");
         this.dataContext.addServlet(new ServletHolder(this.servlet), "/*");
      }

      return this.dataContext;
   }

   public Object call(String var1, Object... var2) throws Exception {
      return this.servlet.execute(var1, var2 == null ? null : (Map)var2[0], null);
   }
}
