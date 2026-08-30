package com.strategyquant.tradinglib;

import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;

public class PieChart extends AbstractChart {
   private ArrayList<PieChart.Section> sections = new ArrayList<>();

   public PieChart() {
      super("pie");
   }

   public void addValue(String var1, Number var2, String var3) {
      PieChart.Section var4 = new PieChart.Section(var1, var2, var3);
      this.sections.add(var4);
   }

   public PieChart.Section getSectionByLabel(String var1) {
      for (int var2 = 0; var2 < this.sections.size(); var2++) {
         PieChart.Section var3 = this.sections.get(var2);
         if (var3.label.equals(var1)) {
            return var3;
         }
      }

      return null;
   }

   @Override
   public JSONObject toJSON() {
      JSONObject var1 = new JSONObject();
      JSONObject var2 = new JSONObject();
      JSONArray var3 = new JSONArray();
      JSONArray var4 = new JSONArray();
      JSONArray var5 = new JSONArray();
      JSONArray var6 = new JSONArray();
      JSONObject var7 = new JSONObject();

      for (int var8 = 0; var8 < this.sections.size(); var8++) {
         PieChart.Section var9 = this.sections.get(var8);
         var4.put(var9.value);
         var5.put(var9.label);
         var6.put(var9.color);
      }

      var7.put("label", "");
      var7.put("backgroundColor", var6);
      var7.put("data", var4);
      var3.put(var7);
      var2.put("labels", var5);
      var2.put("datasets", var3);
      var1.put("data", var2);
      var1.put("options", this.getOptions());
      var1.put("type", this.type);
      return var1;
   }

   private class Section {
      public String label;
      public Number value;
      public String color;

      public Section(String nullx, Number nullxx, String nullxxx) {
         this.label = nullx;
         this.value = nullxx;
         this.color = nullxxx;
      }
   }
}
