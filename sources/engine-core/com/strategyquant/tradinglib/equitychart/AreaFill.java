package com.strategyquant.tradinglib.equitychart;

import com.strategyquant.lib.utils.JsonCreator;
import org.json.JSONObject;

public class AreaFill {
   public long xFrom;
   public long xTo;
   public String color = "#999";
   public String colorDark = "#999";
   public String text = "";
   public String textColor = "black";
   public String textColorDark = "#ddd";
   public String verticalAlign = "top";
   public String borderColor = null;
   public String borderColorDark = null;
   public int borderThickness = 1;
   public boolean borderOnly = false;

   public JSONObject toJSON() {
      JSONObject var1 = new JSONObject();
      var1.put("xFrom", this.xFrom);
      var1.put("xTo", this.xTo);
      var1.put("color", this.color);
      var1.put("text", this.text);
      var1.put("textColor", this.textColor);
      var1.put("textColor_dark", this.textColorDark);
      var1.put("verticalAlign", this.verticalAlign);
      var1.put("borderThickness", this.borderThickness);
      var1.put("borderOnly", this.borderOnly);
      if (this.borderColor != null) {
         var1.put("borderColor", this.borderColor);
      }

      if (this.borderColorDark != null) {
         var1.put("borderColor_dark", this.borderColorDark);
      }

      return var1;
   }

   public void fillJSON(JsonCreator var1) {
      var1.put("xFrom", this.xFrom, true);
      var1.put("xTo", this.xTo, true);
      var1.put("color", this.color, true);
      var1.put("color_dark", this.colorDark, true);
      var1.put("text", this.text, true);
      var1.put("textColor", this.textColor, true);
      var1.put("textColor_dark", this.textColorDark, true);
      var1.put("verticalAlign", this.verticalAlign, true);
      var1.put("borderThickness", this.borderThickness, true);
      if (this.borderColor != null) {
         var1.put("borderColor", this.borderColor, true);
      }

      if (this.borderColorDark != null) {
         var1.put("borderColor_dark", this.borderColorDark, true);
      }

      var1.put("borderOnly", this.borderOnly, false);
   }
}
