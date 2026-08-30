package com.strategyquant.tradinglib;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.InstrumentInfo;
import com.strategyquant.datalib.data.DataException;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.settings.IXMLAble;
import com.strategyquant.lib.utils.ISQCloneable;
import com.strategyquant.tradinglib.connection.Connection;
import com.strategyquant.tradinglib.connection.ConnectionManager;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import org.jdom2.Element;

public class ChartSetup implements IXMLAble, Serializable, ISQCloneable<ChartSetup> {
   private ArrayList<ChartDef> charts = new ArrayList<>();
   private boolean used = false;
   private double minDistance;
   private int testPrecision;
   private boolean realSpread = false;
   private CommissionsMethod commissionsMethod;
   private int backtestEngine;

   public ChartSetup() {
   }

   public ChartSetup(String var1, String var2, String var3, int var4, long var5, String var7) throws DataException {
      ChartDef var8 = new ChartDef(var1, var2, var3, var4, var5, var7);
      this.charts.add(var8);
   }

   public ChartSetup(String var1, String var2, String var3, long var4, long var6, double var8, String var10) throws DataException {
      ChartDef var11 = new ChartDef(var1, var2, var3, var4, var6, var8, var10);
      this.charts.add(var11);
   }

   public ChartSetup(String var1, String var2, long var3, long var5, double var7, String var9) throws DataException {
      this("History", var1, var2, var3, var5, var7, var9);
   }

   public ChartDef addChart(String var1, String var2, String var3, double var4) throws DataException {
      if (this.checkChartExists(var1, var2, var3)) {
         throw new DataException(2, "All subcharts must have unique settings.");
      }

      ChartDef var6 = new ChartDef(
         var1, var2, var3, this.charts.get(0).getHistoryFrom(), this.charts.get(0).getHistoryTo(), var4, this.charts.get(0).getSession()
      );
      var6.setRealSpread(this.realSpread);
      this.charts.add(var6);
      return var6;
   }

   public ChartDef addChart(String var1, String var2, String var3) throws DataException {
      ChartDef var6 = this.getChartDef(var1, var2);
      double var4;
      if (var6 != null) {
         var4 = var6.getSpread();
         var6.setRealSpread(this.realSpread);
      } else {
         var4 = 0.0;
      }

      return this.addChart(var1, var2, var3, var4);
   }

   private ChartDef getChartDef(String var1, String var2) {
      for (ChartDef var4 : this.charts) {
         if (var4.getConnectionName().equalsIgnoreCase(var1) && var4.getSymbol().equalsIgnoreCase(var2)) {
            return var4;
         }
      }

      return null;
   }

   public void registerConnectionSymbols(ConnectionManager var1) throws Exception {
      for (ChartDef var3 : this.charts) {
         var1.registerConnectionAndSymbol(var3);
      }
   }

   public void checkConnectionSymbolExist(ConnectionManager var1) throws DataException {
      for (ChartDef var3 : this.charts) {
         Connection var4 = var1.getConnection(var3.getConnectionName());
         if (!var4.isDataFeed()) {
            throw new DataException(1, "Connection '" + var4.getConnectionName() + "' is not a datafeed !");
         }

         if (!var4.getDataFeed().checkSymbolIsSupported(var3.getSymbol())) {
            throw new DataException(2, "Data '" + var3.getSymbol() + "' is not supported in connection '" + var4.getConnectionName() + "' !");
         }
      }
   }

   public ArrayList<ChartDef> getCharts() {
      return this.charts;
   }

   private boolean checkChartExists(String var1, String var2, String var3) {
      for (ChartDef var5 : this.charts) {
         if (var5.getConnectionName().equalsIgnoreCase(var1) && var5.getSymbol().equalsIgnoreCase(var2) && var5.getTimeframe().equalsIgnoreCase(var3)) {
            return true;
         }
      }

      return false;
   }

   public void runChecks(ConnectionManager var1) throws Exception {
      this.checkConnectionSymbolExist(var1);
      HashMap var2 = new HashMap();

      for (ChartDef var4 : this.charts) {
         int var5 = var4.getConnectionHash() + var4.getSymbolHash();
         if (!var2.containsKey(var5)) {
            var2.put(var5, var4.getSpread());
         } else {
            double var6 = (Double)var2.get(var5);
            if (var6 != var4.getSpread()) {
               var4.setSpread(var6);
            }
         }
      }

      HashMap var8 = new HashMap();

      for (ChartDef var10 : this.charts) {
         int var11 = var10.getConnectionHash() + var10.getSymbolHash();
         if (!var8.containsKey(var11)) {
            var8.put(var11, var10.getSession());
         } else {
            String var7 = (String)var8.get(var11);
            if (!var7.equals(var10.getSession())) {
               var10.setSession(var7);
            }
         }
      }
   }

   public int getChartsCount() {
      return this.charts.size();
   }

   public ChartSetup getClone() {
      ChartSetup var1 = new ChartSetup();
      var1.used = false;
      var1.minDistance = this.minDistance;
      var1.testPrecision = this.testPrecision;
      var1.backtestEngine = this.backtestEngine;
      if (this.charts != null) {
         var1.charts = new ArrayList<>();

         for (ChartDef var3 : this.charts) {
            var1.charts.add(var3.getClone());
         }
      }

      return var1;
   }

   public ChartSetup getClone(long var1, long var3) {
      ChartSetup var5 = new ChartSetup();
      var5.used = false;
      var5.minDistance = this.minDistance;
      var5.testPrecision = this.testPrecision;
      var5.realSpread = this.realSpread;
      var5.backtestEngine = this.backtestEngine;
      if (this.charts != null) {
         var5.charts = new ArrayList<>();

         for (ChartDef var7 : this.charts) {
            var5.charts.add(var7.getClone(var1, var3));
         }
      }

      return var5;
   }

