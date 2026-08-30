package com.strategyquant.tradinglib;

import com.strategyquant.lib.SQTime;
import java.io.Serializable;

public class Order implements Serializable {
   public static final int NOT_DEFINED = -99999999;
   public String Symbol;
   public String SetupName;
   public String StrategyName;
   public String Comment;
   public int Ticket = 0;
   public int Order = 0;
   public byte Type;
   public byte CloseType = 0;
   public byte SampleType = 127;
   public long OriginalOpenTime;
   public byte OriginalType;
   public int MagicNumber;
   public float Size;
   public float OriginalPrice;
   public long OpenTime;
   public float OpenPrice;
   public long CloseTime;
   public float ClosePrice;
   public float StopLoss = -1.0E8F;
   public float TakeProfit = -1.0E8F;
   public int Duration = 0;
   public short BarsInTrade = 0;
   public float PL = -1.0E8F;
   public float PctPL = 0.0F;
   public float PctPL_TWR = 0.0F;
   public float PipsPL = 0.0F;
   public float DD = 0.0F;
   public float PctDD = 0.0F;
   public float PipsDD = 0.0F;
   public float CommSwap = 0.0F;
   public boolean CommSwapApplied = false;
   public float MAE = 0.0F;
   public float PipsMAE = 0.0F;
   public float MFE = 0.0F;
   public float PipsMFE = 0.0F;
   public float AccountBalance = 0.0F;
   public float PctAccountBalance = 0.0F;
   public float PipsAccountBalance = 0.0F;
   public float AccountBalanceTemp = -1.0E8F;
   public float Extra1 = 0.0F;
   public byte IsInPortfolio = 1;
   public float worstDailyEquity = 0.0F;
   public float SlippageInMoney = 0.0F;
   public byte ExitIndex = -1;
   public float ATROnOpen;
   public long OrderId;

   public Order() {
   }

   public Order(ILiveOrder var1) {
      this.setFromLiveOrder(var1);
   }

   public Order(Order var1) {
      this.setFromOrder(var1);
   }

   public void setFromLiveOrder(ILiveOrder var1) {
      this.Symbol = var1.getSymbol();
      this.SetupName = var1.getSetupName();
      this.StrategyName = var1.getStrategyName();
      this.Comment = var1.getComment();
      this.Ticket = var1.getOrderId();
      this.Type = var1.getOrderType();
      this.OriginalType = var1.getOriginalOrderType();
      this.OpenPrice = (float)var1.getOpenPrice();
      this.OriginalPrice = (float)var1.getOriginalOpenPrice();
      this.Size = (float)var1.getSize();
      this.ClosePrice = (float)var1.getClosePrice();
      this.StopLoss = (float)var1.getInitialSL();
      if (this.StopLoss == -1.0E8F) {
         this.StopLoss = 0.0F;
      }

      this.TakeProfit = (float)var1.getPT();
      this.OpenTime = var1.getOpenTime();
      this.OriginalOpenTime = var1.getOriginalOpenTime();
      this.CloseTime = var1.getCloseTime();
      this.CloseType = var1.getCloseType();
      this.BarsInTrade = (short)(var1.getBarsInTrade() > 32767 ? 32767 : var1.getBarsInTrade());
      this.PipsMAE = var1.getMAE();
      this.PipsMFE = var1.getMFE();
      this.DD = 0.0F;
      this.PctDD = 0.0F;
      this.PipsDD = 0.0F;
      this.CommSwap = (float)var1.getCommSwap();
      this.SlippageInMoney = var1.getSlippageInMoney();
      this.MagicNumber = var1.getMagicNumber();
      this.ExitIndex = var1.getExitIndex();
      this.ATROnOpen = var1.getATROnOpen();
   }

   public boolean isBalanceOrder() {
      return this.Type == 9 || this.Type == 10 || this.Type == 11;
   }

   public boolean isCanceledOrder() {
      return this.CloseType == 6 || this.CloseType == 8 || this.CloseType == 9 || this.CloseType == 4 && !this.isFilledOrder();
   }

   public boolean isPendingOrder() {
      return this.Type == 3 || this.Type == 5 || this.Type == 7 || this.Type == 4 || this.Type == 6 || this.Type == 8;
   }

