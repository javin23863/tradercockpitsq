package com.strategyquant.tradinglib.project.bestresults;

import com.strategyquant.pluginlib.SQPluginManager;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.equitychart.EquityChart;
import com.strategyquant.tradinglib.equitychart.EquityChartUtils;
import com.strategyquant.tradinglib.equitychart.MainChartDataset;
import com.strategyquant.tradinglib.equitychart.Periods;
import com.strategyquant.tradinglib.equitychart.SampleMarkers;
import com.strategyquant.tradinglib.equitychartnew.addons.IEquityChartAddon;
import java.util.HashMap;
import java.util.List;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BestResultMiniChart {
   public static final Logger Log = LoggerFactory.getLogger(BestResultMiniChart.class);
   private EquityChart chart = null;
   private Periods periods = null;
   private SampleMarkers sampleMarkers = null;

   public BestResultMiniChart() {
      this.periods = new Periods();
      this.sampleMarkers = new SampleMarkers();
      this.chart = new EquityChart();
      this.chart.setPLType((byte)10);
      this.chart.setDomainAxisType("trade");
      this.chart.hideYTitle(true);
   }

   public JSONObject print(ResultsGroup var1, byte var2) {
      this.chart.reset();
      if (var1 == null) {
         return null;
      }

      Object var3 = null;

      try {
         var3 = var1.orders();
      } catch (Exception var10) {
         Log.error("Exc.", var10);
         return null;
      }

      if (var3 == null) {
         return null;
      }

      try {
         String var4 = var1.getMainResultKey();
         OrdersList var5 = var1.orders().filter(var4, (byte)0, var2, true);
         MainChartDataset var6 = EquityChartUtils.generateDataset(var5, "trade");
         var6.filled = true;
         var6.fillColor = "#DAF5FF";
         var6.fillColorDark = "#333C63";
         var6.fillNegativeColor = "#F49C9C";
         var6.thickness = 1;
         this.chart.addMainChartDataset(var6);

         for (IEquityChartAddon var8 : SQPluginManager.getPlugins(IEquityChartAddon.class)) {
            try {
               if (var8.getName() != null && var8.getName().equals("Drawdown")) {
                  HashMap var9 = new HashMap();
                  var9.put("drawdown", new String[]{"111"});
                  var8.print(this.chart, var1, var4, var5, var9, "trade");
                  break;
               }
            } catch (Exception var11) {
               Log.error("Error while rendering EquityChart addon '" + var8.getName() + "'. Exc.", var11);
            }
         }

         if (var2 == 127 || var2 == 10) {
            List var14 = this.periods.getAvailablePeriods(var5, "trade");
            this.sampleMarkers.print(this.chart, var14, false);
         }

         return this.chart.toJSON();
      } catch (Exception var12) {
         Log.error("Exc.", var12);
         return null;
      }
   }
}
