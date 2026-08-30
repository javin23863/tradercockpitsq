package com.strategyquant.tradinglib.equitychart;

import com.strategyquant.lib.utils.JsonCreator;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;

public class SubChart {
   public String key;
   public String name = "";
   public int weight = 10;
   public boolean hideYValues = false;
   public boolean hideYTitle = false;
   public String titlePosition = null;
   public boolean forceStartYFromZero = false;
   public ArrayList<SubChartDataset> datasets = new ArrayList<>();

   public SubChart(String var1) {
      this.key = var1;
   }

   public void addDataset(SubChartDataset var1) {
      this.datasets.add(var1);
   }

   public JSONObject toJSON(Zoom var1, boolean var2) {
      JSONObject var3 = new JSONObject();
      JSONArray var4 = new JSONArray();

      for (int var5 = 0; var5 < this.datasets.size(); var5++) {
         SubChartDataset var6 = this.datasets.get(var5);
         var4.put(var6.toJSON(var1, var2));
      }

      var3.put("title", this.name);
      var3.put("weight", this.weight);
      var3.put("hideYValues", this.hideYValues);
      var3.put("hideYTitle", this.hideYTitle);
      var3.put("datasets", var4);
      var3.put("forceStartYFromZero", this.forceStartYFromZero);
      if (this.titlePosition != null) {
         var3.put("titlePosition", this.titlePosition);
      }

      return var3;
   }

   public void fillJSON(JsonCreator var1, Zoom var2, boolean var3) {
      var1.put("title", this.name, true);
      var1.put("weight", this.weight, true);
      var1.put("hideYValues", this.hideYValues, true);
      var1.put("hideYTitle", this.hideYTitle, true);
      var1.put("forceStartYFromZero", this.forceStartYFromZero, true);
      if (this.titlePosition != null) {
         var1.put("titlePosition", this.titlePosition, true);
      }

      var1.putBeginArray("datasets");

      for (int var4 = 0; var4 < this.datasets.size(); var4++) {
         SubChartDataset var5 = this.datasets.get(var4);
         var1.beginObject();
         var5.fillJSON(var1, var2, var3);
         var1.endObject(var4 < this.datasets.size() - 1);
      }

      var1.endArray(false);
   }
}
