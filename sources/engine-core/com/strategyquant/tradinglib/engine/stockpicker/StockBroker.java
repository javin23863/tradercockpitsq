package com.strategyquant.tradinglib.engine.stockpicker;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.InstrumentInfo;
import com.strategyquant.datalib.TickEvent;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.CommissionsMethod;
import com.strategyquant.tradinglib.OrderCloseTypes;
import com.strategyquant.tradinglib.OrderTypes;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SwapMethod;
import com.strategyquant.tradinglib.Trader;
import com.strategyquant.tradinglib.engine.stockpicker.backtester.log.StockpickerLog;
import com.strategyquant.tradinglib.engine.stockpicker.constants.PickerEodTypes;
import com.strategyquant.tradinglib.strategy.LiveOrderObj;
import com.strategyquant.tradinglib.swap.SwapCalculator;
import it.unimi.dsi.fastutil.longs.Long2FloatRBTreeMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StockBroker {
   public static final Logger Log = LoggerFactory.getLogger(StockBroker.class);
   private int orderId = 1;
   private ObjectArrayList<LiveOrderObj> liveOrders;
   private OrdersList closedTrades;
   private long lastDayStartTime = -1L;
   private long nextDayStartTime = -1L;
   private double worstDailyEquity;
   private Long2FloatRBTreeMap dailyEquityValues;
   private double peakEquity;
   private double maxDailyDrawdown;
   private double maxDailyPctDrawdown;
   private Long2FloatRBTreeMap maxDailyDDValues;
   private Long2FloatRBTreeMap maxDailyDDPctValues;
   public double maxOpenDrawdown = 0.0;
   public double maxOpenDrawdownPct = 0.0;
   private ObjectArrayList<String> tradedSymbols = new ObjectArrayList();
   private double initialCapital;
   private double accountBalance;
   private double accountEquity;
   private Trader dummyTradeController;
   private TickEvent currentTick = new TickEvent();
   StockpickerLog verboseLog;
   protected int ambiguousTrades;
   private double prevClosePrice;
   private double slippage;
   private CommissionsMethod commission;
   private SwapMethod swap;
   private double LimitOver = 0.0;
   private boolean AllowBetterLimitFill = false;
   public ObjectArrayList<LiveOrderObj> statsOpenedPositions;
   public ObjectArrayList<LiveOrderObj> statsClosedPositions;
   public ObjectArrayList<LiveOrderObj> statsCanceledOrders;
   public static int TickBeforeBarOpen = 1;
   public static int TickOnBarOpen = 2;
   public static int TickOnBarClose = 3;

   public StockBroker(Trader var1) {
      this(var1, null);
   }

   public StockBroker(Trader var1, StockpickerLog var2) {
      this.dummyTradeController = var1;
      this.verboseLog = var2;
      this.liveOrders = new ObjectArrayList();
      this.closedTrades = new OrdersList("ClosedTrades");
      this.dailyEquityValues = new Long2FloatRBTreeMap();
      this.maxDailyDDValues = new Long2FloatRBTreeMap();
      this.maxDailyDDPctValues = new Long2FloatRBTreeMap();
      this.statsOpenedPositions = new ObjectArrayList();
      this.statsClosedPositions = new ObjectArrayList();
      this.statsCanceledOrders = new ObjectArrayList();
   }

   public void resetStats() {
      this.statsOpenedPositions.clear();
      this.statsClosedPositions.clear();
      this.statsCanceledOrders.clear();
   }

   public int openPosCount(int var1) {
      int var2 = 0;

      for (int var3 = 0; var3 < this.liveOrders.size(); var3++) {
         LiveOrderObj var4 = (LiveOrderObj)this.liveOrders.get(var3);
         if (var1 == 1 && OrderTypes.isLongOrder(var4.getOrderType())) {
            var2++;
         }

         if (var1 == -1 && OrderTypes.isShortOrder(var4.getOrderType())) {
            var2++;
         }
      }

      return var2;
   }

   public int openPosCount(boolean var1) {
      int var2 = 0;

      for (int var3 = 0; var3 < this.liveOrders.size(); var3++) {
         LiveOrderObj var4 = (LiveOrderObj)this.liveOrders.get(var3);
         if (!var1 && var4.getPositionStatus() == 1) {
            var2++;
         }

         if (var1 && var4.getPositionStatus() != 1) {
            var2++;
         }
      }

      return var2;
   }

   public double getAccountBalance() {
      return this.accountBalance;
   }

   public double getAccountEquity() {
      return this.accountEquity;
   }

   public void printOpenTrades(StringBuilder var1, boolean var2) {
      for (int var3 = 0; var3 < this.liveOrders.size(); var3++) {
         LiveOrderObj var4 = (LiveOrderObj)this.liveOrders.get(var3);
         if (!var2 && var4.getPositionStatus() == 1) {
            var1.append(var4.getSymbol());
            var1.append(", ");
         }

         if (var2 && var4.getPositionStatus() != 1) {
            var1.append(var4.getSymbol());
            var1.append(", ");
         }
      }
   }

   public String printOpenTrades(boolean var1) {
      StringBuilder var2 = new StringBuilder();
      this.printOpenTrades(var2, var1);
      return var2.toString();
   }

   public ObjectArrayList<LiveOrderObj> getOpenPositions() {
      return this.liveOrders;
   }

   public LiveOrderObj open(
      byte var1, String var2, double var3, double var5, double var7, double var9, int var11, int var12, int var13, byte var14, boolean var15, boolean var16
   ) throws TradingException {
      DataInfo var17 = DataManager.getDataInfo("History", var2, true);
      InstrumentInfo var18 = var17.symbolInfo;
      LiveOrderObj var19 = new LiveOrderObj(this.dummyTradeController, var18, var1, var2);
      var19.setTickData(this.currentTick.getAsk(), this.currentTick.getBid());
      var19.setSize(var3);
      var19.setOpenTime(this.currentTick.getTime());
      var19.setOriginalOpenTime(var19.getOpenTime());
      if (OrderTypes.isMarketOrder(var1)) {
         var19.setOpenPrice(var19.getPriceByType(var1, this.currentTick.getBid(), this.currentTick.getAsk(), this.slippage));
         var19.setOrderStatus((byte)1);
         var19.setCommSwap(this.computeCommissionsOnOpen(var19));
         this.statsOpenedPositions.add(var19);
      } else {
         var19.setOpenPrice(var5);
         var19.setOrderStatus((byte)2);
         var19.setBarsValid(var12);
      }

      var19.setOriginalOpenPrice(var19.getOpenPrice());
      var19.setLastAction((byte)1);
      var19.setError(0, null);
      var19.setOrderId(this.orderId++);
      var19.setSL(var7);
      var19.setPT(var9);
      var19.setSLPTValidFrom(var11);
      var19.setExitAfterBars(var13);
      var19.setEOD(var14);
      var19.setWorkingStatus((byte)-30);
      if (!this.checkOrder(var19)) {
         StringBuilder var21 = new StringBuilder("Order refused with id: ");
         var21.append(var19.getOrderId());
         var21.append(", ");
         this.orderToString(var21, var19);
         var21.append(". Reason: ");
         var21.append(var19.getErrorMessage());
         this.VerboseLog(var21.toString());
         return null;
      } else {
         this.liveOrders.add(var19);
         StringBuilder var20 = new StringBuilder("Order placed with id: ");
         var20.append(var19.getOrderId());
         var20.append(", ");
         this.orderToString(var20, var19);
         this.VerboseLog(var20.toString());
         return var19;
      }
   }

   public void orderToString(StringBuilder var1, LiveOrderObj var2) {
      var1.append(var2.getSymbol());
      var1.append(", Type=");
      var1.append(OrderTypes.toString(var2.getOrderType()));
      var1.append(", Size=");
      var1.append(SQUtils.d2(var2.getSize()));
      var1.append(", OpenTime=");
      var1.append(SQTime.toDateMinuteString(var2.getOpenTime()));
      var1.append(", OpenPrice=");
      var1.append(SQUtils.d2(var2.getOpenPrice()));
      var1.append(", CloseTime=");
      var1.append(var2.getCloseTime() > 0L ? SQTime.toDateMinuteString(var2.getCloseTime()) : "");
      var1.append(", ClosePrice=");
      var1.append(var2.getClosePrice() > 0.0 ? SQUtils.d2(var2.getClosePrice()) : "");
      var1.append("[Exits: ExitEOD=");
      var1.append(PickerEodTypes.toString(var2.getEOD()));
      var1.append(", BarsValid=");
      var1.append(var2.getBarsValid());
      var1.append(", ExitAfterBars=");
      var1.append(var2.getExitAfterBars());
      var1.append(", SL=");
      var1.append(var2.getSL() != -9.9999999E7 ? SQUtils.d2(var2.getSL()) : "");
      var1.append(", PT=");
      var1.append(var2.getSL() != -9.9999999E7 ? SQUtils.d2(var2.getPT()) : "");
      var1.append("],");
   }

   public String orderToString(LiveOrderObj var1) {
      StringBuilder var2 = new StringBuilder("Symbol=");
      this.orderToString(var2, var1);
      return var2.toString();
   }

   private boolean checkOrder(LiveOrderObj var1) {
      int var2 = var1.getSymbolHash();
      byte var3 = var1.getOrderType();
      double var4 = LiveOrderObj.getPriceByType(var3, this.currentTick.getBid(), this.currentTick.getAsk());
      double var6 = var4;
      if (var1.isPendingOrder()) {
         double var8 = var1.getOpenPrice();
         if (!this.checkPriceLevelCorrectness(var3, var6, var8)) {
            if (var3 != 3 && var3 != 4 || !this.AllowBetterLimitFill) {
               var1.setError(6, "Incorrect price level. Open price: " + var6 + ", price level: " + var8);
               return false;
            }

            var8 = var4;
         }

         if (!this.checkPriceLevelDistance(var2, var6, var8)) {
            var1.setError(6, "Incorrect price level distance. Open price: " + var6 + ", price level: " + var8);
            return false;
         }

         var6 = var8;
      }

      if (var1.getSL() != -9.9999999E7) {
         double var12 = var1.getSL();
         var3 = var1.getOrderTypeFromSLPT(-1);
         if (!this.checkPriceLevelCorrectness(var3, var6, var12)) {
            var1.setError(6, "Incorrect stop level. Open price: " + var6 + ", price level: " + var12);
            return false;
         }

         if (!this.checkPriceLevelDistance(var2, var6, var12)) {
            var1.setError(6, "Incorrect price level distance. Open price: " + var6 + ", price level: " + var12);
            var1.setSL(0.0);
            var1.setSLFromModified();
         }
      }

      if (var1.getPT() != -9.9999999E7) {
         double var13 = var1.getPT();
         var3 = var1.getOrderTypeFromSLPT(1);
         if (!this.checkPriceLevelCorrectness(var3, var6, var13)) {
            var1.setError(6, "Incorrect stop level. Open price: " + var6 + ", price level: " + var13);
            return false;
         }

         if (!this.checkPriceLevelDistance(var2, var6, var13)) {
            var1.setError(6, "Incorrect price level distance. Open price: " + var6 + ", price level: " + var13);
            return false;
         }
      }

      return true;
   }

   private boolean checkPriceLevelDistance(int var1, double var2, double var4) {
      return true;
   }

   public void onStart(double var1, double var3, CommissionsMethod var5, SwapMethod var6, double var7, boolean var9) {
      this.initialCapital = var1;
      this.accountBalance = var1;
      this.accountEquity = this.accountBalance;
      this.peakEquity = this.accountEquity;
      this.slippage = var3;
      this.commission = var5;
      this.swap = var6;
      this.LimitOver = var7;
      this.AllowBetterLimitFill = var9;
      this.liveOrders.clear();
      this.closedTrades.clear();
   }

   public void onTick(TickEvent var1, double var2, double var4, double var6, byte var8, byte var9, int var10) throws TradingException {
      this.currentTick.set(var1);
      this.prevClosePrice = var2;
      this.updateLastPrice(this.currentTick, this.currentTick.getSymbolHash());
      this.updateDailyEquity();
      if (this.currentTick.isBarOpen() && (var9 == 15 || var9 == 5)) {
         if (var10 == TickOnBarOpen) {
            this.increaseBarsInTrade(this.currentTick.getSymbolHash());
         }

         this.checkBarsValid(this.currentTick, this.currentTick.getSymbolHash());
         this.checkExitAfterBars(this.currentTick, this.currentTick.getSymbolHash(), var8, var9);
      }

      if (var4 > 0.0 && var6 > 0.0) {
         this.checkStopLimitOrders(this.currentTick, this.currentTick.getSymbolHash(), var4, var6);
         this.checkSLPT(this.currentTick, this.currentTick.getSymbolHash(), var4, var6);
      }

      if (this.currentTick.isBarClose() && var9 == 10) {
         this.increaseBarsInTrade(this.currentTick.getSymbolHash());
         this.checkBarsValid(this.currentTick, this.currentTick.getSymbolHash());
         this.checkExitAfterBars(this.currentTick, this.currentTick.getSymbolHash(), var8, var9);
      }

      if (this.currentTick.isBarClose()) {
         this.checkEOD(this.currentTick, this.currentTick.getSymbolHash(), var8, var9);
      }

      this.updateMAE_MFE(this.currentTick.getSymbolHash(), var4, var6);
   }

   private void updateLastPrice(TickEvent var1, int var2) {
      int var3 = this.liveOrders.size();

      for (int var4 = var3 - 1; var4 >= 0; var4--) {
         LiveOrderObj var5 = (LiveOrderObj)this.liveOrders.get(var4);
         if (var5.getSymbolHash() == var2) {
            var5.lastPrice = var5.isLong() ? var1.getAsk() : var1.getBid();
            var5.setTickData(this.currentTick.getAsk(), this.currentTick.getBid());
         }
      }
   }

   private void checkExitAfterBars(TickEvent var1, int var2, byte var3, byte var4) {
      int var5 = this.liveOrders.size();

      for (int var6 = var5 - 1; var6 >= 0; var6--) {
         LiveOrderObj var7 = (LiveOrderObj)this.liveOrders.get(var6);
         if (var7.getSymbolHash() == var2 && var7.getPositionStatus() == 1) {
            int var8 = var7.getExitAfterBars();
            if (var8 > 0) {
               if (var3 == var4 && (OrderTypes.isMarketOrder(var7.getOriginalOrderType()) || var1.getTime() == var7.getOriginalOpenTime())) {
                  var8++;
               }

               if (var7.getBarsInTrade() >= var8) {
                  this.closeOrder(var7, -1.0, (byte)19);
               }
            }
         }
      }
   }

   private void checkEOD(TickEvent var1, int var2, byte var3, byte var4) {
      int var5 = this.liveOrders.size();

      for (int var6 = var5 - 1; var6 >= 0; var6--) {
         LiveOrderObj var7 = (LiveOrderObj)this.liveOrders.get(var6);
         if (var7.getSymbolHash() == var2
            && var7.getPositionStatus() == 1
            && (
               var3 != var4
                  || var1.getTime() != var7.getOpenTime()
                  || !OrderTypes.isMarketOrder(var7.getOriginalOrderType()) && var1.getTime() != var7.getOriginalOpenTime()
            )
            && var7.getEOD() == 5) {
            this.closeOrder(var7, -1.0, (byte)5);
         }
      }
   }

   public void setCurrentTick(TickEvent var1) {
      this.currentTick.set(var1);
   }

   private void updateDayStartTimes(long var1) {
      this.lastDayStartTime = SQTime.getDateInMs(var1);
      this.nextDayStartTime = this.lastDayStartTime + 86400000L;
   }

   private void updateDailyEquity() {
      long var1 = this.currentTick.getTime();
      if (this.lastDayStartTime == -1L) {
         this.updateDayStartTimes(var1);
      } else {
         if (var1 < this.nextDayStartTime) {
            return;
         }

         this.dailyEquityValues.addTo(this.lastDayStartTime, (float)this.worstDailyEquity);
         this.maxDailyDDValues.addTo(this.lastDayStartTime, (float)this.maxDailyDrawdown);
         this.maxDailyDDPctValues.addTo(this.lastDayStartTime, (float)this.maxDailyPctDrawdown);
         this.maxOpenDrawdown = Math.min(this.maxDailyDrawdown, this.maxOpenDrawdown);
         this.maxOpenDrawdownPct = Math.min(this.maxDailyPctDrawdown, this.maxOpenDrawdownPct);
         this.updateDayStartTimes(var1);
      }

      this.worstDailyEquity = this.liveOrders.size() > 0 ? Float.MAX_VALUE : this.accountBalance - this.initialCapital;
      this.maxDailyDrawdown = Float.MAX_VALUE;
      this.maxDailyPctDrawdown = Float.MAX_VALUE;
   }

   private void updateMAE_MFE(int var1, double var2, double var4) {
      double var6 = this.accountBalance;

      for (int var12 = 0; var12 < this.liveOrders.size(); var12++) {
         LiveOrderObj var13 = (LiveOrderObj)this.liveOrders.get(var12);
         if (!var13.isPendingOrder() && !var13.isClosedOrder()) {
            if (var13.getSymbolHash() == var1 && var2 > 0.0 && var4 > 0.0) {
               double var8;
               double var10;
               if (var13.isLong()) {
                  var10 = (var2 - var13.getOpenPrice()) / var13.getInstrumentInfo().tickSize;
                  var8 = (var13.getOpenPrice() - var4) / var13.getInstrumentInfo().tickSize;
               } else {
                  var10 = (var13.getOpenPrice() - var4) / var13.getInstrumentInfo().tickSize;
                  var8 = (var2 - var13.getOpenPrice()) / var13.getInstrumentInfo().tickSize;
               }

               if (var13.getMFE() == 0.0F || var10 > var13.getMFE()) {
                  var13.setMFE((float)var10);
               }

               if (var13.getMAE() == 0.0F || var8 > var13.getMAE()) {
                  var13.setMAE((float)var8);
               }
            }

            var6 += var13.getPL() - var13.getCommSwap();
         }
      }

      this.accountEquity = var6;
      this.worstDailyEquity = Math.min(var6 - this.initialCapital, this.worstDailyEquity);
      double var16 = specialSubtraction(this.peakEquity, var6);
      double var14 = this.getPercentageDD(var16, this.peakEquity);
      this.maxDailyDrawdown = Math.min(var16, this.maxDailyDrawdown);
      this.maxDailyPctDrawdown = Math.min(var14, this.maxDailyPctDrawdown);
      if (this.accountEquity > this.peakEquity) {
         this.peakEquity = this.accountEquity;
      }
   }

   private void checkSLPT(TickEvent var1, int var2, double var3, double var5) {
      boolean var10 = false;
      int var11 = this.liveOrders.size();

      for (int var12 = var11 - 1; var12 >= 0; var12--) {
         LiveOrderObj var13 = (LiveOrderObj)this.liveOrders.get(var12);
         if (var13.getSymbolHash() == var2 && !var13.isPendingOrder()) {
            double var14 = var13.getSL();
            double var16 = var13.getPT();
            if ((var14 != 0.0 || var16 != 0.0) && (var13.getSLPTValidFrom() <= 0 || var1.getTime() != var13.getOpenTime())) {
               if (var14 > var5 && var14 < var3 && var16 > var5 && var16 < var3) {
                  var13.setAmbiguous();
                  this.ambiguousTrades++;
               }

               if (var14 > 0.0) {
                  byte var7 = var13.getOrderTypeFromSLPT(-1);
                  double var8 = this.priceLevelReached(var1, var3, var5, var2, var7, var14, var13.getInstrumentInfo().tickSize);
                  if (var8 > 0.0) {
                     var8 = this.fixByHighLow(var8, var3, var5);
                     this.closeOrder(var13, this.getRealisticSLPTFilledPrice(var8, var14, var7), (byte)2);
                     continue;
                  }
               }

               if (var16 > 0.0) {
                  byte var18 = var13.getOrderTypeFromSLPT(1);
                  double var19 = this.priceLevelReached(var1, var3, var5, var2, var18, var16, var13.getInstrumentInfo().tickSize);
                  if (var19 > 0.0) {
                     var19 = this.fixByHighLow(var19, var3, var5);
                     this.closeOrder(var13, this.getRealisticSLPTFilledPrice(var19, var16, var18), (byte)3);
                  }
               }
            }
         }
      }
   }

   protected double getRealisticSLPTFilledPrice(double var1, double var3, int var5) {
      if (this.currentTick.isBarOpen() && this.prevClosePrice > 0.0) {
         double var6 = this.currentTick.getBid();
         if (var6 != this.prevClosePrice) {
            if (var5 == 5 || var5 == 6) {
               Log.debug("GAP detected - prev Close:{}, new Open: {}, realistic fill set", this.prevClosePrice, var6);
               return LiveOrderObj.getPriceByType(var5, this.currentTick.getBid(), this.currentTick.getAsk());
            }

            if (var5 == 3 || var5 == 4) {
               Log.debug("GAP detected for Limit - prev Close:{}, new Open: {}, realistic fill set", this.prevClosePrice, var6);
               double var8 = LiveOrderObj.getPriceByType(var5, this.currentTick.getBid(), this.currentTick.getAsk());
               double var10 = this.getSLPTFilledPrice(var1, var3);
               if (var5 == 3) {
                  return Math.min(var8, var10);
               }

               if (var5 == 4) {
                  return Math.max(var8, var10);
               }
            }
         }
      }

      return this.getSLPTFilledPrice(var1, var3);
   }

   private double getSLPTFilledPrice(double var1, double var3) {
      return var1;
   }

   public void onClose() {
      int var1 = this.liveOrders.size();

      for (int var2 = var1 - 1; var2 >= 0; var2--) {
         LiveOrderObj var3 = (LiveOrderObj)this.liveOrders.get(var2);
         this.closeOrder(var3, var3.lastPrice, (byte)4);
      }
   }

   public OrdersList getHistoryOrders() {
      return this.closedTrades;
   }

   public void close(String var1, byte var2, byte var3) {
      int var4 = this.liveOrders.size();

      for (int var5 = var4 - 1; var5 >= 0; var5--) {
         LiveOrderObj var6 = (LiveOrderObj)this.liveOrders.get(var5);
         if ((var2 != 1 || OrderTypes.isLongOrder(var6.getOrderType()))
            && (var2 != -1 || OrderTypes.isShortOrder(var6.getOrderType()))
            && var6.getSymbol().equals(var1)) {
            this.closeOrder(var6, -1.0, var3);
            break;
         }
      }
   }

   private void closeOrder(LiveOrderObj var1, double var2, byte var4) {
      double var5;
      if (var2 >= 0.0) {
         var5 = var1.getPriceByType(OrderTypes.getOppositeType(var1.getOrderType()), var2, var2, var4 == 3 ? 0.0 : this.slippage);
      } else {
         var5 = var1.getPriceByType(
            OrderTypes.getOppositeType(var1.getOrderType()), this.currentTick.getBid(), this.currentTick.getAsk(), var4 == 3 ? 0.0 : this.slippage
         );
      }

      var1.setCloseTime(this.currentTick.getTime());
      var1.setClosePrice(var5);
      var1.setLastAction((byte)2);
      var1.setWorkingStatus((byte)-30);
      var1.setOrderStatus((byte)3);
      var1.setCloseType(var4);
      if (!var1.isPendingOrder()) {
         var1.setCommSwap(this.getCommissionSwap(var1));
      }

      this.accountBalance = this.accountBalance + var1.getPL();
      StringBuilder var7 = new StringBuilder("Order closed [");
      var7.append(OrderCloseTypes.toString(var4));
      var7.append("] with id: ");
      var7.append(var1.getOrderId());
      var7.append(", ");
      this.orderToString(var7, var1);
      this.VerboseLog(var7.toString());
      this.closedTrades.add(var1);
      this.liveOrders.remove(var1);
      if (var1.isPendingOrder()) {
         this.statsCanceledOrders.add(var1);
      } else {
         this.statsClosedPositions.add(var1);
      }

      this.regTradedSymbol(var1.getSymbol());
   }

   private void checkBarsValid(TickEvent var1, int var2) throws TradingException {
      int var3 = this.liveOrders.size();

      for (int var4 = var3 - 1; var4 >= 0; var4--) {
         LiveOrderObj var5 = (LiveOrderObj)this.liveOrders.get(var4);
         if (var5.getSymbolHash() == var2
            && var5.isPendingOrder()
            && !var5.isClosedOrder()
            && var5.getBarsValid() > 0
            && var5.getBarsInTrade() >= var5.getBarsValid()) {
            this.closeOrder(var5, -1.0, (byte)6);
         }
      }
   }

   protected boolean checkPriceLevelCorrectness(int var1, double var2, double var4) {
      if (var1 == 5) {
         if (var2 >= var4) {
            return false;
         }
      } else if (var1 == 6) {
         if (var2 <= var4) {
            return false;
         }
      } else if (var1 == 3) {
         if (var2 <= var4) {
            return false;
         }
      } else if (var1 == 4 && var2 >= var4) {
         return false;
      }

      return true;
   }

   protected void checkStopLimitOrders(TickEvent var1, int var2, double var3, double var5) {
      for (int var9 = 0; var9 < this.liveOrders.size(); var9++) {
         LiveOrderObj var10 = (LiveOrderObj)this.liveOrders.get(var9);
         if (var10.getSymbolHash() == var2 && !var10.isMarketOrder()) {
            double var7 = this.priceLevelReached(var1, var3, var5, var2, var10.getOrderType(), var10.getOpenPrice(), var10.getInstrumentInfo().tickSize);
            if (var7 > 0.0) {
               var7 = this.fixByHighLow(var7, var3, var5);
               if (!OrderTypes.isStopOrder(var10.getOrderType()) && !OrderTypes.isLimitOrder(var10.getOrderType())) {
                  var10.setOpenPrice(var7);
               } else {
                  var10.setOpenPrice(var10.getPriceByType(var10.getOrderType(), var7, var7, this.slippage));
               }

               this.VerboseLog(
                  "Order filled with id "
                     + var10.getOrderId()
                     + ", symbol="
                     + var10.getSymbol()
                     + " reached: "
                     + SQUtils.d2(var7)
                     + " at "
                     + SQTime.toFullDateMinuteString(var1.getTime())
               );
               var10.setOpenTime(var1.getTime());
               var10.fillOrder(var1);
               var10.setLastAction((byte)1);
               var10.setCommSwap(this.computeCommissionsOnOpen(var10));
               this.statsOpenedPositions.add(var10);
            }
         }
      }
   }

   protected double fixByHighLow(double var1, double var3, double var5) {
      if (var1 < var5) {
         return var5;
      } else {
         return var1 > var3 ? var3 : var1;
      }
   }

   protected double priceLevelReached(TickEvent var1, double var2, double var4, int var6, int var7, double var8, double var10) {
      if (this.currentTick.getTime() == -1L) {
         return 0.0;
      }

      if (var7 == 6) {
         if (var4 <= var8) {
            if (var1.isBarOpen() && this.currentTick.getBid() <= var8) {
               return this.currentTick.getBid();
            }

            return var8;
         }
      } else if (var7 == 5) {
         if (var2 >= var8) {
            if (var1.isBarOpen() && this.currentTick.getAsk() >= var8) {
               return this.currentTick.getAsk();
            }

            return var8;
         }
      } else if (var7 == 4) {
         if (var2 >= var8 * (1.0 + this.LimitOver / 100.0)) {
            if (var1.isBarOpen() && this.currentTick.getBid() >= var8) {
               return this.currentTick.getBid();
            }

            return var8;
         }
      } else if (var7 == 3 && var4 <= var8 * (1.0 - this.LimitOver / 100.0)) {
         if (var1.isBarOpen() && this.currentTick.getAsk() <= var8) {
            return this.currentTick.getAsk();
         }

         return var8;
      }

      return 0.0;
   }

   private void increaseBarsInTrade(int var1) {
      for (int var2 = 0; var2 < this.liveOrders.size(); var2++) {
         LiveOrderObj var3 = (LiveOrderObj)this.liveOrders.get(var2);
         if (var3.getSymbolHash() == var1) {
            var3.increaseBarsInTrade();
         }
      }
   }

   private void regTradedSymbol(String var1) {
      if (!this.tradedSymbols.contains(var1)) {
         this.tradedSymbols.add(var1);
      }
   }

   public ObjectArrayList<String> getTradedSymbols() {
      return this.tradedSymbols;
   }

   public void VerboseLog(String var1) {
      if (this.verboseLog == null) {
         Log.debug(var1);
      } else {
         this.verboseLog.print(var1);
      }
   }

   protected double computeCommissionsOnOpen(LiveOrderObj var1) {
      if (this.commission != null) {
         try {
            return -this.commission.computeCommissionsOnOpen(var1, var1.getInstrumentInfo().tickSize, var1.getInstrumentInfo().pointValue);
         } catch (Exception var3) {
            Log.error("Exception computing commissions", var3);
         }
      }

      return 0.0;
   }

   private double getCommissionSwap(LiveOrderObj var1) {
      double var2 = var1.getCommSwap();
      if (this.commission != null) {
         try {
            var2 -= this.commission.computeCommissionsOnClose(var1, var1.getInstrumentInfo().tickSize, var1.getInstrumentInfo().pointValue);
         } catch (Exception var6) {
            Log.error("Exception computing commissions", var6);
         }
      }

      if (this.swap != null && this.swap.isUsed()) {
         try {
            var2 += SwapCalculator.calculate(var1, this.swap);
         } catch (Exception var5) {
            Log.error("Exception computing swap", var5);
         }
      }

      return var2;
   }

   public Long2FloatRBTreeMap getWorstDailyEquity() {
      return this.dailyEquityValues;
   }

   public Long2FloatRBTreeMap getMaxDailyDDValues() {
      return this.maxDailyDDValues;
   }

   public Long2FloatRBTreeMap getMaxDailyDDPctValues() {
      return this.maxDailyDDPctValues;
   }

   private static double specialSubtraction(double var0, double var2) {
      double var4 = var0 - var2;
      if (var4 < 0.0) {
         var4 = 0.0;
      }

      return -1.0 * var4;
   }

   private double getPercentageDD(double var1, double var3) {
      return !(var3 <= 0.0) && !(var1 > var3) ? var1 / (var3 / 100.0) : -1.0;
   }

   public double getMaxOpenDrawdown() {
      return this.maxOpenDrawdown;
   }

   public double getMaxOpenDrawdownPct() {
      return this.maxOpenDrawdownPct;
   }
}
