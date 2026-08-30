package com.strategyquant.tradinglib.stockchart;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.utils.JsonCreator;
import java.util.List;
import java.util.stream.Stream;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class StockData {
   private static final DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
   private StockChartData[] chartData;
   private float[] previewYVals;
   private PreviewCaption[] previewCaptions;
   private int[] previewTrades;
   private String[] stocks;
   private String currentStock;
   private long count;
   private long areaFrom;
   private long areaTo;
   private Double pipValue;
   private long tradesCount;
   private Long selectedTrade;

   public long getAreaFrom() {
      return this.areaFrom;
   }

   public void setAreaFrom(long var1) {
      this.areaFrom = var1;
   }

   public long getAreaTo() {
      return this.areaTo;
   }

   public void setAreaTo(long var1) {
      this.areaTo = var1;
   }

   public StockChartData[] getChartData() {
      return this.chartData;
   }

   public void setChartData(StockChartData[] var1) {
      this.chartData = var1;
   }

   public float[] getPreviewYVals() {
      return this.previewYVals;
   }

   public void setPreviewYVals(float[] var1) {
      this.previewYVals = var1;
   }

   public long getCount() {
      return this.count;
   }

   public void setCount(long var1) {
      this.count = var1;
   }

   private String getTimestampStr(long var1) {
      return formatter.print(var1);
   }

   public String toJSON() {
      JsonCreator var1 = new JsonCreator();
      var1.beginObject();
      var1.put("count", this.count, true);
      var1.put("selectedTradeId", this.selectedTrade, true);
      var1.put("totalTrades", this.tradesCount, true);
      var1.put("pipValue", this.pipValue, true);
      var1.put("area_from", this.areaFrom, true);
      var1.put("area_to", this.areaTo, true);
      var1.put("stocks");
      var1.beginArray();
      if (this.stocks != null) {
         for (int var2 = 0; var2 < this.stocks.length; var2++) {
            var1.setValue(this.stocks[var2], var2 < this.stocks.length - 1);
         }
      }

      var1.endArray(true);
      var1.put("currentStock", this.currentStock, true);
      var1.putBeginObject("preview");
      if (this.previewCaptions != null) {
         var1.putBeginArray("captions");

         for (int var13 = 0; var13 < this.previewCaptions.length; var13++) {
            PreviewCaption var3 = this.previewCaptions[var13];
            var1.beginObject();
            var1.put("index", var3.getIndex(), true);
            var1.put("caption", var3.getCaption(), false);
            var1.endObject(var13 != this.previewCaptions.length - 1);
         }

         var1.endArray(true);
      }

      var1.put("yVals", this.previewYVals, true);
      var1.put("trades");
      var1.beginArray();
      if (this.previewTrades != null) {
         int var14 = 0;

         while (var14 < this.previewTrades.length) {
            var1.beginObject();
            var1.put("from", this.previewTrades[var14], true);
            var1.put("to", this.previewTrades[++var14], false);
            var14++;
            var1.endObject(var14 < this.previewTrades.length);
         }
      }

      var1.endArray(false);
      var1.endObject(true);
      var1.putBeginArray("indicators");
      int var16 = 0;

      for (int var17 = 0; var17 < this.chartData.length; var17++) {
         StockChartData var4 = this.chartData[var17];
         if (var4.getIndicators() != null) {
            for (int var5 = 0; var5 < var4.getIndicators().length; var5++) {
               if (var16 != 0) {
                  var1.separator();
               }

               Indicator var6 = var4.getIndicators()[var5];
               var1.beginObject();
               var1.put("id", "indicator" + var16, true);
               var1.put("title", var6.getTitle(), true);
               var1.put("displayOn", var6.isSubChart() ? "subchart" : "chart", true);
               var1.put("subchartID", String.valueOf(var17), var6.getLevels() != null);
               if (var6.getLevels() != null) {
                  var1.putBeginArray("levels");

                  for (int var7 = 0; var7 < var6.getLevels().length; var7++) {
                     IndicatorLevel var8 = var6.getLevels()[var7];
                     var1.beginObject();
                     var1.put("color", var8.getColor(), true);
                     boolean var9 = var8.getPattern() != null && !var8.getPattern().isEmpty();
                     var1.put("value", var8.getValue(), var9);
                     if (var9) {
                        String[] var10 = var8.getPattern().split(",");
                        int[] var11 = new int[var10.length];

                        for (int var12 = 0; var12 < var10.length; var12++) {
                           var11[var12] = Integer.parseInt(var10[var12]);
                        }

                        var1.put("pattern", var11, false);
                     }

                     var1.endObject(var7 != var6.getLevels().length - 1);
                  }

                  var1.endArray(false);
               }

               var1.separator();
               var1.putBeginArray("datasets");
               if (var6.getLines() != null) {
                  List var24 = Stream.of(var6.getLines()).filter(var0 -> var0.isShowInChart()).toList();

                  for (int var26 = 0; var26 < var24.size(); var26++) {
                     IndicatorLine var27 = (IndicatorLine)var24.get(var26);
                     var1.beginObject();
                     var1.put("line", var27.getLine(), true);
                     var1.put("color", var27.getColor(), true);
                     var1.put("type", var27.getKind().name(), true);
                     var1.put("yVals", var27.getyValues(), false);
                     var1.endObject(var26 != var24.size() - 1);
                  }
               }

               var1.endArray(false);
               var1.endObject(false);
               var16++;
            }
         }
      }

      var1.endArray(true);
      var1.putBeginArray("charts");

      for (int var18 = 0; var18 < this.chartData.length; var18++) {
         StockChartData var19 = this.chartData[var18];
         var1.beginObject();
         var1.put("id", String.valueOf(var18), true);
         var1.put("title", var19.getTitle(), true);
         var1.put("precision", var19.getPrecision(), true);
         int var20 = var18 == 0 ? 2 : 1;
         var1.put("weight", var20, true);
         var1.putBeginArray("xVals");
         if (var19.getxVals() != null) {
            for (int var21 = 0; var21 < var19.getxVals().length; var21++) {
               var1.setValue(this.getTimestampStr(var19.getxVals()[var21]), var21 < var19.getxVals().length - 1);
            }
         }

         var1.endArray(true);
         var1.putBeginArray("yVals");
         if (var19.getxVals() != null) {
            for (int var22 = 0; var22 < var19.getxVals().length; var22++) {
               var1.beginObject();
               var1.put("open", SQUtils.round(var19.getOpen().get(var22), var19.getPrecision()), true);
               var1.put("close", SQUtils.round(var19.getClose().get(var22), var19.getPrecision()), true);
               var1.put("low", SQUtils.round(var19.getLow().get(var22), var19.getPrecision()), true);
               var1.put("high", SQUtils.round(var19.getHigh().get(var22), var19.getPrecision()), false);
               var1.endObject(var22 != var19.getxVals().length - 1);
            }
         }

         var1.endArray(true);
         var1.putBeginArray("tradeMarkers");
         if (var19.getTrades() != null) {
            for (int var23 = 0; var23 < var19.getTrades().length; var23++) {
               Trade var25 = var19.getTrades()[var23];
               var1.beginObject();
               var1.put("id", String.valueOf(var25.getId()), true);
               var1.put("open", this.getTimestampStr(var25.getOpenTime()), true);
               var1.put("close", this.getTimestampStr(var25.getCloseTime()), true);
               var1.put("openValue", SQUtils.round(var25.getOpenValue(), var19.getPrecision()), true);
               var1.put("closeValue", SQUtils.round(var25.getCloseValue(), var19.getPrecision()), true);
               var1.put("slValue", SQUtils.round(var25.getSLValue(), var19.getPrecision()), true);
               var1.put("ptValue", SQUtils.round(var25.getPTValue(), var19.getPrecision()), true);
               var1.put("realOpen", this.getTimestampStr(var25.getRealOpenTime()), true);
               var1.put("realClose", this.getTimestampStr(var25.getRealCloseTime()), true);
               var1.put("long", !var25.isShortTrade(), true);
               var1.put("index", var25.getIndex(), false);
               var1.endObject(var23 != var19.getTrades().length - 1);
            }
         }

         var1.endArray(false);
         var1.endObject(var18 != this.chartData.length - 1);
      }

      var1.endArray(false);
      var1.endObject(false);
      return var1.toJson();
   }

   public PreviewCaption[] getPreviewCaptions() {
      return this.previewCaptions;
   }

   public void setPreviewCaptions(PreviewCaption[] var1) {
      this.previewCaptions = var1;
   }

   public Double getPipValue() {
      return this.pipValue;
   }

   public void setPipValue(Double var1) {
      this.pipValue = var1;
   }

   public int[] getPreviewTrades() {
      return this.previewTrades;
   }

   public void setPreviewTrades(int[] var1) {
      this.previewTrades = var1;
   }

   public long getTradesCount() {
      return this.tradesCount;
   }

   public void setTradesCount(long var1) {
      this.tradesCount = var1;
   }

   public Long getSelectedTrade() {
      return this.selectedTrade;
   }

   public void setSelectedTrade(Long var1) {
      this.selectedTrade = var1;
   }

   public String[] getStocks() {
      return this.stocks;
   }

   public void setStocks(String[] var1) {
      this.stocks = var1;
   }

   public String getCurrentStock() {
      return this.currentStock;
   }

   public void setCurrentStock(String var1) {
      this.currentStock = var1;
   }
}
