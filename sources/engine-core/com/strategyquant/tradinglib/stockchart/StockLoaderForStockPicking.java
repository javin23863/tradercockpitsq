package com.strategyquant.tradinglib.stockchart;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.data.io.IDataLoader;
import com.strategyquant.datalib.data.io.VersatileData;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.blocks.random.BlockDefinition;
import com.strategyquant.tradinglib.blocks.random.BlocksConfig;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.jdom2.JDOMException;

public class StockLoaderForStockPicking {
   private static final String[] COLORS = new String[]{"red", "green", "blue", "brown", "orange"};
   private static final int DEFAULT_DATAWIDTH = 250;
   private StockData data;
   private Map<Long, Long> timestampToIdMap;
   private Map<Long, Long> idToTimestampMap;
   private List<Long> timestamps;
   private List<Long> timeline = new LinkedList<>();
   private Long fromTime;
   private Long toTime;
   private Long fromId;
   private Long toId;
   private String path;
   private DataInfo dataInfo;
   private IDataLoader loader;
   private Map<String, Integer> orderCounts = new HashMap<>();
   private List<String[]> indicators = new LinkedList<>();
   private int timelineFrom = 0;

   public StockData load(JarFile var1, Long var2, Long var3, Long var4, boolean var5, boolean var6, String var7, Integer var8, String var9) throws Exception {
      this.fromId = var2;
      this.toId = var3;
      this.path = "Results/" + this.correctResultKey(var7);
      this.prepareStructure(var1, var9);
      if (var9 == null) {
         var9 = this.data.getStocks()[0];
      }

      this.data.setCurrentStock(var9);
      Integer var10 = this.orderCounts.get(var9);
      this.data.setTradesCount(var10 == null ? 0L : var10.intValue());
      this.prepareLoader(var9);
      if (var4 != null && this.data.getTradesCount() != 0L) {
         this.setDateFromForTrade(var1, var4, var2, var3);
         if (this.data.getSelectedTrade() != null) {
            var6 = true;
         }
      }

      if (var6) {
         this.findFromToIndexes(var8);
      }

      this.correctFromTo();
      this.loadData(var1, var5, var9);
      this.loadTrades(var1, var9);
      this.cleanup();
      return this.data;
   }

   private void loadTimelines(JarFile var1, long var2) throws IOException {
      JarEntry var4 = var1.getJarEntry(this.path + "/timelines.dat");
      InputStream var5 = var1.getInputStream(var4);
      byte[] var6 = this.getBytes(var5);
      int var7 = var6.length / 8;
      ByteBuffer var8 = ByteBuffer.wrap(var6);

      for (int var9 = 0; var9 < var7; var9++) {
         long var10 = var8.getLong();
         this.timeline.add(var10);
         if (this.timelineFrom == 0 && var10 >= var2) {
            this.timelineFrom = var9;
         }
      }

      var5.close();
   }

   private void prepareLoader(String var1) throws Exception {
      this.dataInfo = DataManager.getDataInfo("History", var1);
      if (this.dataInfo == null) {
         throw new Exception("No data info found for symbol '" + var1 + "'.");
      }

      ChartDef var2 = new ChartDef("History", var1, "D1", 0L, SQTime.toLong(2100, 1, 1), 2.5, "History");
      this.loader = DataManager.getDataLoader(var2, 1, null);
      this.loader.open();
      this.timestamps = new LinkedList<>();
      this.timestampToIdMap = new HashMap<>();
      this.idToTimestampMap = new HashMap<>();
      VersatileData var3 = new VersatileData();

      for (long var4 = 0L; this.loader.hasNextTick(); var4++) {
         this.loader.getNextTick(var3);
         long var6 = var3.time;
         this.timestamps.add(var6);
         this.timestampToIdMap.put(var6, var4);
         this.idToTimestampMap.put(var4, var6);
      }
   }

