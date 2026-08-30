package com.strategyquant.datalib;

import com.strategyquant.datalib.bartype.BarType;
import com.strategyquant.datalib.bartype.BarTypeFactory;
import com.strategyquant.datalib.data.DataException;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.instrument.InstrumentManager;
import com.strategyquant.datalib.session.Session;
import com.strategyquant.datalib.session.SessionElement;
import com.strategyquant.datalib.session.SessionManager;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.settings.IXMLAble;
import com.strategyquant.lib.utils.ISQCloneable;
import java.io.Serializable;
import java.util.ArrayList;
import org.jdom2.Element;

public class ChartDef implements IXMLAble, ISQCloneable<ChartDef>, Serializable {
   protected static final int LIVETRADING = 1;
   protected static final int BACKTEST = 2;
   protected String connectionName;
   protected int connectionNameHash;
   protected String symbol = null;
   protected int symbolHash;
   protected String instrument = null;
   protected String timeframe;
   protected long historyFrom;
   protected long historyTo;
   protected double spread = 0.0;
   protected boolean registered = false;
   protected String session;
   protected DataInfo dataInfo = null;
   protected int type = 0;
   protected long loadedHistoryFrom;
   private int backloadType;
   private long backloadNumber;
   private InstrumentInfo instrumentInfo;
   private double minDistance;
   private boolean realSpread = false;

   public ChartDef() {
   }

   public ChartDef(String var1, String var2, String var3, int var4, long var5, String var7) throws DataException {
      this.type = 1;
      this.connectionName = var1;
      this.connectionNameHash = var1.hashCode();
      this.symbol = var2;
      this.symbolHash = var2.hashCode();
      this.dataInfo = DataManager.getDataInfo(var1, var2);
      if (this.dataInfo == null) {
         throw new DataException(2, "Data for connection '" + var1 + "' and symbol '" + var2 + "' cannot be found!");
      }

      this.instrumentInfo = InstrumentManager.getInstrumentInfo(this.dataInfo.instrument);
      this.timeframe = var3;
      if (var7.equals("No Session")) {
      }

      this.session = var7;
      this.backloadType = var4;
      this.backloadNumber = var5;
      this.minDistance = 0.0;
   }

   public ChartDef(String var1, String var2, String var3, long var4, long var6, double var8, String var10) throws DataException {
      BarTypeFactory.checkTimeframeIsValid(var3);
      this.type = 2;
      this.connectionName = var1;
      this.connectionNameHash = var1.hashCode();
      this.symbol = var2;
      this.symbolHash = var2.hashCode();
      this.dataInfo = DataManager.getDataInfo(var1, var2);
      if (this.dataInfo == null) {
         throw new DataException(2, "Data for connection '" + var1 + "' and symbol '" + var2 + "' cannot be found!");
      }

      if (this.dataInfo.barTimeType == 1 && var3.startsWith("D")) {
         var10 = "No Session";
      }

      this.instrumentInfo = InstrumentManager.getInstrumentInfo(this.dataInfo.instrument);
      if (var4 == 0L) {
         var4 = Long.MIN_VALUE;
      }

      this.timeframe = var3;
      this.historyFrom = var4;
      this.historyTo = SQTime.correctDayEnd(var6);
      this.loadedHistoryFrom = var4;
      this.spread = var8;
      if (var10.equals("No Session")) {
      }

      this.session = var10;
      this.backloadType = 0;
   }

   public ChartDef(ChartDef var1) throws DataException {
      this(var1.getConnectionName(), var1.getSymbol(), var1.getTimeframe(), var1.getHistoryFrom(), var1.getHistoryTo(), var1.getSpread(), var1.getSession());
      this.loadedHistoryFrom = var1.getLoadedHistoryFrom();
   }

   public ChartDef(ChartDef var1, String var2) throws DataException {
      this(var1.getConnectionName(), var1.getSymbol(), var2, var1.getHistoryFrom(), var1.getHistoryTo(), var1.getSpread(), var1.getSession());
      this.loadedHistoryFrom = var1.getLoadedHistoryFrom();
   }

   public String getConnectionName() {
      return this.connectionName;
   }

   public boolean isSymbolRecognized() {
      return this.symbol != null;
   }

   public String getTimeframe() {
      return this.timeframe;
   }

   public long getHistoryFrom() {
      return this.historyFrom;
   }

   public void setHistoryFrom(long var1) {
      this.historyFrom = var1;
   }

   public long getHistoryTo() {
      return this.historyTo;
   }

   public void setHistoryTo(long var1) {
      this.historyTo = var1;
   }

   public double getSpread() {
      return this.realSpread ? -1.0 : this.spread;
   }

