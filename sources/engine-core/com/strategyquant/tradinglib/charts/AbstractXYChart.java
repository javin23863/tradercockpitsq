package com.strategyquant.tradinglib.charts;

import com.strategyquant.tradinglib.AbstractChart;
import org.json.JSONArray;
import org.json.JSONObject;

public abstract class AbstractXYChart extends AbstractChart {
   public String xAxisTitle = null;
   public String xAxisType = null;
   public String xAxisTickformat = null;
   public double xAxisRangeMin = -1.0;
   public double xAxisRangeMax = -1.0;
   public String yAxisTitle = null;
   public double yAxisRangeMin = -1.0;
   public double yAxisRangeMax = -1.0;
   public int maxXTicksLimit = -1;
   public int maxYTicksLimit = -1;
   public boolean invertIfNegative = false;
   public int fontSize = 11;
   public boolean tooltips = true;
   public boolean displayXAxesLabels = true;
   public boolean displayYAxesLabels = true;
   public boolean disableEvents = false;
   public int pointRadius = 0;

   public AbstractXYChart(String var1) {
      super(var1);
   }

   @Override
   public JSONObject getOptions() {
      JSONObject var1 = super.getOptions();
      var1.put("elements", new JSONObject().put("point", new JSONObject().put("radius", this.pointRadius)));
      JSONObject var2 = new JSONObject();
      var1.put("scales", var2);
      var2.put(
         "xAxes",
         new JSONArray()
            .put(
               new JSONObject()
                  .put("display", this.displayXAxesLabels)
                  .put(
                     "scaleLabel", new JSONObject().put("display", this.xAxisTitle != null).put("labelString", this.xAxisTitle).put("fontSize", this.fontSize)
                  )
                  .put("gridLines", new JSONObject().put("display", false))
            )
      );
      var2.put(
         "yAxes",
         new JSONArray()
            .put(
               new JSONObject()
                  .put("display", this.displayYAxesLabels)
                  .put(
                     "scaleLabel", new JSONObject().put("display", this.yAxisTitle != null).put("labelString", this.yAxisTitle).put("fontSize", this.fontSize)
                  )
                  .put("gridLines", new JSONObject().put("display", false))
            )
      );
      var2.getJSONArray("xAxes").getJSONObject(0).put("ticks", new JSONObject().put("autoSkip", true).put("fontSize", this.fontSize));
      var2.getJSONArray("yAxes").getJSONObject(0).put("ticks", new JSONObject().put("fontSize", this.fontSize));
      if (this.xAxisRangeMin != -1.0 && this.xAxisRangeMax != -1.0) {
         var2.getJSONArray("xAxes").getJSONObject(0).getJSONObject("ticks").put("min", this.xAxisRangeMin).put("max", this.xAxisRangeMax);
      }

      if (this.yAxisRangeMin != -1.0) {
         var2.getJSONArray("yAxes").getJSONObject(0).getJSONObject("ticks").put("min", this.yAxisRangeMin);
      }

      if (this.yAxisRangeMax != -1.0) {
         var2.getJSONArray("yAxes").getJSONObject(0).getJSONObject("ticks").put("max", this.yAxisRangeMax);
      }

      if (this.type.equals("bar")) {
         var2.getJSONArray("yAxes").getJSONObject(0).getJSONObject("ticks").put("beginAtZero", true);
      }

      if (this.maxXTicksLimit != -1) {
         var2.getJSONArray("xAxes").getJSONObject(0).getJSONObject("ticks").put("maxTicksLimit", this.maxXTicksLimit);
      }

      if (this.maxYTicksLimit != -1) {
         var2.getJSONArray("yAxes").getJSONObject(0).getJSONObject("ticks").put("maxTicksLimit", this.maxYTicksLimit);
      }

      if (this.xAxisType != null) {
         var2.getJSONArray("xAxes").getJSONObject(0).put("type", this.xAxisType);
         if (this.xAxisType != null && this.xAxisType.equals("time")) {
            var2.getJSONArray("xAxes")
               .getJSONObject(0)
               .put("time", new JSONObject().put("displayFormats", new JSONObject().put("second", this.xAxisTickformat)));
            var2.getJSONArray("xAxes").getJSONObject(0).getJSONObject("ticks").put("maxRotation", 0);
         }
      }

      if (this.type.equals("line")) {
         var2.getJSONArray("xAxes").getJSONObject(0).put("bounds", "data");
      }

      if (!this.tooltips) {
         var1.put("tooltips", new JSONObject().put("enabled", false));
      }

      if (this.disableEvents) {
         var1.put("events", new JSONArray());
      }

      return var1;
   }

   public void invertIfNegative(boolean var1) {
      this.invertIfNegative = true;
   }
}
