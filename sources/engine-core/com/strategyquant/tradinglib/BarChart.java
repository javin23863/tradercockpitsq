package com.strategyquant.tradinglib;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.charts.AbstractXYChart;
import java.util.ArrayList;
import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONObject;

public class BarChart extends AbstractXYChart {
   protected ArrayList<BarChart.CategoryValue> values = new ArrayList<>();
   protected ArrayList<String> categories = new ArrayList<>();
   protected HashMap<String, String> categoryColors = new HashMap<>();
   protected HashMap<Comparable, String> individualColors = new HashMap<>();

   public BarChart() {
      super("bar");
   }

   public void addValue(String var1, Comparable var2, Number var3) {
      BarChart.CategoryValue var4 = new BarChart.CategoryValue(var1, var2, var3);
      this.values.add(var4);
      if (!this.categories.contains(var1)) {
         this.categories.add(var1);
      }
   }

   public void addValue(String var1, Comparable var2, Number var3, String var4) {
      BarChart.CategoryValue var5 = new BarChart.CategoryValue(var1, var2, var3);
      this.values.add(var5);
      if (!this.categories.contains(var1)) {
         this.categories.add(var1);
      }

      this.individualColors.put(var2, var4);
   }

   public void setCategoryColor(String var1, String var2) {
      this.categoryColors.put(var1, var2);
   }

   @Override
   public JSONObject toJSON() {
      JSONObject var1 = new JSONObject();
      JSONObject var2 = new JSONObject();
      JSONArray var3 = new JSONArray();
      JSONArray var4 = new JSONArray();

      for (int var5 = 0; var5 < this.categories.size(); var5++) {
         String var6 = this.categories.get(var5);
         JSONObject var7 = new JSONObject();
         JSONArray var8 = new JSONArray();
         JSONArray var9 = new JSONArray();
         var4 = new JSONArray();

         for (int var10 = 0; var10 < this.values.size(); var10++) {
            BarChart.CategoryValue var11 = this.values.get(var10);
            if (var11.name.equals(var6)) {
               var4.put(var11.x);
               var8.put(SQUtils.d2(var11.y));
               if (this.individualColors.containsKey(var11.x)) {
                  var9.put(this.individualColors.get(var11.x));
               } else if (this.categoryColors.containsKey(var6)) {
                  var9.put(this.categoryColors.get(var6));
               } else if (this.invertIfNegative) {
                  var9.put(var11.y.doubleValue() >= 0.0 ? "#008000" : "#E8383C");
               } else {
                  var9.put("#383CE8");
               }
            }
         }

         var7.put("backgroundColor", var9);
         var7.put("label", var6);
         var7.put("data", var8);
         var3.put(var7);
      }

      var2.put("labels", var4);
      var2.put("datasets", var3);
      var1.put("data", var2);
      var1.put("options", this.getOptions());
      var1.put("type", this.type);
      return var1;
   }

   public class CategoryValue {
      public String name;
      public Comparable x;
      public Number y;

      public CategoryValue(String nullx, Comparable nullxx, Number nullxxx) {
         this.name = nullx;
         this.x = nullxx;
         this.y = nullxxx;
      }
   }
}