   public void setSpread(double var1) {
      this.spread = var1;
   }

   public int getConnectionHash() {
      return this.connectionNameHash;
   }

   public void setSymbol(String var1) {
      this.symbol = var1;
   }

   public String getSymbol() {
      return this.symbol;
   }

   public int getSymbolHash() {
      return this.symbolHash;
   }

   public boolean isRegistered() {
      return this.registered;
   }

   public String getSession() {
      return this.session;
   }

   public void setRegistered() {
      this.registered = true;
   }

   public InstrumentInfo getSymbolInfo() {
      return this.instrumentInfo;
   }

   public String getInstrument() {
      return this.instrument;
   }

   public boolean isBacktestDataDef() {
      return this.type == 2;
   }

   public void setLoadedHistoryFrom(long var1) {
      this.loadedHistoryFrom = var1;
   }

   public long getLoadedHistoryFrom() {
      return this.loadedHistoryFrom;
   }

   public boolean isSameConnectionSymbol(ChartDef var1) {
      return this.getSymbolHash() != var1.getSymbolHash() ? false : this.getConnectionHash() == var1.getConnectionHash();
   }

   public int getBackloadType() {
      return this.backloadType;
   }

   public long getBackloadNumber() {
      return this.backloadNumber;
   }

   public Element getXML() {
      Element var1 = new Element("ChartDef");
      XMLUtil.trySetAttr(var1, "connectionName", this.connectionName);
      XMLUtil.trySetAttr(var1, "symbol", this.symbol);
      XMLUtil.trySetAttr(var1, "instrument", this.instrument);
      XMLUtil.trySetAttr(var1, "timeframe", this.timeframe);
      XMLUtil.trySetAttr(var1, "historyFrom", this.historyFrom);
      XMLUtil.trySetAttr(var1, "historyTo", this.historyTo);
      XMLUtil.trySetAttr(var1, "loadedHistoryFrom", this.loadedHistoryFrom);
      XMLUtil.trySetAttr(var1, "spread", this.spread);
      XMLUtil.trySetAttr(var1, "registered", this.registered);
      XMLUtil.trySetAttr(var1, "session", this.session);
      XMLUtil.trySetAttr(var1, "type", this.type);
      XMLUtil.trySetAttr(var1, "backloadType", this.backloadType);
      XMLUtil.trySetAttr(var1, "backloadNumber", this.backloadNumber);
      if (this.dataInfo != null) {
         Element var2 = this.dataInfo.getXML();
         var1.addContent(var2);
      }

      return var1;
   }

   public void setFromXML(Element var1) {
      this.connectionName = var1.getAttributeValue("connectionName");
      this.connectionNameHash = this.connectionName.hashCode();
      this.symbol = var1.getAttributeValue("symbol");
      this.symbolHash = this.symbol.hashCode();
      this.instrument = var1.getAttributeValue("instrument");
      this.timeframe = var1.getAttributeValue("timeframe");
      this.historyFrom = XMLUtil.tryGetLongAttr(var1, "historyFrom");
      this.historyTo = SQTime.correctDayEnd(XMLUtil.tryGetLongAttr(var1, "historyTo"));
      this.loadedHistoryFrom = XMLUtil.tryGetLongAttr(var1, "loadedHistoryFrom");
      this.spread = XMLUtil.tryGetDoubleAttr(var1, "spread");
      this.registered = XMLUtil.tryGetBoolAttr(var1, "registered");
      this.session = var1.getAttributeValue("session");
      this.type = XMLUtil.tryGetIntAttr(var1, "type");
      this.backloadType = XMLUtil.tryGetIntAttr(var1, "backloadType");
      this.backloadNumber = XMLUtil.tryGetLongAttr(var1, "backloadNumber");
      Element var2 = var1.getChild("DataInfo");
      if (var2 != null) {
         this.dataInfo = new DataInfo();
         this.dataInfo.setFromXML(var2);
      }
   }

   public int getBarTimeType() {
      return this.dataInfo.barTimeType;
   }

   public InstrumentInfo getInstrumentInfo() {
      return this.instrumentInfo;
   }

   public void setMinDistance(double var1) {
      this.minDistance = var1;
   }

   public double getMinDistance() {
      return this.minDistance;
   }

   public ChartDef getClone() {
      return this.getClone(0L, 0L);
   }

