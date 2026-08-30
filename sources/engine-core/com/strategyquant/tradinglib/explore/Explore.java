package com.strategyquant.tradinglib.explore;

import it.unimi.dsi.fastutil.longs.Long2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class Explore {
   private Object2ObjectOpenHashMap<String, Long2ObjectAVLTreeMap<double[]>> data = new Object2ObjectOpenHashMap();
   private ObjectArrayList<String> columns;

   public Explore(ObjectArrayList<String> var1) {
      this.columns = var1;
   }

   public ObjectArrayList<String> getColumns() {
      return this.columns;
   }

   public void add(String var1, long var2, int var4, double var5) {
      if (!this.data.containsKey(var1)) {
         this.data.put(var1, new Long2ObjectAVLTreeMap());
      }

      Long2ObjectAVLTreeMap var7 = (Long2ObjectAVLTreeMap)this.data.get(var1);
      if (!var7.containsKey(var2)) {
         var7.put(var2, new double[this.columns.size()]);
      }

      double[] var8 = (double[])var7.get(var2);
      var8[var4] = var5;
   }

   public Object2ObjectOpenHashMap<String, Long2ObjectAVLTreeMap<double[]>> getData() {
      return this.data;
   }

   public Long2ObjectAVLTreeMap<double[]> getSymbolData(String var1) {
      return (Long2ObjectAVLTreeMap<double[]>)this.data.get(var1);
   }

   public double getColumnValue(String var1, long var2, int var4) {
      return ((double[])((Long2ObjectAVLTreeMap)this.data.get(var1)).get(var2))[var4];
   }
}
