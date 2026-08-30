package com.strategyquant.tradinglib;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.charts.AbstractXYChart;
import com.strategyquant.tradinglib.charts.linechart.series.XYValue;
import com.strategyquant.tradinglib.equitychart.ChartOptimalizer;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimeSeriesLineChart extends AbstractXYChart {
   public static final Logger Log = LoggerFactory.getLogger(TimeSeriesLineChart.class);
   private ArrayList<TimeSeries> seriesCollection = new ArrayList<>();
   public static final int PointsCount = 1000;
   public static final int OptimPointsCount = 20;

   public TimeSeriesLineChart() {
      super("line");
      this.xAxisType = "time";
      this.xAxisTickformat = "HH:mm";
   }

   public void addSeries(TimeSeries var1) {
      this.seriesCollection.add(var1);
   }

   public TimeSeries getSeries(int var1) {
      return this.seriesCollection.get(var1);
   }

   public ArrayList<TimeSeries> getSeriesCollection() {
      return this.seriesCollection;
   }

   @Override
   public JSONObject toJSON() {
      JSONObject var1 = new JSONObject();
      JSONObject var2 = new JSONObject();
      JSONArray var3 = new JSONArray();
      JSONArray var4 = new JSONArray();

      for (int var5 = 0; var5 < this.seriesCollection.size(); var5++) {
         TimeSeries var6 = this.seriesCollection.get(var5);
         JSONObject var7 = new JSONObject();
         JSONArray var8 = new JSONArray();
         int var9 = var6.values.size();
         if (var9 <= 20) {
            for (int var14 = 0; var14 < var9; var14++) {
               XYValue var15 = var6.values.get(var14);
               var8.put(new JSONObject().put("x", var15.x).put("y", SQUtils.d2(var15.y)));
            }
         } else {
            long[] var10 = new long[var9];
            double[] var11 = new double[var9];

            for (int var12 = 0; var12 < var9; var12++) {
               var10[var12] = var6.values.get(var12).x;
               var11[var12] = var6.values.get(var12).y;
            }

            ChartOptimalizer.OptimizerResult var16 = new ChartOptimalizer().optimalize(false, var10, var11, null, null, 20);

            for (int var13 = 0; var13 < var16.size(); var13++) {
               if (var16.xVals[var13] != 0L) {
                  var8.put(new JSONObject().put("x", var16.xVals[var13]).put("y", SQUtils.d2(var16.yVals[var13])));
               }
            }
         }

         if (var6.color != null) {
            var7.put("borderColor", var6.color);
         }

         var4.put(var6.name);
         var7.put("borderWidth", 0.5);
         var7.put("label", var6.name);
         var7.put("data", var8);
         var7.put("fill", false);
         var7.put("lineTension", 0);
         var3.put(var7);
      }

      var2.put("datasets", var3);
      var1.put("data", var2);
      var1.put("options", this.getOptions());
      var1.put("type", this.type);
      return var1;
   }
}
