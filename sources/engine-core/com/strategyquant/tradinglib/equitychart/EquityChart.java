package com.strategyquant.tradinglib.equitychart;

import com.strategyquant.lib.L;
import com.strategyquant.lib.utils.JsonCreator;
import com.strategyquant.tradinglib.PlTypes;
import java.util.ArrayList;
import org.json.JSONObject;

public class EquityChart {
   public static final String PORTFOLIO_ONLY = "Portfolio only";
   public static final String PORTFOLIO_PARTS_ONLY = "Portfolio parts only";
   private byte plType = 10;
   private String domainAxisType = "trade";
   private boolean hideYTitle = false;
   public static final String EQUITY_AREA_FILL_COLOR = "#DAF5FF";
   public static final String EQUITY_AREA_FILL_NEGATIVE_COLOR = "#F49C9C";
   public static final String EQUITY_AREA_FILL_COLOR_DARK = "#333C63";
   private ArrayList<MainChartDataset> mainChartDatasets = new ArrayList<>();
   private ArrayList<SubChart> subCharts = new ArrayList<>();
   private ArrayList<AreaFill> areas = new ArrayList<>();
   private ArrayList<Marker> markers = new ArrayList<>();
   private ArrayList<Text> texts = new ArrayList<>();
   private Zoom zoom = new Zoom();
   public BenchmarkTable benchmark = new BenchmarkTable();
   private String cornerCaption = null;

   public void zoom(boolean var1, int var2, long var3, long var5) {
      this.zoom.active = var1;
      this.zoom.width = var2;
      this.zoom.minX = var3;
      this.zoom.maxX = var5;
   }

   public void hideYTitle(boolean var1) {
      this.hideYTitle = var1;
   }

   public void setPLType(byte var1) {
      this.plType = var1;
   }

   public void setDomainAxisType(String var1) {
      this.domainAxisType = var1;
   }

   public void addMainChartDataset(MainChartDataset var1) {
      this.setColor(var1, this.mainChartDatasets.size());
      this.mainChartDatasets.add(var1);
   }

   public ArrayList<MainChartDataset> getMainChartDatasets() {
      return this.mainChartDatasets;
   }

   public SubChart findSubChartByName(String var1) {
      for (int var2 = 0; var2 < this.subCharts.size(); var2++) {
         SubChart var3 = this.subCharts.get(var2);
         if (var3.name.equals(var1)) {
            return var3;
         }
      }

      return null;
   }

   public SubChart findSubChartByKey(String var1) {
      for (int var2 = 0; var2 < this.subCharts.size(); var2++) {
         SubChart var3 = this.subCharts.get(var2);
         if (var3.key.equals(var1)) {
            return var3;
         }
      }

      return null;
   }

   public void addSubChart(SubChart var1) {
      this.subCharts.add(var1);
   }

   public void addArea(AreaFill var1) {
      this.areas.add(var1);
   }

   public void addMarker(Marker var1) {
      this.markers.add(var1);
   }

   private void setColor(ChartDataset var1, int var2) {
      if (var1.color == null) {
         var1.color = EquityChartUtils.getColor(var2, false);
      }

      if (var1.colorDark == null) {
         var1.colorDark = EquityChartUtils.getColor(var2, true);
      }
   }

   public void reset() {
      this.mainChartDatasets.clear();
      this.subCharts.clear();
      this.areas.clear();
      this.markers.clear();
      this.texts.clear();
      this.zoom.reset();
      this.benchmark.clear();
   }

   public void fillJSON(JsonCreator var1) {
      var1.put("domainAxisType", this.domainAxisType.equals("time") ? "time" : "number", true);
      if (this.zoom != null && this.zoom.minX > 0L && this.zoom.active) {
         var1.putBeginObject("zoom");
         var1.put("startZoomX", this.zoom.minX, true);
         var1.put("currentZoomX", this.zoom.maxX, false);
         var1.endObject(true);
      } else {
         var1.putRaw("zoom", null, true);
      }

      var1.putBeginObject("mainCharts");
      String var2 = String.format(L.tsq("Equity (%s)"), PlTypes.print(this.plType));
      var1.put("title", var2, true);
      var1.put("weight", 60, true);
      if (this.cornerCaption != null) {
         var1.put("cornerCaption", this.cornerCaption, true);
      }

      var1.put("hideYTitle", this.hideYTitle, true);
      var1.putBeginArray("datasets");

      for (int var3 = 0; var3 < this.mainChartDatasets.size(); var3++) {
         var1.beginObject();
         ChartDataset var4 = this.mainChartDatasets.get(var3);
         var4.fillJSON(var1, this.zoom, this.domainAxisType.equals("time"));
         var1.endObject(var3 < this.mainChartDatasets.size() - 1);
      }

      var1.endArray(true);
      var1.putBeginArray("markers");

      for (int var5 = 0; var5 < this.markers.size(); var5++) {
         Marker var9 = this.markers.get(var5);
         var1.beginObject();
         var9.fillJSON(var1);
         var1.endObject(var5 < this.markers.size() - 1);
      }

      var1.endArray(true);
      var1.putBeginArray("texts");

      for (int var6 = 0; var6 < this.texts.size(); var6++) {
         Text var10 = this.texts.get(var6);
         var1.beginObject();
         var10.fillJSON(var1);
         var1.endObject(var6 < this.texts.size() - 1);
      }

      var1.endArray(true);
      var1.putBeginArray("areaFill");

      for (int var7 = 0; var7 < this.areas.size(); var7++) {
         AreaFill var11 = this.areas.get(var7);
         var1.beginObject();
         var11.fillJSON(var1);
         var1.endObject(var7 < this.areas.size() - 1);
      }

      var1.endArray(false);
      var1.endObject(true);
      var1.putBeginObject("subCharts");
      var1.putBeginArray("charts");

      for (int var8 = 0; var8 < this.subCharts.size(); var8++) {
         SubChart var12 = this.subCharts.get(var8);
         var1.beginObject();
         var12.fillJSON(var1, this.zoom, this.domainAxisType.equals("time"));
         var1.endObject(var8 < this.subCharts.size() - 1);
      }

      var1.endArray(false);
      var1.endObject(true);
      var1.putBeginObject("benchmark");
      var1.putRaw("data", this.benchmark.toJSON(), false);
      var1.endObject(false);
   }

   public JSONObject toJSON() {
      JsonCreator var1 = new JsonCreator();
      var1.beginObject();
      this.fillJSON(var1);
      var1.endObject(false);
      return new JSONObject(var1.toJson());
   }

   public String getCornerCaption() {
      return this.cornerCaption;
   }

   public void setCornerCaption(String var1) {
      this.cornerCaption = var1;
   }

   public void drawText(String var1, String var2, String var3, int var4, int var5) {
      this.texts.add(new Text(var1, var2, var3, var4, var5));
   }
}
