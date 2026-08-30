package com.strategyquant.tradinglib;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.charts.linechart.series.XYValue;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;

public class TimeSeriesScatterChart extends AbstractChart {
   private ArrayList<TimeSeries> seriesCollection = new ArrayList<>();

   public TimeSeriesScatterChart() {
      super("scatter");
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

      for (int var4 = 0; var4 < this.seriesCollection.size(); var4++) {
         TimeSeries var5 = this.seriesCollection.get(var4);
         JSONObject var6 = new JSONObject();
         JSONArray var7 = new JSONArray();

         for (int var8 = 0; var8 < var5.values.size(); var8++) {
            XYValue var9 = var5.values.get(var8);
            JSONObject var10 = new JSONObject().put("x", var9.x).put("y", SQUtils.d2(var9.y));
            var7.put(var10);
         }

         if (var5.color != null) {
            var6.put("borderColor", var5.color);
            var6.put("backgroundColor", var5.color);
         }

         var6.put("borderWidth", 0.5);
         var6.put("label", var5.name);
         var6.put("data", var7);
         var6.put("fill", "start");
         var6.put("lineTension", 0);
         var3.put(var6);
      }

      var2.put("datasets", var3);
      var1.put("data", var2);
      var1.put("options", this.getOptions());
      var1.put("type", this.type);
      return var1;
   }
}