   private void fillPreview(StockData var1, long var2, IDataLoader var4) throws Exception {
      short var5 = 1000;
      int var6;
      double var7;
      if (var2 < var5) {
         var6 = (int)var2;
         var7 = 1.0;
      } else {
         var7 = (double)var2 / var5;
         var6 = var5;
      }

      LinkedList var9 = new LinkedList();
      float[] var10 = new float[var6];
      double var11 = 0.0;
      int var13 = 0;
      Integer var14 = null;
      VersatileData var15 = new VersatileData();

      while (var11 < var2 && var13 < var6) {
         var4.seek((int)var11);
         var4.hasNextTick();
         var4.getNextTick(var15);
         var10[var13] = (float)(Math.round(var15.close * 10000.0) / 10000.0);
         int var16 = SQTime.getFullYear(var15.time);
         if (var14 == null || var16 != var14) {
            PreviewCaption var17 = new PreviewCaption(var13, String.valueOf(var16));
            var9.add(var17);
            var14 = var16;
         }

         var11 += var7;
         var13++;
      }

      var1.setPreviewYVals(var10);
      var1.setPreviewCaptions(var9.toArray(new PreviewCaption[0]));
   }

   private void loadData(JarFile var1, boolean var2, String var3) throws Exception {
      this.data.setAreaFrom(this.fromId);
      this.data.setAreaTo(this.toId);
      this.loader.seek(this.fromId.intValue());
      long var4 = this.loader.getTotalRecords();
      this.data.setCount(var4);
      StockChartData var6 = this.fillStockData(this.data, this.loader, this.dataInfo, var3);
      this.loadTimelines(var1, var6.getxVals()[0]);
      this.fillIndicators(var1, var3, var6);
      if (var2) {
         this.fillPreview(this.data, var4, this.loader);
      }
   }

   private void fillIndicators(JarFile var1, String var2, StockChartData var3) throws JDOMException, Exception {
      BlocksConfig var4 = BlocksConfig.getConfig();
      int var5 = 0;
      LinkedList var6 = new LinkedList();
      String var7 = this.path + "/indicator_";

      for (String[] var9 : this.indicators) {
         BlockDefinition var10 = var4.getBlock("talib_" + var9[2]);
         boolean var11 = var10.returnType != 2;
         Indicator var12 = new Indicator();
         var12.setTitle(var9[3].replace("@Chart@", this.data.getCurrentStock() + ","));
         var12.setSubChart(var11);
         LinkedList var13 = new LinkedList();
         String var14 = var7 + var9[1] + ".dat";
         JarEntry var15 = var1.getJarEntry(var14);
         InputStream var16 = var1.getInputStream(var15);
         int var17 = var3.getClose().size();
         byte[] var18 = this.getBytes(var16);
         int var19 = var18.length / 4;
         ByteBuffer var20 = ByteBuffer.wrap(var18);
         LinkedList var21 = new LinkedList();

         for (int var22 = 0; var22 < var19; var22++) {
            var21.add(var20.getFloat());
         }

         LinkedList var25 = new LinkedList();

         for (int var23 = this.timelineFrom; var23 < var17 + this.timelineFrom && var23 < var19; var23++) {
            var25.add((Float)var21.get(var23));
         }

         IndicatorLine var26 = new IndicatorLine();
         String var24 = COLORS[var5 % COLORS.length];
         var26.setLine(var9[4]);
         var26.setColor(var24);
         var26.setKind(ChartKind.line);
         var26.setyValues(var25.toArray(new Float[0]));
         var13.add(var26);
         var5++;
         if (!var13.isEmpty()) {
            var6.add(var12);
            var12.setLines(var13.toArray(new IndicatorLine[0]));
         }
      }

      var3.setIndicators(var6.toArray(new Indicator[0]));
   }

   private StockChartData fillStockData(StockData var1, IDataLoader var2, DataInfo var3, String var4) throws Exception {
      long var5 = var1.getAreaTo() - var1.getAreaFrom();
      int var7 = 0;
      StockChartData var8 = new StockChartData();
      var8.setTitle(var4);
      var8.setPrecision(var3.symbolInfo.decimals);
      var1.setChartData(new StockChartData[1]);
      var1.getChartData()[0] = var8;
      VersatileData var9 = new VersatileData();
      var8.setClose(new LinkedList<>());
      var8.setLow(new LinkedList<>());
      var8.setHigh(new LinkedList<>());
      var8.setOpen(new LinkedList<>());
      LinkedList var10 = new LinkedList();
      this.fromTime = null;

      while (var2.hasNextTick()) {
         var2.getNextTick(var9);
         var10.add(var9.time);
         var8.getClose().add(var9.close);
         var8.getOpen().add(var9.open);
         var8.getHigh().add(var9.high);
         var8.getLow().add(var9.low);
         if (this.fromTime == null) {
            this.fromTime = var9.time;
         }

         this.toTime = var9.time;
         if (var7++ == var5) {
            break;
         }
      }

      long[] var11 = new long[var10.size()];
      int var12 = 0;

      for (Long var14 : var10) {
         var11[var12++] = var14;
      }

      var8.setxVals(var11);
      return var8;
   }

