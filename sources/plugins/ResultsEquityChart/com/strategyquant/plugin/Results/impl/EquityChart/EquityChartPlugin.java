package com.strategyquant.plugin.Results.impl.EquityChart;

import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.pluginlib.program.IProgram;
import com.strategyquant.pluginlib.program.Program;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.results.AbstractResultsPlugin;
import jakarta.servlet.DispatcherType;
import java.util.EnumSet;
import java.util.Map;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.json.JSONObject;

@Author(name = "Tamas Takacs")
@Name(name = "EquityChart Servlet Plugin - SQ")
@Category(name = "Server Extension")
@License(text = "")
@ShortDesc(text = "Server API for results equity chart tab")
@PluginImplementation
public class EquityChartPlugin extends AbstractResultsPlugin implements IProgram {
   private ServletContextHandler dataContext;
   private EquityChartServlet servlet;

   public String getProduct() {
      return "SQUANTAlgoWizardBACKTESTNODE";
   }

   public int getPreferredPosition() {
      return 0;
   }

   public void initPlugin() throws Exception {
      if (!Program.isRegistered("ResultsEquityChart")) {
         Program.register("ResultsEquityChart", this);
      }
   }

   public Handler getHandler() {
      if (this.dataContext == null) {
         this.servlet = new EquityChartServlet(this.rgProvider);
         this.dataContext = new ServletContextHandler(1);
         this.dataContext.setContextPath("/equitychart/");
         this.addCrossOrigin(this.dataContext);
         this.dataContext.addServlet(new ServletHolder(this.servlet), "/*");
      }

      return this.dataContext;
   }

   private void addCrossOrigin(ServletContextHandler var1) {
      FilterHolder var2 = var1.addFilter(CrossOriginFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
      var2.setInitParameter("allowedOrigins", "*");
      var2.setInitParameter("Access-Control-Allow-Origin", "*");
      var2.setInitParameter("allowedMethods", "GET,POST,HEAD");
      var2.setInitParameter("allowedHeaders", "X-Requested-With,Content-Type,Accept,Origin");
   }

   public boolean containsResult(ResultsGroup var1) {
      return true;
   }

   public String getKey() {
      return "equityChart";
   }

   public JSONObject getInitializationData() throws Exception {
      JSONObject var1 = new JSONObject();
      var1.put("lastSettings", this.servlet.loadLastSettings());
      return var1;
   }

   public Object call(String var1, Object... var2) throws Exception {
      if (var1.equals("printResultsGroup")) {
         this.servlet.rg = (ResultsGroup)var2[0];
         return this.servlet.execute("print", (Map<String, String[]>)var2[1], null);
      } else {
         return this.servlet.execute(var1, var2 == null ? null : (Map)var2[0], null);
      }
   }
}
