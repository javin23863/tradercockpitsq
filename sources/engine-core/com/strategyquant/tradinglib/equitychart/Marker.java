package com.strategyquant.tradinglib.equitychart;

import com.strategyquant.lib.utils.JsonCreator;
import org.json.JSONObject;

public class Marker {
   public String color = "red";
   public long x;
   public int thickness = 1;

   public JSONObject toJSON() {
      JSONObject var1 = new JSONObject();
      var1.put("color", this.color);
      var1.put("x", this.x);
      var1.put("thickness", this.thickness);
      return var1;
   }

   public void fillJSON(JsonCreator var1) {
      var1.put("color", this.color, true);
      var1.put("x", this.x, true);
      var1.put("thickness", this.thickness, false);
   }
}
