package com.strategyquant.tradinglib.stockchart;

import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.blocks.random.BlockDefinition;
import com.strategyquant.tradinglib.blocks.random.BlocksConfig;
import com.strategyquant.tradinglib.engine.TradingSetup;
import com.strategyquant.tradinglib.indicator.IndicatorBase;
import com.strategyquant.tradinglib.indicator.IndicatorsCache;
import com.strategyquant.tradinglib.strategy.MarketData;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jdom2.JDOMException;

public class StockSaverOriginalFormat {
   private static final String[] COLORS = new String[]{"red", "green", "blue", "brown", "orange"};
   private static final int PREVIEW_SIZE = 1960;
   private static final int MAX_SIZE = 1000;
   public static final int MAX_TRADES = 1000;
   private JarOutputStream jarOutStream;
   private OrdersList orders;
   private MarketData marketData;
   private IndicatorsCache indicatorsCache;
   private int total;
   private String path;

   public void save(JarOutputStream var1, TradingSetup var2, OrdersList var3, String var4) throws JDOMException, Exception {
      this.jarOutStream = var1;
      this.marketData = var2.getMarketData();
      this.indicatorsCache = var2.getStrategy().getIndicatorsCache();
      this.orders = var3;
      this.path = "Results/" + this.correctResultKey(var4);
      this.saveIds();
      this.saveMetadata();
      this.savePreview();
      this.saveStockData();
      this.saveTrades();
   }

   private int getIdsCount() throws TradingException {
      int var1 = 0;
      int var2 = this.marketData.getChartData().length;
      int[] var3 = new int[var2];

      for (int var4 = 0; var4 < var2; var4++) {
         var3[var4] = this.marketData.getChartData()[var4].Time.size() - 1;
      }

      while (true) {
         long var10 = -1L;

         for (int var6 = 0; var6 < var2; var6++) {
            int var7 = var3[var6];
            if (var7 != -1) {
               long var8 = this.marketData.getChartData()[var6].Time.get(var7);
               if (var10 == -1L || var8 < var10) {
                  var10 = var8;
               }
            }
         }

         if (var10 == -1L) {
            return var1;
         }

         var1++;

         for (int var11 = 0; var11 < var2; var11++) {
            if (var3[var11] != -1) {
               long var12 = this.marketData.getChartData()[var11].Time.get(var3[var11]);
               if (var12 == var10) {
                  var3[var11]--;
               }
            }
         }
      }
   }

   private void writeIdsToBuffer(ByteBuffer var1) throws TradingException {
      int var2 = this.marketData.getChartData().length;
      int[] var3 = new int[var2];

      for (int var4 = 0; var4 < var2; var4++) {
         var3[var4] = this.marketData.getChartData()[var4].Time.size() - 1;
      }

      while (true) {
         long var10 = -1L;

         for (int var6 = 0; var6 < var2; var6++) {
            int var7 = var3[var6];
            if (var7 != -1) {
               long var8 = this.marketData.getChartData()[var6].Time.get(var7);
               if (var10 == -1L || var8 < var10) {
                  var10 = var8;
               }
            }
         }

         if (var10 == -1L) {
            return;
         }

         var1.putLong(var10);

         for (int var11 = 0; var11 < var2; var11++) {
            if (var3[var11] != -1) {
               long var12 = this.marketData.getChartData()[var11].Time.get(var3[var11]);
               if (var12 == var10) {
                  var3[var11]--;
               }
            }
         }
      }
   }

   private void saveIds() throws IOException, TradingException {
      this.total = this.getIdsCount();
      this.createEntry("ids.dat");
      ByteBuffer var1 = ByteBuffer.allocate(8 * this.total);
      this.writeIdsToBuffer(var1);
      BufferedOutputStream var2 = new BufferedOutputStream(this.jarOutStream);
      var2.write(var1.array());
      var2.flush();
      this.jarOutStream.closeEntry();
   }