   public ChartDef getClone(long var1, long var3) {
      ChartDef var5 = new ChartDef();
      var5.connectionName = this.connectionName;
      var5.connectionNameHash = this.connectionNameHash;
      var5.symbol = this.symbol;
      var5.symbolHash = this.symbolHash;
      var5.instrument = this.instrument;
      var5.timeframe = this.timeframe;
      var5.historyFrom = var1 > 0L ? var1 : this.historyFrom;
      var5.historyTo = var3 > 0L ? var3 : this.historyTo;
      var5.spread = this.spread;
      var5.registered = this.registered;
      var5.session = this.session;
      var5.dataInfo = this.dataInfo != null ? this.dataInfo.clone() : null;
      var5.type = this.type;
      var5.loadedHistoryFrom = this.loadedHistoryFrom;
      var5.backloadType = this.backloadType;
      var5.backloadNumber = this.backloadNumber;
      var5.instrumentInfo = this.instrumentInfo != null ? this.instrumentInfo.clone() : null;
      var5.minDistance = this.minDistance;
      var5.realSpread = this.realSpread;
      return var5;
   }

   public ChartDef getClone(String var1) {
      ChartDef var2 = new ChartDef();
      var2.connectionName = this.connectionName;
      var2.connectionNameHash = this.connectionNameHash;
      var2.symbol = var1;
      var2.symbolHash = var1.hashCode();
      var2.instrument = this.instrument;
      var2.timeframe = this.timeframe;
      var2.historyFrom = this.historyFrom > 0L ? this.historyFrom : this.historyFrom;
      var2.historyTo = this.historyTo > 0L ? this.historyTo : this.historyTo;
      var2.spread = this.spread;
      var2.registered = this.registered;
      var2.session = this.session;
      var2.dataInfo = this.dataInfo != null ? this.dataInfo.clone() : null;
      var2.type = this.type;
      var2.loadedHistoryFrom = this.loadedHistoryFrom;
      var2.backloadType = this.backloadType;
      var2.backloadNumber = this.backloadNumber;
      var2.instrumentInfo = this.instrumentInfo != null ? this.instrumentInfo.clone() : null;
      var2.minDistance = this.minDistance;
      var2.realSpread = this.realSpread;
      return var2;
   }

   public String getChartsHash() {
      StringBuilder var1 = new StringBuilder(this.getSymbol());
      var1.append(this.getConnectionName());
      var1.append(this.getHistoryFrom());
      var1.append(this.getHistoryTo());
      var1.append(this.getSessionStr(this.getSession()));
      var1.append(this.getTimeframe());
      var1.append(this.getDataFileHash());
      return var1.toString();
   }

   private long getDataFileHash() {
      return this.dataInfo.fileHash;
   }

   private String getSessionStr(String var1) {
      Session var2 = SessionManager.getSession(var1);
      if (var2 == null) {
         return "NULL";
      }

      StringBuilder var3 = new StringBuilder();
      var3.append(var2.getSessionName());
      var3.append("_");
      ArrayList var4 = var2.getElements();
      if (var4 != null) {
         for (int var5 = 0; var5 < var4.size(); var5++) {
            SessionElement var6 = (SessionElement)var4.get(var5);

            try {
               var3.append(var6.getDayFromStr());
            } catch (Exception var11) {
               var3.append("N/A");
            }

            try {
               var3.append(var6.getTimeFromStr());
            } catch (Exception var10) {
               var3.append("N/A");
            }

            try {
               var3.append(var6.getDayToStr());
            } catch (Exception var9) {
               var3.append("N/A");
            }

            try {
               var3.append(var6.getTimeToStr());
            } catch (Exception var8) {
               var3.append("N/A");
            }

            var3.append(var6.isEOD() ? "Y" : "N");
         }
      } else {
         var3.append("NONE");
      }

      return var3.toString();
   }

   public boolean canBeComputedFrom(ChartDef var1) throws DataException {
      if (this.getSymbolHash() != var1.getSymbolHash()) {
         return false;
      }

      if (this.getConnectionHash() != var1.getConnectionHash()) {
         return false;
      }

      BarType var2 = BarTypeFactory.getBarType(this.getTimeframe(), this.getBarTimeType());
      return var2.checkCanBeComputedFrom(var1.getTimeframe()) == null;
   }

   public void modifySpread(double var1) {
      this.spread = var1;
   }

   public void setRealSpread(boolean var1) {
      this.realSpread = var1;
   }

   public boolean isSmallerThan(ChartDef var1) throws DataException {
      if (this.getSymbolHash() != var1.getSymbolHash()) {
         return false;
      }

      if (this.getConnectionHash() != var1.getConnectionHash()) {
         return false;
      }

      BarType var2 = BarTypeFactory.getBarType(this.getTimeframe(), this.getBarTimeType());
      BarType var3 = BarTypeFactory.getBarType(var1.getTimeframe(), this.getBarTimeType());
      return var2.getPeriodInMS() < var3.getPeriodInMS();
   }

   public void setSession(String var1) {
      this.session = var1;
   }
}
