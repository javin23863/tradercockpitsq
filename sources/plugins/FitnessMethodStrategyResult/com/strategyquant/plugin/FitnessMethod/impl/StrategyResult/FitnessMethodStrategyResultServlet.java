package com.strategyquant.plugin.FitnessMethod.impl.StrategyResult;

import com.strategyquant.tradinglib.PrecachedRequest;
import com.strategyquant.tradinglib.fitnessfunction.FitnessOptions;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FitnessMethodStrategyResultServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(FitnessMethodStrategyResultServlet.class);

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      switch (var1) {
         case "list":
            return this.onList();
         default:
            throw new Exception("Unknown command '" + var1 + "'.");
      }
   }

   @PrecachedRequest(relativeURL = "/fitnessMethodStrategyResult/list")
   private String onList() throws Exception {
      return FitnessOptions.list();
   }
}
