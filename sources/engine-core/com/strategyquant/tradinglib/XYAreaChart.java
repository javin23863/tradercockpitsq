package com.strategyquant.tradinglib;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.charts.AbstractXYChart;
import com.strategyquant.tradinglib.charts.linechart.series.XYSeries;
import com.strategyquant.tradinglib.charts.linechart.series.XYValue;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;

public class XYAreaChart extends AbstractXYChart {
   private ArrayList<XYSeries> seriesCollection = new ArrayList<>();

   public XYAreaChart() {
      super("line");
   }

   public void addSeries(XYSeries var1) {
      this.seriesCollection.add(var1);
   }

   public XYSeries getSeries(int var1) {
      return this.seriesCollection.get(var1);
   }

   public ArrayList<XYSeries> getSeriesCollection() {
      return this.seriesCollection;
   }

   @Override
   public JSONObject toJSON() {
      JSONObject var1 = new JSONObject();
      JSONObject var2 = new JSONObject();
      JSONArray var3 = new JSONArray();
      JSONArray var4 = new JSONArray();

      for (int var5 = 0; var5 < this.seriesCollection.size(); var5++) {
         XYSeries var6 = this.seriesCollection.get(var5);
         JSONObject var7 = new JSONObject();
         JSONArray var8 = new JSONArray();

         for (int var9 = 0; var9 < var6.values.size(); var9++) {
            XYValue var10 = var6.values.get(var9);
            JSONObject var11 = new JSONObject().put("x", var10.x).put("y", SQUtils.d2(var10.y));
            var8.put(var11);
         }

         if (var6.color != null) {
            var7.put("borderColor", var6.color);
            var7.put("backgroundColor", var6.color);
         }

         var4.put(var6.name);
         var7.put("label", var6.name);
         var7.put("data", var8);
         var7.put("fill", "start");
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
