package com.strategyquant.tradinglib;

import com.strategyquant.datalib.InstrumentInfo;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.event.ITradingEventListener;

public interface ILiveOrder {
   double getSL();

   byte getSLType();

   ILiveOrder setSL(double var1);

   ILiveOrder setSL(byte var1, double var2);

   ILiveOrder setSLType(byte var1);

   double getPT();

   ILiveOrder setPT(double var1);

   ILiveOrder setPT(byte var1, double var2);

   String getComment();

   ILiveOrder setComment(String var1);

   int getMagicNumber();

   ILiveOrder setMagicNumber(int var1);

   double getMaxSlippage();

   ILiveOrder setMaxSlippage(double var1);

   long getExpiration();

   ILiveOrder setExpiration(long var1);

   int getSymbolHash();

   String getSymbol();

   byte getOrderType();

   byte getCloseType();

   byte getOriginalOrderType();

   long getOpenTime();

   double getOpenPrice();

   int getOrderId();

   double getClosePrice();

   boolean isSuccessful();

   int getBarsInTrade();

   long getCloseTime();

   double getSize();

   ILiveOrder Send() throws TradingException;

   ILiveOrder SendAsync() throws TradingException;

   ILiveOrder SendAsync(ITradingEventListener var1) throws TradingException;

   boolean isModified();

   ILiveOrder ModifyAsync() throws TradingException;

   ILiveOrder ModifyAsync(ITradingEventListener var1) throws TradingException;

   ILiveOrder Close(byte var1) throws TradingException;

   ILiveOrder CloseAsync() throws TradingException;

   ILiveOrder CloseAsync(ITradingEventListener var1) throws TradingException;

   byte getWorkingStatus();

   byte getPositionStatus();

   String getStrategyName();

   String getSetupName();

   ILiveOrder setSize(double var1);

   ILiveOrder computeSizeIfMissing() throws TradingException;

   void refuse(String var1);

   boolean isRefused();

   int getDirection();

   double getOriginalOpenPrice();

   long getOriginalOpenTime();

   boolean isPendingOrder();

   boolean isMarketOrder();

   boolean isClosedOrder();

   InstrumentInfo getInstrumentInfo();

   int getErrorCode();

   String getErrorMessage();

   double getPL();

   double getPLInPips();

   boolean isLong();

   double getCommSwap();

   float getMAE();

   float getMFE();

   boolean isFilled();

   boolean isNettingMode();

   double getLastOpenPrice();

   void registerEvent(int var1, IActionEventListener var2) throws TradingException;

   float getSlippageInMoney();

   double getInitialSL();

   ILiveOrder setExitIndex(byte var1);

   byte getExitIndex();

   boolean usesATM();

   float getATROnOpen();

   boolean isMarketOpen();
}
