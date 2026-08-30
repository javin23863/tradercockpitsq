package com.strategyquant.plugin.Results.impl.PortfolioCorrelation;

import com.strategyquant.lib.L;
import com.strategyquant.plugin.Results.impl.PortfolioCorrelation.correlation.PortfolioCorrelationComputer;
import com.strategyquant.plugin.Results.impl.PortfolioCorrelation.overlappingTrades.OverlappingTradesComputer;
import com.strategyquant.tradinglib.CorrelationType;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.correlation.CorrelationTypes;
import com.strategyquant.tradinglib.results.IResultsGroupProvider;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.util.Map;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PortfolioCorrelationServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(PortfolioCorrelationServlet.class);
   private static final String LOCK_PORTFCORRELSERVLET = "PortfolioCorrelationServlet";
   private PortfolioCorrelationResultsSender sender = null;
   private PortfolioCorrelationComputer correlationComputer = null;
   private OverlappingTradesComputer overlappingTradesComputer = null;
   private static IResultsGroupProvider rgProvider;
   private boolean running = false;

   public PortfolioCorrelationServlet(IResultsGroupProvider var1) {
      this.sender = new PortfolioCorrelationResultsSender();
      this.correlationComputer = new PortfolioCorrelationComputer();
      this.overlappingTradesComputer = new OverlappingTradesComputer();
      rgProvider = var1;
   }

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      switch (var1) {
         case "compute":
            return this.onCompute(var2);
         case "stop":
            return this.onStop(var2);
         case "correlation":
            return this.onCorrelation(var2);
         case "correlationSave":
            return this.onCorrelationSave(var2);
         case "overlapping":
            return this.onOverlapping(var2);
         default:
            throw new Exception("Unknown command '" + var1 + "'.");
      }
   }

   private String onCorrelation(Map<String, String[]> var1) {
      int var2;
      try {
         var2 = Integer.parseInt(((String[])var1.get("posStart"))[0]);
      } catch (Exception var7) {
         var2 = 0;
      }

      int var3;
      try {
         var3 = Integer.parseInt(((String[])var1.get("count"))[0]);
      } catch (Exception var6) {
         var3 = 100;
      }

      String var4 = ((String[])var1.get("symbol1"))[0].toString();
      String var5 = ((String[])var1.get("symbol2"))[0].toString();
      return this.correlationComputer.list(var4, var5, var2, var3);
   }

   private String onCorrelationSave(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      String var3 = ((String[])var1.get("path"))[0];
      this.correlationComputer.saveToCsv(var3);
      var2.put("success", L.t("Correlation matrix saved.", new Object[0]));
      return var2.toString();
   }

   private String onOverlapping(Map<String, String[]> var1) {
      int var2;
      try {
         var2 = Integer.parseInt(((String[])var1.get("posStart"))[0]);
      } catch (Exception var7) {
         var2 = 0;
      }

      int var3;
      try {
         var3 = Integer.parseInt(((String[])var1.get("count"))[0]);
      } catch (Exception var6) {
         var3 = 100;
      }

      String var4 = ((String[])var1.get("symbol1"))[0].toString();
      String var5 = ((String[])var1.get("symbol2"))[0].toString();
      return this.overlappingTradesComputer.list(var4, var5, var2, var3);
   }

   private String onCompute(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"period", "type", "allowNegativeCorrelation", "addEmptyPeriods"});
      if (this.running) {
         throw new Exception(L.t("Computing Portfolio correlation is already running. Cannot start another process.", new Object[0]));
      }

      final int var2 = Integer.parseInt(((String[])var1.get("period"))[0]);
      String var3 = ((String[])var1.get("type"))[0].toString();
      final Boolean var4 = Boolean.parseBoolean(((String[])var1.get("allowNegativeCorrelation"))[0]);
      final Boolean var5 = Boolean.parseBoolean(((String[])var1.get("addEmptyPeriods"))[0]);
      ResultsGroup var6 = rgProvider.get(var1, "PortfolioCorrelationServlet");

      final ResultsGroup var7;
      try {
         var7 = var6.clone();
      } finally {
         var6.releaseLock("PortfolioCorrelationServlet");
      }

      JSONObject var8 = new JSONObject();
      final CorrelationType var9 = (CorrelationType)CorrelationTypes.getInstance().findClassByName(var3);
      (new Thread() {
            @Override
            public void run() {
               JSONObject var1x = new JSONObject();
               PortfolioCorrelationServlet.this.running = true;

               try {
                  long var2x = System.currentTimeMillis();
                  JSONObject var4x = PortfolioCorrelationServlet.this.correlationComputer
                     .compute(var7, var2, var9, var4, var5, PortfolioCorrelationServlet.this.sender);
                  long var5x = System.currentTimeMillis();
                  JSONObject var7x = PortfolioCorrelationServlet.this.overlappingTradesComputer.compute(var7, PortfolioCorrelationServlet.this.sender);
                  var5x = System.currentTimeMillis();
                  var1x.put("correlation", var4x);
                  var1x.put("overlapping", var7x);
               } catch (PortfolioCorrelationInterruptException var12) {
                  var1x.put("error", "Stopped");
                  PortfolioCorrelationServlet.this.correlationComputer.clear();
                  PortfolioCorrelationServlet.this.overlappingTradesComputer.clear();
               } catch (Exception var13) {
                  PortfolioCorrelationServlet.Log.error("Error while computing portfolio correlation.", var13);
                  var1x.put("error", "Error while computing portfolio correlation. " + var13.getMessage());
               } finally {
                  PortfolioCorrelationServlet.this.running = false;
                  var1x.put("progress", 100);
                  PortfolioCorrelationServlet.this.sender.sendData(var1x);
               }
            }
         })
         .start();
      var8.put("success", "ok");
      return var8.toString();
   }

   private String onStop(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      this.correlationComputer.stop();
      this.overlappingTradesComputer.stop();
      System.gc();
      var2.put("success", "ok");
      return var2.toString();
   }
}
