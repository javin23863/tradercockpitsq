package com.strategyquant.plugin.Servlet.impl.GridControl;

import com.strategyquant.gridlib.client.SQGrid;
import com.strategyquant.lib.L;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.util.Map;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GridControlServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(GridControlServlet.class);

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      switch (var1) {
         case "getData":
            return this.onGetData(var2);
         default:
            throw new Exception("Unknown command '" + var1 + "'.");
      }
   }

   private String onGetData(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      var2.put("gridData", new JSONObject(SQGrid.getGridClient().getGridDescriptions()));
      var2.put("success", L.t("Databanks listed.", new Object[0]));
      return var2.toString();
   }
}
