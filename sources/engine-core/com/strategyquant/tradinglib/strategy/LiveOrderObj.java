package com.strategyquant.tradinglib.strategy;

import com.strategyquant.datalib.InstrumentInfo;
import com.strategyquant.datalib.TickEvent;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.IActionEventListener;
import com.strategyquant.tradinglib.ILiveOrder;
import com.strategyquant.tradinglib.OrderCloseTypes;
import com.strategyquant.tradinglib.OrderTypes;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.Trader;
import com.strategyquant.tradinglib.event.ITradingEventListener;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class LiveOrderObj implements ILiveOrder {
   private Trader trader;
   private byte orderType;
   private byte originalOrderType;
   private byte workingStatus;
   private byte orderStatus;
   private byte closeType;
   private String symbol;
   private int symbolHash;
   private String strategyName;
   public String setupName;
   private double size;
   private double openPrice = -1.0;
   private double originalOpenPrice;
   private byte slType;
   private double sl = -9.9999999E7;
   public double modifiedSL = -9.9999999E7;
   private byte modifiedSLType = -1;
   private double initialSL = -9.9999999E7;
   private double pt = -9.9999999E7;
   public double modifiedPT = -9.9999999E7;
   private String comment;
   private double maxSlippage;
   private int barsInTrade;
   private long openTime;
   private long originalOpenTime;
   private int orderId;
   private double closePrice;
   private byte lastAction;
   private long closeTime;
   private byte nextAction;
   private int errorCode;
   private String errorMessage;
   private int magicNumber = 0;
   private long expirationDate;
   private InstrumentInfo instrumentInfo;
   private double pl;
   private double plInPips;
   private double ask;
   private double bid;
   private double lastPLPrice = -1.0;
   private double lastPLCommSwap = 0.0;
   private double commSwap = 0.0;
   private float pipsMAE = 0.0F;
   private float pipsMFE = 0.0F;
   private boolean ambiguous = false;
   boolean filled = true;
   ObjectArrayList<IActionEventListener> barActionListeners = null;
   ObjectArrayList<IActionEventListener> tickActionListeners = null;
   ObjectArrayList<IActionEventListener> orderFilledListeners = null;
   private byte waitingBars = 0;
   private boolean isNettingMode = false;
   private double lastOpenPrice = 0.0;
   private float slippageInMoney = 0.0F;
   private byte exitIndex = -1;
   private float atrOnOpen = 0.0F;
   private int barsValid = 0;
   private byte eod = -1;
   private int exitAfterBars = 0;
   private int slptValidFrom = 0;
   private boolean onBarOpen = true;
   public double lastPrice = 0.0;

   public LiveOrderObj(Trader var1, InstrumentInfo var2, byte var3, String var4) {
      this.init(var1, var2, var3, var4);
   }

   public LiveOrderObj(Trader var1, InstrumentInfo var2, byte var3, String var4, double var5) {
      this.init(var1, var2, var3, var4, var5);
   }

   void init(Trader var1, InstrumentInfo var2, byte var3, String var4) {
      this.closeType = 0;
      this.openPrice = -1.0;
      this.originalOpenPrice = 0.0;
      this.slType = 0;
      this.sl = -9.9999999E7;
      this.modifiedSL = -9.9999999E7;
      this.modifiedSLType = -1;
      this.initialSL = -9.9999999E7;
      this.pt = -9.9999999E7;
      this.modifiedPT = -9.9999999E7;
      this.comment = null;
      this.openTime = 0L;
      this.originalOpenTime = 0L;
      this.orderId = 0;
      this.closePrice = 0.0;
      this.lastAction = 0;
      this.closeTime = 0L;
      this.nextAction = 0;
      this.errorCode = 0;
      this.errorMessage = null;
      this.magicNumber = 0;
      this.expirationDate = 0L;
      this.pl = 0.0;
      this.ask = 0.0;
      this.bid = 0.0;
      this.lastPLPrice = -1.0;
      this.commSwap = 0.0;
      this.slippageInMoney = 0.0F;
      this.pipsMAE = 0.0F;
      this.pipsMFE = 0.0F;
      this.ambiguous = false;
      this.filled = true;
      this.setSymbol(var4);
      this.strategyName = var1.getStrategyName();
      this.setupName = var1.getSetupName();
      if (this.setupName == null) {
         this.setupName = var4;
      }

      this.nextAction = 1;
      this.errorCode = 0;
      this.trader = var1;
      this.orderType = var3;
      this.originalOrderType = var3;
      this.workingStatus = -10;
      this.orderStatus = 0;
      this.size = 0.0;
      this.barsInTrade = 0;
      this.maxSlippage = 0.0;
      this.expirationDate = 0L;
      this.instrumentInfo = var2;
      if (this.barActionListeners != null) {
         this.barActionListeners.clear();
      }

      if (this.tickActionListeners != null) {
         this.tickActionListeners.clear();
      }

      if (this.orderFilledListeners != null) {
         this.orderFilledListeners.clear();
      }

      this.exitIndex = -1;
      this.atrOnOpen = 0.0F;
      this.barsValid = 0;
      this.eod = -1;
      this.exitAfterBars = 0;
      this.slptValidFrom = 0;
      this.onBarOpen = true;
   }

   void init(Trader var1, InstrumentInfo var2, byte var3, String var4, double var5) {
      this.init(var1, var2, var3, var4);
      if (var3 != 1 && var3 != 2) {
         this.originalOpenPrice = this.fixPrice(var5);
         this.openPrice = this.originalOpenPrice;
      }
   }

   private void setSymbol(String var1) {
      this.symbol = var1;
      this.symbolHash = var1.hashCode();
   }

   @Override
   public double getSL() {
      return this.sl;
   }

   @Override
   public double getInitialSL() {
      return this.initialSL;
   }

   public void setInitialSL(double var1) {
      this.initialSL = var1;
   }

   @Override
   public double getPL() {
      this.computePL();
      return this.pl;
   }

   @Override
   public double getPLInPips() {
      this.computePL();
      return this.plInPips;
   }

   public void computePL() {
      if (!OrderTypes.isMarketOrder(this.orderType)) {
         this.pl = 0.0;
         this.plInPips = 0.0;
      } else {
         double var1;
         if (this.orderStatus == 3) {
            var1 = this.closePrice;
         } else {
            var1 = getClosePriceByType(this.orderType, this.bid, this.ask);
         }

         if (var1 != this.lastPLPrice || this.commSwap != this.lastPLCommSwap) {
            double var3 = 0.0;
            if (this.orderType == 1) {
               var3 = var1 - this.openPrice;
            } else {
               var3 = this.openPrice - var1;
            }

            this.pl = SQUtils.round(var3 * this.size * this.instrumentInfo.pointValue, 2) + this.commSwap;
            this.plInPips = var3 / this.instrumentInfo.tickSize;
            this.lastPLPrice = var1;
            this.lastPLCommSwap = this.commSwap;
         }
      }
   }

   @Override
   public byte getSLType() {
      return this.slType;
   }

   public LiveOrderObj setSL(double var1) {
      return this.setSL((byte)2, var1);
   }

   public LiveOrderObj setSL(byte var1, double var2) {
      if (this.getWorkingStatus() == -10) {
         this.slType = var1;
         this.sl = this.fixPrice(var2);
         if (this.initialSL == -9.9999999E7) {
            this.initialSL = this.sl;
         }

         return this;
      } else {
         this.modifiedSLType = var1;
         this.modifiedSL = this.fixPrice(var2);
         this.sl = this.modifiedSL;
         return this;
      }
   }

   @Override
   public ILiveOrder setSLType(byte var1) {
      this.slType = var1;
      return this;
   }

   @Override
   public double getPT() {
      return this.pt;
   }

   public LiveOrderObj setPT(double var1) {
      return this.setPT((byte)3, var1);
   }

   public LiveOrderObj setPT(byte var1, double var2) {
      if (this.getWorkingStatus() == -10) {
         this.pt = this.fixPrice(var2);
         return this;
      } else {
         this.modifiedPT = this.fixPrice(var2);
         this.pt = this.modifiedPT;
         return this;
      }
   }

   @Override
   public String getComment() {
      return this.comment;
   }

   public LiveOrderObj setComment(String var1) {
      if (this.getWorkingStatus() == -10) {
         this.comment = var1;
      }

      return this;
   }

   @Override
   public int getMagicNumber() {
      return this.magicNumber;
   }

   public LiveOrderObj setMagicNumber(int var1) {
      if (this.getWorkingStatus() == -10) {
         this.magicNumber = var1;
      }

      return this;
   }

   @Override
   public double getMaxSlippage() {
      return this.maxSlippage;
   }

   public LiveOrderObj setMaxSlippage(double var1) {
      if (this.getWorkingStatus() == -10) {
         this.maxSlippage = var1;
      }

      return this;
   }

   @Override
   public long getExpiration() {
      return this.expirationDate;
   }

   public LiveOrderObj setExpiration(long var1) {
      if (this.getWorkingStatus() == -10) {
         this.expirationDate = var1;
      }

      return this;
   }

   @Override
   public int getSymbolHash() {
      return this.symbolHash;
   }

   @Override
   public String getSymbol() {
      return this.symbol;
   }

   @Override
   public byte getOrderType() {
      return this.orderType;
   }

   @Override
   public byte getCloseType() {
      return this.closeType;
   }

   @Override
   public byte getOriginalOrderType() {
      return this.originalOrderType;
   }

   public void setOriginalOrderType(byte var1) {
      this.originalOrderType = var1;
   }

   public void setOpenTime(long var1) {
      this.openTime = var1;
   }

   @Override
   public long getOpenTime() {
      return this.openTime;
   }

   public void setOriginalOpenTime(long var1) {
      this.originalOpenTime = var1;
   }

   @Override
   public long getOriginalOpenTime() {
      return this.originalOpenTime;
   }

   public void setOpenPrice(double var1) {
      this.openPrice = var1;
   }

   @Override
   public double getOpenPrice() {
      return this.openPrice;
   }

   public void setOriginalOpenPrice(double var1) {
      this.originalOpenPrice = var1;
   }

   @Override
   public double getOriginalOpenPrice() {
      return this.originalOpenPrice;
   }

   public void setOrderId(int var1) {
      this.orderId = var1;
   }

   @Override
   public int getOrderId() {
      return this.orderId;
   }

   @Override
   public int getBarsInTrade() {
      return this.barsInTrade;
   }

   public void increaseBarsInTrade() {
      this.barsInTrade++;
   }

   public void resetBarsInTrade() {
      this.barsInTrade = 0;
   }

   public void setClosePrice(double var1) {
      this.closePrice = var1;
   }

   @Override
   public double getClosePrice() {
      return this.closePrice;
   }

   public void setWorkingStatus(byte var1) {
      this.workingStatus = var1;
   }

   public void setOrderStatus(byte var1) {
      this.orderStatus = var1;
   }

   @Override
   public boolean isSuccessful() {
      return this.errorCode == 0;
   }

   @Override
   public boolean isFilled() {
      return this.filled;
   }

   public void setLastAction(byte var1) {
      this.lastAction = var1;
   }

   public void setCloseTime(long var1) {
      this.closeTime = var1;
   }

   @Override
   public long getCloseTime() {
      return this.closeTime;
   }

   @Override
   public double getSize() {
      return this.size;
   }

   @Override
   public ILiveOrder Send() throws TradingException {
      if (!this.trader.IsMarketOpen()) {
         return this;
      }

      if (this.nextAction == 3 && !this.isModified()) {
         return this;
      }

      ILiveOrder var1 = this.trader.send(this, this.nextAction, 0, null);
      this.nextAction = 3;
      return var1;
   }

   @Override
   public ILiveOrder SendAsync() throws TradingException {
      throw new TradingException("Not used");
   }

   @Override
   public ILiveOrder SendAsync(ITradingEventListener var1) throws TradingException {
      throw new TradingException("Not used");
   }

   @Override
   public boolean isModified() {
      if (this.modifiedPT != -9.9999999E7 && this.modifiedPT != this.pt) {
         return true;
      } else {
         return this.modifiedSLType != -1 && this.modifiedSLType != this.slType ? true : this.modifiedSL != -9.9999999E7 && this.modifiedSL != this.sl;
      }
   }

   @Override
   public ILiveOrder ModifyAsync() throws TradingException {
      throw new TradingException("Not used");
   }

   @Override
   public ILiveOrder ModifyAsync(ITradingEventListener var1) throws TradingException {
      throw new TradingException("Not used");
   }

   @Override
   public ILiveOrder Close(byte var1) throws TradingException {
      return !this.trader.IsMarketOpen() ? this : this.trader.send(this, (byte)2, var1, 0, null);
   }

   @Override
   public ILiveOrder CloseAsync() throws TradingException {
      throw new TradingException("Not used");
   }

   @Override
   public ILiveOrder CloseAsync(ITradingEventListener var1) throws TradingException {
      throw new TradingException("Not used");
   }

   @Override
   public byte getWorkingStatus() {
      return this.workingStatus;
   }

   @Override
   public byte getPositionStatus() {
      return this.orderStatus;
   }

   public LiveOrderObj setError(int var1, String var2) {
      this.errorCode = var1;
      this.errorMessage = var2;
      return this;
   }

   @Override
   public int getErrorCode() {
      return this.errorCode;
   }

   @Override
   public String getErrorMessage() {
      return this.errorMessage;
   }

   @Override
   public String getStrategyName() {
      return this.strategyName;
   }

   @Override
   public String getSetupName() {
      return this.setupName;
   }

   @Override
   public ILiveOrder setSize(double var1) {
      this.size = var1;
      return this;
   }

   @Override
   public ILiveOrder computeSizeIfMissing() throws TradingException {
      if (this.getSize() <= 0.0) {
         this.size = 0.0;
      }

      return this;
   }

   @Override
   public void refuse(String var1) {
      this.orderStatus = 10;
      this.errorMessage = var1;
   }

   @Override
   public boolean isRefused() {
      return this.orderStatus == 10;
   }

   @Override
   public int getDirection() {
      return OrderTypes.isLongOrder(this.orderType) ? 1 : -1;
   }

   public void setCloseType(byte var1) {
      this.closeType = var1;
   }

   private double fixPrice(double var1) {
      return var1 == -9.9999999E7 ? var1 : SQUtils.fixPrice(this.instrumentInfo.tickStep, var1, this.instrumentInfo.decimals);
   }

   public void setSLFromModified() {
      if (this.modifiedSL != -9.9999999E7) {
         this.sl = this.modifiedSL;
         if (this.initialSL == -9.9999999E7) {
            this.initialSL = this.sl;
         }

         this.modifiedSL = -9.9999999E7;
      }

      if (this.modifiedSLType != -1) {
         this.slType = this.modifiedSLType;
         this.modifiedSLType = -1;
      }
   }

   public void setPTFromModified() {
      if (this.modifiedPT != -9.9999999E7) {
         this.pt = this.modifiedPT;
         this.modifiedPT = -9.9999999E7;
      }
   }

   public void fillOrder(TickEvent var1) {
      this.openTime = var1.getTime();
      this.orderType = (byte)(this.orderType != 3 && this.orderType != 5 && this.orderType != 7 ? 2 : 1);
      this.orderStatus = 1;
      this.barsInTrade = 0;
   }

   public static double getPriceByType(int var0, double var1, double var3) {
      return var0 != 1 && var0 != 5 && var0 != 3 && var0 != 7 ? var1 : var3;
   }

   public static double getClosePriceByType(int var0, double var1, double var3) {
      return var0 != 1 && var0 != 5 && var0 != 3 && var0 != 7 ? var3 : var1;
   }

   public double getPriceByType(int var1, double var2, double var4, double var6) {
      double var8 = getPriceByType(var1, var2, var4);
      if (var1 == 1) {
         var8 += var6 * this.instrumentInfo.tickSize;
         var8 = SQUtils.fixPrice(this.instrumentInfo.tickStep, var8, this.instrumentInfo.decimals);
      } else if (var1 == 2) {
         var8 -= var6 * this.instrumentInfo.tickSize;
         var8 = SQUtils.fixPrice(this.instrumentInfo.tickStep, var8, this.instrumentInfo.decimals);
      }

      return var8;
   }

   @Override
   public boolean isPendingOrder() {
      return this.orderType == 3 || this.orderType == 4 || this.orderType == 5 || this.orderType == 6;
   }

   @Override
   public boolean isMarketOrder() {
      return this.orderType == 1 || this.orderType == 2;
   }

   public boolean isExitOrder() {
      return this.orderType == 100 || this.orderType == 101 || this.orderType == 102 || this.orderType == 103;
   }

   @Override
   public boolean isClosedOrder() {
      return this.orderStatus != 1 && this.orderStatus != 2;
   }

   @Override
   public InstrumentInfo getInstrumentInfo() {
      return this.instrumentInfo;
   }

   public void setTickData(double var1, double var3) {
      this.ask = var1;
      this.bid = var3;
   }

   public double getBid() {
      return this.bid;
   }

   public double getAsk() {
      return this.ask;
   }

   public double getClosePriceWithSlippage(double var1, double var3, byte var5) {
      if (var1 == 0.0) {
         return var3;
      } else {
         byte var6 = this.getOrderTypeFromSLPT(var5 == 2 ? -1 : 1);
         if (var6 == 6) {
            var3 -= var1 * this.instrumentInfo.tickSize;
            return SQUtils.fixPrice(this.instrumentInfo.tickStep, var3, this.instrumentInfo.decimals);
         } else if (var6 == 5) {
            var3 += var1 * this.instrumentInfo.tickSize;
            return SQUtils.fixPrice(this.instrumentInfo.tickStep, var3, this.instrumentInfo.decimals);
         } else {
            return var3;
         }
      }
   }

   public byte getOrderTypeFromSLPT(int var1) {
      if (var1 == -1) {
         return (byte)(this.getDirection() == 1 ? 6 : 5);
      } else {
         return (byte)(this.getDirection() == 1 ? 4 : 3);
      }
   }

   @Override
   public boolean isLong() {
      return this.orderType == 1 || this.orderType == 3 || this.orderType == 5 || this.orderType == 7;
   }

   public void setCommSwap(double var1) {
      this.commSwap = var1;
   }

   @Override
   public double getCommSwap() {
      return this.commSwap;
   }

   public void setMAE(float var1) {
      this.pipsMAE = var1;
   }

   public void setMFE(float var1) {
      this.pipsMFE = var1;
   }

   @Override
   public float getMAE() {
      return this.pipsMAE;
   }

   @Override
   public float getMFE() {
      return this.pipsMFE;
   }

   public Trader getTrader() {
      return this.trader;
   }

   public void setOrderType(byte var1) {
      this.orderType = var1;
   }

   public void transformMAE_MFE() {
      this.pipsMAE = (float)(this.pipsMAE / this.instrumentInfo.tickSize);
      this.pipsMFE = (float)(this.pipsMFE / this.instrumentInfo.tickSize);
   }

   public boolean isAmbiguous() {
      return this.ambiguous;
   }

   public void setAmbiguous() {
      this.ambiguous = true;
   }

   public void setFilled(boolean var1) {
      this.filled = var1;
   }

   @Override
   public String toString() {
      StringBuilder var1 = new StringBuilder();
      var1.append(SQTime.toDateMinuteString(this.originalOpenTime));
      var1.append("/");
      var1.append(SQTime.toDateMinuteString(this.openTime));
      var1.append(" : ");
      var1.append(OrderTypes.toString(this.orderType));
      var1.append(" : ");
      var1.append(this.openPrice);
      var1.append(" - ");
      var1.append(SQTime.toDateMinuteString(this.closeTime));
      if (this.closeType > 0) {
         var1.append(" : ");
         var1.append(OrderCloseTypes.toString(this.closeType));
         var1.append(" : ");
         var1.append(this.closePrice);
         var1.append(", bars=");
         var1.append(this.barsInTrade);
      }

      return var1.toString();
   }

   @Override
   public void registerEvent(int var1, IActionEventListener var2) throws TradingException {
      if (var1 == 2) {
         if (this.barActionListeners == null) {
            this.barActionListeners = new ObjectArrayList();
         }

         if (this.barActionListeners.size() > 10) {
            throw new TradingException("Too many bar action listeners!");
         }

         this.barActionListeners.add(var2);
      } else if (var1 == 4) {
         if (this.tickActionListeners == null) {
            this.tickActionListeners = new ObjectArrayList();
         }

         if (this.tickActionListeners.size() > 10) {
            throw new TradingException("Too many tick action listeners!");
         }

         this.tickActionListeners.add(var2);
      } else {
         if (var1 != 7) {
            throw new TradingException("Unknown updateEventType parameter. Only BarOpen and BarTick are supported!");
         }

         if (this.orderFilledListeners == null) {
            this.orderFilledListeners = new ObjectArrayList();
         }

         if (this.orderFilledListeners.size() > 10) {
            throw new TradingException("Too many order fill listeners!");
         }

         this.orderFilledListeners.add(var2);
      }
   }

   public void evaluateActionListeners(int var1, StrategyBase var2) throws TradingException {
      if ((var1 == 2 || var1 == 3) && this.barActionListeners != null && this.barActionListeners.size() > 0) {
         for (int var5 = 0; var5 < this.barActionListeners.size(); var5++) {
            ((IActionEventListener)this.barActionListeners.get(var5)).OnActionEvent(var2);
         }
      } else if (var1 == 4 && this.tickActionListeners != null && this.tickActionListeners.size() > 0) {
         for (int var4 = 0; var4 < this.tickActionListeners.size(); var4++) {
            ((IActionEventListener)this.tickActionListeners.get(var4)).OnActionEvent(var2);
         }
      } else if (var1 == 7 && this.orderFilledListeners != null && this.orderFilledListeners.size() > 0) {
         for (int var3 = 0; var3 < this.orderFilledListeners.size(); var3++) {
            ((IActionEventListener)this.orderFilledListeners.get(var3)).OnActionEvent(var2);
         }
      }
   }

   public void setWaitingBars(int var1) {
      this.waitingBars = (byte)var1;
   }

   public int getWaitingBars() {
      return this.waitingBars;
   }

   @Override
   public double getLastOpenPrice() {
      return this.lastOpenPrice;
   }

   public void setLastOpenPrice(double var1) {
      this.lastOpenPrice = var1;
   }

   @Override
   public boolean isNettingMode() {
      return this.isNettingMode;
   }

   public void setNettingMode(boolean var1) {
      this.isNettingMode = var1;
   }

   @Override
   public float getSlippageInMoney() {
      return this.slippageInMoney;
   }

   public void setSlippageInMoney(double var1) {
      this.slippageInMoney = (float)var1;
   }

   @Override
   public ILiveOrder setExitIndex(byte var1) {
      this.exitIndex = var1;
      return this;
   }

   @Override
   public byte getExitIndex() {
      return this.exitIndex;
   }

   @Override
   public boolean usesATM() {
      return this.exitIndex != -1;
   }

   public void setATROnOpen(double var1) {
      this.atrOnOpen = (float)var1;
   }

   @Override
   public float getATROnOpen() {
      return this.atrOnOpen;
   }

   @Override
   public boolean isMarketOpen() {
      return this.trader == null ? true : this.trader.IsMarketOpen();
   }

   public int getBarsValid() {
      return this.barsValid;
   }

   public void setBarsValid(int var1) {
      this.barsValid = var1;
   }

   public void setEOD(byte var1) {
      this.eod = var1;
   }

   public byte getEOD() {
      return this.eod;
   }

   public void setExitAfterBars(int var1) {
      this.exitAfterBars = var1;
   }

   public int getExitAfterBars() {
      return this.exitAfterBars;
   }

   public void setSLPTValidFrom(int var1) {
      this.slptValidFrom = var1;
   }

   public int getSLPTValidFrom() {
      return this.slptValidFrom;
   }
}
