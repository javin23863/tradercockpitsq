package com.strategyquant.plugin.Results.impl.Chart;

import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.results.AbstractResultsPlugin;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.json.JSONObject;

@Author(name = "Tomas Brynda")
@Name(name = "Chart Servlet Plugin - SQ")
@Category(name = "Server Extension")
@License(text = "")
@ShortDesc(text = "Server API for results overview tab")
@PluginImplementation
public class ChartServletPlugin extends AbstractResultsPlugin {
   private ServletContextHandler dataContext;

   public String getProduct() {
      return "SQUANTAlgoWizardBACKTESTNODE";
   }

   public int getPreferredPosition() {
      return 0;
   }

   public void initPlugin() throws Exception {
   }

   public Handler getHandler() {
      if (this.dataContext == null) {
         this.dataContext = new ServletContextHandler(1);
         this.dataContext.setContextPath("/resultsCharts/");
         this.dataContext.addServlet(new ServletHolder(new ChartServlet(this.rgProvider)), "/*");
      }

      return this.dataContext;
   }

   public boolean containsResult(ResultsGroup var1) throws Exception {
      return var1.specialValues().getBoolean("ContainsChart");
   }

   private String correctResultKey(String var1) {
      return var1.replace("/", "_LOM_");
   }

   public String getKey() {
      return "tradesOnChart";
   }

   public JSONObject getInitializationData() {
      return null;
   }
}