   private void createEntry(String var1) throws IOException {
      JarEntry var2 = new JarEntry(this.path + "/" + var1);
      this.jarOutStream.putNextEntry(var2);
   }

   private void saveCount(BufferedWriter var1, long var2) throws IOException {
      var1.write(var2 + "");
      var1.write(System.getProperty("line.separator"));
   }

   private void saveFromTo(ByteBuffer var1, long var2, long var4) throws IOException {
      var1.putLong(var2);
      var1.putLong(var4);
   }

   private float getFloat(double var1) {
      return (float)((float)var1 * 10000.0 / 10000.0);
   }

   private void saveMetadata() throws JDOMException, Exception {
      this.createEntry("stock_desc.csv");
      BufferedWriter var1 = new BufferedWriter(new OutputStreamWriter(this.jarOutStream, "US-ASCII"));
      this.saveCount(var1, this.total);
      this.saveCount(var1, this.orders.size());
      int var2 = 0;
      BlocksConfig var3 = BlocksConfig.getConfig();

      for (ChartData var7 : this.marketData.getChartData()) {
         int var8 = var7.getInstrumentInfo().decimals;
         var1.write("S;" + var7.Symbol + "/" + var7.Timeframe + ";" + var8 + System.getProperty("line.separator"));

         for (IndicatorBase var11 : this.indicatorsCache.getIndicatorsForThisData(var7)) {
            BlockDefinition var12 = var3.getBlock(var11.getClass().getSimpleName());
            boolean var13 = var12.returnType != 2;
            String var14 = IndicatorDisplayFormatter.format(var11, var12);
            var1.write("I;" + var14 + ";" + var13 + System.getProperty("line.separator"));
            ArrayList var15 = var11.getOutputLines();

            for (int var16 = 0; var16 < var15.size(); var16++) {
               DataSeries var17 = (DataSeries)var15.get(var16);
               String var18 = var17.getColor();
               if (var18 == null) {
                  var18 = COLORS[var2 % COLORS.length];
               }

               String var19 = ((DataSeries)var15.get(var16)).isShowInChart() ? "1" : "0";
               String var20 = var17.getChartType() != null ? var17.getChartType() : ChartKind.line.name();
               boolean var21 = var17.isShowZeroValues();
               var1.write(
                  "L;"
                     + ((DataSeries)var15.get(var16)).getLineName()
                     + ";"
                     + var18
                     + ";"
                     + var20
                     + ";"
                     + var19
                     + ";"
                     + var21
                     + System.getProperty("line.separator")
               );
               var2++;
            }
         }
      }

      var1.flush();
      this.jarOutStream.closeEntry();
   }

   private void saveStockData() throws IOException, TradingException {
      int var1 = 0;

      try {
         for (ChartData var5 : this.marketData.getChartData()) {
            ArrayList var6 = this.indicatorsCache.getIndicatorsForThisData(var5);
            this.saveChartData(String.valueOf(var1), var5, var6);
            var1++;
         }
      } catch (IllegalArgumentException | IllegalAccessException var7) {
         throw new TradingException("Cannot create chart data, error; " + var7.getMessage(), var7);
      }
   }

   private void writeChartDataPart(String var1, ChartData var2, ArrayList<IndicatorBase> var3, int var4, int var5) throws TradingException, IOException, IllegalArgumentException, IllegalAccessException {
      this.createEntry("chartdata_" + var1 + ".dat");
      BufferedOutputStream var6 = new BufferedOutputStream(this.jarOutStream);
      int var7 = this.getLinesCount(var3);
      int var8 = (var5 - var4 + 1) * ((4 + var7) * 4 + 8) + 16;
      ByteBuffer var9 = ByteBuffer.allocate(var8);
      this.saveFromTo(var9, var2.Time.get(var5), var2.Time.get(var4));

      for (int var10 = var5; var10 >= var4; var10--) {
         long var11 = var2.Time.get(var10);
         var9.putLong(var11);
         double var13 = var2.Open.get(var10);
         var9.putFloat(this.getFloat(var13));
         double var15 = var2.Close.get(var10);
         var9.putFloat(this.getFloat(var15));
         double var17 = var2.Low.get(var10);
         var9.putFloat(this.getFloat(var17));
         double var19 = var2.High.get(var10);
         var9.putFloat(this.getFloat(var19));

         for (IndicatorBase var22 : var3) {
            ArrayList var23 = var22.getOutputLines();

            for (int var24 = 0; var24 < var23.size(); var24++) {
               DataSeries var25 = (DataSeries)var23.get(var24);
               double var26 = 0.0;
               if (var10 < var25.size()) {
                  var26 = var25.get(var10);
               } else {
                  var26 = var25.get(var25.size() - 1);
               }

               float var28 = this.getFloat(var26);
               var9.putFloat(var28);
            }
         }
      }

      var6.write(var9.array());
      var6.flush();
      this.jarOutStream.closeEntry();
   }