   public float getPLByType(byte var1) {
      switch (var1) {
         case 20:
            return this.PctPL;
         case 30:
            return this.PipsPL;
         default:
            return this.PL;
      }
   }

   public float getDDByType(byte var1) {
      switch (var1) {
         case 20:
            return this.PctDD;
         case 30:
            return this.PipsDD;
         default:
            return this.DD;
      }
   }

   public long getTimeByPeriodType(byte var1) {
      return var1 == 1 ? this.OpenTime : this.CloseTime;
   }

   public boolean isLong() {
      return OrderTypes.isLongOrder(this.Type);
   }

   public boolean isShort() {
      return OrderTypes.isShortOrder(this.Type);
   }

   @Override
   public String toString() {
      StringBuilder var1 = new StringBuilder();
      var1.append(this.Order);
      var1.append(" - ");
      var1.append(this.Ticket);
      var1.append(" - ");
      var1.append(SQTime.toDateMinuteString(this.OriginalOpenTime));
      var1.append("/");
      var1.append(SQTime.toDateMinuteString(this.OpenTime));
      var1.append(" : ");
      var1.append(OrderTypes.toString(this.Type));
      var1.append(" : ");
      var1.append(this.OpenPrice);
      var1.append(" - ");
      var1.append(SQTime.toDateMinuteString(this.CloseTime));
      var1.append(" : ");
      var1.append(OrderCloseTypes.toString(this.CloseType));
      var1.append(" : ");
      var1.append(this.ClosePrice);
      var1.append(", PL: ");
      var1.append(this.PL);
      var1.append(", Symbol: ");
      var1.append(this.Symbol);
      var1.append(", Size: ");
      var1.append(this.Size);
      return var1.toString();
   }

   public boolean isRealOrder() {
      return this.CloseType != 18;
   }

   public boolean isFilledOrder() {
      return this.Type == 1 || this.Type == 2 || this.Type == 11 || this.Type == 9;
   }

   public void setFromOrder(Order var1) {
      this.Symbol = var1.Symbol;
      this.SetupName = var1.SetupName;
      this.StrategyName = var1.StrategyName;
      this.Comment = var1.Comment;
      this.Ticket = var1.Ticket;
      this.Order = var1.Order;
      this.Type = var1.Type;
      this.CloseType = var1.CloseType;
      this.SampleType = var1.SampleType;
      this.OriginalOpenTime = var1.OriginalOpenTime;
      this.OriginalType = var1.OriginalType;
      this.Size = var1.Size;
      this.OriginalPrice = var1.OriginalPrice;
      this.OpenTime = var1.OpenTime;
      this.OpenPrice = var1.OpenPrice;
      this.CloseTime = var1.CloseTime;
      this.ClosePrice = var1.ClosePrice;
      this.StopLoss = var1.StopLoss;
      this.TakeProfit = var1.TakeProfit;
      this.BarsInTrade = var1.BarsInTrade;
      this.PL = var1.PL;
      this.PctPL = var1.PctPL;
      this.PctPL_TWR = var1.PctPL_TWR;
      this.PipsPL = var1.PipsPL;
      this.DD = var1.DD;
      this.PctDD = var1.PctDD;
      this.PipsDD = var1.PipsDD;
      this.CommSwap = var1.CommSwap;
      this.CommSwapApplied = var1.CommSwapApplied;
      this.SlippageInMoney = var1.SlippageInMoney;
      this.MAE = var1.MAE;
      this.PipsMAE = var1.PipsMAE;
      this.MFE = var1.MFE;
      this.PipsMFE = var1.PipsMFE;
      this.AccountBalance = var1.AccountBalance;
      this.PctAccountBalance = var1.PctAccountBalance;
      this.PipsAccountBalance = var1.PipsAccountBalance;
      this.Extra1 = var1.Extra1;
      this.MagicNumber = var1.MagicNumber;
      this.Duration = var1.Duration;
      this.IsInPortfolio = var1.IsInPortfolio;
      this.ExitIndex = var1.ExitIndex;
      this.ATROnOpen = var1.ATROnOpen;
      this.OrderId = var1.OrderId;
   }

   public int getDirection() {
      return this.Type != 1 && this.Type != 3 && this.Type != 5 && this.Type != 7 ? -1 : 1;
   }
}
