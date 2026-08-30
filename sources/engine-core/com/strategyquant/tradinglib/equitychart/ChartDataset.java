package com.strategyquant.tradinglib.equitychart;

import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.utils.JsonCreator;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONObject;

public abstract class ChartDataset {
   public static final String TYPE_LINE = "line";
   public static final String TYPE_BAR = "bar";
   public static final String TYPE_POINT = "point";
   public String key;
   public String name = "";
   public String type = "line";
   public String color = null;
   public String colorDark = null;
   public int thickness = 1;
   public long[] xVals;
   public double[] yVals;
   public int length = 0;
   public int index = -1;
   public int decimals = 2;
   private DateTimeFormatter dtf = DateTimeFormat.forPattern("yyyy-MM-dd");

   public void d3Format() {
      this.decimals = 3;
   }

   public int getSize() {
      return this.xVals.length;
   }

   public ChartDataset(String var1, int var2) {
      this.key = var1;
      this.length = var2;
      this.xVals = new long[var2];
      this.yVals = new double[var2];
   }

   public void addValue(long var1, double var3) {
      this.index++;
      this.xVals[this.index] = var1;
      this.yVals[this.index] = var3;
   }

   protected String formatTime(long var1) {
      return SQTime.toString(var1, this.dtf);
   }

   public double d(double var1) {
      return SQUtils.round(var1, this.decimals);
   }

   public abstract void fillJSON(JsonCreator var1, Zoom var2, boolean var3);

   public abstract JSONObject toJSON(Zoom var1, boolean var2);
}
