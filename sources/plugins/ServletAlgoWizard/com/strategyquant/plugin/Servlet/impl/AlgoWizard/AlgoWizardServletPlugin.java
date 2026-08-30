package com.strategyquant.plugin.Servlet.impl.AlgoWizard;

import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.results.AbstractBacktestPlugin;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

@Author(name = "Tomas Brynda")
@Name(name = "AlgoWizard Servlet Plugin")
@Category(name = "Server Extension")
@License(text = "")
@ShortDesc(text = "AlgoWizard plugin")
@PluginImplementation
public class AlgoWizardServletPlugin extends AbstractBacktestPlugin {
   private ServletContextHandler dataContext;

   public String getProduct() {
      return "SQUANTSQEDITORAlgoWizardBACKTESTNODEAlgoWizardStandalone";
   }

   public int getPreferredPosition() {
      return 0;
   }

   public void initPlugin() throws Exception {
   }

   public Handler getHandler() {
      if (this.dataContext == null) {
         this.dataContext = new ServletContextHandler(1);
         this.dataContext.setContextPath("/algowizard/");
         this.dataContext.addServlet(new ServletHolder(new AlgoWizardServlet(this.backtester, this.rgProvider)), "/*");
      }

      return this.dataContext;
   }
}
