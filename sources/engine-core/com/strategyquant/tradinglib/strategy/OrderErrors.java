package com.strategyquant.tradinglib.strategy;

public class OrderErrors {
   public static final int None = 0;
   public static final int NotSupportedSymbol = 1;
   public static final int UnrecognizedSend = 2;
   public static final int ErrorInOrder = 3;
   public static final int NothingWasModified = 4;
   public static final int Refused = 5;
   public static final int IncorrectStopLevel = 6;
   public static final int TooManyOpenOrders = 7;
}
