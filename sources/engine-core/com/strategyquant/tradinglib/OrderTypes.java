package com.strategyquant.tradinglib;

import com.strategyquant.lib.L;

public class OrderTypes {
   public static final byte Any = 0;
   public static final byte Buy = 1;
   public static final byte Sell = 2;
   public static final byte BuyLimit = 3;
   public static final byte SellLimit = 4;
   public static final byte BuyStop = 5;
   public static final byte SellStop = 6;
   public static final byte BuyStopLimit = 7;
   public static final byte SellStopLimit = 8;
   public static final byte Deposit = 9;
   public static final byte Withdrawal = 10;
   public static final byte Balance = 11;
   public static final byte BuyToCoverStop = 100;
   public static final byte BuyToCoverLimit = 101;
   public static final byte SellToCoverStop = 102;
   public static final byte SellToCoverLimit = 103;

   public static boolean isMarketOrder(byte var0) {
      return var0 == 1 || var0 == 2;
   }

   public static boolean isLongOrder(byte var0) {
      return var0 == 1 || var0 == 3 || var0 == 5 || var0 == 7;
   }

   public static boolean isShortOrder(byte var0) {
      return var0 == 2 || var0 == 4 || var0 == 6 || var0 == 8;
   }

   public static boolean areOppositeOrders(byte var0, byte var1) {
      return isShortOrder(var0) && isLongOrder(var1) ? true : isLongOrder(var0) && isShortOrder(var1);
   }

   public static byte getOppositeType(byte var0) {
      if (var0 == 1) {
         return 2;
      } else {
         return (byte)(var0 == 2 ? 1 : 0);
      }
   }

   public static String toString(byte var0) {
      switch (var0) {
         case 0:
            return L.t("Any", new Object[0]);
         case 1:
            return L.t("Buy", new Object[0]);
         case 2:
            return L.t("Sell", new Object[0]);
         case 3:
            return L.t("BuyLimit", new Object[0]);
         case 4:
            return L.t("SellLimit", new Object[0]);
         case 5:
            return L.t("BuyStop", new Object[0]);
         case 6:
            return L.t("SellStop", new Object[0]);
         case 7:
            return L.t("BuyStopLimit", new Object[0]);
         case 8:
            return L.t("SellStopLimit", new Object[0]);
         case 9:
            return L.t("Deposit", new Object[0]);
         case 10:
            return L.t("Withdrawal", new Object[0]);
         case 11:
            return L.t("Balance", new Object[0]);
         case 100:
            return L.t("BuyToCoverStop", new Object[0]);
         case 101:
            return L.t("BuyToCoverLimit", new Object[0]);
         case 102:
            return L.t("SellToCoverStop", new Object[0]);
         case 103:
            return L.t("SellToCoverLimit", new Object[0]);
         default:
            return L.t("Unknown", new Object[0]);
      }
   }

   public static byte parseMT4(String var0) {
      switch (var0) {
         case "buy":
            return 1;
         case "buy stop":
            return 5;
         case "buy limit":
            return 3;
         case "sell":
            return 2;
         case "sell stop":
            return 6;
         case "sell limit":
            return 4;
         default:
            return -1;
      }
   }

   public static byte parseMT5(String var0) {
      switch (var0) {
         case "buy":
            return 1;
         case "buy stop":
            return 1;
         case "buy limit":
            return 1;
         case "sell":
            return 2;
         case "sell stop":
            return 2;
         case "sell limit":
            return 2;
         default:
            return -1;
      }
   }

   public static boolean isStopOrder(byte var0) {
      return var0 == 5 || var0 == 6;
   }

   public static boolean isLimitOrder(byte var0) {
      return var0 == 3 || var0 == 4;
   }
}
