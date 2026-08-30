package com.strategyquant.datalib;

import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.settings.IXMLAble;
import java.io.Serializable;
import org.jdom2.Element;

public class DataInfo implements IXMLAble, Serializable {
   public int id;
   public int sourceDataId;
   public String connection;
   public String symbol;
   public String originalSymbol;
   public String instrument;
   public String timeframe;
   public String filename;
   public long dateFrom = 0L;
   public long dateTo = 0L;
   public int rows = 0;
   public long secondsRecords;
   public int totalDays;
   public int barTimeType;
   public InstrumentInfo symbolInfo;
   public int decimals;
   public String timezone;
   public int source;
   public String uSymbol;
   public String uSymbolName;
   public boolean removeWeekends;
   public boolean show;
   public String dateFromStr;
   public String dateToStr;
   public int basketId;
   public int brokerId = -1;
   public long fileHash;

   public Element getXML() {
      Element var1 = new Element("DataInfo");
      XMLUtil.trySetAttr(var1, "connection", this.connection);
      XMLUtil.trySetAttr(var1, "symbol", this.symbol);
      XMLUtil.trySetAttr(var1, "originalSymbol", this.originalSymbol);
      XMLUtil.trySetAttr(var1, "id", this.id);
      XMLUtil.trySetAttr(var1, "sourceDataId", this.sourceDataId);
      XMLUtil.trySetAttr(var1, "instrument", this.instrument);
      XMLUtil.trySetAttr(var1, "timeframe", this.timeframe);
      XMLUtil.trySetAttr(var1, "timezone", this.timezone);
      XMLUtil.trySetAttr(var1, "filename", this.filename);
      XMLUtil.trySetAttr(var1, "dateFrom", String.valueOf(this.dateFrom));
      XMLUtil.trySetAttr(var1, "dateTo", String.valueOf(this.dateTo));
      XMLUtil.trySetAttr(var1, "rows", String.valueOf(this.rows));
      XMLUtil.trySetAttr(var1, "totalDays", String.valueOf(this.totalDays));
      XMLUtil.trySetAttr(var1, "barType", String.valueOf(this.barTimeType));
      XMLUtil.trySetAttr(var1, "decimals", String.valueOf(this.decimals));
      XMLUtil.trySetAttr(var1, "source", String.valueOf(this.source));
      XMLUtil.trySetAttr(var1, "uSymbol", String.valueOf(this.uSymbol));
      XMLUtil.trySetAttr(var1, "uSymbolName", String.valueOf(this.uSymbolName));
      XMLUtil.trySetAttr(var1, "removeWeekends", String.valueOf(this.removeWeekends));
      XMLUtil.trySetAttr(var1, "fileHash", String.valueOf(this.fileHash));
      XMLUtil.trySetAttr(var1, "basketId", this.basketId);
      XMLUtil.trySetAttr(var1, "brokerId", this.brokerId);
      if (this.symbolInfo != null) {
         Element var2 = this.symbolInfo.getXML();
         var1.addContent(var2);
      }

      return var1;
   }

   public void setFromXML(Element var1) {
      this.connection = var1.getAttributeValue("connection");
      this.symbol = var1.getAttributeValue("symbol");
      this.originalSymbol = var1.getAttributeValue("originalSymbol");
      this.instrument = var1.getAttributeValue("instrument");
      this.timeframe = var1.getAttributeValue("timeframe");
      this.filename = var1.getAttributeValue("filename");
      this.dateFrom = XMLUtil.tryGetLongAttr(var1, "dateFrom");
      this.dateTo = XMLUtil.tryGetLongAttr(var1, "dateTo");
      this.rows = XMLUtil.tryGetIntAttr(var1, "rows");
      this.totalDays = XMLUtil.tryGetIntAttr(var1, "totalDays");
      this.barTimeType = XMLUtil.tryGetIntAttr(var1, "barType");
      this.source = XMLUtil.tryGetIntAttr(var1, "source");

      try {
         this.decimals = XMLUtil.tryGetIntAttr(var1, "decimals");
      } catch (Exception var3) {
         this.decimals = 5;
      }

      this.timezone = var1.getAttributeValue("timezone");
      Element var2 = var1.getChild("InstrumentInfo");
      if (var2 != null) {
         this.symbolInfo = new InstrumentInfo();
         this.symbolInfo.setFromXML(var2);
      }

      this.uSymbol = var1.getAttributeValue("uSymbol");
      this.uSymbolName = var1.getAttributeValue("uSymbolName");
      this.removeWeekends = XMLUtil.tryGetBoolAttr(var1, "removeWeekends");
      this.fileHash = XMLUtil.tryGetLongAttr(var1, "fileHash");
      this.id = XMLUtil.getIntAttr(var1, "id", -1);
      this.sourceDataId = XMLUtil.getIntAttr(var1, "sourceDataId", -1);
      this.basketId = XMLUtil.getIntAttr(var1, "basketId", -1);
      this.brokerId = XMLUtil.getIntAttr(var1, "brokerId", -1);
   }

   public DataInfo clone() {
      DataInfo var1 = new DataInfo();
      var1.connection = this.connection;
      var1.symbol = this.symbol;
      var1.originalSymbol = this.originalSymbol;
      var1.instrument = this.instrument;
      var1.timeframe = this.timeframe;
      var1.filename = this.filename;
      var1.dateFrom = this.dateFrom;
      var1.dateTo = this.dateTo;
      var1.rows = this.rows;
      var1.secondsRecords = this.secondsRecords;
      var1.totalDays = this.totalDays;
      var1.barTimeType = this.barTimeType;
      var1.symbolInfo = this.symbolInfo != null ? this.symbolInfo.clone() : null;
      var1.decimals = this.decimals;
      var1.timezone = this.timezone;
      var1.source = this.source;
      var1.uSymbol = this.uSymbol;
      var1.uSymbolName = this.uSymbolName;
      var1.removeWeekends = this.removeWeekends;
      var1.fileHash = this.fileHash;
      var1.basketId = this.basketId;
      var1.brokerId = this.brokerId;
      return var1;
   }

   @Override
   public String toString() {
      return "DataInfo [symbol=" + this.symbol + ", instrument=" + this.instrument + ", timeframe=" + this.timeframe + ", filename=" + this.filename + "]";
   }
}