   public ChartSetup getClone(String var1) {
      ChartSetup var2 = new ChartSetup();
      var2.used = false;
      var2.minDistance = this.minDistance;
      var2.testPrecision = this.testPrecision;
      var2.realSpread = this.realSpread;
      var2.backtestEngine = this.backtestEngine;
      if (this.charts != null) {
         var2.charts = new ArrayList<>();

         for (int var3 = 0; var3 < this.charts.size(); var3++) {
            ChartDef var4 = this.charts.get(var3);
            if (var3 == 0) {
               var2.charts.add(var4.getClone(var1));
            } else {
               var2.charts.add(var4.getClone());
            }
         }
      }

      return var2;
   }

   public boolean isUsed() {
      return this.used;
   }

   public void setUsed() {
      this.used = true;
   }

   public boolean isCreatedUsingBackload() {
      ChartDef var1 = this.charts.get(0);
      return var1.getBackloadType() != 0;
   }

   public ChartDef getMainChart() {
      return this.charts.get(0);
   }

   public Element getXML() {
      Element var1 = new Element("ChartSetup");
      var1.setAttribute("use", String.valueOf(this.used));

      for (ChartDef var3 : this.charts) {
         var1.addContent(var3.getXML());
      }

      return var1;
   }

   public void setFromXML(Element var1) {
      this.used = XMLUtil.tryGetBoolAttr(var1, "use");
      this.charts.clear();

      for (Element var3 : var1.getChildren("ChartDef")) {
         ChartDef var4 = new ChartDef();
         var4.setFromXML(var3);
         this.charts.add(var4);
      }
   }

   public void setMinDistance(double var1) {
      this.minDistance = var1;
      if (this.charts.size() > 0) {
         this.getMainChart().setMinDistance(var1);
      }
   }

   public void setTestPrecision(int var1) {
      this.testPrecision = var1;
      this.realSpread = var1 == 4;

      for (ChartDef var3 : this.charts) {
         var3.setRealSpread(this.realSpread);
      }
   }

   public int getTestPrecision() {
      return this.testPrecision;
   }

   public void addAllCharts(ChartSetup var1) {
      for (int var2 = 0; var2 < var1.charts.size(); var2++) {
         ChartDef var3 = var1.charts.get(var2);
         this.addChart(var3);
      }
   }

   public void addChart(ChartDef var1) {
      for (int var2 = 0; var2 < this.charts.size(); var2++) {
         ChartDef var3 = this.charts.get(var2);
         if (var1.getSymbolHash() == var3.getSymbolHash() && var1.getConnectionHash() == var3.getConnectionHash()) {
            return;
         }
      }

      this.charts.add(var1.getClone());
   }

   public CommissionsMethod getCommissionsMethod() {
      return this.commissionsMethod;
   }

   public void setCommissionsMethod(CommissionsMethod var1) {
      this.commissionsMethod = var1;
   }

   public int getBacktestEngine() {
      return this.backtestEngine;
   }

   public void setBacktestEngine(int var1) {
      this.backtestEngine = var1;
   }

   public String getTFsAsString() {
      if (this.charts.size() == 0) {
         return "";
      }

      StringBuilder var1 = new StringBuilder(this.charts.get(0).getTimeframe());
      int var2 = this.charts.get(0).getSymbolHash();

      for (int var3 = 1; var3 < this.charts.size(); var3++) {
         ChartDef var4 = this.charts.get(var3);
         if (var4.getSymbolHash() == var2) {
            var1.append(", ");
            var1.append(var4.getTimeframe());
         }
      }

      return var1.toString();
   }

   public String getSymbol() {
      ChartDef var1 = this.getMainChart();
      StringBuilder var2 = new StringBuilder(var1.getSymbol());
      if (this.charts.size() > 1) {
         for (int var3 = 1; var3 < this.charts.size(); var3++) {
            var1 = this.charts.get(var3);
            boolean var4 = false;

            for (int var5 = 0; var5 < var3; var5++) {
               ChartDef var6 = this.charts.get(var5);
               if (var6.getSymbol().equals(var1.getSymbol())) {
                  var4 = true;
                  break;
               }
            }

            if (!var4) {
               var2.append(",");
               var2.append(var1.getSymbol());
            }
         }
      }

      return var2.toString();
   }

   public String getTimeframe() {
      ChartDef var1 = this.getMainChart();
      StringBuilder var2 = new StringBuilder(var1.getTimeframe());
      if (this.charts.size() > 1) {
         for (int var3 = 1; var3 < this.charts.size(); var3++) {
            var1 = this.charts.get(var3);
            boolean var4 = false;

            for (int var5 = 0; var5 < var3; var5++) {
               ChartDef var6 = this.charts.get(var5);
               if (var6.getTimeframe().equals(var1.getTimeframe())) {
                  var4 = true;
                  break;
               }
            }

            if (!var4) {
               var2.append(",");
               var2.append(var1.getTimeframe());
            }
         }
      }

      return var2.toString();
   }

   public static ChartSetup createFirstAvailableData() throws Exception {
      ArrayList var0 = DataManager.list();
      if (var0 != null && var0.size() != 0) {
         DataInfo var1 = (DataInfo)var0.get(0);
         InstrumentInfo var2 = var1.symbolInfo;
         return new ChartSetup(var1.symbol, var1.timeframe, var1.dateFrom, var1.dateTo, var2.defaultSpread, "No Session");
      } else {
         return null;
      }
   }
}
