package com.strategyquant.tradinglib.connection;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.datalib.data.io.VersatileData;
import java.util.ArrayList;

public class HistoryData {
   public static final int TypeTick = 0;
   public static final int TypeMinute = 1;
   private ChartDef chart;
   private int type;
   private long fromTime;
   private long toTime;
   private ArrayList<VersatileData> data = new ArrayList<>();

   public HistoryData(ChartDef var1, int var2, long var3, long var5) {
      this.chart = var1;
      this.type = var2;
      this.fromTime = var3;
      this.toTime = var5;
   }

   public void addMinuteData(long var1, double var3, double var5, double var7, double var9, double var11) {
      VersatileData var13 = new VersatileData();
      var13.set(2, var1, this.chart.getConnectionHash(), this.chart.getSymbolHash(), var3, var5, var7, var9, var11);
      this.data.add(var13);
   }

   public ArrayList<VersatileData> getData() {
      return this.data;
   }
}
