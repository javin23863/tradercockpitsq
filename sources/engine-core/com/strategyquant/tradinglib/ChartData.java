package com.strategyquant.tradinglib;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.InstrumentInfo;
import com.strategyquant.datalib.TickEvent;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.datalib.bartype.BarType;
import com.strategyquant.datalib.bartype.BarTypeFactory;
import com.strategyquant.datalib.bartype.BarTypeStatus;
import com.strategyquant.datalib.bartype.impl.FuturesTimeBar;
import com.strategyquant.datalib.data.DataException;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.dataseries.ComputedDataSeries;
import com.strategyquant.datalib.dataseries.MedianDataSeries;
import com.strategyquant.datalib.dataseries.TimeDataSeries;
import com.strategyquant.datalib.dataseries.TypicalDataSeries;
import com.strategyquant.datalib.dataseries.WeightedDataSeries;
import com.strategyquant.datalib.session.Session;
import com.strategyquant.datalib.session.SessionManager;
import com.strategyquant.datalib.session.SessionStatus;
import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.debug.Debugger;
import com.strategyquant.tradinglib.strategy.MarketData;
import java.io.Serializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChartData extends Debugger implements Serializable {
   public static final Logger Log = LoggerFactory.getLogger("ChartData");
   public static final int ChartTF_D1 = 1;
   public static final int ChartTF_W1 = 2;
   public static final int ChartTF_M1 = 3;
   public TimeDataSeries Time;
   public DataSeries Open;
   public DataSeries High;
   public DataSeries Low;
   public DataSeries Close;
   public ComputedDataSeries Median;
   public ComputedDataSeries Typical;
   public ComputedDataSeries Weighted;
   protected TimeDataSeries TimeD;
   protected DataSeries OpenD;
   protected DataSeries HighD;
   protected DataSeries LowD;
   protected DataSeries CloseD;
   protected TimeDataSeries TimeW;
   protected DataSeries OpenW;
   protected DataSeries HighW;
   protected DataSeries LowW;
   protected DataSeries CloseW;
   protected TimeDataSeries TimeM;
   protected DataSeries OpenM;
   protected DataSeries HighM;
   protected DataSeries LowM;
   protected DataSeries CloseM;
   private double dailyHigh;
   private double dailyLow;
   private double weeklyHigh;
   private double weeklyLow;
   private double monthlyHigh;
   private double monthlyLow;
   public DataSeries Volume;
   public MarketData MarketData;
   public String Connection;
   public String Symbol;
   public String Instrument;
   public String Timeframe;
   protected String session;
   protected int requestedEventType = 4;
   protected int connectionHash;
   protected int symbolHash;
   private boolean updated;
   protected BarType barType;
   protected InstrumentInfo SymbolInfo;
   private final BarTypeStatus barTypeStatus = new BarTypeStatus();
   private long TimeCurrent;
   private double Bid;
   private double Ask;
   protected ChartDef chartDef;
   public int EventType = 0;
   private ChartData connectedObject = null;
   private double barHigh;
   private double barLow;
   private double barVolume;
   private long nextDayTick = Long.MIN_VALUE;
   private long nextWeekTick = Long.MIN_VALUE;
   private long nextMonthTick = Long.MIN_VALUE;
   private long currentBarTime = Long.MIN_VALUE;
   private long currentDayEnd = Long.MIN_VALUE;
   private long nextBarTime = Long.MIN_VALUE;
   private int nextBarIndex = -1;
   protected boolean hasOnTickRule = true;
   protected boolean hasDailyDataBlock = true;
   protected boolean hasWeeklyDataBlock = true;
   protected boolean hasMonthlyDataBlock = true;
   private int hashCode = -1;
   private int currentBar = -1;
   protected int serieIndex = 0;
   private int chartTF = 0;
   private Session sessionDaily;
   private Session sessionWeekly;
   private Session sessionMonthly;
   private SessionStatus sessionStatus = new SessionStatus();
   private boolean useSessionWhenComputingDWM = false;
   private int indyStartingBar = 0;
   private TickEvent backupTick = new TickEvent();

   public ChartData() {
      boolean var1 = true;
   }

   public ChartData(
      String var1,
      TimeDataSeries var2,
      DataSeries var3,
      DataSeries var4,
      DataSeries var5,
      DataSeries var6,
      DataSeries var7,
      TimeDataSeries var8,
      DataSeries var9,
      DataSeries var10,
      DataSeries var11,
      DataSeries var12,
      TimeDataSeries var13,
      DataSeries var14,
      DataSeries var15,
      DataSeries var16,
      DataSeries var17,
      TimeDataSeries var18,
      DataSeries var19,
      DataSeries var20,
      DataSeries var21,
      DataSeries var22
   ) {
      this.Timeframe = var1;
      this.Time = var2;
      if (this.Time != null) {
         this.Time.setName("Time");
      }

      this.Open = var3;
      if (this.Open != null) {
         this.Open.setName("Open");
      }

      this.High = var4;
      if (this.High != null) {
         this.High.setName("High");
      }

      this.Low = var5;
      if (this.Low != null) {
         this.Low.setName("Low");
      }

      this.Close = var6;
      if (this.Close != null) {
         this.Close.setName("Close");
      }

      this.Volume = var7;
      if (this.Volume != null) {
         this.Volume.setName("Volume");
      }

      this.TimeD = var8;
      if (this.TimeD != null) {
         this.TimeD.setName("TimeD");
      }

      this.OpenD = var9;
      if (this.OpenD != null) {
         this.OpenD.setName("OpenD");
      }

      this.HighD = var10;
      if (this.HighD != null) {
         this.HighD.setName("HighD");
      }

      this.LowD = var11;
      if (this.LowD != null) {
         this.LowD.setName("LowD");
      }

      this.CloseD = var12;
      if (this.CloseD != null) {
         this.CloseD.setName("CloseD");
      }

      this.TimeW = var13;
      if (this.TimeW != null) {
         this.TimeW.setName("TimeW");
      }

      this.OpenW = var14;
      if (this.OpenW != null) {
         this.OpenW.setName("OpenW");
      }

      this.HighW = var15;
      if (this.HighW != null) {
         this.HighW.setName("HighW");
      }

      this.LowW = var16;
      if (this.LowW != null) {
         this.LowW.setName("LowW");
      }

      this.CloseW = var17;
      if (this.CloseW != null) {
         this.CloseW.setName("CloseW");
      }

      this.TimeM = var18;
      if (this.TimeM != null) {
         this.TimeM.setName("TimeM");
      }

      this.OpenM = var19;
      if (this.OpenM != null) {
         this.OpenM.setName("OpenM");
      }

      this.HighM = var20;
      if (this.HighM != null) {
         this.HighM.setName("HighM");
      }

      this.LowM = var21;
      if (this.LowM != null) {
         this.LowM.setName("LowM");
      }

      this.CloseM = var22;
      if (this.CloseM != null) {
         this.CloseM.setName("CloseM");
      }

      this.initializeComputedDataSeries();
   }

   public ChartData(MarketData var1, ChartDef var2, int var3, int var4) throws Exception {
      if (var1 != null || var2 != null) {
         this.chartDef = var2;
         this.serieIndex = var3;
         this.MarketData = var1;
         this.Connection = var2.getConnectionName();
         this.Symbol = var2.getSymbol();
         this.Instrument = var2.getInstrument();
         this.connectionHash = var2.getConnectionHash();
         this.symbolHash = var2.getSymbolHash();
         this.Timeframe = var2.getTimeframe();
         this.session = var2.getSession();
         this.Time = new TimeDataSeries();
         this.Time.setName("Time", var2);
         if (var1 != null) {
            this.Time.setIndyStartingBar(var1.getIndyStartingBar());
         }

         this.Open = new DataSeries();
         this.Open.setName("Open", var2);
         if (var1 != null) {
            this.Open.setIndyStartingBar(var1.getIndyStartingBar());
         }

         this.High = new DataSeries();
         this.High.setName("High", var2);
         if (var1 != null) {
            this.High.setIndyStartingBar(var1.getIndyStartingBar());
         }

         this.Low = new DataSeries();
         this.Low.setName("Low", var2);
         if (var1 != null) {
            this.Low.setIndyStartingBar(var1.getIndyStartingBar());
         }

         this.Close = new DataSeries();
         this.Close.setName("Close", var2);
         if (var1 != null) {
            this.Close.setIndyStartingBar(var1.getIndyStartingBar());
         }

         this.Volume = new DataSeries();
         this.Volume.setName("Volume", var2);
         if (var1 != null) {
            this.Volume.setIndyStartingBar(var1.getIndyStartingBar());
         }

         this.SymbolInfo = var2.getSymbolInfo();
         this.barType = BarTypeFactory.getBarType(this.Timeframe, var2.getBarTimeType()).clone();
         String var5 = null;

         try {
            var5 = DataManager.getDataInfo("History", this.Symbol).timeframe;
         } catch (Exception var7) {
            Log.error("Cannot get original data timeframe");
         }

         if (this.barType instanceof FuturesTimeBar) {
            if (var4 == -659455871 || var4 == 395961824 || var4 == 1441180233) {
               ((FuturesTimeBar)this.barType).MetaTraderEngineUsed = true;
            } else if (var5 != null && var5.equals(this.Timeframe)) {
               ((FuturesTimeBar)this.barType).LoadAsIs = true;
            }
         }

         if (this.usesSameDailyDataSeries()) {
            this.TimeD = this.Time;
            this.OpenD = this.Open;
            this.HighD = this.High;
            this.LowD = this.Low;
            this.CloseD = this.Close;
            this.TimeW = this.Time;
            this.OpenW = this.Open;
            this.HighW = this.High;
            this.LowW = this.Low;
            this.CloseW = this.Close;
            this.TimeM = this.Time;
            this.OpenM = this.Open;
            this.HighM = this.High;
            this.LowM = this.Low;
            this.CloseM = this.Close;
         } else {
            this.TimeD = null;
            this.OpenD = null;
            this.HighD = null;
            this.LowD = null;
            this.CloseD = null;
            this.TimeW = null;
            this.OpenW = null;
            this.HighW = null;
            this.LowW = null;
            this.CloseW = null;
            this.TimeM = null;
            this.OpenM = null;
            this.HighM = null;
            this.LowM = null;
            this.CloseM = null;
         }

         this.initializeComputedDataSeries();
         if (var1 != null) {
            this.setIndyStartingBar(var1.getIndyStartingBar());
         }
      }
   }

   protected boolean usesSameDailyDataSeries() {
      return this.serieIndex > 0 && this.chartDef.getBarTimeType() == 2;
   }

   public int chartHashCode() {
      if (this.hashCode == -1) {
         this.hashCode = this.hashCode();
      }

      return this.hashCode;
   }

   protected void initializeFromMarketData(MarketData var1) {
      this.MarketData = var1;
      this.Connection = this.MarketData.Chart(0).Connection;
      this.Symbol = this.MarketData.Chart(0).Symbol;
      this.connectionHash = this.MarketData.Chart(0).getConnectionHash();
      this.symbolHash = this.MarketData.Chart(0).getSymbolHash();
      this.Timeframe = this.MarketData.Chart(0).Timeframe;
      this.Time = this.MarketData.Chart(0).Time;
      this.Time.setName("Time");
      this.Open = this.MarketData.Chart(0).Open;
      this.Open.setName("Open");
      this.High = this.MarketData.Chart(0).High;
      this.High.setName("High");
      this.Low = this.MarketData.Chart(0).Low;
      this.Low.setName("Low");
      this.Close = this.MarketData.Chart(0).Close;
      this.Close.setName("Close");
      this.Volume = this.MarketData.Chart(0).Volume;
      this.Volume.setName("Volume");
      this.TimeD = this.MarketData.Chart(0).TimeD;
      this.OpenD = this.MarketData.Chart(0).OpenD;
      this.HighD = this.MarketData.Chart(0).HighD;
      this.LowD = this.MarketData.Chart(0).LowD;
      this.CloseD = this.MarketData.Chart(0).CloseD;
      this.TimeW = this.MarketData.Chart(0).TimeW;
      this.OpenW = this.MarketData.Chart(0).OpenW;
      this.HighW = this.MarketData.Chart(0).HighW;
      this.LowW = this.MarketData.Chart(0).LowW;
      this.CloseW = this.MarketData.Chart(0).CloseW;
      this.TimeM = this.MarketData.Chart(0).TimeM;
      this.OpenM = this.MarketData.Chart(0).OpenM;
      this.HighM = this.MarketData.Chart(0).HighM;
      this.LowM = this.MarketData.Chart(0).LowM;
      this.CloseM = this.MarketData.Chart(0).CloseM;
      this.initializeComputedDataSeries();
      this.SymbolInfo = this.MarketData.Chart(0).SymbolInfo;
      this.barType = this.MarketData.Chart(0).barType;
      var1.Chart(0).addConnectedObject(this);
      this.setIndyStartingBar(var1.getIndyStartingBar());
   }

   protected void initializeComputedDataSeries() {
      this.Median = new MedianDataSeries(this.Open, this.High, this.Low, this.Close, this.Volume);
      this.Median.setName("Median", this.chartDef);
      this.Typical = new TypicalDataSeries(this.Open, this.High, this.Low, this.Close, this.Volume);
      this.Typical.setName("Typical", this.chartDef);
      this.Weighted = new WeightedDataSeries(this.Open, this.High, this.Low, this.Close, this.Volume);
      this.Weighted.setName("Weighted", this.chartDef);
   }

   private void addConnectedObject(ChartData var1) {
      if (this.connectedObject != null) {
         throw new IllegalArgumentException("Connected object must be null!");
      }

      this.connectedObject = var1;
   }

   public long TimeCurrent() throws TradingException {
      return this.TimeCurrent;
   }

   public long Time(int var1) throws TradingException {
      return this.Time.get(var1);
   }

   public long Time() throws TradingException {
      return this.Time(0);
   }

   public long Time(int var1, int var2) throws TradingException {
      return this.MarketData.Chart(var1).Time(var2);
   }

   public long TimeD(int var1) throws TradingException {
      return this.TimeD == null ? 0L : this.TimeD.get(var1);
   }

   public long TimeW(int var1) throws TradingException {
      return this.TimeW == null ? 0L : this.TimeW.get(var1);
   }

   public long TimeM(int var1) throws TradingException {
      return this.TimeM == null ? 0L : this.TimeM.get(var1);
   }

   public double Ask() throws TradingException {
      return this.Ask != 0.0 ? this.Ask : this.Close(0);
   }

   public double Bid() throws TradingException {
      return this.Bid != 0.0 ? this.Bid : this.Close(0);
   }

   public double Open(int var1) throws TradingException {
      return this.Open.get(var1);
   }

   public double Open(int var1, int var2) throws TradingException {
      return this.MarketData.Chart(var1).Open(var2);
   }

   public double OpenD(int var1) throws TradingException {
      return this.OpenD == null ? 0.0 : this.OpenD.get(var1);
   }

   public double OpenD(int var1, int var2) throws TradingException {
      return this.MarketData.Chart(var1).OpenD(var2);
   }

   public double OpenW(int var1) throws TradingException {
      return this.OpenW == null ? 0.0 : this.OpenW.get(var1);
   }

   public double OpenW(int var1, int var2) throws TradingException {
      return this.MarketData.Chart(var1).OpenW(var2);
   }

   public double OpenM(int var1) throws TradingException {
      return this.OpenM == null ? 0.0 : this.OpenM.get(var1);
   }

   public double OpenM(int var1, int var2) throws TradingException {
      return this.MarketData.Chart(var1).OpenM(var2);
   }

   public double Close(int var1) throws TradingException {
      return this.Close.get(var1);
   }

   public double Close(int var1, int var2) throws TradingException {
      return this.MarketData.Chart(var1).Close(var2);
   }

   public double CloseD(int var1) throws TradingException {
      return this.CloseD == null ? 0.0 : this.CloseD.get(var1);
   }

   public double CloseD(int var1, int var2) throws TradingException {
      return this.MarketData.Chart(var1).CloseD(var2);
   }

   public double CloseW(int var1) throws TradingException {
      return this.CloseW == null ? 0.0 : this.CloseW.get(var1);
   }

   public double CloseW(int var1, int var2) throws TradingException {
      return this.MarketData.Chart(var1).CloseW(var2);
   }

   public double CloseM(int var1) throws TradingException {
      return this.CloseM == null ? 0.0 : this.CloseM.get(var1);
   }

   public double CloseM(int var1, int var2) throws TradingException {
      return this.MarketData.Chart(var1).CloseM(var2);
   }

   public double High(int var1) throws TradingException {
      return this.High.get(var1);
   }

   public double High(int var1, int var2) throws TradingException {
      return this.MarketData.Chart(var1).High(var2);
   }

   public double HighD(int var1) throws TradingException {
      return this.HighD == null ? 0.0 : this.HighD.get(var1);
   }

   public double HighD(int var1, int var2) throws TradingException {
      return this.MarketData.Chart(var1).HighD(var2);
   }

   public double HighW(int var1) throws TradingException {
      return this.HighW == null ? 0.0 : this.HighW.get(var1);
   }

   public double HighW(int var1, int var2) throws TradingException {
      return this.MarketData.Chart(var1).HighW(var2);
   }

   public double HighM(int var1) throws TradingException {
      return this.HighM == null ? 0.0 : this.HighM.get(var1);
   }

   public double HighM(int var1, int var2) throws TradingException {
      return this.MarketData.Chart(var1).HighM(var2);
   }

   public double Low(int var1) throws TradingException {
      return this.Low.get(var1);
   }

   public double Low(int var1, int var2) throws TradingException {
      return this.MarketData.Chart(var1).Low(var2);
   }

   public double LowD(int var1) throws TradingException {
      return this.LowD == null ? 0.0 : this.LowD.get(var1);
   }

   public double LowD(int var1, int var2) throws TradingException {
      return this.MarketData.Chart(var1).LowD(var2);
   }

   public double LowW(int var1) throws TradingException {
      return this.LowW == null ? 0.0 : this.LowW.get(var1);
   }

   public double LowW(int var1, int var2) throws TradingException {
      return this.MarketData.Chart(var1).LowW(var2);
   }

   public double LowM(int var1) throws TradingException {
      return this.LowM == null ? 0.0 : this.LowM.get(var1);
   }

   public double LowM(int var1, int var2) throws TradingException {
      return this.MarketData.Chart(var1).LowM(var2);
   }

   public double Volume(int var1) throws TradingException {
      return this.Volume.get(var1);
   }

   public double Openint(int var1) throws TradingException {
      return 0.0;
   }

   public double Volume(int var1, int var2) throws TradingException {
      return this.MarketData.Chart(var1).Volume(var2);
   }

   public double Median(int var1) throws TradingException {
      return this.Median.get(var1);
   }

   public double Typical(int var1) throws TradingException {
      return this.Typical.get(var1);
   }

   public int Bars() {
      return this.Time.size();
   }

   public int Bars(int var1) {
      return this.MarketData.Chart(var1).Time.size();
   }

   public int getConnectionHash() {
      return this.connectionHash;
   }

   public int getSymbolHash() {
      return this.symbolHash;
   }

   public String getSymbol() {
      return this.Symbol;
   }

   public void setSymbol(String var1) {
      this.Symbol = var1;
      this.symbolHash = var1.hashCode();
   }

   public String getConnectionName() {
      return this.Connection;
   }

   final void setUpdated(boolean var1) {
      this.updated = var1;
   }

   public boolean isUpdated() {
      return this.updated;
   }

   public void processTick(TickEvent var1, int var2, int var3) throws Exception {
      if (var2 == 3) {
         this.updated = false;
         this.EventType = 1;
      }

      if (this.connectionHash == var1.getConnectionHash()) {
         if (this.symbolHash == var1.getSymbolHash()) {
            if (var2 == 3) {
               this.barType.processTick(var1, this.barTypeStatus, var3);
            }

            if (this.barTypeStatus.status != 0) {
               if (this.barTypeStatus.status == 2) {
                  if (var2 == 3) {
                     this.updateBarData(var1, true);
                     this.setUpdated(true);
                  }
               } else if (var2 == 3) {
                  if (this.Time.size() > 0) {
                     this.EventType = 3;
                     this.setUpdated(true);
                  }
               } else {
                  this.createBarData(var1, this.barTypeStatus.barTime);
                  this.EventType = 2;
                  this.setUpdated(true);
               }

               this.useSessionWhenComputingDWM = !"No Session".equals(this.chartDef.getSession());
               if (this.hasDailyDataBlock) {
                  this.processDailyData(var1);
               }

               if (this.hasWeeklyDataBlock) {
                  this.processWeeklyData(var1);
               }

               if (this.hasMonthlyDataBlock) {
                  this.processMonthlyData(var1);
               }
            }
         }
      }
   }

   public void findTickInData(TickEvent var1, int var2, boolean var3, boolean var4, int var5) throws Exception {
      if (!var3 || var2 == 3) {
         this.updated = false;
         this.EventType = 1;
      }

      if (this.connectionHash == var1.getConnectionHash()) {
         if (this.symbolHash == var1.getSymbolHash()) {
            if (this.currentBarTime != Long.MIN_VALUE
               && (this.nextBarTime <= Long.MIN_VALUE || var1.getTime() < this.nextBarTime || this.nextBarTime <= this.currentBarTime)) {
               if (var1.getTime() >= this.currentBarTime && (!var3 || var2 == 3)) {
                  if (this.hasOnTickRule && var5 == 0) {
                     this.updateBarData(var1, true);
                  }

                  this.setUpdated(true);
               }
            } else if (var2 == 3) {
               if (!var3) {
                  return;
               }

               if (this.currentBarTime > Long.MIN_VALUE) {
                  this.EventType = 3;
                  this.setUpdated(true);
               }
            } else {
               int var6;
               if (this.currentBarTime == Long.MIN_VALUE) {
                  var6 = this.findShift(var1.getTime());
                  if (var6 == -1) {
                     throw new DataException(5, "Cannot find bar corresponding to tick");
                  }
               } else {
                  var6 = this.Time.getShift() - 1;
               }

               this.moveDataSeriesShift(var6);
               if (var4) {
                  this.updateBarData(var1, true);
                  this.callDataChangeListeners();
               }

               this.EventType = 2;
               this.setUpdated(true);
            }

            this.useSessionWhenComputingDWM = !"No Session".equals(this.chartDef.getSession());
            this.backupTick.copyValues(var1);
            if (this.hasDailyDataBlock && var2 != 3 && (this.serieIndex == 0 || this.barType.getBarTimeType() != 2)) {
               this.processDailyData(var1);
            }

            if (this.hasWeeklyDataBlock && var2 != 3 && (this.serieIndex == 0 || this.barType.getBarTimeType() != 2)) {
               this.processWeeklyData(var1);
            }

            if (this.hasMonthlyDataBlock && var2 != 3 && (this.serieIndex == 0 || this.barType.getBarTimeType() != 2)) {
               this.processMonthlyData(var1);
            }

            var1.copyValues(this.backupTick);
         }
      }
   }

   public boolean isNextDay(TickEvent var1) {
      return this.currentBarTime == Long.MIN_VALUE ? true : var1.getTime() > this.currentDayEnd;
   }

   public void findTickInDataComplete(TickEvent var1, int var2) throws Exception {
      if (var2 == 3) {
         this.updated = false;
         this.EventType = 1;
      }

      if (this.connectionHash == var1.getConnectionHash()) {
         if (this.symbolHash == var1.getSymbolHash()) {
            if (this.currentBarTime == Long.MIN_VALUE
               || this.nextBarTime > Long.MIN_VALUE && var1.getTime() >= this.nextBarTime && this.nextBarTime > this.currentBarTime) {
               if (var2 == 3) {
                  if (this.currentBarTime > Long.MIN_VALUE) {
                     this.EventType = 3;
                     this.setUpdated(true);
                  }
               } else {
                  int var3;
                  if (this.currentBarTime == Long.MIN_VALUE) {
                     var3 = this.findShift(var1.getTime());
                     if (var3 == -1) {
                        throw new DataException(5, "Cannot find bar corresponding to tick");
                     }
                  } else {
                     var3 = this.Time.getShift() - 1;
                  }

                  this.moveDataSeriesShift(var3);
                  this.updateBarData(var1, true);
                  this.callDataChangeListeners();
               }
            } else {
               if (var1.getTime() < this.currentBarTime) {
                  throw new DataException(5, "Bar time doesn't correspond to tick");
               }

               if (var2 == 3) {
                  this.updateBarData(var1, true);
                  this.setUpdated(true);
               }
            }

            this.processDailyData(var1);
         }
      }
   }

   private int findShift(long var1) throws DataException, TradingException {
      int var3 = this.Time.getShift();
      int var4 = -1;

      for (int var5 = 1; var5 < 100; var5++) {
         long var6 = this.Time.get(0);
         this.Time.setShift(var3 - var5);
         long var8 = this.Time.get(0);
         if (var1 >= var6 && var1 < var8) {
            var4 = var3 - var5 + 1;
            break;
         }
      }

      if (var4 == -1) {
         this.Time.setShift(var3);
         if (var1 <= this.Time.get(0)) {
            return var3;
         } else {
            throw new DataException(5, "Cannot find bar corresponding to tick");
         }
      } else {
         return var4;
      }
   }

   private void moveDataSeriesShift(int var1) throws TradingException {
      if (var1 == this.nextBarIndex) {
         this.currentBarTime = this.nextBarTime;
      } else {
         this.Time.setShift(var1);
         this.currentBarTime = this.Time.get(0);
      }

      if (this.currentBarTime > this.currentDayEnd) {
         this.currentDayEnd = SQTime.correctDayEnd(this.currentBarTime);
      }

      this.nextBarIndex = var1 - 1;
      this.Time.setShift(this.nextBarIndex);
      this.nextBarTime = this.Time.get(0);
      this.Time.setShift(var1);
      this.Open.setShift(var1);
      this.High.setShift(var1);
      this.Low.setShift(var1);
      this.Close.setShift(var1);
      this.Volume.setShift(var1);
      this.TimeCurrent = this.Time.get(0);
      this.barLow = Double.MAX_VALUE;
      this.barHigh = -Double.MAX_VALUE;
   }

   private void processDailyData(TickEvent var1) throws TradingException {
      long var2 = var1.getTime();
      double var4 = var1.getBid();
      this.ensureDWMExists(1);
      if (this.useSessionWhenComputingDWM && this.sessionDaily == null) {
         this.sessionDaily = SessionManager.getSession(this.chartDef.getSession()).clone();
      }

      if (this.TimeD.size() != 0 && var2 < this.nextDayTick) {
         if (var4 > this.dailyHigh) {
            this.HighD.set(0, var4);
            this.dailyHigh = var4;
         }

         if (var4 < this.dailyLow) {
            this.LowD.set(0, var4);
            this.dailyLow = var4;
         }

         this.CloseD.set(0, var4);
      } else {
         if (this.useSessionWhenComputingDWM) {
            this.sessionDaily.clearSessionTempData();
            this.sessionDaily.fixD1DataTime(var1);
            this.sessionDaily.checkTimeIsInSession(var1.getTime(), this.sessionStatus, "D1");
         }

         if (!this.useSessionWhenComputingDWM || this.sessionStatus.sessionStartTime == Long.MIN_VALUE) {
            this.sessionStatus.sessionStartTime = var2;
            this.sessionStatus.sessionStartTime = SQTime.setHour(this.sessionStatus.sessionStartTime, 0);
            this.sessionStatus.sessionStartTime = SQTime.setMinute(this.sessionStatus.sessionStartTime, 0);
            this.sessionStatus.sessionStartTime = SQTime.setSecond(this.sessionStatus.sessionStartTime, 0);
            this.sessionStatus.sessionStartTime = SQTime.setMiliSeconds(this.sessionStatus.sessionStartTime, 0);
            this.sessionStatus.sessionEndTime = SQTime.correctDayEnd(this.sessionStatus.sessionStartTime);
         }

         boolean var6 = this.barType.getBarTimeType() == 2;
         this.TimeD.add(var6 ? this.sessionStatus.sessionEndTime : this.sessionStatus.sessionStartTime);
         this.OpenD.add(var4);
         this.HighD.add(var4);
         this.LowD.add(var4);
         this.CloseD.add(var4);
         this.dailyHigh = var4;
         this.dailyLow = var4;
         this.nextDayTick = SQTime.addDays(this.sessionStatus.sessionStartTime, 1);
      }
   }

   private void ensureDWMExists(int var1) {
      switch (var1) {
         case 1:
            if (this.TimeD == null) {
               this.TimeD = new TimeDataSeries("TimeD", 1000, this.chartDef);
               this.OpenD = new DataSeries("OpenD", 1000, this.chartDef);
               this.HighD = new DataSeries("HighD", 1000, this.chartDef);
               this.LowD = new DataSeries("LowD", 1000, this.chartDef);
               this.CloseD = new DataSeries("CloseD", 1000, this.chartDef);
            }
            break;
         case 2:
            if (this.TimeW == null) {
               this.TimeW = new TimeDataSeries("TimeW", 200, this.chartDef);
               this.OpenW = new DataSeries("OpenW", 200, this.chartDef);
               this.HighW = new DataSeries("HighW", 200, this.chartDef);
               this.LowW = new DataSeries("LowW", 200, this.chartDef);
               this.CloseW = new DataSeries("CloseW", 200, this.chartDef);
            }
            break;
         case 3:
            if (this.TimeM == null) {
               this.TimeM = new TimeDataSeries("TimeM", 50, this.chartDef);
               this.OpenM = new DataSeries("OpenM", 50, this.chartDef);
               this.HighM = new DataSeries("HighM", 50, this.chartDef);
               this.LowM = new DataSeries("LowM", 50, this.chartDef);
               this.CloseM = new DataSeries("CloseM", 50, this.chartDef);
            }
      }
   }

   private void processWeeklyData(TickEvent var1) throws TradingException {
      long var2 = var1.getTime();
      double var4 = var1.getBid();
      this.ensureDWMExists(2);
      if (this.useSessionWhenComputingDWM && this.sessionWeekly == null) {
         this.sessionWeekly = SessionManager.getSession(this.chartDef.getSession()).clone();
      }

      if (this.TimeW.size() != 0 && var2 < this.nextWeekTick) {
         if (var4 > this.weeklyHigh) {
            this.HighW.set(0, var4);
            this.weeklyHigh = var4;
         }

         if (var4 < this.weeklyLow) {
            this.LowW.set(0, var4);
            this.weeklyLow = var4;
         }

         this.CloseW.set(0, var4);
      } else {
         if (this.useSessionWhenComputingDWM) {
            this.sessionWeekly.clearSessionTempData();
            this.sessionWeekly.fixD1DataTime(var1);
            this.sessionWeekly.checkTimeIsInSession(var1.getTime(), this.sessionStatus, "Weekly");
         }

         if (!this.useSessionWhenComputingDWM || this.sessionStatus.sessionStartTime == Long.MIN_VALUE) {
            this.sessionStatus.sessionStartTime = var2;
            this.sessionStatus.sessionStartTime = SQTime.setHour(this.sessionStatus.sessionStartTime, 0);
            this.sessionStatus.sessionStartTime = SQTime.setMinute(this.sessionStatus.sessionStartTime, 0);
            this.sessionStatus.sessionStartTime = SQTime.setSecond(this.sessionStatus.sessionStartTime, 0);
            this.sessionStatus.sessionStartTime = SQTime.setMiliSeconds(this.sessionStatus.sessionStartTime, 0);
            this.sessionStatus.sessionStartTime = SQTime.setDayOfWeek(this.sessionStatus.sessionStartTime, 1);
            this.sessionStatus.sessionEndTime = SQTime.setDayOfWeek(this.sessionStatus.sessionStartTime, 7);
            this.sessionStatus.sessionEndTime = SQTime.correctDayEnd(this.sessionStatus.sessionEndTime);
         }

         boolean var6 = this.barType.getBarTimeType() == 2;
         this.TimeW.add(var6 ? this.sessionStatus.sessionEndTime : this.sessionStatus.sessionStartTime);
         this.OpenW.add(var4);
         this.HighW.add(var4);
         this.LowW.add(var4);
         this.CloseW.add(var4);
         this.weeklyHigh = var4;
         this.weeklyLow = var4;
         this.nextWeekTick = SQTime.addDays(this.sessionStatus.sessionStartTime, 7);
      }
   }

   private void processMonthlyData(TickEvent var1) throws TradingException {
      long var2 = var1.getTime();
      double var4 = var1.getBid();
      this.ensureDWMExists(3);
      if (this.useSessionWhenComputingDWM && this.sessionMonthly == null) {
         this.sessionMonthly = SessionManager.getSession(this.chartDef.getSession()).clone();
      }

      if (this.TimeM.size() != 0 && var2 < this.nextMonthTick) {
         if (var4 > this.monthlyHigh) {
            this.HighM.set(0, var4);
            this.monthlyHigh = var4;
         }

         if (var4 < this.monthlyLow) {
            this.LowM.set(0, var4);
            this.monthlyLow = var4;
         }

         this.CloseM.set(0, var4);
      } else {
         if (this.useSessionWhenComputingDWM) {
            this.sessionMonthly.clearSessionTempData();
            this.sessionMonthly.fixD1DataTime(var1);
            this.sessionMonthly.checkTimeIsInSession(var1.getTime(), this.sessionStatus, "Monthly");
         }

         if (!this.useSessionWhenComputingDWM || this.sessionStatus.sessionStartTime == Long.MIN_VALUE) {
            this.sessionStatus.sessionStartTime = var2;
            this.sessionStatus.sessionStartTime = SQTime.setHour(this.sessionStatus.sessionStartTime, 0);
            this.sessionStatus.sessionStartTime = SQTime.setMinute(this.sessionStatus.sessionStartTime, 0);
            this.sessionStatus.sessionStartTime = SQTime.setSecond(this.sessionStatus.sessionStartTime, 0);
            this.sessionStatus.sessionStartTime = SQTime.setMiliSeconds(this.sessionStatus.sessionStartTime, 0);
            this.sessionStatus.sessionStartTime = SQTime.setDayOfMonth(this.sessionStatus.sessionStartTime, 1);
            this.sessionStatus.sessionEndTime = SQTime.addMonths(this.sessionStatus.sessionStartTime, 1) - 1L;
         }

         boolean var6 = this.barType.getBarTimeType() == 2;
         this.TimeM.add(var6 ? this.sessionStatus.sessionEndTime : this.sessionStatus.sessionStartTime);
         this.OpenM.add(var4);
         this.HighM.add(var4);
         this.LowM.add(var4);
         this.CloseM.add(var4);
         this.monthlyHigh = var4;
         this.monthlyLow = var4;
         this.nextMonthTick = SQTime.addMonths(this.sessionStatus.sessionStartTime, 1);
         this.nextMonthTick = SQTime.setDayOfMonth(this.nextMonthTick, 1);
      }
   }

   private void createBarData(TickEvent var1, long var2) throws Exception {
      double var4 = var1.getBid();
      this.updateVariables(var1.getTime(), var4, var1.getAsk());
      this.Time.add(var2);
      this.Open.add(var4);
      this.High.add(var4);
      this.Low.add(var4);
      this.Close.add(var4);
      this.Volume.add(var1.getVolume());
      this.barHigh = var4;
      this.barLow = var4;
      this.barVolume = var1.getVolume();
      this.EventType = 2;
   }

   private void updateBarData(TickEvent var1, boolean var2) throws Exception {
      double var3 = var1.getBid();
      this.updateVariables(var1.getTime(), var3, var1.getAsk());
      if (var3 > this.barHigh) {
         this.High.set(0, var3, var2);
         this.barHigh = var3;
      }

      if (var3 < this.barLow) {
         this.Low.set(0, var3, var2);
         this.barLow = var3;
      }

      this.Close.set(0, var3, var2);
      if (var1.getVolume() != 0.0) {
         this.barVolume = this.barVolume + var1.getVolume();
         this.Volume.set(0, this.barVolume, var2);
      }

      this.EventType = 4;
   }

   private void callDataChangeListeners() throws TradingException {
      this.Time.callDataChangeListeners();
      this.Open.callDataChangeListeners();
      this.High.callDataChangeListeners();
      this.Low.callDataChangeListeners();
      this.Close.callDataChangeListeners();
      this.Volume.callDataChangeListeners();
   }

   private void updateVariables(long var1, double var3, double var5) {
      this.TimeCurrent = var1;
      this.Bid = var3;
      this.Ask = var5;
      if (this.connectedObject != null) {
         this.connectedObject.TimeCurrent = this.TimeCurrent;
         this.connectedObject.Bid = this.Bid;
         this.connectedObject.Ask = this.Ask;
      }
   }

   public void processTickSimplified(TickEvent var1, int var2) throws Exception {
      if (this.connectionHash == var1.getConnectionHash()) {
         if (this.symbolHash == var1.getSymbolHash()) {
            this.barType.processTick(var1, this.barTypeStatus, var2);
            if (this.barTypeStatus.status != 0) {
               if (this.barTypeStatus.status == 2) {
                  this.updateBarData(var1, true);
               } else {
                  this.createBarData(var1, this.barTypeStatus.barTime);
               }
            }
         }
      }
   }

   public DataSeries getSeries(int var1) throws TradingException {
      return this.getSeries(var1, this.chartTF);
   }

   public DataSeries getSeries(int var1, int var2) throws TradingException {
      if (var2 == 0 || var1 == 4 || var1 == 5 || var1 == 6) {
         switch (var1) {
            case 0:
               return this.Close;
            case 1:
               return this.Open;
            case 2:
               return this.High;
            case 3:
               return this.Low;
            case 4:
               return this.Median;
            case 5:
               return this.Typical;
            case 6:
               return this.Weighted;
            case 7:
               return this.Volume;
         }
      } else if (var2 == 1) {
         this.ensureDWMExists(1);
         switch (var1) {
            case 0:
               return this.CloseD;
            case 1:
               return this.OpenD;
            case 2:
               return this.HighD;
            case 3:
               return this.LowD;
         }
      } else if (var2 == 2) {
         this.ensureDWMExists(2);
         switch (var1) {
            case 0:
               return this.CloseW;
            case 1:
               return this.OpenW;
            case 2:
               return this.HighW;
            case 3:
               return this.LowW;
         }
      } else if (var2 == 3) {
         this.ensureDWMExists(3);
         switch (var1) {
            case 0:
               return this.CloseM;
            case 1:
               return this.OpenM;
            case 2:
               return this.HighM;
            case 3:
               return this.LowM;
         }
      }

      throw new TradingException("Unknown DataSerie type: " + var1 + ", ChartTF: " + var2);
   }

   public void destroy() {
      if (this.Time != null) {
         this.Time.destroy();
         this.Time = null;
      }

      if (this.Open != null) {
         this.Open.destroy();
         this.Open = null;
      }

      if (this.High != null) {
         this.High.destroy();
         this.High = null;
      }

      if (this.Low != null) {
         this.Low.destroy();
         this.Low = null;
      }

      if (this.Close != null) {
         this.Close.destroy();
         this.Close = null;
      }

      if (this.Volume != null) {
         this.Volume.destroy();
         this.Volume = null;
      }

      if (!this.usesSameDailyDataSeries()) {
         if (this.TimeD != null) {
            this.TimeD.destroy();
            this.TimeD = null;
         }

         if (this.OpenD != null) {
            this.OpenD.destroy();
            this.OpenD = null;
         }

         if (this.HighD != null) {
            this.HighD.destroy();
            this.HighD = null;
         }

         if (this.LowD != null) {
            this.LowD.destroy();
            this.LowD = null;
         }

         if (this.CloseD != null) {
            this.CloseD.destroy();
            this.CloseD = null;
         }

         if (this.TimeW != null) {
            this.TimeW.destroy();
            this.TimeW = null;
         }

         if (this.OpenW != null) {
            this.OpenW.destroy();
            this.OpenW = null;
         }

         if (this.HighW != null) {
            this.HighW.destroy();
            this.HighW = null;
         }

         if (this.LowW != null) {
            this.LowW.destroy();
            this.LowW = null;
         }

         if (this.CloseW != null) {
            this.CloseW.destroy();
            this.CloseW = null;
         }

         if (this.TimeM != null) {
            this.TimeM.destroy();
            this.TimeM = null;
         }

         if (this.OpenM != null) {
            this.OpenM.destroy();
            this.OpenM = null;
         }

         if (this.HighM != null) {
            this.HighM.destroy();
            this.HighM = null;
         }

         if (this.LowM != null) {
            this.LowM.destroy();
            this.LowM = null;
         }

         if (this.CloseM != null) {
            this.CloseM.destroy();
            this.CloseM = null;
         }
      }

      if (this.Typical != null) {
         this.Typical.destroy();
         this.Typical = null;
      }

      if (this.Median != null) {
         this.Median.destroy();
         this.Median = null;
      }

      if (this.Weighted != null) {
         this.Weighted.destroy();
         this.Weighted = null;
      }
   }

   public InstrumentInfo getInstrumentInfo() {
      return this.SymbolInfo;
   }

   public void setInstrumentInfo(InstrumentInfo var1) {
      this.SymbolInfo = var1;
   }

   public double MinMove() {
      return this.SymbolInfo == null ? 25.0 : this.SymbolInfo.tickStep * this.PriceScale();
   }

   public double BigPointValue() {
      return this.SymbolInfo == null ? 20.0 : this.SymbolInfo.pointValue;
   }

   public double PriceScale() {
      return this.SymbolInfo == null ? 100.0 : Math.pow(10.0, this.SymbolInfo.decimals);
   }

   public double getMinDistance() {
      return this.chartDef != null ? this.chartDef.getMinDistance() : this.MarketData.Chart(0).getMinDistance();
   }

   public void resetPreparedDataShifts() {
      int var1 = this.Time.size() - 1;
      this.Time.setShift(var1);
      this.Open.setShift(var1);
      this.High.setShift(var1);
      this.Low.setShift(var1);
      this.Close.setShift(var1);
      this.currentBarTime = Long.MIN_VALUE;
      this.currentDayEnd = Long.MIN_VALUE;
      this.nextBarTime = Long.MIN_VALUE;
   }

   public void setPerformanceParams(boolean var1, boolean var2, boolean var3, boolean var4) {
      this.hasOnTickRule = var1;
      this.hasDailyDataBlock = var2;
      this.hasWeeklyDataBlock = var3;
      this.hasMonthlyDataBlock = var4;
   }

   public int getCurrentBar() {
      return this.currentBar;
   }

   public void setCurrentBar(int var1, int var2) {
      this.currentBar = this.Bars() - var1 - var2;
   }

   public int getSerieIndex() {
      return this.serieIndex;
   }

   public ChartData cloneForTF(int var1) {
      if (var1 != 1 && var1 != 2 && var1 != 3) {
         return this;
      }

      ChartData var2 = new ChartData();
      var2.Time = this.Time;
      var2.Open = this.Open;
      var2.High = this.High;
      var2.Low = this.Low;
      var2.Close = this.Close;
      var2.Median = this.Median;
      var2.Typical = this.Typical;
      var2.Weighted = this.Weighted;
      var2.TimeD = this.TimeD;
      var2.OpenD = this.OpenD;
      var2.HighD = this.HighD;
      var2.LowD = this.LowD;
      var2.CloseD = this.CloseD;
      var2.TimeW = this.TimeW;
      var2.OpenW = this.OpenW;
      var2.HighW = this.HighW;
      var2.LowW = this.LowW;
      var2.CloseW = this.CloseW;
      var2.TimeM = this.TimeM;
      var2.OpenM = this.OpenM;
      var2.HighM = this.HighM;
      var2.LowM = this.LowM;
      var2.CloseM = this.CloseM;
      var2.Volume = this.Volume;
      var2.MarketData = this.MarketData;
      var2.Connection = this.Connection;
      var2.Symbol = this.Symbol;
      var2.Instrument = this.Instrument;
      var2.Timeframe = this.Timeframe;
      var2.session = this.session;
      var2.requestedEventType = this.requestedEventType;
      var2.connectionHash = this.connectionHash;
      var2.symbolHash = this.symbolHash;
      var2.updated = this.updated;
      var2.barType = this.barType;
      var2.SymbolInfo = this.SymbolInfo;
      var2.TimeCurrent = this.TimeCurrent;
      var2.chartDef = this.chartDef;
      var2.chartTF = var1;
      var2.Time = this.Time;
      return var2;
   }

   public ChartData cloneOnlyTF(int var1) {
      if (var1 != 1 && var1 != 2 && var1 != 3) {
         return this;
      }

      ChartData var2 = new ChartData();
      if (var1 == 1) {
         this.ensureDWMExists(1);
         var2.Time = this.TimeD;
         var2.Open = this.OpenD;
         var2.High = this.HighD;
         var2.Low = this.LowD;
         var2.Close = this.CloseD;
      } else if (var1 == 2) {
         this.ensureDWMExists(2);
         var2.Time = this.TimeW;
         var2.Open = this.OpenW;
         var2.High = this.HighW;
         var2.Low = this.LowW;
         var2.Close = this.CloseW;
      } else if (var1 == 3) {
         this.ensureDWMExists(3);
         var2.Time = this.TimeM;
         var2.Open = this.OpenM;
         var2.High = this.HighM;
         var2.Low = this.LowM;
         var2.Close = this.CloseM;
      }

      var2.Volume = this.Volume;
      var2.MarketData = this.MarketData;
      var2.Connection = this.Connection;
      var2.Symbol = this.Symbol;
      var2.Instrument = this.Instrument;
      var2.Timeframe = this.Timeframe;
      var2.session = this.session;
      var2.requestedEventType = this.requestedEventType;
      var2.connectionHash = this.connectionHash;
      var2.symbolHash = this.symbolHash;
      var2.updated = this.updated;
      var2.barType = this.barType;
      var2.SymbolInfo = this.SymbolInfo;
      var2.TimeCurrent = this.TimeCurrent;
      var2.chartDef = this.chartDef;
      var2.chartTF = var1;
      var2.Time = this.Time;
      return var2;
   }

   public boolean isNewBarOnMainSymbolTF(TickEvent var1) {
      if (this.connectionHash != var1.getConnectionHash()) {
         return false;
      } else {
         return this.symbolHash != var1.getSymbolHash()
            ? false
            : this.currentBarTime == Long.MIN_VALUE
               || this.nextBarTime > Long.MIN_VALUE && var1.getTime() >= this.nextBarTime && this.nextBarTime > this.currentBarTime;
      }
   }

   public int getIndyStartingBar() {
      return this.indyStartingBar;
   }

   public void setIndyStartingBar(int var1) {
      this.indyStartingBar = var1;
   }
}