   private void setDateFromForTrade(JarFile var1, Long var2, Long var3, Long var4) throws IOException {
      var3 = this.dataInfo.dateFrom;
      var4 = this.dataInfo.dateTo;
      if (var2 >= 0L) {
         this.setTradeFromForSpecificTrade(var1, var2);
      } else if (var2 == -1L) {
         this.setTradeFromFromFirst(var1);
      } else if (var2 == -2L) {
         this.setTradeFromFromLast(var1);
      }
   }

   private String getLastTradeFile(JarFile var1) {
      int var2 = 0;
      String var3 = null;

      while (true) {
         String var4 = this.path + "/trades_" + this.data.getCurrentStock() + "_" + var2 + ".dat";
         JarEntry var5 = var1.getJarEntry(var4);
         if (var5 == null) {
            return var3;
         }

         var3 = var4;
         var2++;
      }
   }

   private void setTradeFromFromLast(JarFile var1) throws IOException {
      String var2 = this.getLastTradeFile(var1);
      if (var2 != null) {
         JarEntry var3 = var1.getJarEntry(var2);
         InputStream var4 = var1.getInputStream(var3);
         byte[] var5 = new byte[16];
         var4.read(var5);
         byte[] var6 = this.getBytes(var4);
         int var7 = 0;

         while (var7 < var6.length) {
            long var8 = ByteBuffer.wrap(var6, var7, 8).getLong();
            var7 += 8;
            long var10 = ByteBuffer.wrap(var6, var7, 8).getLong();
            var7 += 8;
            this.data.setSelectedTrade(var8);
            this.fromId = var10;
            var7 += 25;
         }

         var4.close();
      }
   }

   private void setTradeFromFromFirst(JarFile var1) throws IOException {
      JarEntry var2 = var1.getJarEntry(this.path + "/trades_" + this.data.getCurrentStock() + "_0.dat");
      if (var2 != null) {
         InputStream var3 = var1.getInputStream(var2);
         byte[] var4 = new byte[16];
         var3.read(var4);
         byte[] var5 = this.getBytes(var3);
         int var6 = 0;
         if (var6 < var5.length) {
            long var7 = ByteBuffer.wrap(var5, var6, 8).getLong();
            var6 += 8;
            long var9 = ByteBuffer.wrap(var5, var6, 8).getLong();
            var6 += 8;
            this.data.setSelectedTrade(var7);
            this.fromId = var9;
         }

         var3.close();
      }
   }

   private void setTradeFromForSpecificTrade(JarFile var1, Long var2) throws IOException {
      int var3 = 0;
      int var32 = 0;

      while (true) {
         JarEntry var5 = var1.getJarEntry(this.path + "/trades_" + this.data.getCurrentStock() + "_" + var3 + ".dat");
         if (var5 == null) {
            return;
         }

         InputStream var6 = var1.getInputStream(var5);
         byte[] var7 = new byte[16];
         var6.read(var7);
         byte[] var8 = this.getBytes(var6);

         for (int var9 = 0; var32 < var8.length; var9++) {
            long var10 = ByteBuffer.wrap(var8, var32, 8).getLong();
            int var26 = var32 + 8;
            long var12 = ByteBuffer.wrap(var8, var26, 8).getLong();
            int var27 = var26 + 8;
            long var14 = ByteBuffer.wrap(var8, var27, 8).getLong();
            int var28 = var27 + 8;
            double var16 = ByteBuffer.wrap(var8, var28, 4).getFloat();
            int var29 = var28 + 4;
            double var18 = ByteBuffer.wrap(var8, var29, 4).getFloat();
            int var30 = var29 + 4;
            double var20 = ByteBuffer.wrap(var8, var30, 4).getFloat();
            int var31 = var30 + 4;
            double var22 = ByteBuffer.wrap(var8, var31, 4).getFloat();
            var32 = var31 + 4;
            byte var24 = ByteBuffer.wrap(var8, var32, 1).get();
            var32++;
            boolean var25 = var24 == 1;
            if (var10 == var2 || var9 == var2) {
               this.fromId = var12;
               this.data.setSelectedTrade(var10);
               return;
            }
         }

         var6.close();
         var3++;
      }
   }

