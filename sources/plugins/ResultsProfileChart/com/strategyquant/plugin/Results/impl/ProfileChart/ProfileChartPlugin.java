package com.strategyquant.plugin.Results.impl.ProfileChart;

import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.results.AbstractResultsPlugin;
import com.strategyquant.tradinglib.results.SpecialValues;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.json.JSONObject;

@Author(name = "SQ")
@Name(name = "ProfileChart Servlet Plugin - SQ")
@Category(name = "Server Extension")
@License(text = "")
@ShortDesc(text = "Server API for Profile Chart results tab")
@PluginImplementation
public class ProfileChartPlugin extends AbstractResultsPlugin {
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
         this.dataContext.setContextPath("/profilechart/");
         this.dataContext.addServlet(new ServletHolder(new ProfileChartServlet(this.rgProvider)), "/*");
      }

      return this.dataContext;
   }

   public boolean containsResult(ResultsGroup var1) throws Exception {
      if (var1 == null) {
         return false;
      }

      String var2 = var1.specialValues().getString(SpecialValues.ProfileChartPaths, "");
      return var2 != null && !var2.isEmpty();
   }

   public String getKey() {
      return "profileChart";
   }

   public JSONObject getInitializationData() {
      return null;
   }
}
