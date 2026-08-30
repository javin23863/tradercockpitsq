package com.strategyquant.tradinglib.stockchart;

public class IndicatorLevel {
   public static final String PATTERN_SOLID = "";
   public static final String PATTERN_DOT = "1,1";
   private String color;
   private double value;
   private String pattern;

   public String getColor() {
      return this.color;
   }

   public void setColor(String var1) {
      this.color = var1;
   }

   public double getValue() {
      return this.value;
   }

   public void setValue(double var1) {
      this.value = var1;
   }

   public String getPattern() {
      return this.pattern;
   }

   public void setPattern(String var1) {
      this.pattern = var1;
   }
}
