package com.strategyquant.tradinglib.equitychart;

import com.strategyquant.lib.utils.JsonCreator;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainChartDataset extends ChartDataset {
   public String title = "";
   public boolean filled = false;
   public String fillColor = null;
   public String fillColorDark = null;
   public String fillNegativeColor = null;

   public MainChartDataset(String var1, int var2) {
      super(var1, var2);
   }

   @Override
   public JSONObject toJSON(Zoom var1, boolean var2) {
      JSONObject var3 = new JSONObject();
      JSONArray var4 = new JSONArray();
      JSONArray var5 = new JSONArray();
      ChartOptimalizer.OptimizerResult var6 = new ChartOptimalizer()
         .optimalize(var2, this.xVals, this.yVals, var1.getMin(this.xVals), var1.getMax(this.xVals), var1.width);

      for (int var7 = 0; var7 < var6.size(); var7++) {
         var4.put(var2 ? this.formatTime(var6.xVals[var7]) : var6.xVals[var7]);
         var5.put(this.d(var6.yVals[var7]));
      }

      var3.put("legend", this.name);
      var3.put("title", this.title);
      var3.put("type", this.type);
      var3.put("color", this.color);
      var3.put("thickness", this.thickness);
      var3.put("xVals", var4);
      var3.put("yVals", var5);
      if (this.filled) {
         var3.put("filled", true);
      }

      if (this.fillColor != null) {
         var3.put("fillColor", this.fillColor);
      }

      return var3;
   }

   @Override
   public void fillJSON(JsonCreator var1, Zoom var2, boolean var3) {
      var1.put("legend", this.name, true);
      var1.put("title", this.title, true);
      var1.put("type", this.type, true);
      var1.put("color", this.color, true);
      if (this.colorDark != null) {
         var1.put("color_dark", this.colorDark, true);
      }

      var1.put("thickness", this.thickness, true);
      if (this.fillColorDark != null) {
         var1.put("fillColor_dark", this.fillColorDark, true);
      }

      ChartOptimalizer.OptimizerResult var4 = new ChartOptimalizer()
         .optimalize(var3, this.xVals, this.yVals, var2.getMin(this.xVals), var2.getMax(this.xVals), var2.width);
      var1.putBeginArray("xVals");

      for (int var5 = 0; var5 < var4.size(); var5++) {
         var1.setValue(var3 ? this.formatTime(var4.xVals[var5]) : var4.xVals[var5], var5 < var4.size() - 1);
      }

      var1.endArray(true);
      var1.putBeginArray("yVals");

      for (int var6 = 0; var6 < var4.size(); var6++) {
         var1.setValue(this.d(var4.yVals[var6]), var6 < var4.size() - 1);
      }

      var1.endArray(this.filled || this.fillColor != null);
      if (this.filled) {
         var1.put("filled", true, this.fillColor != null || this.fillNegativeColor != null);
      }

      if (this.fillColor != null) {
         var1.put("fillColor", this.fillColor, this.fillNegativeColor != null);
      }

      if (this.fillNegativeColor != null) {
         var1.put("fillNegativeColor", this.fillNegativeColor, false);
      }
   }
}
