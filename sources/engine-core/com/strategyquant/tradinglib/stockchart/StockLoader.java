package com.strategyquant.tradinglib.stockchart;

import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StockLoader {
   public static final Logger Log = LoggerFactory.getLogger(StockLoader.class);
   private static final int DEFAULT_DATAWIDTH = 250;
   private StockData data;
   private Map<Long, Long> timestampToIdMap;
   private List<Long> timestamps;
   private Map<Long, Long> idToTimestampMap;
   private Long fromTime;
   private Long toTime;
   private Long fromId;
   private Long toId;
   private String path;

   public StockData load(JarFile var1, Long var2, Long var3, Long var4, boolean var5, boolean var6, String var7, Integer var8) throws Exception {
      this.fromId = var2;
      this.toId = var3;
      this.path = "Results/" + this.correctResultKey(var7);
      this.prepareStructure(var1);
      if (var5) {
         this.loadPreview(var1);
      }

      this.loadIds(var1, var5);
      if (var4 != null && this.data.getTradesCount() != 0L) {
         if (var4 >= this.data.getTradesCount()) {
            var4 = this.data.getTradesCount() - 1L;
         }

         this.setDateFromForTrade(var1, var4, var2, var3);
         if (this.data.getSelectedTrade() != null) {
            var6 = true;
         }
      }

      if (var6) {
         this.findFromToIndexes(var8);
      }

      this.correctFromTo();
      this.data.setAreaFrom(this.fromId);
      this.data.setAreaTo(this.toId);
      this.loadMainData(var1);
      this.loadTrades(var1);
      this.correctIndicatorNames();
      this.cleanup();
      return this.data;
   }

   private void correctIndicatorNames() {
      for (int var1 = 0; var1 < this.data.getChartData().length; var1++) {
         StockChartData var2 = this.data.getChartData()[var1];
         String var3 = var2.getTitle();

         for (int var4 = 0; var4 < var2.getIndicators().length; var4++) {
            Indicator var5 = var2.getIndicators()[var4];
            String var6 = var5.getTitle();
            var6 = var6.replace("@Chart@)", var3 + ")");
            var6 = var6.replace("@Chart@", var3 + ",");
            var5.setTitle(var6);
         }
      }
   }

   private void setDateFromForTrade(JarFile var1, Long var2, Long var3, Long var4) throws IOException {
      var3 = this.idToTimestampMap.get(var3);
      var4 = this.idToTimestampMap.get(var4);
      if (var2 >= 0L) {
         this.setTradeFromForSpecificTrade(var1, var2);
      } else if (var2 == -1L) {
         this.setTradeFromFromFirst(var1, var4);
      } else if (var2 == -2L) {
         this.setTradeFromFromLast(var1, var3);
      }
   }

   private void setTradeFromFromLast(JarFile var1, Long var2) throws IOException {
      int var3 = 0;

      while (true) {
         JarEntry var4 = var1.getJarEntry(this.path + "/trades_0_" + var3 + ".dat");
         if (var4 == null) {
            var3++;
            return;
         }

         InputStream var5 = var1.getInputStream(var4);
         byte[] var6 = new byte[16];
         var5.read(var6);
         long var7 = ByteBuffer.wrap(var6, 0, 8).getLong();
         long var9 = ByteBuffer.wrap(var6, 8, 8).getLong();
         byte[] var11 = this.getBytes(var5);
         int var12 = 0;

         while (var12 < var11.length) {
            long var13 = ByteBuffer.wrap(var11, var12, 8).getLong();
            var12 += 8;
            long var15 = ByteBuffer.wrap(var11, var12, 8).getLong();
            var12 += 8;
            if (var15 > var2) {
               return;
            }

            this.data.setSelectedTrade(var13);
            this.fromId = var15;
            var12 += 25;
         }

         var5.close();
      }
   }

   private void setTradeFromFromFirst(JarFile var1, Long var2) throws IOException {
      int var3 = 0;

      while (true) {
         JarEntry var4 = var1.getJarEntry(this.path + "/trades_0_" + var3 + ".dat");
         if (var4 == null) {
            return;
         }

         InputStream var5 = var1.getInputStream(var4);
         byte[] var6 = new byte[16];
         var5.read(var6);
         long var7 = ByteBuffer.wrap(var6, 0, 8).getLong();
         long var9 = ByteBuffer.wrap(var6, 8, 8).getLong();
         if (var9 > var2) {
            byte[] var11 = this.getBytes(var5);
            int var12 = 0;

            while (var12 < var11.length) {
               long var13 = ByteBuffer.wrap(var11, var12, 8).getLong();
               var12 += 8;
               long var15 = ByteBuffer.wrap(var11, var12, 8).getLong();
               var12 += 8;
               if (var15 > var2) {
                  this.data.setSelectedTrade(var13);
                  this.fromId = var15;
                  return;
               }

               var12 += 25;
            }
         }

         var5.close();
         var3++;
      }
   }

   private void setTradeFromForSpecificTrade(JarFile var1, Long var2) throws IOException {
      int var3 = (int)(var2 / 1000L);
      byte var4 = 0;
      JarEntry var5 = var1.getJarEntry(this.path + "/trades_" + var4 + "_" + var3 + ".dat");
      if (var5 != null) {
         InputStream var6 = var1.getInputStream(var5);
         byte[] var7 = this.getBytes(var6);
         var6.close();
         int var8 = (int)(var2 - var3 * 1000);
         int var9 = var8 * 41 + 16;
         this.data.setSelectedTrade(ByteBuffer.wrap(var7, var9, 8).getLong());
         var9 += 8;
         long var10 = ByteBuffer.wrap(var7, var9, 8).getLong();
         var9 += 8;
         this.fromId = var10;
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
      this.timestamps = null;
   }

   private void correctFromTo() {
      if (this.fromId == null || this.fromId < 0L || this.fromId > this.toId) {
         this.fromId = 0L;
      }

      if (this.toId == null) {
         this.toId = this.fromId + 250L;
      }

      if (this.toId > this.timestampToIdMap.size() - 1) {
         long var1 = this.toId - this.fromId;
         this.toId = (long)(this.timestampToIdMap.size() - 1);
         this.fromId = this.toId - var1;
         if (this.fromId < 0L) {
            this.fromId = 0L;
         }
      }

      this.fromTime = this.idToTimestampMap.get(this.fromId);
      this.toTime = this.idToTimestampMap.get(this.toId);
   }

   private String correctResultKey(String var1) {
      return var1.replace("/", "_LOM_");
   }

   private void loadTrades(JarFile var1) throws IOException {
      for (int var2 = 0; var2 < this.data.getChartData().length; var2++) {
         this.loadTrades(var1, var2);
      }
   }

   private void loadTrades(JarFile var1, int var2) throws IOException {
      int var3 = 0;
      StockChartData var4 = this.data.getChartData()[var2];
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
            var27.setOpenId(var21);
            var27.setCloseId(var26);
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

   private void loadMainData(JarFile var1) throws IOException {
      for (int var2 = 0; var2 < this.data.getChartData().length; var2++) {
         this.loadMainData(var1, var2);
      }
   }

   private boolean fitDate(InputStream var1) throws IOException {
      byte[] var2 = new byte[16];
      var1.read(var2);
      long var3 = ByteBuffer.wrap(var2, 0, 8).getLong();
      long var5 = ByteBuffer.wrap(var2, 8, 8).getLong();
      return this.toTime >= var3 && this.fromTime <= var5;
   }

   private int getIndicatorLineCount(StockChartData var1) {
      int var2 = 0;
      if (var1.getIndicators() == null) {
         return var2;
      }

      for (Indicator var6 : var1.getIndicators()) {
         var2 += var6.getLines().length;
      }

      return var2;
   }

   private void loadMainData(JarFile var1, int var2) throws IOException {
      int var3 = 0;
      StockChartData var4 = this.data.getChartData()[var2];
      int var5 = this.getIndicatorLineCount(var4);
      StockLoader.ChartInfo var6 = new StockLoader.ChartInfo(var5);

      while (true) {
         JarEntry var7 = var1.getJarEntry(this.path + "/chartdata_" + var2 + "_" + var3 + ".dat");
         if (var7 == null) {
            var4.setxVals(var6.getXvalsArray());
            var4.setOpen(var6.getOpen());
            var4.setClose(var6.getClose());
            var4.setLow(var6.getLow());
            var4.setHigh(var6.getHigh());
            if (var4.getIndicators() != null) {
               int var16 = 0;

               for (Indicator var11 : var4.getIndicators()) {
                  for (IndicatorLine var15 : var11.getLines()) {
                     var15.setyValues(var6.getIndicatorArray(var16++, var15));
                  }
               }
            }

            return;
         }

         InputStream var8 = var1.getInputStream(var7);
         if (this.fitDate(var8)) {
            byte[] var9 = this.getBytes(var8);
            this.loadData(var6, var5, var9);
         }

         var8.close();
         var3++;
      }
   }

   private void loadData(StockLoader.ChartInfo var1, int var2, byte[] var3) {
      int var4 = 0;
      ByteBuffer var5 = ByteBuffer.wrap(var3);

      while (var4 < var3.length) {
         long var6 = var5.getLong(var4);
         var4 += 8;
         if (var6 < this.fromTime) {
            var4 += (4 + var2) * 4;
         } else {
            if (var6 > this.toTime) {
               break;
            }

            var1.getXvals().add(var6);
            var1.getOpen().add((double)ByteBuffer.wrap(var3, var4, 4).getFloat());
            var4 += 4;
            var1.getClose().add((double)ByteBuffer.wrap(var3, var4, 4).getFloat());
            var4 += 4;
            var1.getLow().add((double)ByteBuffer.wrap(var3, var4, 4).getFloat());
            var4 += 4;
            var1.getHigh().add((double)ByteBuffer.wrap(var3, var4, 4).getFloat());
            var4 += 4;

            for (int var8 = 0; var8 < var2; var8++) {
               List var9 = var1.getIndicators()[var8];
               var9.add(var5.getFloat(var4));
               var4 += 4;
            }
         }
      }
   }

   private void prepareStructure(JarFile var1) throws Exception {
      this.data = new StockData();
      this.timestampToIdMap = new HashMap<>();
      this.idToTimestampMap = new HashMap<>();
      this.timestamps = new LinkedList<>();
      JarEntry var2 = var1.getJarEntry(this.path + "/stock_desc.csv");
      InputStream var3 = var1.getInputStream(var2);
      String var4 = SQUtils.inputStreamToString(var3);
      String[] var5 = var4.split("\n");
      this.data.setCount(Long.parseLong(var5[0]));
      this.data.setTradesCount(Long.parseLong(var5[1]));
      StockChartData var6 = null;
      Indicator var7 = null;
      this.data.setChartData(new StockChartData[this.getChartCounts(var5)]);
      int var8 = 0;
      int var9 = 0;
      int var10 = 0;
      int var11 = 0;

      for (int var12 = 2; var12 < var5.length; var12++) {
         String[] var13 = var5[var12].split(";");
         String var14 = var13[0];
         if (var14.equals("S")) {
            var6 = new StockChartData();
            var6.setTitle(var13[1]);
            if (var13.length > 2) {
               try {
                  var6.setPrecision(Integer.parseInt(var13[2]));
               } catch (Exception var19) {
               }
            }

            this.data.getChartData()[var8] = var6;
            var6.setIndicators(new Indicator[this.getIndicatorsCount(var5, var12 + 1)]);
            var8++;
            var10 = 0;
         } else if (var14.equals("I")) {
            var7 = new Indicator();
            var7.setTitle(var13[1]);
            var7.setSubChart(Boolean.parseBoolean(var13[2]));
            var7.setLines(new IndicatorLine[this.getIndicatorLineCount(var5, var12 + 1)]);
            var7.setLevels(new IndicatorLevel[this.getLevelCount(var5, var12 + 1)]);
            var9 = 0;
            var11 = 0;
            var6.getIndicators()[var10++] = var7;
         } else if (var14.equals("L")) {
            IndicatorLine var15 = new IndicatorLine();
            var15.setLine(var13[1]);
            var15.setColor(var13[2]);
            var15.setKind(ChartKind.valueOf(var13[3]));
            if (var13.length > 4) {
               var15.setShowInChart("1".equals(var13[4]));
            }

            var7.getLines()[var9++] = var15;
            if (var13.length > 5) {
               try {
                  var15.setShowZeroValues(Boolean.valueOf(var13[5]));
               } catch (Exception var20) {
                  Log.error("Error while parsin showZeroValues: " + var13[5]);
               }
            }
         } else if (var14.equals("IL")) {
            String var21 = var13[1];
            String var16 = var13[2];
            String var17 = var13[3];
            IndicatorLevel var18 = new IndicatorLevel();
            var18.setColor(var21);
            var18.setPattern(var17);
            var18.setValue(Double.parseDouble(var16));
            var7.getLevels()[var11++] = var18;
         }
      }

      var3.close();
   }

   private int getIndicatorLineCount(String[] var1, int var2) {
      int var3 = 0;

      for (int var4 = var2; var4 < var1.length; var4++) {
         String var5 = var1[var4];
         if (var5.startsWith("L")) {
            var3++;
         } else if (!var5.startsWith("IL")) {
            break;
         }
      }

      return var3;
   }

   private int getLevelCount(String[] var1, int var2) {
      int var3 = 0;

      for (int var4 = var2; var4 < var1.length; var4++) {
         String var5 = var1[var4];
         if (!var5.startsWith("IL;")) {
            break;
         }

         var3++;
      }

      return var3;
   }

   private int getIndicatorsCount(String[] var1, int var2) {
      int var3 = 0;

      for (int var4 = var2; var4 < var1.length; var4++) {
         String var5 = var1[var4];
         if (var5.startsWith("I;")) {
            var3++;
         } else if (var5.startsWith("S;")) {
            break;
         }
      }

      return var3;
   }

   private int getChartCounts(String[] var1) {
      int var2 = 0;

      for (String var6 : var1) {
         if (var6.startsWith("S;")) {
            var2++;
         }
      }

      return var2;
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

   private void loadIds(JarFile var1, boolean var2) throws IOException {
      JarEntry var3 = var1.getJarEntry(this.path + "/ids.dat");
      InputStream var4 = var1.getInputStream(var3);
      byte[] var5 = this.getBytes(var4);
      long var6 = var5.length / 8;
      byte var8 = 0;
      Integer var9 = null;
      LinkedList var10 = new LinkedList();

      for (long var11 = 0L; var11 < var6; var11++) {
         Long var13 = ByteBuffer.wrap(var5, var8, 8).getLong();
         var8 += 8;
         if (var2) {
            int var14 = SQTime.getFullYear(var13);
            int var15 = this.data.getPreviewYVals().length;
            if (var9 == null || var14 != var9) {
               int var16 = (int)(var11 * var15 / var6);
               PreviewCaption var17 = new PreviewCaption(var16, String.valueOf(var14));
               var10.add(var17);
               var9 = var14;
            }
         }

         this.timestamps.add(var13);
         this.timestampToIdMap.put(var13, var11);
         this.idToTimestampMap.put(var11, var13);
      }

      if (var2) {
         this.data.setPreviewCaptions(var10.toArray(new PreviewCaption[0]));
      }

      var4.close();
   }

   private void loadPreview(JarFile var1) throws IOException {
      JarEntry var2 = var1.getJarEntry(this.path + "/preview.dat");
      InputStream var3 = var1.getInputStream(var2);
      byte[] var4 = this.getBytes(var3);
      ByteBuffer.wrap(var4);
      byte var5 = 0;
      int var6 = ByteBuffer.wrap(var4, var5, 4).getInt();
      var5 = 4;
      int[] var7 = new int[var6];

      for (int var8 = 0; var8 < var6; var8++) {
         var7[var8] = ByteBuffer.wrap(var4, var5, 4).getInt();
         var5 += 4;
      }

      this.data.setPreviewTrades(var7);
      int var12 = (var4.length - (1 + var6) * 4) / 4;
      float[] var9 = new float[var12];

      for (int var10 = 0; var10 < var12; var10++) {
         var9[var10] = ByteBuffer.wrap(var4, var5, 4).getFloat();
         var5 += 4;
      }

      this.data.setPreviewYVals(var9);
      var3.close();
   }

   private static class ChartInfo {
      private final List<Long> xvals = new LinkedList<>();
      private final List<Double> open = new LinkedList<>();
      private final List<Double> close = new LinkedList<>();
      private final List<Double> low = new LinkedList<>();
      private final List<Double> high = new LinkedList<>();
      private final List[] indicators;

      public ChartInfo(int var1) {
         this.indicators = new List[var1];

         for (int var2 = 0; var2 < var1; var2++) {
            this.indicators[var2] = new LinkedList();
         }
      }

      public long[] getXvalsArray() {
         long[] var1 = new long[this.xvals.size()];
         int var2 = 0;

         for (Long var4 : this.xvals) {
            var1[var2++] = var4;
         }

         return var1;
      }

      public List<Long> getXvals() {
         return this.xvals;
      }

      public Float[] getIndicatorArray(int var1, IndicatorLine var2) {
         return this.getSafeFloatArrayFromFloat(this.indicators[var1], var2);
      }

      private Float[] getSafeFloatArrayFromFloat(List<Float> var1, IndicatorLine var2) {
         Float[] var3 = new Float[var1.size()];
         int var4 = 0;

         for (Float var6 : var1) {
            Float var7;
            if (var6.equals(Float.NaN)) {
               var7 = null;
            } else {
               var7 = var6;
               if (var7.floatValue() == 0.0 && !var2.isShowZeroValues()) {
                  var7 = null;
               }
            }

            var3[var4++] = var7;
         }

         return var3;
      }

      public List<Double> getOpen() {
         return this.open;
      }

      public List<Double> getClose() {
         return this.close;
      }

      public List<Double> getLow() {
         return this.low;
      }

      public List<Double> getHigh() {
         return this.high;
      }

      public List[] getIndicators() {
         return this.indicators;
      }
   }
}