   private int getLinesCount(ArrayList<IndicatorBase> var1) {
      int var2 = 0;

      for (IndicatorBase var4 : var1) {
         var2 += var4.getOutputsCount();
      }

      return var2;
   }

   private void saveChartData(String var1, ChartData var2, ArrayList<IndicatorBase> var3) throws TradingException, IOException, IllegalArgumentException, IllegalAccessException {
      var2.Time.setShift(0);
      var2.Open.setShift(0);
      var2.High.setShift(0);
      var2.Low.setShift(0);
      var2.Close.setShift(0);
      var2.Volume.setShift(0);

      for (IndicatorBase var5 : var3) {
         int var6 = var5.getOutputsCount();

         for (int var7 = 0; var7 < var6; var7++) {
            var5.setDataShift(var7, 0);
         }
      }

      int var9 = var2.Time.size();
      int var10 = var9 / 1000;
      if (var9 % 1000 != 0) {
         var10++;
      }

      int var11 = var9 - 1;

      for (int var12 = 0; var12 < var10; var12++) {
         int var8 = var11 - 1000 + 1;
         if (var8 < 0) {
            var8 = 0;
         }

         this.writeChartDataPart(var1 + "_" + var12, var2, var3, var8, var11);
         var11 = var8 - 1;
      }
   }

   private void saveTrades() throws IOException {
      int var1 = 0;
      String[] var2 = new String[this.marketData.getChartData().length];

      for (ChartData var6 : this.marketData.getChartData()) {
         this.saveTrades(String.valueOf(var1), var6.Symbol, var2);
         var2[var1] = var6.Symbol;
         var1++;
      }
   }

   private List<Long> getTradeTimes() {
      LinkedList var1 = new LinkedList();
      if (this.orders != null && this.orders.size() != 0) {
         for (int var2 = 0; var2 < this.orders.size(); var2++) {
            Order var3 = this.orders.get(var2);
            var1.add(var3.OpenTime);
            var1.add(var3.CloseTime);
         }

         return var1;
      } else {
         return var1;
      }
   }

   private boolean skipTrade(Order var1, String var2, String[] var3) {
      if (var1.Type != 1 && var1.Type != 2) {
         return true;
      }

      if (!var2.equals(var1.Symbol)) {
         return true;
      }

      for (String var7 : var3) {
         if (var7 != null && var7.equals(var1.Symbol)) {
            return true;
         }
      }

      return false;
   }

   private boolean shouldShowControlOrders() {
      boolean var1 = false;

      try {
         var1 = Boolean.parseBoolean(MainApp.settings().get("showControlOrders", "false"));
      } catch (Exception var3) {
      }

      return var1;
   }

