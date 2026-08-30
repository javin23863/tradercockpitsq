package com.strategyquant.plugin.Dashboard.impl.Results;

import com.strategyquant.lib.app.MainApp;
import com.strategyquant.tradinglib.project.ProjectEngine;
import com.strategyquant.tradinglib.project.SQProject;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.util.Map;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DashboardResultsServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(DashboardResultsServlet.class);

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      switch (var1) {
         case "print":
            return this.onPrint(var2);
         case "getSettings":
            return this.onGetSettings(var2);
         default:
            throw new Exception("Unknown command '" + var1 + "'.");
      }
   }

   private String onPrint(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      String var3 = ((String[])var1.get("projectName"))[0].toString();
      byte var4 = Byte.parseByte(this.tryGetParam(var1, "sampleType")[0]);
      SQProject var5 = ProjectEngine.get(var3);
      var5.bestResults.setSampleType(var4);
      var5.bestResults.resetLastData();
      var5.publisher.resetLastData("best-results-channel");
      MainApp.settings().set("DRST_" + var3, var4 + "");
      var2.put("success", "ok");
      return var2.toString();
   }

   private String onGetSettings(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      String var3 = ((String[])var1.get("projectName"))[0].toString();
      String var4 = MainApp.settings().get("DRST_" + var3);
      byte var5 = var4 != null ? Byte.parseByte(var4) : 127;
      var2.put("sampleType", var5);
      var2.put("success", "ok");
      return var2.toString();
   }
}
