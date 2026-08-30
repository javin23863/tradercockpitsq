package com.strategyquant.tradinglib.project;

import org.json.JSONObject;

public class TaskStat {
   public static String BAR = "bar";
   public static String MILLISECONDS = "milliseconds";
   public String action;
   public String display;
   public long count;
   public int order;
   public String percentageCompareAction;

   public JSONObject getJSONObject() {
      JSONObject var1 = new JSONObject();
      var1.put("display", this.display);
      var1.put("action", this.action);
      var1.put("count", this.count);
      var1.put("order", this.order);
      var1.put("percentageCompareAction", this.percentageCompareAction);
      return var1;
   }
}
