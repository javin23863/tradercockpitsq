package com.strategyquant.tradinglib.simulator.impl;

import com.strategyquant.datalib.InstrumentInfo;
import com.strategyquant.datalib.TickEvent;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.datalib.ticksimulator.TSTickSimulator;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.tradinglib.BadStrategyException;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.CommissionsMethod;
import com.strategyquant.tradinglib.ILiveOrder;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrderTypes;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.SwapMethod;
import com.strategyquant.tradinglib.Trader;
import com.strategyquant.tradinglib.TradingOption;
import com.strategyquant.tradinglib.event.ITradingEventListener;
import com.strategyquant.tradinglib.event.TradingEventObj;
import com.strategyquant.tradinglib.event.TradingEventsArray;
import com.strategyquant.tradinglib.execution.ReservedBarsType;
import com.strategyquant.tradinglib.options.TradingOptions;
import com.strategyquant.tradinglib.setup.BarUpdateInfo;
import com.strategyquant.tradinglib.simulator.ITradingSimulator;
import com.strategyquant.tradinglib.strategy.LiveOrderObj;
import com.strategyquant.tradinglib.strategy.MarketData;
import com.strategyquant.tradinglib.swap.SwapCalculator;
import it.unimi.dsi.fastutil.longs.Long2FloatRBTreeMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TradestationSimulator extends AbstractTradingSimulator {
   public static final Logger Log = LoggerFactory.getLogger("TradestationSimulator");
   public static final Logger BacktestLog = LoggerFactory.getLogger("BacktestLog");
   private static final long DAY_SECONDS = 86400000L;
   protected MarketPriceHolder marketPriceHolder = new MarketPriceHolder();
   protected OrdersList closedTrades;
   protected Long2FloatRBTreeMap dailyEquityValues;
   private long lastDayStartTime = -1L;
   private long nextDayStartTime = -1L;
   private float worstDailyEquity;
   private int orderId = -1000;
   private int connectionHash;
   private int lastSymbolHash;
   protected TickEvent tickData;
   private ChartData chartData = null;
   private TradingEventsArray tradingEventsArray = new TradingEventsArray();
   private float accountBalance;
   private double accountEquity;
   private float initialCapital;
   private double slippage;
   private CommissionsMethod commission;
   private SwapMethod swap;
   private boolean computeDailyStats = true;
   private ObjectArrayList<LiveOrderObj> preparedTrades;
   private ObjectArrayList<LiveOrderObj> tradesToClose;
   private ObjectArrayList<LiveOrderObj> tradesToCloseOnClose;
   protected double lastFilledPriceThisBar;
   private long lastTickTime;
   private double lastTickPrice;
   private double lastBidPrice;
   private double lastSpread;
   protected LiveOrderObj[] exitOrders = null;
   protected int exitOrdersCount = 0;
   private String connectionName;
   protected double bouncingTicksPct = 0.1;
   protected boolean useInitialSLPT = false;
   private final String initialSLComment = "InitialSL";
   private final String initialPTComment = "InitialPT";

   public TradestationSimulator() {
      super(new ReservedBarsType(1, 0, 0), new TSTickSimulator());
      this.preparedTrades = new ObjectArrayList();
      this.tradesToClose = new ObjectArrayList();
      this.tradesToCloseOnClose = new ObjectArrayList();
   }

   @Override
   public ITradingSimulator clone() {
      TradestationSimulator var1 = new TradestationSimulator();
      var1.setTestPrecision(this.getTestPrecision());
      return var1;
   }

   private void processNewTick(long var1, double var3, double var5, BarUpdateInfo var7) throws TradingException {
      if (var7 != null) {
         if (var7.eventType == 3) {
            return;
         }

         if (var7.eventType == 2) {
            this.lastFilledPriceThisBar = 0.0;
         }
      }

      if (this.computeDailyStats) {
         this.tryUpdateDailyEquity();
      }

      this.lastFilledPriceThisBar = 0.0;
      if (this.tradesToClose.size() > 0 && var7.eventType == 2) {
         this.closePreparedOrders(this.tradesToClose);
      }

      if (this.tradesToCloseOnClose.size() > 0) {
         this.closePreparedOrders(this.tradesToCloseOnClose);
      }

      if (this.preparedTrades.size() > 0) {
         this.openPreparedOrders();
      }

      if (this.liveOrdersCount > 0) {
         this.accountEquity = -1.0;
      }

      TickEvent var8 = this.marketPriceHolder.getCurrentTick(this.lastSymbolHash);
      this.lastBidPrice = var8.getBid();
      this.lastSpread = var8.getAsk() - var8.getBid();
      double var9 = var3 - var5;

      while (this.liveOrdersCount != 0 || this.exitOrdersCount != 0) {
         this.checkSLPT(var8, this.lastSymbolHash, var1, var3, var5, var9, var7);
         this.checkStopLimitOrders(var8, this.lastSymbolHash, var1, var3, var5, var9, var7);
         double var11 = var8.getBid();
         if (this.lastBidPrice == var11) {
            break;
         }

         var8.setAsk(this.lastBidPrice);
         var8.setBid(this.lastBidPrice);
      }

      if (this.liveOrdersCount > 0 && this.computeDailyStats) {
         this.updateMAE_MFE();
      }
   }

   private void printLiveOrders() {
   }

   protected boolean checkPriceLevelCorrectness(int var1, double var2, double var4) {
      return true;
   }

   @Override
   public boolean fillAtRealAskBidPrice() {
      return false;
   }

   protected double getSLPTFilledPrice(double var1, double var3, int var5) {
      return var5 == 2 ? var1 : var3;
   }

   @Override
   public void initSettings(SettingsMap var1) throws TradingException {
      if (var1.containsKey("MoneyManagement.InitialCapital") && var1.get("MoneyManagement.InitialCapital") instanceof Double) {
         this.initialCapital = (float)var1.getDouble("MoneyManagement.InitialCapital");
         this.accountBalance = this.initialCapital;
         this.accountEquity = this.accountBalance;
         this.slippage = 0.0;
         if (var1.containsKey("Slippage") && var1.get("Slippage") instanceof Double || var1.get("Slippage") instanceof Integer) {
            this.slippage = var1.getDouble("Slippage");
         }

         if (var1.containsKey("Commission") && var1.get("Commission") instanceof CommissionsMethod) {
            this.commission = (CommissionsMethod)var1.get("Commission");
         }

         if (var1.containsKey("Swap") && var1.get("Swap") instanceof SwapMethod) {
            this.swap = (SwapMethod)var1.get("Swap");
         }

         this.storePendingOrders = !Boolean.parseBoolean(MainApp.settings().get("dontStorePendingOrders", "true"));
         this.useInitialSLPT = false;
         if (var1.containsKey("TradingOptions") && var1.get("TradingOptions") instanceof TradingOptions) {
            TradingOptions var2 = (TradingOptions)var1.get("TradingOptions");

            for (int var3 = 0; var3 < var2.size(); var3++) {
               TradingOption var4 = var2.get(var3);
               if (var4.getClass().getSimpleName().equals("UseInitialSLPT")) {
                  try {
                     this.useInitialSLPT = (Boolean)var4.getParameterValue("UseInitialSLPT");
                  } catch (Exception var6) {
                     Log.error("Unable to read UseInitialSLPT value", var6);
                     this.useInitialSLPT = false;
                  }
                  break;
               }
            }
         }
      } else {
         throw new TradingException("Setting 'TradingSetup.InitialCapital' is not set or has incorrect value! It must be double!");
      }
   }

   @Override
   public void initialize(MarketData var1) {
      this.liveOrders = new LiveOrderObj[100];
      this.liveOrdersCount = 0;
      this.exitOrders = new LiveOrderObj[100];
      this.exitOrdersCount = 0;
      this.closedTrades = new OrdersList("ClosedTrades");
      this.dailyEquityValues = new Long2FloatRBTreeMap();
      this.orderId = 1;
      this.tickData = new TickEvent();

      for (int var2 = 0; var2 < var1.getChartsCount(); var2++) {
         if (this.connectionHash == var1.Chart(var2).getConnectionHash()) {
            var1.Chart(var2).getMinDistance();
            this.marketPriceHolder.registerNewSymbol(var1.Chart(var2));
         }
      }

      this.tradingSymbolHash = var1.Chart(0).Symbol.hashCode();
   }

   @Override
   public void destroy() {
      if (this.closedTrades != null) {
         this.closedTrades.clear();
      }
   }

   @Override
   public void setConnection(String var1) {
      this.connectionName = var1;
      this.connectionHash = var1.hashCode();
   }

   @Override
   public void eventNewTick(TickEvent var1, double var2, double var4, BarUpdateInfo var6) throws TradingException {
      if (var1.getConnectionHash() == this.connectionHash) {
         this.lastSymbolHash = var1.getSymbolHash();
         if (this.lastSymbolHash == this.tradingSymbolHash) {
            TickEvent var7 = this.marketPriceHolder.setNewTick(this.lastSymbolHash, var1);
            this.lastTickTime = var7.getTime();
            this.lastTickPrice = var7.getBid();
            if (var7 != null) {
               this.tickData = var7;
               if (this.getTestPrecision() != 5) {
                  this.processNewTick(var1.getTime(), var2, var4, var6);
               }
            }
         }
      }
   }

   private void closePreparedOrders(ObjectArrayList<LiveOrderObj> var1) {
      if (var1.size() != 0) {
         ObjectListIterator var2 = var1.iterator();

         while (var2.hasNext()) {
            LiveOrderObj var3 = (LiveOrderObj)var2.next();
            int var4 = var3.getWaitingBars();
            if (var4 > 0) {
               var3.setWaitingBars(var4 - 1);
            } else {
               var2.remove();
               if (this.removeOpenTrade(var3)) {
                  this.tickData = this.marketPriceHolder.getCurrentTick(var3.getSymbolHash());
                  if (OrderTypes.isMarketOrder(var3.getOrderType())) {
                     double var5 = var3.getPriceByType(OrderTypes.getOppositeType(var3.getOrderType()), this.tickData.getBid(), this.tickData.getAsk(), 0.0);
                     var3.setClosePrice(var5);
                  } else {
                     var3.setClosePrice(var3.getOpenPrice());
                  }

                  var3.setCloseTime(this.tickData.getTime());
                  var3.setLastAction((byte)2);
                  var3.setWorkingStatus((byte)-30);
                  var3.setOrderStatus((byte)3);
                  this.accountBalance = (float)(this.accountBalance + var3.getPL());
                  if (var3.isMarketOrder()) {
                     this.accountBalance = (float)(this.accountBalance - this.getCommSlippage(var3));
                  }

                  if (this.storePendingOrders || !var3.isPendingOrder() && !var3.isExitOrder()) {
                     this.fixMAEMFEByRealPrice(var3);
                     var3.transformMAE_MFE();
                     this.addToClosedTrades(var3);
                  }
               }
            }
         }
      }
   }

   private double getCommSlippage(LiveOrderObj var1) {
      return -1.0 * this.getCommissionSwap(var1) + this.getSlippageInMoney(var1);
   }

   private void addToClosedTrades(LiveOrderObj var1) {
      if (var1.getOpenTime() != var1.getCloseTime()) {
         var1.increaseBarsInTrade();
      }

      var1.setCommSwap(this.getCommissionSwap(var1));
      var1.setSlippageInMoney(this.getSlippageInMoney(var1));
      this.closedTrades.add(var1);
   }

   private double getCommissionSwap(LiveOrderObj var1) {
      if (var1.isPendingOrder()) {
         return 0.0;
      }

      double var2 = var1.getCommSwap();
      if (this.commission != null) {
         if (this.chartData == null || this.chartData.getSymbolHash() != var1.getSymbolHash()) {
            this.chartData = this.marketPriceHolder.getChartData(var1.getSymbolHash());
         }

         try {
            var2 -= this.commission.computeCommissionsOnOpen(var1, this.chartData.getInstrumentInfo().tickSize, this.chartData.getInstrumentInfo().pointValue);
            var2 -= this.commission.computeCommissionsOnClose(var1, this.chartData.getInstrumentInfo().tickSize, this.chartData.getInstrumentInfo().pointValue);
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

   private double getSlippageInMoney(LiveOrderObj var1) {
      return var1.isPendingOrder() ? 0.0 : 2.0 * this.slippage;
   }

   private void openPreparedOrders() {
      while (this.preparedTrades.size() > 0) {
         LiveOrderObj var1 = (LiveOrderObj)this.preparedTrades.get(0);
         this.preparedTrades.remove(0);

         try {
            if (!var1.isMarketOrder()) {
               throw new Exception("Not implemented here!");
            }

            this.tickData = this.marketPriceHolder.getCurrentTick(var1.getSymbolHash());
            byte var2 = var1.getOrderType();
            double var3 = var1.getOpenPrice();
            var1.setOpenTime(this.tickData.getTime());
            var1.setOpenPrice(var1.getPriceByType(var2, this.tickData.getBid(), this.tickData.getAsk(), 0.0));
            var1.setOriginalOpenPrice(var1.getOpenPrice());
            var1.setWorkingStatus((byte)-30);
            var1.setFilled(true);
            this.addOpenTrade(var1);
            this.openInitialSLOrder(var1, var3, false);
            this.openInitialPTOrder(var1, var3, false);
         } catch (Exception var5) {
            Log.error("openPreparedOrders()", var5);
         }
      }
   }

   private void tryUpdateDailyEquity() {
      long var1 = this.tickData.getTime();
      if (this.lastDayStartTime == -1L) {
         this.updateDayStartTimes(var1);
      } else {
         if (var1 < this.nextDayStartTime) {
            return;
         }

         this.dailyEquityValues.addTo(this.lastDayStartTime, this.worstDailyEquity);
         this.updateDayStartTimes(var1);
      }

      this.worstDailyEquity = this.liveOrdersCount > 0 ? Float.MAX_VALUE : this.accountBalance - this.initialCapital;
   }

   private void updateDayStartTimes(long var1) {
      this.lastDayStartTime = SQTime.getDateInMs(var1);
      this.nextDayStartTime = this.lastDayStartTime + 86400000L;
   }

   private void updateMAE_MFE() {
      float var1 = this.accountBalance;

      for (int var4 = 0; var4 < this.liveOrdersCount; var4++) {
         LiveOrderObj var5 = this.liveOrders[var4];
         if (!var5.isPendingOrder() && !var5.isClosedOrder()) {
            double var2;
            if (var5.isLong()) {
               var2 = this.tickData.getBid() - var5.getOpenPrice();
            } else {
               var2 = var5.getOpenPrice() - this.tickData.getAsk();
            }

            if (var5.getMFE() < var2) {
               var5.setMFE((float)var2);
            }

            var2 *= -1.0;
            if (var5.getMAE() < var2) {
               var5.setMAE((float)var2);
            }

            var5.setTickData(this.tickData.getAsk(), this.tickData.getBid());
            var1 = (float)(var1 + var5.getPL());
         }
      }

      this.accountEquity = var1;
      this.worstDailyEquity = Math.min(var1 - this.initialCapital, this.worstDailyEquity);
   }

   protected void checkSLPT(TickEvent var1, int var2, long var3, double var5, double var7, double var9, BarUpdateInfo var11) {
      LiveOrderObj var12 = this.findExistingMarketOrder(-1, -1);
      if (var12 != null) {
         boolean var13 = false;
         boolean var14 = false;

         for (int var15 = 0; var15 < this.exitOrdersCount; var15++) {
            LiveOrderObj var16 = this.exitOrders[var15];
            if (var16 != null && var16.getSymbolHash() == var2) {
               if (!var16.isExitOrder()) {
                  throw new IllegalArgumentException("This shouldn't happen");
               }

               if (var16.getBarsInTrade() >= 1) {
                  this.removeClosingTradeNoFix(var15);
                  var14 = true;
               } else {
                  int var17 = var16.getMagicNumber();
                  if (var17 == 0 || var17 == var12.getMagicNumber()) {
                     boolean var18 = var16.getOrderType() == 102 || var16.getOrderType() == 100;
                     boolean var19 = var16.getOrderType() == 103 || var16.getOrderType() == 101;
                     if ((this.useInitialSLPT && (var18 || var19) || var16.getOpenTime() >= var12.getOpenTime())
                        && (!var18 && !var19 || var16.getOpenTime() >= var12.getOpenTime())) {
                        var16.setTickData(this.tickData.getAsk(), this.tickData.getBid());
                        double var20 = this.priceLevelReached(var1, var2, var16, var11);
                        if (var20 != 0.0
                           && !this.stopLimitShouldBeFilledFirst(var1, var2, var16.getOrderType(), var16.getOpenPrice(), var11)
                           && !this.otherExitStopShouldBeFilledFirst(var1, var15, var2, var16.getOrderType(), var16.getOpenPrice(), var11)) {
                           byte var22 = var16.getOrderType();
                           if (var22 != 100 && var22 != 101) {
                              var20 = this.fixByHighLow(var20, var5, var7);
                           } else {
                              var20 = this.fixByHighLow(var20, var5 + this.lastSpread, var7 + this.lastSpread);
                           }

                           var20 = this.getStopLimitFillPrice(var20, var16);
                           if (var11.eventType != 2) {
                              this.tradestationBouncingTicks(
                                 var1, this.bouncingTicksPct, var5, var7, var9, var2, var16.getOrderType(), var16.getInstrumentInfo(), var16.getOpenPrice()
                              );
                           }

                           this.lastFilledPriceThisBar = var20;
                           this.closeOrder(var12, var20, this.getOrderCloseType(var16.getOrderType()));
                           this.removeOpenTrade(var12, false);
                           var13 = true;
                           this.removeClosingTradeNoFix(var15);
                           var14 = true;

                           try {
                              var16.evaluateActionListeners(7, null);
                           } catch (TradingException var24) {
                              Log.error("Error while calling Order filled action listeners", var24);
                           }
                           break;
                        }
                     }
                  }
               }
            }
         }

         if (var13) {
            this.fixRemovedElements();
         }

         if (var14) {
            this.fixRemovedClosingElements();
         }
      }
   }

   protected boolean otherExitStopShouldBeFilledFirst(TickEvent var1, int var2, int var3, byte var4, double var5, BarUpdateInfo var7) {
      for (int var10 = 0; var10 < this.exitOrdersCount; var10++) {
         LiveOrderObj var11 = this.exitOrders[var10];
         if (var11 != null
            && var10 != var2
            && var11.getSymbolHash() == var3
            && var11.getBarsInTrade() != 1
            && (this.liveOrdersCount <= 1 || this.findExistingMarketOrder(var11.getMagicNumber(), var10) == null)) {
            double var8 = this.priceLevelReached(var1, var3, var11, var7);
            if (var8 != 0.0) {
               byte var12 = var11.getOrderType();
               if (var12 == 102 && var4 == 102) {
                  if (var11.getOpenPrice() > var5) {
                     return true;
                  }
               } else if (var12 == 100 && var4 == 100 && var11.getOpenPrice() < var5) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   protected boolean stopLimitShouldBeFilledFirst(TickEvent var1, int var2, byte var3, double var4, BarUpdateInfo var6) {
      for (int var9 = 0; var9 < this.liveOrdersCount; var9++) {
         LiveOrderObj var10 = this.liveOrders[var9];
         if (var10 != null && var10.getSymbolHash() == var2 && !var10.isMarketOrder()) {
            if (var10.isExitOrder()) {
               throw new IllegalArgumentException("This shouldn't happen");
            }

            if (var10.getBarsInTrade() != 1
               && (var10.getExitIndex() >= 0 || this.liveOrdersCount <= 1 || this.findExistingMarketOrder(var10.getMagicNumber(), var9) == null)) {
               double var7 = this.priceLevelReached(var1, var2, var10, var6);
               if (var7 != 0.0) {
                  byte var11 = var10.getOrderType();
                  if (var11 == 6 && var3 == 102) {
                     return var10.getOpenPrice() > var4;
                  }

                  if (var11 == 5 && var3 == 100) {
                     return var10.getOpenPrice() < var4;
                  }
               }
            }
         }
      }

      return false;
   }

   protected byte getOrderCloseType(byte var1) {
      return (byte)(var1 != 100 && var1 != 102 ? 3 : 2);
   }

   protected void checkStopLimitOrders(TickEvent var1, int var2, long var3, double var5, double var7, double var9, BarUpdateInfo var11) throws TradingException {
      boolean var14 = false;
      boolean var15 = false;

      for (int var16 = 0; var16 < this.liveOrdersCount; var16++) {
         LiveOrderObj var17 = this.liveOrders[var16];
         if (var17 != null && var17.getSymbolHash() == var2 && !var17.isMarketOrder()) {
            if (var17.isExitOrder()) {
               throw new IllegalArgumentException("This shouldn't happen");
            }

            if (!var17.usesATM() && var17.getBarsInTrade() == 1) {
               this.removeOpenTradeNoFix(var16);
               var14 = true;
            } else {
               if (!var17.usesATM() && this.liveOrdersCount > 1) {
                  LiveOrderObj var18 = this.findExistingMarketOrder(var17.getMagicNumber(), var16);
                  if (var18 != null && !OrderTypes.areOppositeOrders(var17.getOrderType(), var18.getOrderType())) {
                     continue;
                  }
               }

               double var30 = var17.getOpenPrice();
               boolean var20 = false;
               double var12 = this.priceLevelReached(var1, var2, var17, var11);
               if (var12 != 0.0) {
                  byte var21 = var17.getOrderType();
                  if (var21 != 5 && var21 != 3) {
                     var12 = this.fixByHighLow(var12, var5, var7);
                  } else {
                     var12 = this.fixByHighLow(var12, var5 + this.lastSpread, var7 + this.lastSpread);
                  }

                  var12 = this.getStopLimitFillPrice(var12, var17);
                  var17.setOpenTime(this.tickData.getTime());
                  var17.setOpenPrice(var12);
                  if (!var17.usesATM() && this.closeExistingOppositeDirectionLiveOrders(var16, var2, var17)) {
                     var14 = true;
                  }

                  var15 = var14;
                  if (var11.eventType != 2) {
                     var20 = this.tradestationBouncingTicks(
                        var1, this.bouncingTicksPct, var5, var7, var9, var2, var17.getOrderType(), var17.getInstrumentInfo(), var17.getOpenPrice()
                     );
                  }

                  this.lastFilledPriceThisBar = var12;
                  var17.fillOrder(this.tickData);
                  var17.setLastAction((byte)1);
                  var17.setFilled(true);
                  if (var17.usesATM()) {
                     this.tryPairMarketOrders(var17);
                     var17.setWorkingStatus((byte)-30);
                     var17.setOrderStatus((byte)3);
                     var17.setCloseType((byte)1);
                     this.removeOpenTrade(var17, false);
                     var14 = true;
                  } else {
                     double var22 = var12;
                     LiveOrderObj var24 = this.checkSLPTForOrder(var2, var17, var11);
                     if (var24 == null && var20) {
                        var24 = this.checkSLPTForOrderByHighLow(var2, var17, var11, var1.getAsk(), var1.getBid());
                        if (var24 != null) {
                           var22 = var24.getOpenPrice();
                        }
                     }

                     if (var24 != null) {
                        this.closeOrder(var17, var22, this.getOrderCloseType(var24.getOrderType()));
                        this.removeOpenTrade(var17, false);
                        var14 = true;
                        this.removeOpenTrade(var24, false);
                        var15 = true;
                     }

                     if (var24 == null) {
                        this.openInitialSLOrder(var17, var30, true);
                        this.openInitialPTOrder(var17, var30, true);
                     }
                  }

                  try {
                     var17.evaluateActionListeners(7, null);
                  } catch (TradingException var25) {
                     Log.error("Error while calling Order filled action listeners", var25);
                  }
               }
            }
         }
      }

      for (int var28 = 0; var28 < this.exitOrdersCount; var28++) {
         LiveOrderObj var29 = this.exitOrders[var28];
         if (var29 != null && var29.getSymbolHash() == var2 && !var29.isMarketOrder() && !var29.usesATM() && var29.getBarsInTrade() == 1) {
            this.exitOrders[var28] = null;
            var15 = true;
         }
      }

      if (var14) {
         this.fixRemovedElements();
      }

      if (var15) {
         this.fixRemovedClosingElements();
      }
   }

   private boolean closeExistingOppositeDirectionLiveOrders(int var1, int var2, LiveOrderObj var3) {
      int var4 = var3.getDirection() * -1;
      boolean var5 = false;

      for (int var6 = 0; var6 < this.liveOrdersCount; var6++) {
         if (var6 != var1) {
            LiveOrderObj var7 = this.liveOrders[var6];
            if (var7 != null && var7.getSymbolHash() == var2 && var7.isMarketOrder() && var7.getDirection() == var4) {
               this.closeOrder(var7, var3.getOpenPrice(), (byte)7);
               this.removeOpenTrade(var7, false);
               var5 = true;
            }
         }
      }

      return var5;
   }

   protected double fixByHighLow(double var1, double var3, double var5) {
      if (var1 < var5) {
         return var5;
      } else {
         return var1 > var3 ? var3 : var1;
      }
   }

   private LiveOrderObj checkSLPTForOrder(int var1, LiveOrderObj var2, BarUpdateInfo var3) {
      double var4 = var2.getOpenPrice();
      int var6 = var2.getDirection();

      for (int var7 = 0; var7 < this.exitOrdersCount; var7++) {
         LiveOrderObj var8 = this.exitOrders[var7];
         if (var8 != null && var8.getSymbolHash() == var1) {
            if (!var8.isExitOrder()) {
               throw new IllegalArgumentException("This shouldn't happen");
            }

            if (var8.getBarsInTrade() < 1) {
               int var9 = var8.getMagicNumber();
               if (var9 == 0 || var9 == var2.getMagicNumber()) {
                  byte var10 = var8.getOrderType();
                  double var11 = var8.getOpenPrice();
                  if (var2.getOriginalOpenTime() == var8.getOpenTime()) {
                     if ((var10 == 102 || var10 == 100)
                        && (OrderTypes.isStopOrder(var2.getOriginalOrderType()) || OrderTypes.isLimitOrder(var2.getOriginalOrderType()))) {
                        double var13 = this.computeRealSLFromOrder(var2, var8.getOpenPrice(), true);
                        var2.setSL(var13);
                        var2.setInitialSL(var13);
                        var11 = var13;
                        var8.setOpenPrice(var11);
                     }

                     if ((var10 == 103 || var10 == 101)
                        && (OrderTypes.isStopOrder(var2.getOriginalOrderType()) || OrderTypes.isLimitOrder(var2.getOriginalOrderType()))) {
                        double var15 = this.computeRealPTFromOrder(var2, var8.getOpenPrice(), true);
                        var2.setPT(var15);
                        var11 = var15;
                        var8.setOpenPrice(var11);
                     }
                  }

                  if (var6 > 0) {
                     if (var10 == 102) {
                        if (var4 <= var11) {
                           return var8;
                        }
                     } else if (var10 == 103 && var4 >= var11) {
                        return var8;
                     }
                  } else if (var10 == 100) {
                     if (var4 >= var11) {
                        return var8;
                     }
                  } else if (var10 == 101 && var4 <= var11) {
                     return var8;
                  }
               }
            }
         }
      }

      return null;
   }

   private LiveOrderObj checkSLPTForOrderByHighLow(int var1, LiveOrderObj var2, BarUpdateInfo var3, double var4, double var6) {
      double var8 = var2.getOpenPrice();
      int var10 = var2.getDirection();

      for (int var11 = 0; var11 < this.exitOrdersCount; var11++) {
         LiveOrderObj var12 = this.exitOrders[var11];
         if (var12 != null && var12.getSymbolHash() == var1) {
            if (!var12.isExitOrder()) {
               throw new IllegalArgumentException("This shouldn't happen");
            }

            if (var12.getBarsInTrade() < 1) {
               int var13 = var12.getMagicNumber();
               if (var13 == 0 || var13 == var2.getMagicNumber()) {
                  byte var14 = var12.getOrderType();
                  double var15 = var12.getOpenPrice();
                  if (var2.getOriginalOpenTime() == var12.getOpenTime()
                     && (OrderTypes.isStopOrder(var2.getOriginalOrderType()) || OrderTypes.isLimitOrder(var2.getOriginalOrderType()))) {
                     var15 = var12.getOpenPrice();
                     if (var10 > 0) {
                        if (var14 == 102) {
                           if (var6 <= var15) {
                              return var12;
                           }
                        } else if (var14 == 103 && var4 >= var15) {
                           return var12;
                        }
                     } else if (var14 == 100) {
                        if (var6 >= var15) {
                           return var12;
                        }
                     } else if (var14 == 101 && var4 <= var15) {
                        return var12;
                     }
                  }
               }
            }
         }
      }

      return null;
   }

   protected double getStopLimitFillPrice(double var1, LiveOrderObj var3) {
      byte var4 = var3.getOrderType();
      if (var4 != 101 && var4 != 103) {
         int var5 = var3.getDirection();
         if (this.lastFilledPriceThisBar <= 0.0) {
            return var1;
         }

         if (var5 > 0) {
            if ((OrderTypes.isStopOrder(var4) || OrderTypes.isLimitOrder(var4)) && this.lastFilledPriceThisBar > var1) {
               return this.lastFilledPriceThisBar;
            }

            if (OrderTypes.isLimitOrder(var4) && this.lastFilledPriceThisBar < var1) {
               return this.lastFilledPriceThisBar;
            }
         } else if (var5 < 0) {
            if ((OrderTypes.isStopOrder(var4) || OrderTypes.isLimitOrder(var4)) && this.lastFilledPriceThisBar < var1) {
               return this.lastFilledPriceThisBar;
            }

            if (OrderTypes.isLimitOrder(var4) && this.lastFilledPriceThisBar > var1) {
               return this.lastFilledPriceThisBar;
            }
         }

         return var1;
      } else {
         return var1;
      }
   }

   protected LiveOrderObj findExistingMarketOrder(int var1, int var2) {
      for (int var3 = 0; var3 < this.liveOrdersCount; var3++) {
         if (var3 != var2) {
            LiveOrderObj var4 = this.liveOrders[var3];
            if (var4 != null && !var4.isExitOrder() && !var4.usesATM() && var4.isMarketOrder() && (var1 == -1 || var4.getMagicNumber() == var1)) {
               return var4;
            }
         }
      }

      return null;
   }

   protected void closeOrder(LiveOrderObj var1, double var2, byte var4) {
      if (!var1.isExitOrder()) {
         this.tickData = this.marketPriceHolder.getCurrentTick(var1.getSymbolHash());
         var1.setCloseTime(this.tickData.getTime());
         var1.setClosePrice(var1.getClosePriceWithSlippage(0.0, var2, var4));
         var1.setLastAction((byte)2);
         var1.setWorkingStatus((byte)-30);
         var1.setOrderStatus((byte)3);
         var1.setCloseType(var4);
         this.accountBalance = (float)(this.accountBalance + var1.getPL());
         if (var1.isMarketOrder()) {
            this.accountBalance = (float)(this.accountBalance - this.getCommSlippage(var1));
         }

         if (this.storePendingOrders || !var1.isPendingOrder()) {
            this.fixMAEMFEByRealPrice(var1);
            var1.transformMAE_MFE();
            this.addToClosedTrades(var1);
         }
      }
   }

   @Override
   public ILiveOrder orderClose(StrategyBase var1, ILiveOrder var2, byte var3, int var4, ITradingEventListener var5) {
      LiveOrderObj var6 = (LiveOrderObj)var2;
      if (var6.isExitOrder()) {
         return var6;
      }

      if (var3 == 5 || var3 == 14) {
         this.removeOpenTrade(var6);
         var6.setClosePrice(this.lastTickPrice);
         var6.setCloseTime(this.lastTickTime);
         var6.setLastAction((byte)2);
         var6.setWorkingStatus((byte)-30);
         var6.setOrderStatus((byte)3);
         var6.setCloseType(var3);
         this.accountBalance = (float)(this.accountBalance + var6.getPL());
         if (var6.isMarketOrder()) {
            this.accountBalance = (float)(this.accountBalance - this.getCommSlippage(var6));
         }

         if (this.storePendingOrders || !var6.isPendingOrder()) {
            this.fixMAEMFEByRealPrice(var6);
            var6.transformMAE_MFE();
            this.addToClosedTrades(var6);
         }
      } else if (var3 != 13 && var3 != 16) {
         var6.setCloseType(var3);
         this.tradesToClose.add(var6);
      } else if (!this.tradesToCloseOnClose.contains(var6)) {
         var6.setCloseType(var3);
         this.tradesToCloseOnClose.add(var6);
      }

      return var6;
   }

   @Override
   public void eventBarUpdated(int var1) {
      if (var1 == 3) {
         this.increaseBarsInTrade();
      }
   }

   private void increaseBarsInTrade() {
      for (int var1 = 0; var1 < this.liveOrdersCount; var1++) {
         if (this.tickData.getTime() != this.liveOrders[var1].getOpenTime()) {
            this.liveOrders[var1].increaseBarsInTrade();
         }
      }

      for (int var2 = 0; var2 < this.exitOrdersCount; var2++) {
         if (this.tickData.getTime() != this.exitOrders[var2].getOpenTime()) {
            this.exitOrders[var2].increaseBarsInTrade();
         }
      }
   }

   @Override
   public ILiveOrder orderOpen(StrategyBase var1, ILiveOrder var2, int var3, ITradingEventListener var4) throws TradingException {
      LiveOrderObj var5 = (LiveOrderObj)var2;
      this.tickData = this.marketPriceHolder.getCurrentTick(var5.getSymbolHash());
      if (!this.checkOrder(var5)) {
         this.reuseTemporaryOrder(var5);
         return var5;
      }

      byte var6 = var5.getOrderType();
      var5.setOpenTime(this.tickData.getTime());
      var5.setOriginalOpenTime(var5.getOpenTime());
      var5.setATROnOpen(this.getATR(var1, var5.getSymbol()));
      byte var7;
      if (OrderTypes.isMarketOrder(var6)) {
         var5.setOpenPrice(var5.getPriceByType(var6, this.tickData.getBid(), this.tickData.getAsk(), 0.0));
         var5.setOriginalOpenPrice(var5.getOpenPrice());
         var5.setOrderStatus((byte)1);
         var7 = 1;
      } else {
         var5.setOrderStatus((byte)2);
         var7 = 2;
      }

      var5.setLastAction((byte)1);
      var5.setError(0, null);
      var5.setOrderId(this.orderId++);
      var5.setWorkingStatus((byte)-30);
      var5.setFilled(false);
      this.addPreparedTrade(var5);
      this.sendEvent(var1, var4, var7, var2);
      return var2;
   }

   protected void addOpenTrade(LiveOrderObj var1) throws TradingException {
      if (var1.usesATM()) {
         this.tryPairMarketOrders(var1);
         var1.setWorkingStatus((byte)-30);
         var1.setOrderStatus((byte)3);
         var1.setCloseType((byte)1);
         this.removeOpenTrade(var1, true);
      } else if (this.liveOrdersCount < this.liveOrders.length - 1) {
         this.liveOrders[this.liveOrdersCount++] = var1;
      } else {
         throw new BadStrategyException(32);
      }
   }

   private void tryPairMarketOrders(LiveOrderObj var1) {
      double var2 = var1.getSize();

      for (int var4 = 0; var4 < this.liveOrdersCount; var4++) {
         LiveOrderObj var5 = this.liveOrders[var4];
         if (var5 != null && var5.isMarketOrder() && !var5.usesATM() && var1.isLong() != var5.isLong()) {
            if (var5.getSize() >= var2) {
               var1.setClosePrice(var1.getOpenPrice());
               var1.setCloseTime(var1.getOpenTime());
               var1.setCloseType((byte)1);
               var1.setOriginalOpenTime(var5.getOriginalOpenTime());
               var1.setOpenPrice(var5.getOpenPrice());
               var1.setOpenTime(var5.getOpenTime());
               var1.setOrderType(var5.getOrderType());
               var1.setWorkingStatus((byte)-30);
               var1.setOrderStatus((byte)3);
               this.fixMAEMFEByRealPrice(var1);
               var1.transformMAE_MFE();
               this.accountBalance = (float)(this.accountBalance + var1.getPL());
               if (var1.isMarketOrder()) {
                  this.accountBalance = (float)(this.accountBalance - this.getCommSlippage(var1));
               }

               this.addToClosedTrades(var1);
               var5.setSize(var5.getSize() - var2);
               if (var5.getSize() == 0.0) {
                  this.removeOpenTrade(var5);
               }

               var2 = 0.0;
               break;
            }

            var5.setClosePrice(var1.getOpenPrice());
            var5.setCloseTime(var1.getOpenTime());
            var5.setCloseType((byte)1);
            var5.setWorkingStatus((byte)-30);
            var5.setOrderStatus((byte)3);
            var2 -= var5.getSize();
            var1.setSize(var2);
            if (var2 == 0.0) {
               break;
            }
         }
      }
   }

   protected void addPreparedTrade(LiveOrderObj var1) throws TradingException {
      if (var1.isMarketOrder()) {
         this.preparedTrades.add(var1);

         try {
            var1.evaluateActionListeners(7, null);
         } catch (TradingException var3) {
            Log.error("Error while calling Order filled action listeners", var3);
         }
      } else {
         if (var1.isExitOrder()) {
            if (this.exitOrdersCount >= this.exitOrders.length - 1) {
               throw new BadStrategyException(32);
            }

            this.exitOrders[this.exitOrdersCount++] = var1;
         } else {
            if (this.liveOrdersCount >= this.liveOrders.length - 1) {
               throw new BadStrategyException(32);
            }

            this.liveOrders[this.liveOrdersCount++] = var1;
         }
      }
   }

   private boolean checkOrder(LiveOrderObj var1) {
      int var2 = var1.getSymbolHash();
      if (!this.marketPriceHolder.containsSymbol(var2)) {
         String var12 = "Symbol '" + var1.getSymbol() + "' is not supported in this Trade Controller";
         var1.setError(1, var12);
         return false;
      }

      byte var3 = var1.getOrderType();
      double var4 = LiveOrderObj.getPriceByType(var3, this.tickData.getBid(), this.tickData.getAsk());
      double var6 = var4;
      if (var1.isPendingOrder()) {
         double var8 = var1.getOpenPrice();
         if (!this.checkPriceLevelCorrectness(var3, var6, var8)) {
            var1.setError(6, "Incorrect price level. Open price: " + var6 + ", price level: " + var8);
            return false;
         }

         if (!this.checkPriceLevelDistance(var2, var6, var8)) {
            var1.setError(6, "Incorrect price level distance. Open price: " + var6 + ", price level: " + var8);
            return false;
         }

         var6 = var8;
      }

      if (var1.getSL() != 0.0) {
         double var13 = var1.getSL();
         var3 = var1.getOrderTypeFromSLPT(-1);
         if (!this.checkPriceLevelCorrectness(var3, var6, var13)) {
            var1.setError(6, "Incorrect stop level. Open price: " + var6 + ", price level: " + var13);
            return false;
         }

         if (!this.checkPriceLevelDistance(var2, var6, var13)) {
            var1.setError(6, "Incorrect price level distance. Open price: " + var6 + ", price level: " + var13);
            return false;
         }
      }

      if (var1.getPT() != 0.0) {
         double var14 = var1.getPT();
         var3 = var1.getOrderTypeFromSLPT(1);
         if (!this.checkPriceLevelCorrectness(var3, var6, var14)) {
            var1.setError(6, "Incorrect stop level. Open price: " + var6 + ", price level: " + var14);
            return false;
         }

         if (!this.checkPriceLevelDistance(var2, var6, var14)) {
            var1.setError(6, "Incorrect price level distance. Open price: " + var6 + ", price level: " + var14);
            return false;
         }
      }

      return true;
   }

   private boolean checkPriceLevelDistance(int var1, double var2, double var4) {
      return true;
   }

   private void sendEvent(StrategyBase var1, ITradingEventListener var2, int var3, ILiveOrder var4) {
      TradingEventObj var5 = this.tradingEventsArray.getAvailableEventObject();
      var5.setEventType(var3);
      var5.setOrder(var4);

      try {
         var1.OnEvent(var5);
      } catch (Exception var8) {
         Log.error("Exception during strategy.onEvent()", var8);
      }

      if (var2 != null) {
         try {
            var2.OnTradingEvent(var5);
         } catch (Exception var7) {
            Log.error("Exception during OrderEventListener.onTradingEvent()", var7);
         }
      }

      var5.releaseEvent();
   }

   @Override
   public OrdersList getHistoryOrders() {
      return this.closedTrades;
   }

   @Override
   public int getHistoryOrdersCount() {
      return this.closedTrades.size();
   }

   @Override
   public Order getHistoryOrder(int var1) {
      return var1 >= 0 && var1 < this.closedTrades.size() ? this.closedTrades.get(var1) : null;
   }

   @Override
   public double getAccountBalance() {
      return this.accountBalance;
   }

   @Override
   public double getAccountEquity() {
      if (this.accountEquity == -1.0) {
         this.computeAccountEquity();
      }

      return this.accountEquity;
   }

   private void computeAccountEquity() {
      this.accountEquity = this.accountBalance;

      for (int var1 = 0; var1 < this.liveOrdersCount; var1++) {
         LiveOrderObj var2 = this.liveOrders[var1];
         this.accountEquity = this.accountEquity + var2.getPL();
      }
   }

   @Override
   public double getInitialBalance() {
      return this.initialCapital;
   }

   @Override
   public double getTotalPL() {
      return 0.0;
   }

   @Override
   public String getMainCurrency() {
      return null;
   }

   @Override
   public ILiveOrder orderModify(StrategyBase var1, ILiveOrder var2, int var3, ITradingEventListener var4) {
      for (int var5 = 0; var5 < this.liveOrdersCount; var5++) {
         LiveOrderObj var6 = this.liveOrders[var5];
         if (var6 == var2) {
            if (!this.checkOrderSLPT(var6)) {
               return var6;
            }

            var6.setSLFromModified();
            var6.setPTFromModified();
            return var2;
         }
      }

      if (this.useInitialSLPT) {
         for (int var7 = 0; var7 < this.preparedTrades.size(); var7++) {
            LiveOrderObj var8 = (LiveOrderObj)this.preparedTrades.get(var7);
            if (var8 == var2) {
               if (!this.checkOrderSLPT(var8)) {
                  return var8;
               }

               var8.setSLFromModified();
               var8.setPTFromModified();
            }
         }
      }

      return var2;
   }

   private boolean checkOrderSLPT(LiveOrderObj var1) {
      byte var2 = var1.getOrderType();
      double var3;
      if (var1.isPendingOrder()) {
         var3 = var1.getOpenPrice();
      } else {
         var3 = LiveOrderObj.getClosePriceByType(var2, this.tickData.getBid(), this.tickData.getAsk());
      }

      int var5 = var1.getSymbolHash();
      if (var1.modifiedSL > 0.0) {
         double var6 = var1.modifiedSL;
         var2 = var1.getOrderTypeFromSLPT(-1);
         if (!this.checkPriceLevelCorrectness(var2, var3, var6)) {
            var1.setError(6, null);
            return false;
         }

         if (!this.checkPriceLevelDistance(var5, var3, var6)) {
            var1.setError(6, null);
            return false;
         }
      }

      if (var1.modifiedPT > 0.0) {
         double var10 = var1.modifiedPT;
         var2 = var1.getOrderTypeFromSLPT(1);
         if (!this.checkPriceLevelCorrectness(var2, var3, var10)) {
            var1.setError(6, null);
            return false;
         }

         if (!this.checkPriceLevelDistance(var5, var3, var10)) {
            var1.setError(6, null);
            return false;
         }
      }

      return true;
   }

   @Override
   public void eventEndOfTest() {
      this.eventNoTrade();
      this.liveOrders = null;
   }

   @Override
   public void eventNoTrade() {
      float var1 = this.accountBalance;

      for (int var2 = 0; var2 < this.liveOrdersCount; var2++) {
         LiveOrderObj var3 = this.liveOrders[var2];
         if (!var3.isExitOrder()) {
            this.tickData = this.marketPriceHolder.getCurrentTick(var3.getSymbolHash());
            double var4 = LiveOrderObj.getPriceByType(OrderTypes.getOppositeType(var3.getOrderType()), this.tickData.getBid(), this.tickData.getAsk());
            var3.setCloseTime(this.tickData.getTime());
            var3.setClosePrice(var4);
            var3.setLastAction((byte)2);
            var3.setWorkingStatus((byte)-30);
            var3.setOrderStatus((byte)3);
            var3.setCloseType((byte)4);
            this.fixMAEMFEByRealPrice(var3);
            var3.transformMAE_MFE();
            this.addToClosedTrades(var3);
            var1 = (float)(var1 + var3.getPL());
            this.removeOpenTradeNoFix(var2);
         }
      }

      this.liveOrdersCount = 0;
      this.accountEquity = var1;
   }

   @Override
   public int getEngineId() {
      return 56756755;
   }

   @Override
   public TickEvent getTickData() {
      return this.tickData;
   }

   @Override
   public Long2FloatRBTreeMap getWorstDailyEquity() {
      return this.dailyEquityValues;
   }

   @Override
   public int getBarEventType() {
      return 3;
   }

   @Override
   public void eventNewTick(TickEvent var1, double var2, double var4) {
   }

   @Override
   public boolean getApplyExitsAtTheEndOfRule() {
      return true;
   }

   @Override
   protected double getSLPTFilledPrice(double var1, double var3) {
      throw new IllegalArgumentException("Not used in Tradestation");
   }

   @Override
   protected boolean removeOpenTrade(LiveOrderObj var1) {
      return this.removeOpenTrade(var1, true);
   }

   @Override
   protected boolean removeOpenTrade(LiveOrderObj var1, boolean var2) {
      boolean var3 = false;

      for (int var4 = 0; var4 < this.liveOrdersCount; var4++) {
         if (this.liveOrders[var4] == var1) {
            this.removeOpenTradeNoFix(var4);
            var3 = true;
            break;
         }
      }

      if (var2 && var3) {
         this.fixRemovedElements();
      }

      if (!var3) {
         for (int var5 = 0; var5 < this.exitOrdersCount; var5++) {
            if (this.exitOrders[var5] == var1) {
               this.removeClosingTradeNoFix(var5);
               var3 = true;
               break;
            }
         }

         if (var2 && var3) {
            this.fixRemovedClosingElements();
         }
      }

      return var3;
   }

   protected void fixRemovedClosingElements() {
      int var1 = -1;

      for (int var2 = 0; var2 < this.exitOrdersCount; var2++) {
         if (this.exitOrders[var2] != null) {
            this.exitOrders[++var1] = this.exitOrders[var2];
         }
      }

      this.exitOrdersCount = var1 == -1 ? 0 : var1 + 1;
   }

   protected void removeClosingTradeNoFix(int var1) {
      if (var1 >= 0 && var1 < this.exitOrdersCount) {
         this.reuseTemporaryOrder(this.exitOrders[var1]);
         this.exitOrders[var1] = null;
      }
   }

   @Override
   public void evaluateActionListeners(int var1, StrategyBase var2) throws TradingException {
      for (int var3 = this.liveOrdersCount - 1; var3 >= 0; var3--) {
         LiveOrderObj var4 = this.liveOrders[var3];
         var4.evaluateActionListeners(var1, var2);
      }
   }

   protected boolean tradestationBouncingTicks(
      TickEvent var1, double var2, double var4, double var6, double var8, int var10, byte var11, InstrumentInfo var12, double var13
   ) {
      double var15 = 0.0;
      switch (var11) {
         case 3:
            var15 = 1.0;
            break;
         case 4:
            var15 = -1.0;
            break;
         case 5:
            var15 = -1.0;
            break;
         case 6:
            var15 = 1.0;
            break;
         case 100:
            var15 = -1.0;
            break;
         case 101:
            var15 = 1.0;
            break;
         case 102:
            var15 = 1.0;
            break;
         case 103:
            var15 = -1.0;
      }

      double var17 = var12.tickStep;
      double var19 = var8 / var17;
      double var21 = var2 * var19;
      var21 = Math.floor(var21);
      double var23 = var21 * var17;
      double var25 = var1.getBid();
      double var27 = var13 + var15 * var23;
      if (var27 < var6) {
         if (var6 < var25) {
            var27 = var6;
         } else {
            var27 = var25;
         }
      } else if (var27 > var4) {
         if (var4 > var25) {
            var27 = var4;
         } else {
            var27 = var25;
         }
      }

      if (var27 != var25) {
         var1.setAsk(var27);
         var1.setBid(var27);
         return true;
      } else {
         return false;
      }
   }

   private double priceLevelReached(TickEvent var1, int var2, LiveOrderObj var3, BarUpdateInfo var4) {
      if (var1.getTime() == -1L) {
         return 0.0;
      }

      byte var5 = var3.getOrderType();
      double var6 = var3.getOpenPrice();
      if (var5 == 6 || var5 == 102) {
         if (var1.getBid() <= var6) {
            if (var4 != null && var4.eventType == 2) {
               return var1.getBid();
            }

            return var6;
         }
      } else if (var5 == 5 || var5 == 100) {
         if (var1.getAsk() >= var6) {
            if (var4 != null && var4.eventType == 2) {
               return var1.getAsk();
            }

            return var6;
         }
      } else if (var5 == 4 || var5 == 103) {
         if (var1.getBid() >= var6) {
            if (var4 != null && var4.eventType == 2) {
               return var1.getBid();
            }

            return var6;
         }
      } else if ((var5 == 3 || var5 == 101) && var1.getAsk() <= var6) {
         if (var4 != null && var4.eventType == 2) {
            return var1.getAsk();
         }

         return var6;
      }

      return 0.0;
   }

   @Override
   public boolean supportsDuplicateTrades() {
      return false;
   }

   @Override
   public int getOpenOrdersCount(boolean var1) {
      if (this.liveOrdersCount == 0) {
         return 0;
      }

      int var2 = 0;

      for (int var3 = 0; var3 < this.liveOrdersCount; var3++) {
         LiveOrderObj var4 = this.liveOrders[var3];
         if ((var4.getPositionStatus() == 1 || var4.getPositionStatus() == 2)
            && (var1 || !this.tradesToClose.contains(var4))
            && (var1 || !this.tradesToCloseOnClose.contains(var4))) {
            var2++;
         }
      }

      return var2;
   }

   @Override
   public ILiveOrder getOpenOrder(int var1, boolean var2) {
      if (var1 >= 0 && var1 < this.liveOrdersCount) {
         int var3 = 0;

         for (int var4 = 0; var4 < this.liveOrdersCount; var4++) {
            LiveOrderObj var5 = this.liveOrders[var4];
            if ((var5.getPositionStatus() == 1 || var5.getPositionStatus() == 2)
               && (var2 || !this.tradesToClose.contains(var5))
               && (var2 || !this.tradesToCloseOnClose.contains(var5))) {
               if (var3 == var1) {
                  return var5;
               }

               var3++;
            }
         }

         throw new IllegalArgumentException("Trade index is out of range!");
      } else {
         throw new IllegalArgumentException("Trade index is out of range!");
      }
   }

   private void openInitialSLOrder(LiveOrderObj var1, double var2, boolean var4) {
      try {
         if (this.useInitialSLPT && var1.getSL() != 0.0 && var1.getSL() != -9.9999999E7) {
            int var5 = var1.getDirection() > 0 ? 102 : 100;
            LiveOrderObj var6 = this.createLiveOrder(var1.getTrader(), var1.getInstrumentInfo(), (byte)var5, var1.getSymbol());
            double var7 = this.computeRealSLFromOrder(var1, var1.getSL(), var4);
            var6.setOpenPrice(var7);
            var6.setComment("InitialSL");
            this.orderOpen(var1.getTrader().getStrategy(), var6, 0, null);
         }
      } catch (Exception var9) {
         Log.error("Opening InitialSL order failed", var9);
      }
   }

   private double computeRealSLFromOrder(LiveOrderObj var1, double var2, boolean var4) {
      int var5 = var1.isLong() ? -1 : 1;
      double var6 = 0.0;
      if (var4) {
         if (var5 > 0) {
            var6 = var1.getOpenPrice() + var2;
         } else {
            var6 = var1.getOpenPrice() - var2;
         }
      } else {
         var6 = var2;
      }

      return var6;
   }

   private double computeRealPTFromOrder(LiveOrderObj var1, double var2, boolean var4) {
      int var5 = var1.isLong() ? -1 : 1;
      double var6 = 0.0;
      if (var4) {
         if (var5 > 0) {
            var6 = var1.getOpenPrice() - var2;
         } else {
            var6 = var1.getOpenPrice() + var2;
         }
      } else {
         var6 = var2;
      }

      return var6;
   }

   private void openInitialPTOrder(LiveOrderObj var1, double var2, boolean var4) {
      try {
         if (this.useInitialSLPT && var1.getPT() != 0.0 && var1.getPT() != -9.9999999E7) {
            int var5 = var1.getDirection() > 0 ? 103 : 101;
            LiveOrderObj var6 = this.createLiveOrder(var1.getTrader(), var1.getInstrumentInfo(), (byte)var5, var1.getSymbol());
            double var7 = this.computeRealPTFromOrder(var1, var1.getPT(), var4);
            var6.setOpenPrice(var7);
            var6.setComment("InitialPT");
            this.orderOpen(var1.getTrader().getStrategy(), var6, 0, null);
         }
      } catch (Exception var9) {
         Log.error("Opening InitialPT order failed", var9);
      }
   }

   @Override
   public LiveOrderObj createLiveOrder(Trader var1, InstrumentInfo var2, byte var3, String var4, double var5) {
      var5 = this.fixTSStopPrice(var2, var3, var5);
      return super.createLiveOrder(var1, var2, var3, var4, var5);
   }

   private double fixTSStopPrice(InstrumentInfo var1, byte var2, double var3) {
      byte var5 = 0;
      if (var2 == 5 || var2 == 7 || var2 == 100) {
         var5 = 1;
      } else if (var2 == 6 || var2 == 8 || var2 == 102) {
         var5 = -1;
      }

      if (var5 == 0) {
         return var3;
      }

      if (var1.tickStep == Math.pow(10.0, -var1.decimals)) {
         return SQUtils.round(var3, var1.decimals);
      }

      var3 = SQUtils.round(var3, var1.decimals);
      double var6;
      if (var5 == -1) {
         var6 = Math.floor(var3 / var1.tickStep);
      } else {
         var6 = Math.ceil(var3 / var1.tickStep);
      }

      var3 = var6 * var1.tickStep;
      return SQUtils.round(var3, var1.decimals);
   }

   @Override
   public boolean IsMarketOpen() {
      return true;
   }
}
