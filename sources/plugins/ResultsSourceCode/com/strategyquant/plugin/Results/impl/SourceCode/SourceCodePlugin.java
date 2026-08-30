package com.strategyquant.plugin.Results.impl.SourceCode;

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

@Author(name = "Tamas Takacs")
@Name(name = "SourceCode Plugin - SQ")
@Category(name = "Server Extension")
@License(text = "")
@ShortDesc(text = "SourceCode tab")
@PluginImplementation
public class SourceCodePlugin extends AbstractResultsPlugin {
   private ServletContextHandler dataContext;
   private SourceCodeServlet servlet;

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
         this.servlet = new SourceCodeServlet(this.rgProvider);
         this.dataContext = new ServletContextHandler(1);
         this.dataContext.setContextPath("/sourcecode/");
         this.dataContext.addServlet(new ServletHolder(this.servlet), "/*");
      }

      return this.dataContext;
   }

   public boolean containsResult(ResultsGroup var1) {
      return true;
   }

   public String getKey() {
      return "sourceCode";
   }

   public JSONObject getInitializationData() throws Exception {
      JSONObject var1 = new JSONObject();
      var1.put("engineTypes", this.servlet.onList());
      var1.put("mmTypes", this.servlet.onListMM());
      return var1;
   }
}