   private void findFromToIndexes(Integer var1) {
      Long var2 = this.findNearestTime(this.fromId);
      this.fromId = this.timestampToIdMap.get(var2);
      Long var3 = this.fromId;
      this.fromId = this.fromId - 1L;
      long var5 = var1 != null ? var1.intValue() : 250L;
      this.fromId = this.fromId - var5 / 3L;
      if (this.fromId < 0L) {
         this.fromId = 0L;
      }

      this.toId = this.fromId + var5;
   }

   private void cleanup() {
      this.idToTimestampMap = null;
      this.timestampToIdMap = null;
   }

   private void correctFromTo() throws Exception {
      if (this.fromId == null || this.fromId < 0L || this.fromId > this.toId) {
         this.fromId = 0L;
      }

      if (this.toId == null) {
         this.toId = this.fromId + 250L;
      }

      long var1 = this.loader.getTotalRecords();
      if (this.toId > var1 - 1L) {
         long var3 = this.toId - this.fromId;
         this.toId = var1 - 1L;
         this.fromId = this.toId - var3;
         if (this.fromId < 0L) {
            this.fromId = 0L;
         }
      }

      this.data.setAreaFrom(this.fromId);
      this.data.setAreaTo(this.toId);
   }

   private String correctResultKey(String var1) {
      return var1.replace("/", "_LOM_");
   }

   private void loadTrades(JarFile var1, String var2) throws IOException {
      int var3 = 0;
      StockChartData var4 = this.data.getChartData()[0];
      LinkedList var5 = new LinkedList();
      short var6 = 0;

      while (true) {
         JarEntry var7 = var1.getJarEntry(this.path + "/trades_" + var2 + "_" + var3 + ".dat");
         if (var7 == null) {
            var4.setTrades(var5.toArray(new Trade[0]));
            return;
         }

         InputStream var8 = var1.getInputStream(var7);
         if (this.fitDate(var8)) {
            byte[] var9 = this.getBytes(var8);
            this.loadTrades(var5, var9, var6);
         }

         var8.close();
         var3++;
         var6 += 1000;
      }
   }

   private void loadTrades(List<Trade> var1, byte[] var2, int var3) {
      int var34 = 0;

      while (var34 < var2.length) {
         long var5 = ByteBuffer.wrap(var2, var34, 8).getLong();
         int var28 = var34 + 8;
         long var7 = ByteBuffer.wrap(var2, var28, 8).getLong();
         int var29 = var28 + 8;
         long var9 = ByteBuffer.wrap(var2, var29, 8).getLong();
         int var30 = var29 + 8;
         double var11 = ByteBuffer.wrap(var2, var30, 4).getFloat();
         int var31 = var30 + 4;
         double var13 = ByteBuffer.wrap(var2, var31, 4).getFloat();
         int var32 = var31 + 4;
         double var15 = ByteBuffer.wrap(var2, var32, 4).getFloat();
         int var33 = var32 + 4;
         double var17 = ByteBuffer.wrap(var2, var33, 4).getFloat();
         var34 = var33 + 4;
         byte var19 = ByteBuffer.wrap(var2, var34, 1).get();
         var34++;
         boolean var20 = var19 == 1;
         if (this.toTime >= var7 && this.fromTime <= var9) {
            Long var21 = this.timestampToIdMap.get(var7);
            long var22 = var7;
            long var24 = var9;
            if (var21 == null) {
               var22 = this.findCandleAtOrBefore(var7);
               var21 = this.timestampToIdMap.get(var22);
            }

            Long var26 = this.timestampToIdMap.get(var9);
            if (var26 == null) {
               var24 = this.findCandleAtOrBefore(var9);
               var26 = this.timestampToIdMap.get(var24);
            }

            Trade var27 = new Trade();
            var27.setIndex(var3);
            var27.setId(var5);
            var27.setOpenTime(var22);
            var27.setOpenValue(var11);
            var27.setSLValue(var15);
            var27.setPTValue(var17);
            var27.setCloseTime(var24);
            var27.setCloseValue(var13);
            var27.setShortTrade(var20);
            var27.setRealOpenTime(var7);
            var27.setRealCloseTime(var9);
            var1.add(var27);
         }

         var3++;
      }
   }

