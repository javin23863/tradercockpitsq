package com.strategyquant.tradinglib.data;

import com.strategyquant.lib.L;
import com.strategyquant.lib.settings.IXMLAble;
import com.strategyquant.tradinglib.ChartSetup;
import com.strategyquant.tradinglib.strategy.OutOfSample;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TradingData implements IXMLAble {
   public static final Logger Log = LoggerFactory.getLogger("TradingData");
   private OutOfSample oosRanges = new OutOfSample();
   private ArrayList<ChartSetup> chartSetups = new ArrayList<>();
   private String databankName;
   private SimpleDateFormat df = new SimpleDateFormat();

   public TradingData() {
      this.df.applyPattern("dd.MM.yyyy");
      this.df.setLenient(true);
   }

   public void setFromXML(Element var1) throws Exception {
      this.chartSetups.clear();
      this.oosRanges.clear();
      Element var2 = var1.getChild("Setups");
      if (var2 == null) {
         throw new Exception(L.t("Cannot load Data setups. XML element Setups not found.", new Object[0]));
      }

      List var3 = var2.getChildren("Setup");
      if (var3 != null) {
         for (Element var5 : var3) {
            ChartSetup var6 = this.createChartSetup(var5);
            if (var6 != null) {
               this.chartSetups.add(var6);
            }
         }
      }

      Element var7 = var1.getChild("OutOfSample");
      if (var7 == null) {
         throw new Exception(L.t("Cannot load Data OOS periods. XML element OutOfSample not found.", new Object[0]));
      }

      List var8 = var7.getChildren("Range");
      if (var8 != null) {
         for (Element var10 : var8) {
            this.oosRanges.addRange(var10);
         }
      }
   }

   public Element getXML() {
      Element var1 = new Element("Data");
      Element var2 = new Element("Setups");

      for (ChartSetup var4 : this.chartSetups) {
         var2.addContent(var4.getXML());
      }

      var1.addContent(var2);
      new Element("OutOfSample");
      var1.addContent(this.oosRanges.getXML());
      return var1;
   }

   private ChartSetup createChartSetup(Element var1) throws Exception {
      if (!var1.getAttribute("use").getValue().equals("true")) {
         return null;
      }

      List var2 = var1.getChildren("Chart");
      if (var2.size() < 1) {
         throw new Exception(L.t("No charts defined for a setup!", new Object[0]));
      }

      Element var3 = (Element)var2.get(0);
      String var4 = var3.getAttributeValue("symbol");
      String var5 = var3.getAttributeValue("timeframe");
      if (!var4.equals("not set") && !var5.equals("not set")) {
         ChartSetup var6 = new ChartSetup(
            "History",
            var4,
            var5,
            this.recognizeDate(var1.getAttributeValue("dateFrom")),
            this.recognizeDate(var1.getAttributeValue("dateTo")),
            Double.parseDouble(var3.getAttributeValue("spread")),
            var1.getAttributeValue("session")
         );

         for (int var7 = 1; var7 < var2.size(); var7++) {
            Element var8 = (Element)var2.get(var7);

            try {
               var6.addChart("History", var8.getAttributeValue("symbol"), var8.getAttributeValue("timeframe"));
            } catch (Exception var10) {
               Log.error("Cannot add chart. ", var10);
            }
         }

         return var6;
      } else {
         return null;
      }
   }

   private long recognizeDate(String var1) throws ParseException {
      Date var2 = this.df.parse(var1);
      return var2.getTime();
   }

   public OutOfSample getOOSRanges() {
      return this.oosRanges;
   }

   public void setOosRanges(OutOfSample var1) {
      this.oosRanges = var1;
   }

   public ArrayList<ChartSetup> getChartSetups() {
      return this.chartSetups;
   }

   public void setChartSetups(ArrayList<ChartSetup> var1) {
      this.chartSetups = var1;
   }

   public String getDatabankName() {
      return this.databankName;
   }

   public void setDatabankName(String var1) {
      this.databankName = var1;
   }
}
