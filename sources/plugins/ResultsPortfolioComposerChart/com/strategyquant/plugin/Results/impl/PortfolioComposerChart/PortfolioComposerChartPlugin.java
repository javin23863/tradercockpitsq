package com.strategyquant.plugin.Results.impl.PortfolioComposerChart;

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

@Author(name = "Tamas Takacs")
@Name(name = "PortfolioComposerChart Plugin - SQ")
@Category(name = "Server Extension")
@License(text = "")
@ShortDesc(text = "PortfolioComposer chart tab")
@PluginImplementation
public class PortfolioComposerChartPlugin extends AbstractResultsPlugin {
   private ServletContextHandler dataContext;
   private PortfolioComposerChartServlet servlet;

   public String getProduct() {
      return "SQUANTAlgoWizardBACKTESTNODEAlgoWizardStandalone";
   }

   public int getPreferredPosition() {
      return 0;
   }

   public void initPlugin() throws Exception {
   }

   public Handler getHandler() {
      if (this.dataContext == null) {
         this.servlet = new PortfolioComposerChartServlet(this.rgProvider);
         this.dataContext = new ServletContextHandler(1);
         this.dataContext.setContextPath("/portfoliocomposerchart/");
         this.dataContext.addServlet(new ServletHolder(this.servlet), "/*");
      }

      return this.dataContext;
   }

   public boolean containsResult(ResultsGroup var1) {
      return var1.specialValues().containsKey(SpecialValues.PortfolioComposerChart);
   }

   public String getKey() {
      return "portfolioComposerChart";
   }

   public JSONObject getInitializationData() throws Exception {
      return null;
   }
}
