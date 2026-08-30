package com.strategyquant.plugin.Settings.impl.AdvancedTM;

import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdvancedTMServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(AdvancedTMServlet.class);

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      String var4 = var1;
      byte var5 = -1;
      var4.hashCode();
      switch (var5) {
         default:
            throw new Exception("Unknown command '" + var1 + "'.");
      }
   }
}
