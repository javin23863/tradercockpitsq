package com.strategyquant.tradinglib.stockchart;

public class IndicatorLine {
   private String line;
   private ChartKind kind;
   private String color;
   private Float[] yValues;
   private boolean showInChart = true;
   private boolean showZeroValues = true;

   public String getLine() {
      return this.line;
   }

   public void setLine(String var1) {
      this.line = var1;
   }

   public ChartKind getKind() {
      return this.kind;
   }

   public void setKind(ChartKind var1) {
      this.kind = var1;
   }

   public String getColor() {
      return this.color;
   }

   public void setColor(String var1) {
      this.color = var1;
   }

   public Float[] getyValues() {
      return this.yValues;
   }

   public void setyValues(Float[] var1) {
      this.yValues = var1;
   }

   public boolean isShowInChart() {
      return this.showInChart;
   }

   public void setShowInChart(boolean var1) {
      this.showInChart = var1;
   }

   public boolean isShowZeroValues() {
      return this.showZeroValues;
   }

   public void setShowZeroValues(boolean var1) {
      this.showZeroValues = var1;
   }
}
