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
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.longs.Long2FloatRBTreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class MetaTraderSimulatorNetting extends AbstractTradingSimulator {
   public static final Logger Log = LoggerFactory.getLogger("MetaTraderSimulator");
   public static final Logger BacktestLog = LoggerFactory.getLogger("BacktestLog");
   protected MarketPriceHolder marketPriceHolder = new MarketPriceHolder();
   protected Int2ObjectOpenHashMap<LiveOrderObj> openPositions;
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
   protected String marketOpenSessionString;
   protected Session marketOpenSession = null;

   public MetaTraderSimulatorNetting() {
      super(new ReservedBarsType(1, 100, 1001), new DefaultTickSimulator());
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
      this.liveOrders = new LiveOrderObj[100];
      this.openPositions = new Int2ObjectOpenHashMap();
      this.dailyEquityValues = new Long2FloatRBTreeMap();
      this.liveOrdersCount = 0;
      this.closedTrades = new OrdersList("ClosedTrades");
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
         TickEvent var6 = this.marketPriceHolder.setNewTick(this.lastSymbolHash, var1);
         if (var6 != null) {
            this.tickData = var6;
            if (this.getTestPrecision() != 5) {
               this.processNewTick(var2, var4);
            }
         }
      }
   }

   private void processNewTick(double var1, double var3) {
      if (this.computeDailyStats) {
         this.tryUpdateDailyEquity();
      }

      TickEvent var5 = this.marketPriceHolder.getCurrentTick(this.lastSymbolHash);
      if (this.IsMarketOpen()) {
         this.checkSLPT(var5, this.lastSymbolHash, var1, var3);
         if (this.liveOrdersCount > 0) {
            this.accountEquity = -1.0;
            if (this.computeDailyStats) {
               this.updateMAE_MFE();
            }

            this.checkStopLimitOrders(var5, this.lastSymbolHash);
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
      IntIterator var4 = this.openPositions.keySet().iterator();

      while (var4.hasNext()) {
         int var5 = (Integer)var4.next();
         if (var5 == this.lastSymbolHash) {
            LiveOrderObj var6 = (LiveOrderObj)this.openPositions.get(var5);
            double var2;
            if (var6.isLong()) {
               var2 = this.tickData.getBid() - var6.getOpenPrice();
            } else {
               var2 = var6.getOpenPrice() - this.tickData.getBid();
            }

            if (var6.getMFE() < var2) {
               var6.setMFE((float)var2);
            }

            var2 *= -1.0;
            if (var6.getMAE() < var2) {
               var6.setMAE((float)var2);
            }

            var6.setTickData(this.tickData.getAsk(), this.tickData.getBid());
            var1 = (float)(var1 + var6.getPL());
         }
      }

      this.accountEquity = var1;
      this.worstDailyEquity = Math.min(var1 - this.initialCapital, this.worstDailyEquity);
   }

   private void checkSLPT(TickEvent var1, int var2, double var3, double var5) {
      LiveOrderObj var10 = (LiveOrderObj)this.openPositions.get(var2);
      if (var10 != null) {
         var10.setTickData(this.tickData.getAsk(), this.tickData.getBid());
         double var11 = var10.getSL();
         double var13 = var10.getPT();
         if (var11 == 0.0 && var13 == 0.0) {
            return;
         }

         if (var11 > var5 && var11 < var3 && var13 > var5 && var13 < var3) {
            var10.setAmbiguous();
            this.ambiguousTrades++;
         }

         if (var11 > 0.0) {
            byte var7 = var10.getOrderTypeFromSLPT(-1);
            double var8 = this.priceLevelReached(var1, var2, var7, var11);
            if (var8 > 0.0) {
               this.closePosition(var10, this.getRealisticSLPTFilledPrice(var8, var11, var1, var7), (byte)2);
               return;
            }
         }

         if (var13 > 0.0) {
            byte var15 = var10.getOrderTypeFromSLPT(1);
            double var16 = this.priceLevelReached(var1, var2, var15, var13);
            if (var16 > 0.0) {
               this.closePosition(var10, this.getRealisticSLPTFilledPrice(var16, var13, var1, var15), (byte)3);
            }
         }
      }
   }

   private void closePosition(LiveOrderObj var1, double var2, byte var4) {
      this.tickData = this.marketPriceHolder.getCurrentTick(var1.getSymbolHash());
      if (!var1.isPendingOrder()) {
         var1.setOrderId(this.orderId++);
      }

      var1.setCloseTime(this.tickData.getTime());
      var1.setClosePrice(var1.getClosePriceWithSlippage(this.slippage, var2, var4));
      var1.setLastAction((byte)2);
      var1.setWorkingStatus((byte)-30);
      var1.setOrderStatus((byte)3);
      var1.setCloseType(var4);
      var1.setCommSwap(this.getCommissionSwap(var1));
      this.fixMAEMFEByRealPrice(var1);
      this.saveOrder(var1, false);
      this.removeOpenTrade(var1);
      if (!var1.isPendingOrder()) {
         this.openPositions.remove(var1.getSymbolHash());
         this.accountBalance = (float)(this.accountBalance + var1.getPL());
      }
   }

   protected void checkStopLimitOrders(TickEvent var1, int var2) {
      for (int var5 = 0; var5 < this.liveOrdersCount; var5++) {
         LiveOrderObj var6 = this.liveOrders[var5];
         if (var6.getSymbolHash() == var2 && !var6.isMarketOrder()) {
            byte var7 = var6.getOrderType();
            double var3 = this.priceLevelReached(var1, var2, var6.getOrderType(), var6.getOpenPrice());
            if (var3 > 0.0) {
               var6.fillOrder(this.tickData);
               if (this.thereWasAGap(var7, var1)) {
                  Log.debug("GAP detected - prev Close:{}, new Open: {}, realistic fill for MT4 set", this.closingPrice, var1.getBid());
                  var6.setOpenPrice(getPriceByType(var7, this.tickData.getBid(), this.tickData.getAsk()));
               }

               var6.setLastAction((byte)1);
               var6.setCommSwap(this.computeCommissionsOnOpen(var6));
               this.onOrderFill(var6);
               var5--;
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
      var5.setOrderId(this.orderId++);
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
         var5.setLastOpenPrice(var5.getOpenPrice());
         var7 = 2;
      }

      var5.setLastAction((byte)1);
      var5.setError(0, null);
      var5.setWorkingStatus((byte)-30);
      this.addOpenTrade(var5);
      this.sendEvent(var1, var4, var7, var2);
      return var2;
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
            return var6;
         }
      }

      return var2;
   }

   @Override
   public ILiveOrder orderClose(StrategyBase var1, ILiveOrder var2, byte var3, int var4, ITradingEventListener var5) {
      LiveOrderObj var6 = (LiveOrderObj)var2;
      double var7 = 0.0;
      this.tickData = this.marketPriceHolder.getCurrentTick(var6.getSymbolHash());
      if (OrderTypes.isMarketOrder(var6.getOrderType())) {
         var7 = var6.getPriceByType(OrderTypes.getOppositeType(var6.getOrderType()), this.tickData.getBid(), this.tickData.getAsk(), this.slippage);
      } else {
         var7 = var6.getOpenPrice();
      }

      var6.setClosePrice(var7);
      var6.setCloseTime(this.tickData.getTime());
      var6.setLastAction((byte)2);
      var6.setWorkingStatus((byte)-30);
      var6.setOrderStatus((byte)3);
      var6.setCloseType(var3);

      for (int var9 = 0; var9 < this.liveOrdersCount; var9++) {
         LiveOrderObj var10 = this.liveOrders[var9];
         if (var10 == var6) {
            this.closePosition(var6, var7, var3);
            this.removeOpenTrade(var6);
            return var6;
         }
      }

      return null;
   }

   protected void addOpenTrade(LiveOrderObj var1) throws TradingException {
      if (this.liveOrdersCount < this.liveOrders.length - 1) {
         this.liveOrders[this.liveOrdersCount++] = var1;
         if (var1.isMarketOrder()) {
            this.onOrderFill(var1);
         }
      } else {
         throw new BadStrategyException(32);
      }
   }

   private void onOrderFill(LiveOrderObj var1) {
      var1.setNettingMode(true);
      LiveOrderObj var2 = (LiveOrderObj)this.openPositions.get(var1.getSymbolHash());
      if (var2 == null) {
         var1.setLastOpenPrice(var1.getOpenPrice());
         this.saveOrder(var1, true);
         this.openPositions.put(var1.getSymbolHash(), var1);
      } else if (var1.getDirection() == var2.getDirection()) {
         this.saveOrder(var1, true);
         var1.setLastOpenPrice(var1.getOpenPrice());
         var1.setOpenPrice((var2.getOpenPrice() * var2.getSize() + var1.getOpenPrice() * var1.getSize()) / (var2.getSize() + var1.getSize()));
         var1.setSize(SQUtils.round(var2.getSize() + var1.getSize(), 5));
         var1.setCommSwap(var1.getCommSwap() + var2.getCommSwap());

         for (int var3 = 0; var3 < var2.getBarsInTrade(); var3++) {
            var1.increaseBarsInTrade();
         }

         this.openPositions.put(var1.getSymbolHash(), var1);
         this.removeOpenTrade(var2);
      } else {
         double var12 = var1.getSize();
         double var5 = var2.getSize();
         double var7 = var1.getOpenPrice();
         LiveOrderObj var9 = this.createLiveOrder(var1.getTrader(), var1.getInstrumentInfo(), var2.getOrderType(), var1.getSymbol());
         var9.setSize(Math.min(var5, var12));
         var9.setOriginalOpenPrice(var2.getOriginalOpenPrice());
         var9.setOpenPrice(var2.getOpenPrice());
         var9.setClosePrice(var7);
         var9.setTickData(this.tickData.getAsk(), this.tickData.getBid());
         var9.setOriginalOpenTime(var2.getOriginalOpenTime());
         var9.setOriginalOrderType(var2.getOriginalOrderType());
         var9.setOpenTime(var2.getOpenTime());
         var9.setCloseTime(var1.getOpenTime());
         var9.setOrderType(var2.getOrderType());
         var9.setCommSwap(this.computeCommissionsOnOpen(var1));
         var9.setCommSwap(this.getCommissionSwap(var1));
         var9.setOrderStatus((byte)3);
         this.accountBalance = (float)(this.accountBalance + var9.getPL());
         var9.setExitIndex(var1.getExitIndex());
         var9.setATROnOpen(var2.getATROnOpen());
         byte var10 = var1.getSLType();
         if (var10 == -1 || var10 != 20 && var10 != 21) {
            var9.setCloseType((byte)1);
         } else {
            var9.setCloseType(var10);
         }

         var9.setComment(var1.getComment());
         if (var12 > var5) {
            var1.setWorkingStatus((byte)-10);
            var1.setSize(SQUtils.round(var12 - var5, 5));
            var1.setWorkingStatus((byte)-30);
            this.openPositions.put(var1.getSymbolHash(), var1);
            this.removeOpenTrade(var2);
         } else if (var12 == var5) {
            this.openPositions.remove(var1.getSymbolHash());
            var1.setCloseType((byte)1);
            var1.setCloseTime(this.tickData.getTime());
            this.removeOpenTrade(var2);
            this.removeOpenTrade(var1);
         } else {
            var2.setWorkingStatus((byte)-10);
            var2.setSize(SQUtils.round(var5 - var12, 5));
            var2.setWorkingStatus((byte)-30);
            var2.setCommSwap(this.computeCommissionsOnOpen(var2));
            var1.setCloseType((byte)1);
            var1.setCloseTime(this.tickData.getTime());
            var1.setOrderType((byte)(var1.isLong() ? 1 : 2));
            this.removeOpenTrade(var1);
         }

         this.saveOrder(var9, false);
         this.removeOpenTrade(var9);
      }

      try {
         var1.evaluateActionListeners(7, null);
      } catch (TradingException var11) {
         Log.error("Error while calling Order filled action listeners", var11);
      }
   }

   private void saveOrder(LiveOrderObj var1, boolean var2) {
      if (var2) {
         LiveOrderObj var3 = this.createLiveOrder(var1.getTrader(), var1.getInstrumentInfo(), var1.getOrderType(), var1.getSymbol());
         var3.setOrderId(var1.getOrderId());
         var3.setMagicNumber(var1.getMagicNumber());
         var3.setOriginalOpenTime(var1.getOriginalOpenTime());
         var3.setOpenTime(var1.getOpenTime());
         var3.setOriginalOpenPrice(var1.getOriginalOpenPrice());
         var3.setOpenPrice(var1.getOpenPrice());
         var3.setSize(var1.getSize());
         var3.setCloseTime(this.tickData.getTime());
         var3.setClosePrice(var1.getOpenPrice());
         var3.setLastAction((byte)2);
         var3.setWorkingStatus((byte)-30);
         var3.setOrderStatus((byte)3);
         var3.setCloseType((byte)18);
         var3.setATROnOpen(var1.getATROnOpen());
         this.fixMAEMFEByRealPrice(var3);
         this.closedTrades.add(var3);
         this.removeOpenTrade(var3);
      } else if (this.storePendingOrders || !var1.isPendingOrder()) {
         var1.transformMAE_MFE();
         this.fixMAEMFEByRealPrice(var1);
         this.closedTrades.add(var1);
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

      if (var1.getSL() != -9.9999999E7) {
         double var13 = var1.getSL();
         var3 = var1.getOrderTypeFromSLPT(-1);
         if (!this.checkPriceLevelCorrectness(var3, var6, var13)) {
            var1.setError(6, "Incorrect stop level. Open price: " + var6 + ", price level: " + var13);
            return false;
         }

         if (!this.checkPriceLevelDistance(var2, var6, var13)) {
            var1.setError(6, "Incorrect price level distance. Open price: " + var6 + ", price level: " + var13);
            var1.setSL(0.0);
            var1.setSLFromModified();
         }
      }

      if (var1.getPT() != -9.9999999E7) {
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

   protected abstract boolean checkPriceLevelCorrectness(int var1, double var2, double var4);

   private boolean checkPriceLevelDistance(int var1, double var2, double var4) {
      if (this.chartData == null || this.chartData.getSymbolHash() != var1) {
         this.chartData = this.marketPriceHolder.getChartData(var1);
         this.chartDataMinDistance = this.chartData.getMinDistance() * this.chartData.getInstrumentInfo().tickSize;
         this.chartDataDecimals = this.chartData.getInstrumentInfo().decimals;
      }

      double var6 = Math.abs(var4 - var2);
      return SQUtils.compare(var6, this.chartDataMinDistance, this.chartDataDecimals) >= 0;
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
   public int getOpenOrdersCount(boolean var1) {
      return this.liveOrdersCount;
   }

   public ILiveOrder getOpenOrder(int var1) {
      if (var1 < 0 || var1 >= this.getOpenOrdersCount(false)) {
         throw new IllegalArgumentException("Trade index is out of range!");
      } else {
         return var1 < this.liveOrdersCount ? this.liveOrders[var1] : null;
      }
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
      IntIterator var1 = this.openPositions.keySet().iterator();

      while (var1.hasNext()) {
         int var2 = (Integer)var1.next();
         LiveOrderObj var3 = (LiveOrderObj)this.openPositions.get(var2);
         this.accountEquity = this.accountEquity + var3.getPL();
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
         this.tickData = this.marketPriceHolder.getCurrentTick(var3.getSymbolHash());
         double var4 = LiveOrderObj.getPriceByType(OrderTypes.getOppositeType(var3.getOrderType()), this.tickData.getBid(), this.tickData.getAsk());
         var3.setCloseTime(this.tickData.getTime());
         var3.setClosePrice(var4);
         var3.setLastAction((byte)2);
         var3.setWorkingStatus((byte)-30);
         var3.setOrderStatus((byte)3);
         var3.setCloseType((byte)4);
         var1 = (float)(var1 + var3.getPL());
         if (!var3.isPendingOrder()) {
            var3.setOrderId(this.orderId++);
         }

         this.fixMAEMFEByRealPrice(var3);
         this.closedTrades.add(var3);
         this.removeOpenTradeNoFix(var2);
      }

      this.liveOrdersCount = 0;
      this.openPositions.clear();
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
      IntIterator var3 = this.openPositions.keySet().iterator();

      while (var3.hasNext()) {
         int var4 = (Integer)var3.next();
         LiveOrderObj var5 = (LiveOrderObj)this.openPositions.get(var4);
         if (var5 != null) {
            var5.evaluateActionListeners(var1, var2);
         }
      }

      for (int var6 = this.liveOrdersCount - 1; var6 >= 0; var6--) {
         LiveOrderObj var7 = this.liveOrders[var6];
         if (var7 != null) {
            var7.evaluateActionListeners(var1, var2);
         }
      }
   }

   @Override
   public boolean supportsDuplicateTrades() {
      return true;
   }
}