   private Long findCandleAtOrBefore(long var1) {
      int var3 = this.timestamps.size();
      if (var3 == 0) {
         return null;
      }

      long var4 = this.timestamps.get(0);
      long var6 = this.timestamps.get(var3 - 1);
      if (var1 <= var4) {
         return var4;
      }

      if (var1 >= var6) {
         return var6;
      }

      int var8 = 0;
      int var9 = var3 - 1;

      while (var8 < var9) {
         int var10 = var8 + var9 + 1 >>> 1;
         if (this.timestamps.get(var10) <= var1) {
            var8 = var10;
         } else {
            var9 = var10 - 1;
         }
      }

      return this.timestamps.get(var8);
   }

   private Long findCandleCloseAtOrAfter(long var1) {
      int var3 = this.timestamps.size();
      if (var3 == 0) {
         return null;
      }

      long var4 = this.timestamps.get(0);
      long var6 = this.timestamps.get(var3 - 1);
      if (var1 <= var4) {
         return var4;
      }

      if (var1 >= var6) {
         return var6;
      }

      int var8 = 0;
      int var9 = var3 - 1;

      while (var8 < var9) {
         int var10 = var8 + var9 >>> 1;
         if (this.timestamps.get(var10) < var1) {
            var8 = var10 + 1;
         } else {
            var9 = var10;
         }
      }

      return this.timestamps.get(var8);
   }

   private Long findNearestTime(long var1) {
      int var3 = 0;
      int var4 = this.timestamps.size() - 1;

      while (var3 <= var4) {
         int var5 = (var3 + var4) / 2;
         Long var6 = this.timestamps.get(var5);
         Long var7 = this.timestamps.get(var4);
         int var8 = var4 - var3;
         if (var8 == 0) {
            return var7;
         }

         if (var8 == 1) {
            if (var7 > var1) {
               return this.timestamps.get(var3);
            }

            return var7;
         }

         if (var6 > var1) {
            var4 = var5;
         } else if (var1 > var6) {
            var3 = var5;
         } else if (var6 == var1) {
            return var6;
         }
      }

      return this.timestamps.get(var4);
   }

   private boolean fitDate(InputStream var1) throws IOException {
      byte[] var2 = new byte[16];
      var1.read(var2);
      long var3 = ByteBuffer.wrap(var2, 0, 8).getLong();
      long var5 = ByteBuffer.wrap(var2, 8, 8).getLong();
      return this.toTime >= var3 && this.fromTime <= var5;
   }

   private void prepareStructure(JarFile var1, String var2) throws Exception {
      this.data = new StockData();
      this.timestampToIdMap = new HashMap<>();
      this.idToTimestampMap = new HashMap<>();
      JarEntry var3 = var1.getJarEntry(this.path + "/stock_desc.csv");
      InputStream var4 = var1.getInputStream(var3);
      String var5 = SQUtils.inputStreamToString(var4);
      String[] var6 = var5.split("\n");
      LinkedList var7 = new LinkedList();
      String var8 = null;
      this.data.setChartData(new StockChartData[1]);

      for (int var9 = 0; var9 < var6.length; var9++) {
         String var10 = var6[var9];
         String[] var11 = var10.split(";");
         String var12 = var11[0];
         if (var12.equals("S")) {
            var8 = var11[1];
            if (var2 == null) {
               var2 = var8;
            }

            var7.add(var8);
            int var13 = 0;

            try {
               var13 = Integer.valueOf(var11[2]);
            } catch (Exception var15) {
            }

            this.orderCounts.put(var11[1], var13);
         } else if (var12.equals("I") && var2.equals(var8)) {
            this.indicators.add(var11);
         }
      }

      var4.close();
      this.data.setStocks(var7.toArray(new String[0]));
   }

   private byte[] getBytes(InputStream var1) throws IOException {
      byte[] var2 = new byte[4096];
      ByteArrayOutputStream var4 = new ByteArrayOutputStream();

      int var3;
      while ((var3 = var1.read(var2)) != -1) {
         var4.write(var2, 0, var3);
      }

      var4.flush();
      return var4.toByteArray();
   }
}
