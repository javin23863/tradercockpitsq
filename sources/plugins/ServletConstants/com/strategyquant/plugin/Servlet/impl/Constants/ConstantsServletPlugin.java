package com.strategyquant.plugin.Servlet.impl.Constants;

import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.servlet.IServletPlugin;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

@Author(name = "Tomas Brynda")
@Name(name = "Constants Servlet Plugin")
@Category(name = "Server Extension")
@License(text = "")
@ShortDesc(text = "Server API for constants listing")
@PluginImplementation
public class ConstantsServletPlugin implements IServletPlugin {
   private ServletContextHandler connectionContext;

   public String getProduct() {
      return "SQMANAGERSQUANTSQTRADERTESTAPPSQEDITORAlgoWizardBACKTESTNODEAlgoWizardStandaloneQDM";
   }

   public int getPreferredPosition() {
      return 0;
   }

   public void initPlugin() throws Exception {
   }

   public Handler getHandler() {
      if (this.connectionContext == null) {
         this.connectionContext = new ServletContextHandler(1);
         this.connectionContext.setContextPath("/constants/");
         this.connectionContext.addServlet(new ServletHolder(new ConstantsServlet()), "/*");
      }

      return this.connectionContext;
   }
}
