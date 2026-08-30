package com.strategyquant.tradinglib.simulator.impl;

import com.strategyquant.datalib.TickEvent;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.datalib.session.Session;
import com.strategyquant.datalib.session.SessionManager;
import com.strategyquant.datalib.ticksimulator.DefaultTickSimulator;
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
import com.strategyquant.tradinglib.TradingOption;
import com.strategyquant.tradinglib.event.ITradingEventListener;
import com.strategyquant.tradinglib.event.TradingEventObj;
import com.strategyquant.tradinglib.event.TradingEventsArray;
import com.strategyquant.tradinglib.execution.ReservedBarsType;
import com.strategyquant.tradinglib.options.TradingOptions;
import com.strategyquant.tradinglib.options.parameters.MarketOpenSession;
import com.strategyquant.tradinglib.options.parameters.RealisticGapsHandling;
import com.strategyquant.tradinglib.strategy.LiveOrderObj;
import com.strategyquant.tradinglib.strategy.MarketData;
import com.strategyquant.tradinglib.swap.SwapCalculator;
import it.unimi.dsi.fastutil.longs.Long2FloatRBTreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class MetaTraderSimulatorHedging extends AbstractTradingSimulator {
   public static final Logger Log = LoggerFactory.getLogger("MetaTraderSimulator");
   public static final Logger BacktestLog = LoggerFactory.getLogger("BacktestLog");
   private static final long DAY_SECONDS = 86400000L;
   protected MarketPriceHolder marketPriceHolder = new MarketPriceHolder();
   protected OrdersList closedTrades;
   protected Long2FloatRBTreeMap dailyEquityValues;
   private long lastDayStartTime = -1L;
   private long nextDayStartTime = -1L;
   private float worstDailyEquity;
   private int orderId = -1000;
   private String connectionName = null;
   private int connectionHash;
   private int lastSymbolHash;
   private ChartData chartData = null;
   private TradingEventsArray tradingEventsArray = new TradingEventsArray();
   private double chartDataMinDistance;
   private int chartDataDecimals;
   private float accountBalance;
   private double accountEquity;
   private float initialCapital;
   private double slippage;
   private CommissionsMethod commission;
   private SwapMethod swap;
   private boolean computeDailyStats = true;
   private boolean orderErrorDetailedMessage = false;
   protected String marketOpenSessionString;
   protected Session marketOpenSession = null;

   public MetaTraderSimulatorHedging() {
      super(new ReservedBarsType(1, 100, 1001), new DefaultTickSimulator());
   }

   @Override
   protected abstract double getSLPTFilledPrice(double var1, double var3);

   protected abstract boolean checkPriceLevelCorrectness(int var1, double var2, double var4);

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
         if (var1.containsKey("TradingOptions") && var1.get("TradingOptions") instanceof TradingOptions) {
            TradingOptions var2 = (TradingOptions)var1.get("TradingOptions");

            for (int var3 = 0; var3 < var2.size(); var3++) {
               TradingOption var4 = var2.get(var3);
               if (var4 instanceof RealisticGapsHandling) {
                  this.realisticGapsHandling = ((RealisticGapsHandling)var4).RealisticGapsHandling;
               }

               if (var4 instanceof MarketOpenSession) {
                  this.marketOpenSessionString = ((MarketOpenSession)var4).MarketOpenSession;
                  Session var5 = SessionManager.getSession(this.marketOpenSessionString);
                  if (var5 != null) {
                     this.marketOpenSession = var5.clone();
                  }
               }
            }
         }
      } else {
         throw new TradingException("Setting 'TradingSetup.InitialCapital' is not set or has incorrect value! It must be double!");
      }
   }

   @Override
   public void initialize(MarketData var1) {
      this.liveOrders = new LiveOrderObj[300];
      this.liveOrdersCount = 0;
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
      this.mainChartData = var1.Chart(0);
   }

   @Override
   public void destroy() {
      if (this.closedTrades != null) {
         this.closedTrades.clear();
         this.closedTrades = null;
      }
   }

   @Override
   public void setConnection(String var1) {
      this.connectionName = var1;
      this.connectionHash = var1.hashCode();
   }

   @Override
   public void eventNewTick(TickEvent var1, double var2, double var4) {
      if (var1.getConnectionHash() == this.connectionHash) {
         this.lastSymbolHash = var1.getSymbolHash();
         if (this.lastSymbolHash == this.tradingSymbolHash) {
            TickEvent var6 = this.marketPriceHolder.setNewTick(this.lastSymbolHash, var1);
            if (var6 != null) {
               this.tickData = var6;
               if (this.getTestPrecision() != 5) {
                  this.processNewTick(var2, var4);
               }
            }
         }
      }
   }

   private void processNewTick(double var1, double var3) {
      if (this.computeDailyStats) {
         this.tryUpdateDailyEquity();
      }

      if (this.liveOrdersCount > 0) {
         this.accountEquity = -1.0;
         TickEvent var5 = this.marketPriceHolder.getCurrentTick(this.lastSymbolHash);
         if (this.IsMarketOpen()) {
            this.checkSLPT(var5, this.lastSymbolHash, var1, var3);
            this.checkStopLimitOrders(var5, this.lastSymbolHash);
         }

         if (this.computeDailyStats) {
            this.updateMAE_MFE();
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

   protected void checkSLPT(TickEvent var1, int var2, double var3, double var5) {
      boolean var10 = false;
      int var11 = this.liveOrdersCount;

      for (int var12 = var11 - 1; var12 >= 0; var12--) {
         LiveOrderObj var13 = this.liveOrders[var12];
         if (var13.getSymbolHash() == var2 && !var13.isPendingOrder()) {
            var13.setTickData(this.tickData.getAsk(), this.tickData.getBid());
            double var14 = var13.getSL();
            double var16 = var13.getPT();
            if (var14 != 0.0 || var16 != 0.0) {
               if (var14 > var5 && var14 < var3 && var16 > var5 && var16 < var3) {
                  var13.setAmbiguous();
                  this.ambiguousTrades++;
               }

               if (var14 > 0.0) {
                  byte var7 = var13.getOrderTypeFromSLPT(-1);
                  double var8 = this.priceLevelReached(var1, var2, var7, var14);
                  if (var8 > 0.0) {
                     this.closeOrder(var13, this.getRealisticSLPTFilledPrice(var8, var14, var1, var7), (byte)2);
                     this.removeOpenTradeNoFix(var12);
                     var10 = true;
                     continue;
                  }
               }

               if (var16 > 0.0) {
                  byte var18 = var13.getOrderTypeFromSLPT(1);
                  double var19 = this.priceLevelReached(var1, var2, var18, var16);
                  if (var19 > 0.0) {
                     this.closeOrder(var13, this.getRealisticSLPTFilledPrice(var19, var16, var1, var18), (byte)3);
                     this.removeOpenTradeNoFix(var12);
                     var10 = true;
                  }
               }
            }
         }
      }

      if (var10) {
         this.fixRemovedElements();
      }
   }

   protected void checkStopLimitOrders(TickEvent var1, int var2) {
      for (int var5 = 0; var5 < this.liveOrdersCount; var5++) {
         LiveOrderObj var6 = this.liveOrders[var5];
         if (var6.getSymbolHash() == var2 && !var6.isMarketOrder()) {
            byte var7 = var6.getOrderType();
            double var3 = this.priceLevelReached(var1, var2, var6.getOrderType(), var6.getOpenPrice());
            if (var3 > 0.0) {
               var6.setOpenTime(this.tickData.getTime());
               if (this.fillAtRealAskBidPrice()) {
                  var6.setOpenPrice(var3);
               }

               var6.fillOrder(this.tickData);
               if (this.thereWasAGap(var7, var1)) {
                  Log.debug("GAP detected - prev Close:{}, new Open: {}, realistic fill for MT4 set", this.closingPrice, var1.getBid());
                  var6.setOpenPrice(getPriceByType(var7, this.tickData.getBid(), this.tickData.getAsk()));
               }

               var6.setLastAction((byte)1);
               var6.setCommSwap(this.computeCommissionsOnOpen(var6));
            }
         }
      }

      if (var1.isBarClose()) {
         this.isNewBar = true;
         this.closingPrice = var1.getBid();
      } else if (this.isNewBar) {
         this.isNewBar = false;
      }
   }

   protected void closeOrder(LiveOrderObj var1, double var2, byte var4) {
      this.tickData = this.marketPriceHolder.getCurrentTick(var1.getSymbolHash());
      var1.setCloseTime(this.tickData.getTime());
      var1.setClosePrice(var1.getClosePriceWithSlippage(this.slippage, var2, var4));
      var1.setLastAction((byte)2);
      var1.setWorkingStatus((byte)-30);
      var1.setOrderStatus((byte)3);
      if (var4 == 2) {
         byte var5 = var1.getSLType();
         if (var5 == -1 || var5 != 20 && var5 != 21) {
            var1.setCloseType(var4);
         } else {
            var1.setCloseType(var5);
         }
      } else {
         var1.setCloseType(var4);
      }

      var1.setCommSwap(this.getCommissionSwap(var1));
      this.accountBalance = (float)(this.accountBalance + var1.getPL());
      this.accountEquity = -1.0;
      this.fixMAEMFEByRealPrice(var1);
      var1.transformMAE_MFE();
      if (this.storePendingOrders || !var1.isPendingOrder()) {
         this.closedTrades.add(var1);
      }
   }

   @Override
   public ILiveOrder orderClose(StrategyBase var1, ILiveOrder var2, byte var3, int var4, ITradingEventListener var5) {
      LiveOrderObj var6 = (LiveOrderObj)var2;
      this.removeOpenTrade(var6);
      this.tickData = this.marketPriceHolder.getCurrentTick(var6.getSymbolHash());
      if (OrderTypes.isMarketOrder(var6.getOrderType())) {
         double var7 = var6.getPriceByType(OrderTypes.getOppositeType(var6.getOrderType()), this.tickData.getBid(), this.tickData.getAsk(), this.slippage);
         var6.setClosePrice(var7);
      } else {
         var6.setClosePrice(var6.getOpenPrice());
      }

      var6.setCloseTime(this.tickData.getTime());
      var6.setLastAction((byte)2);
      var6.setWorkingStatus((byte)-30);
      var6.setOrderStatus((byte)3);
      var6.setCloseType(var3);
      var6.setCommSwap(this.getCommissionSwap(var6));
      this.accountBalance = (float)(this.accountBalance + var6.getPL());
      this.accountEquity = -1.0;
      this.fixMAEMFEByRealPrice(var6);
      var6.transformMAE_MFE();
      if (this.storePendingOrders || !var6.isPendingOrder()) {
         this.closedTrades.add(var6);
      }

      return var6;
   }

   @Override
   public void eventBarUpdated(int var1) {
      if (var1 == 2) {
         this.increaseBarsInTrade();
      }
   }

   private void increaseBarsInTrade() {
      for (int var1 = 0; var1 < this.liveOrdersCount; var1++) {
         if (this.tickData.getTime() != this.liveOrders[var1].getOpenTime()) {
            this.liveOrders[var1].increaseBarsInTrade();
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
         var5.setOpenPrice(var5.getPriceByType(var6, this.tickData.getBid(), this.tickData.getAsk(), this.slippage));
         var5.setOriginalOpenPrice(var5.getOpenPrice());
         var5.setOrderStatus((byte)1);
         var5.setCommSwap(this.computeCommissionsOnOpen(var5));
         var7 = 1;
      } else {
         var5.setOrderStatus((byte)2);
         var7 = 2;
      }

      var5.setLastAction((byte)1);
      var5.setError(0, null);
      var5.setOrderId(this.orderId++);
      var5.setWorkingStatus((byte)-30);
      this.addOpenTrade(var5);
      this.sendEvent(var1, var4, var7, var2);
      return var2;
   }

   protected void addOpenTrade(LiveOrderObj var1) throws TradingException {
      if (this.liveOrdersCount < this.liveOrders.length - 1) {
         this.liveOrders[this.liveOrdersCount++] = var1;
      } else {
         throw new BadStrategyException(32);
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
            if (this.orderErrorDetailedMessage) {
               var1.setError(6, "Incorrect price level. Open price: " + var6 + ", price level: " + var8);
            } else {
               var1.setError(6, null);
            }

            return false;
         }

         if (!this.checkPriceLevelDistance(var2, var6, var8, false)) {
            if (this.orderErrorDetailedMessage) {
               var1.setError(6, "Incorrect price level distance. Open price: " + var6 + ", price level: " + var8);
            } else {
               var1.setError(6, null);
            }

            return false;
         }

         var6 = var8;
      }

      if (var1.getSL() != -9.9999999E7) {
         double var13 = var1.getSL();
         var3 = var1.getOrderTypeFromSLPT(-1);
         if (!this.checkPriceLevelCorrectness(var3, var6, var13)) {
            if (this.orderErrorDetailedMessage) {
               var1.setError(6, "Incorrect stop level. Open price: " + var6 + ", price level: " + var13);
            } else {
               var1.setError(6, null);
            }

            return false;
         }

         if (!this.checkPriceLevelDistance(var2, var6, var13, true)) {
            if (this.orderErrorDetailedMessage) {
               var1.setError(6, "Incorrect price level distance. Open price: " + var6 + ", price level: " + var13);
            } else {
               var1.setError(6, null);
            }

            return false;
         }
      }

      if (var1.getPT() != -9.9999999E7) {
         double var14 = var1.getPT();
         var3 = var1.getOrderTypeFromSLPT(1);
         if (!this.checkPriceLevelCorrectness(var3, var6, var14)) {
            if (this.orderErrorDetailedMessage) {
               var1.setError(6, "Incorrect stop level. Open price: " + var6 + ", price level: " + var14);
            } else {
               var1.setError(6, null);
            }

            return false;
         }

         if (!this.checkPriceLevelDistance(var2, var6, var14, true)) {
            if (this.orderErrorDetailedMessage) {
               var1.setError(6, "Incorrect price level distance. Open price: " + var6 + ", price level: " + var14);
            } else {
               var1.setError(6, null);
            }

            return false;
         }
      }

      return true;
   }

   private String generatePriceLevelCorrectnessMsg(String var1, int var2) {
      switch (var2) {
         case 3:
            return String.format("Incorrect %s for Buy Limit order. Price must be below market price.", var1);
         case 4:
            return String.format("Incorrect %s for Sell Limit order. Price must be above market price.", var1);
         case 5:
            return String.format("Incorrect %s for Buy Stop order. Price must be above market price.", var1);
         case 6:
            return String.format("Incorrect %s for Sell Stop order. Price must be below market price.", var1);
         default:
            return String.format("Unknown orderType: %d", var2);
      }
   }

   private boolean checkPriceLevelDistance(int var1, double var2, double var4, boolean var6) {
      if (this.chartData == null || this.chartData.getSymbolHash() != var1) {
         this.chartData = this.marketPriceHolder.getChartData(var1);
         this.chartDataMinDistance = this.chartData.getMinDistance() * this.chartData.getInstrumentInfo().tickSize;
         this.chartDataDecimals = this.chartData.getInstrumentInfo().decimals;
      }

      double var7 = Math.abs(var4 - var2);
      return var6 && var7 == 0.0 ? false : SQUtils.compare(var7, this.chartDataMinDistance, this.chartDataDecimals) >= 0;
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

   private double getCommissionSwap(LiveOrderObj var1) {
      if (var1.isPendingOrder()) {
         return 0.0;
      }

      double var2 = var1.getCommSwap();
      if (this.commission != null) {
         if (this.chartData == null || this.chartData.getSymbolHash() != var1.getSymbolHash()) {
            this.chartData = this.marketPriceHolder.getChartData(var1.getSymbolHash());
         }

         this.chartDataMinDistance = this.chartData.getMinDistance() * this.chartData.getInstrumentInfo().tickSize;
         this.chartDataDecimals = this.chartData.getInstrumentInfo().decimals;

         try {
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

   protected double computeCommissionsOnOpen(LiveOrderObj var1) {
      if (this.commission != null) {
         if (this.chartData == null || this.chartData.getSymbolHash() != var1.getSymbolHash()) {
            this.chartData = this.marketPriceHolder.getChartData(var1.getSymbolHash());
         }

         this.chartDataMinDistance = this.chartData.getMinDistance() * this.chartData.getInstrumentInfo().tickSize;
         this.chartDataDecimals = this.chartData.getInstrumentInfo().decimals;

         try {
            return -this.commission.computeCommissionsOnOpen(var1, this.chartData.getInstrumentInfo().tickSize, this.chartData.getInstrumentInfo().pointValue);
         } catch (Exception var3) {
            Log.error("Exception computing commissions", var3);
         }
      }

      return 0.0;
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

         if (!this.checkPriceLevelDistance(var5, var3, var6, true)) {
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

         if (!this.checkPriceLevelDistance(var5, var3, var10, true)) {
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
         this.closedTrades.add(var3);
         var1 = (float)(var1 + var3.getPL());
         this.removeOpenTradeNoFix(var2);
      }

      this.liveOrdersCount = 0;
      this.accountEquity = var1;
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
      return 2;
   }

   @Override
   public void evaluateActionListeners(int var1, StrategyBase var2) throws TradingException {
      for (int var3 = this.liveOrdersCount - 1; var3 >= 0; var3--) {
         LiveOrderObj var4 = this.liveOrders[var3];
         var4.evaluateActionListeners(var1, var2);
      }
   }

   @Override
   public boolean supportsDuplicateTrades() {
      return true;
   }
}
