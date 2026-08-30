package com.strategyquant.tradinglib.stockchart;

public class PreviewCaption {
   private int index;
   private String caption;

   public PreviewCaption() {
   }

   public PreviewCaption(int var1, String var2) {
      this.index = var1;
      this.caption = var2;
   }

   public int getIndex() {
      return this.index;
   }

   public void setIndex(int var1) {
      this.index = var1;
   }

   public String getCaption() {
      return this.caption;
   }

   public void setCaption(String var1) {
      this.caption = var1;
   }
}