   private void saveTrades(String var1, String var2, String[] var3) throws IOException {
      if (this.orders != null && this.orders.size() != 0) {
         boolean var4 = this.shouldShowControlOrders();
         int var5 = 0;

         for (int var6 = 0; var6 < this.orders.size(); var5++) {
            char var7 = 'ꀸ';
            ByteBuffer var8 = ByteBuffer.allocate(var7);
            var8.position(16);
            int var9 = 0;
            long var10 = -1L;
            long var12 = -1L;

            while (var9 < 1000 && var6 < this.orders.size()) {
               Order var14 = this.orders.get(var6);
               var6++;
               if (!this.skipTrade(var14, var2, var3) && (var4 || var14.isRealOrder())) {
                  byte var15 = var14.Type;
                  boolean var16;
                  if (var15 == 1) {
                     var16 = false;
                  } else {
                     var16 = true;
                  }

                  var8.putLong(var14.Ticket);
                  var8.putLong(var14.OpenTime);
                  var8.putLong(var14.CloseTime);
                  var8.putFloat(this.getFloat(var14.OpenPrice));
                  var8.putFloat(this.getFloat(var14.ClosePrice));
                  var8.putFloat(this.getFloat(var14.StopLoss));
                  var8.putFloat(this.getFloat(var14.TakeProfit));
                  var8.put((byte)(var16 ? 1 : 0));
                  if (var14.OpenTime < var10 || var10 == -1L) {
                     var10 = var14.OpenTime;
                  }

                  if (var14.CloseTime > var12 || var12 == -1L) {
                     var12 = var14.CloseTime;
                  }

                  var9++;
               }
            }

            if (var12 != -1L && var10 != -1L) {
               var8.position(0);
               this.saveFromTo(var8, var10, var12);
               String var17 = var1 + "_" + var5;
               this.createEntry("trades_" + var17 + ".dat");
               BufferedOutputStream var18 = new BufferedOutputStream(this.jarOutStream);
               var18.write(var8.array());
               var18.flush();
               this.jarOutStream.closeEntry();
            }
         }
      }
   }

   private Pair<Long, Long> getMinMax(Trade[] var1, int var2, int var3) {
      Long var4 = null;
      Long var5 = null;

      for (int var6 = var2; var6 <= var3; var6++) {
         Trade var7 = var1[var6];
         if (var4 == null || var4 > var7.getOpenTime()) {
            var4 = var7.getOpenTime();
         }

         if (var5 == null || var5 < var7.getCloseTime()) {
            var5 = var7.getCloseTime();
         }
      }

      return new ImmutablePair(var4, var5);
   }

   private void savePreview() throws IOException, TradingException {
      this.createEntry("preview.dat");
      int var1 = this.marketData.getChartData()[0].Close.size();
      int var2;
      double var3;
      if (var1 < 1960) {
         var2 = var1;
         var3 = 1.0;
      } else {
         var2 = 1960;
         var3 = var1 / 1960.0;
      }

      List var5 = this.getTradeTimes();
      int var6 = var5.size();
      ByteBuffer var7 = ByteBuffer.allocate(4 * var2 + (1 + var6) * 4);
      var7.putInt(var6);
      ChartData var8 = this.marketData.getChartData()[0];
      int var9 = var8.Time.size();
      long var10 = var8.Time.get(var9 - 1);
      long var12 = var8.Time.get(0);
      long var14 = var12 - var10;
      double var16 = (double)var2 / var14;

      for (Long var19 : var5) {
         long var20 = var19 - var10;
         double var22 = var20 * var16;
         int var24 = (int)var22;
         var7.putInt(var24);
      }

      int var25 = 0;
      int var26 = 0;
      LinkedList var27 = new LinkedList();
      ChartData var21 = this.marketData.getChartData()[0];

      while (var25 < var1 && var26 < var2) {
         var26++;
         double var28 = var21.Open.get(var25);
         var27.add(this.getFloat(var28));
         var25 = (int)(var25 + var3);
      }

      Collections.reverse(var27);

      for (Float var23 : var27) {
         var7.putFloat(this.getFloat(var23.floatValue()));
      }

      BufferedOutputStream var30 = new BufferedOutputStream(this.jarOutStream);
      byte[] var31 = var7.array();
      var30.write(var31);
      var30.flush();
      this.jarOutStream.closeEntry();
   }

   private String correctResultKey(String var1) {
      return var1.replace("/", "_LOM_");
   }
}
