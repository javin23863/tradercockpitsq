package com.strategyquant.datalib;

import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.settings.IXMLAble;
import java.io.Serializable;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstrumentInfo implements IXMLAble, Serializable {
   public static final Logger Log = LoggerFactory.getLogger(InstrumentInfo.class);
   public String connection;
   public String instrument;
   public int broker = -1;
   public String description;
   public double tickSize;
   public double tickStep;
   public double tickValueInMoney;
   public String filename;
   public long dateFrom = 0L;
   public long dateTo = 0L;
   public int rows;
   public int totalDays;
   public double defaultSpread;
   public double defaultSlippage;
   public double minDistance;
   public int decimals;
   public String timeframe;
   public double pointValue;
   public int dataType;
   public boolean recognizedFromOrders = false;
   public String alias;
   public String exchange;
   public String country;
   public String sector;
   public String commissions;
   public String swap;
   public double orderSizeMultiplier = 1.0;
   public double orderSizeStep = 0.0;

   public InstrumentInfo() {
   }

   public InstrumentInfo(Element var1) {
      this.setFromXML(var1);
   }

   public Element getXML() {
      Element var1 = new Element("InstrumentInfo");
      XMLUtil.trySetAttr(var1, "connection", this.connection);
      XMLUtil.trySetAttr(var1, "instrument", this.instrument);
      XMLUtil.trySetAttr(var1, "description", this.description);
      XMLUtil.trySetAttr(var1, "tickSize", String.valueOf(this.tickSize));
      XMLUtil.trySetAttr(var1, "tickStep", String.valueOf(this.tickStep));
      XMLUtil.trySetAttr(var1, "minDistance", String.valueOf(this.minDistance));
      XMLUtil.trySetAttr(var1, "tickValueInMoney", String.valueOf(this.tickValueInMoney));
      XMLUtil.trySetAttr(var1, "filename", this.filename);
      XMLUtil.trySetAttr(var1, "dateFrom", String.valueOf(this.dateFrom));
      XMLUtil.trySetAttr(var1, "dateTo", String.valueOf(this.dateTo));
      XMLUtil.trySetAttr(var1, "rows", String.valueOf(this.rows));
      XMLUtil.trySetAttr(var1, "totalDays", String.valueOf(this.totalDays));
      XMLUtil.trySetAttr(var1, "defaultSpread", String.valueOf(this.defaultSpread));
      XMLUtil.trySetAttr(var1, "defaultSlippage", String.valueOf(this.defaultSlippage));
      XMLUtil.trySetAttr(var1, "decimals", String.valueOf(this.decimals));
      XMLUtil.trySetAttr(var1, "timeframe", this.timeframe);
      XMLUtil.trySetAttr(var1, "commissions", String.valueOf(this.commissions));
      XMLUtil.trySetAttr(var1, "pointValue", String.valueOf(this.pointValue));
      XMLUtil.trySetAttr(var1, "dataType", String.valueOf(this.dataType));
      XMLUtil.trySetAttr(var1, "recognizedFromOrders", String.valueOf(this.recognizedFromOrders));
      XMLUtil.trySetAttr(var1, "alias", this.alias);
      XMLUtil.trySetAttr(var1, "exchange", this.exchange);
      XMLUtil.trySetAttr(var1, "country", this.country);
      XMLUtil.trySetAttr(var1, "sector", this.sector);
      XMLUtil.trySetAttr(var1, "swap", String.valueOf(this.swap));
      XMLUtil.trySetAttr(var1, "orderSizeMultiplier", String.valueOf(this.orderSizeMultiplier));
      XMLUtil.trySetAttr(var1, "orderSizeStep", String.valueOf(this.orderSizeStep));
      XMLUtil.trySetAttr(var1, "broker", String.valueOf(this.broker));
      return var1;
   }

   public void setFromXML(Element var1) {
      this.connection = this.internIfNotNull(var1.getAttributeValue("connection"));
      this.instrument = this.internIfNotNull(var1.getAttributeValue("instrument"));
      this.description = this.internIfNotNull(var1.getAttributeValue("description"));
      this.tickSize = XMLUtil.tryGetDoubleAttr(var1, "tickSize");
      this.tickStep = XMLUtil.tryGetDoubleAttr(var1, "tickStep");
      this.minDistance = XMLUtil.getDoubleAttr(var1, "minDistance", 0.0);
      this.tickValueInMoney = XMLUtil.tryGetDoubleAttr(var1, "tickValueInMoney");
      this.filename = this.internIfNotNull(var1.getAttributeValue("filename"));
      this.dateFrom = XMLUtil.tryGetLongAttr(var1, "dateFrom");
      this.dateTo = XMLUtil.tryGetLongAttr(var1, "dateTo");
      this.rows = XMLUtil.tryGetIntAttr(var1, "rows");
      this.totalDays = XMLUtil.tryGetIntAttr(var1, "totalDays");
      this.defaultSpread = XMLUtil.tryGetDoubleAttr(var1, "defaultSpread");
      this.defaultSlippage = XMLUtil.getDoubleAttr(var1, "defaultSlippage", 0.0);
      this.decimals = XMLUtil.tryGetIntAttr(var1, "decimals");
      this.timeframe = this.internIfNotNull(var1.getAttributeValue("timeframe"));
      this.commissions = this.internIfNotNull(var1.getAttributeValue("commissions"));
      this.pointValue = XMLUtil.tryGetDoubleAttr(var1, "pointValue");
      this.dataType = XMLUtil.tryGetIntAttr(var1, "dataType");
      this.recognizedFromOrders = XMLUtil.tryGetBoolAttr(var1, "recognizedFromOrders");
      this.alias = this.internIfNotNull(var1.getAttributeValue("alias"));
      this.exchange = this.internIfNotNull(var1.getAttributeValue("exchange"));
      this.country = this.internIfNotNull(var1.getAttributeValue("country"));
      this.sector = this.internIfNotNull(var1.getAttributeValue("sector"));
      this.swap = this.internIfNotNull(var1.getAttributeValue("swap"));
      this.orderSizeMultiplier = XMLUtil.getDoubleAttr(var1, "orderSizeMultiplier", 1.0);
      this.orderSizeStep = XMLUtil.getDoubleAttr(var1, "orderSizeStep", 0.0);
      this.broker = XMLUtil.getIntAttr(var1, "broker", -1);
   }

   private String internIfNotNull(String var1) {
      return var1 == null ? null : var1.intern();
   }

   public InstrumentInfo clone() {
      InstrumentInfo var1 = new InstrumentInfo();
      var1.connection = this.connection;
      var1.instrument = this.instrument;
      var1.description = this.description;
      var1.tickSize = this.tickSize;
      var1.tickStep = this.tickStep;
      var1.tickValueInMoney = this.tickValueInMoney;
      var1.filename = this.filename;
      var1.dateFrom = this.dateFrom;
      var1.dateTo = this.dateTo;
      var1.rows = this.rows;
      var1.totalDays = this.totalDays;
      var1.defaultSpread = this.defaultSpread;
      var1.defaultSlippage = this.defaultSlippage;
      var1.decimals = this.decimals;
      var1.timeframe = this.timeframe;
      var1.commissions = this.commissions;
      var1.pointValue = this.pointValue;
      var1.dataType = this.dataType;
      var1.recognizedFromOrders = this.recognizedFromOrders;
      var1.alias = this.alias;
      var1.exchange = this.exchange;
      var1.country = this.country;
      var1.sector = this.sector;
      var1.swap = this.swap;
      var1.orderSizeMultiplier = this.orderSizeMultiplier;
      var1.orderSizeStep = this.orderSizeStep;
      var1.minDistance = this.minDistance;
      var1.broker = this.broker;
      return var1;
   }
}
