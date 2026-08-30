package com.strategyquant.tradinglib.equitychart;

import com.strategyquant.lib.utils.JsonCreator;
import org.json.JSONObject;

public class Text {
   public String text;
   public String color;
   public String font;
   public int x;
   public int y;

   public Text(String var1, String var2, String var3, int var4, int var5) {
      this.text = var1;
      this.color = var2;
      this.font = var3;
      this.x = var4;
      this.y = var5;
   }

   public JSONObject toJSON() {
      JSONObject var1 = new JSONObject();
      var1.put("text", this.text);
      var1.put("color", this.color);
      var1.put("font", this.font);
      var1.put("x", this.x);
      var1.put("y", this.y);
      return var1;
   }

   public void fillJSON(JsonCreator var1) {
      var1.put("text", this.text, true);
      var1.put("color", this.color, true);
      var1.put("font", this.font, true);
      var1.put("x", this.x, true);
      var1.put("y", this.y, false);
   }
}
