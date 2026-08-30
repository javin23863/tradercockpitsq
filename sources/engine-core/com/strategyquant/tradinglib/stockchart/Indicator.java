package com.strategyquant.tradinglib.stockchart;

public class Indicator {
   private String title;
   private boolean subChart;
   private IndicatorLine[] lines;
   private IndicatorLevel[] levels;

   public IndicatorLevel[] getLevels() {
      return this.levels;
   }

   public void setLevels(IndicatorLevel[] var1) {
      this.levels = var1;
   }

   public String getTitle() {
      return this.title;
   }

   public void setTitle(String var1) {
      this.title = var1;
   }

   public boolean isSubChart() {
      return this.subChart;
   }

   public void setSubChart(boolean var1) {
      this.subChart = var1;
   }

   public IndicatorLine[] getLines() {
      return this.lines;
   }

   public void setLines(IndicatorLine[] var1) {
      this.lines = var1;
   }
}
