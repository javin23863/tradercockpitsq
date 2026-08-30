package com.strategyquant.tradinglib;

import org.json.JSONObject;

public abstract class AbstractChart {
   public String type = null;
   public String title = null;
   public boolean showLegend = false;
   public boolean xAxisTicksDurationInMS = false;
   public boolean yAxisTicksMemory = false;
   public boolean categoryAxis = false;

   public AbstractChart(String var1) {
      this.type = var1;
   }

   public abstract JSONObject toJSON();

   public JSONObject getOptions() {
      JSONObject var1 = new JSONObject();
      var1.put("xAxisTicksDurationInMS", this.xAxisTicksDurationInMS);
      var1.put("yAxisTicksMemory", this.yAxisTicksMemory);
      var1.put("animation", false);
      var1.put("title", new JSONObject().put("display", this.title != null).put("text", this.title));
      var1.put(
         "legend",
         new JSONObject().put("display", this.showLegend).put("position", "bottom").put("labels", new JSONObject().put("fontSize", 10).put("boxWidth", 15))
      );
      var1.put("responsive", true);
      var1.put("maintainAspectRatio", false);
      return var1;
   }
}
