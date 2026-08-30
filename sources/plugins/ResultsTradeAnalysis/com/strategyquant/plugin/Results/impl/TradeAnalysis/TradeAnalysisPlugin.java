package com.strategyquant.plugin.Results.impl.TradeAnalysis;

import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.pluginlib.program.IProgram;
import com.strategyquant.pluginlib.program.Program;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.results.AbstractResultsPlugin;
import java.util.Map;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.json.JSONObject;

@Author(name = "Tamas Takacs")
@Name(name = "Trade Analysis Servlet Plugin - SQ")
@Category(name = "Server Extension")
@License(text = "")
@ShortDesc(text = "Server API for results Trade Analysis tab")
@PluginImplementation
public class TradeAnalysisPlugin extends AbstractResultsPlugin implements IProgram {
   private ServletContextHandler dataContext;
   private TradeAnalysisServlet servlet;

   public String getProduct() {
      return "SQUANTAlgoWizardBACKTESTNODE";
   }

   public int getPreferredPosition() {
      return 0;
   }

   public void initPlugin() throws Exception {
      if (!Program.isRegistered("ResultsTradeAnalysis")) {
         Program.register("ResultsTradeAnalysis", this);
      }
   }

   public Handler getHandler() {
      if (this.dataContext == null) {
         this.servlet = new TradeAnalysisServlet(this.rgProvider);
         this.dataContext = new ServletContextHandler(1);
         this.dataContext.setContextPath("/tradeanalysis/");
         this.dataContext.addServlet(new ServletHolder(this.servlet), "/*");
      }

      return this.dataContext;
   }

   public boolean containsResult(ResultsGroup var1) {
      return true;
   }

   public String getKey() {
      return "tradeAnalysis";
   }

   public JSONObject getInitializationData() throws Exception {
      JSONObject var1 = new JSONObject();
      var1.put("types", this.servlet.getTypes());
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
