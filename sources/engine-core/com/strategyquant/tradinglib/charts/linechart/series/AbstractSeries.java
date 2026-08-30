package com.strategyquant.tradinglib.charts.linechart.series;

import java.util.ArrayList;

public abstract class AbstractSeries {
   public ArrayList<XYValue> values = new ArrayList<XYValue>() {
      public boolean add(XYValue var1) {
         super.add(var1);
         if (AbstractSeries.this.capacity > 0 && AbstractSeries.this.values.size() > AbstractSeries.this.capacity) {
            this.remove(0);
         }

         return true;
      }
   };
   public String name = null;
   public int capacity = -1;
   public String color = null;

   public AbstractSeries(String var1) {
      this.name = var1;
   }

   public AbstractSeries(String var1, int var2) {
      this.name = var1;
      this.capacity = var2;
   }

   public void clear() {
      this.values.clear();
   }
}
