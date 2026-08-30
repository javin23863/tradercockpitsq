package com.strategyquant.indicatorTester;

import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;

public class CalibrationInfo {
   public String key;
   public String name;
   public ArrayList<CalibrationRangesInfo> ranges = new ArrayList<>();

   public CalibrationInfo(String var1) {
      this.key = var1;
   }

   public void addRange(String var1, String var2, double var3, double var5) {
      this.ranges.add(new CalibrationRangesInfo(var1, var2, var3, var5));
   }

   public JSONObject toJSON() {
      JSONObject var1 = new JSONObject();
      var1.put("key", this.key);
      var1.put("name", this.name);
      JSONArray var2 = new JSONArray();

      for (int var3 = 0; var3 < this.ranges.size(); var3++) {
         CalibrationRangesInfo var4 = this.ranges.get(var3);
         var2.put(var4.toJSON());
      }

      var1.put("ranges", var2);
      return var1;
   }
}
