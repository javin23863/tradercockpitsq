package com.strategyquant.indicatorTester;

import org.json.JSONObject;

public class IndyParameter {
   public String name;
   public String className;
   public String defaultValue;
   public String value;
   public String options;
   public String calibrationValues;

   public JSONObject toJSON() {
      JSONObject var1 = new JSONObject();
      var1.put("name", this.name);
      var1.put("className", this.className);
      var1.put("defaultValue", this.defaultValue);
      var1.put("value", this.value);
      var1.put("options", this.options);
      var1.put("calibrationValues", this.calibrationValues);
      return var1;
   }
}
