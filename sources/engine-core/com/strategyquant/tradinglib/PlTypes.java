package com.strategyquant.tradinglib;

import com.strategyquant.lib.SQUtils;

public class PlTypes {
   public static final byte Money = 10;
   public static final byte Percent = 20;
   public static final byte Pips = 30;
   public static final byte OpenMoney = 40;
   public static final byte OpenPercent = 50;
   public static final byte Ticks = 30;

   public static String print(byte var0) {
      switch (var0) {
         case 20:
         case 50:
            return "%";
         case 30:
            return "pips";
         default:
            return "$";
      }
   }

   public static String printPL(double var0, byte var2) {
      String var3 = SQUtils.d2String(var0, 2) + "";
      switch (var2) {
         case 10:
         case 40:
            return "$" + var3;
         case 20:
         case 50:
            return var3 + " %";
         case 30:
            return var3 + " pips";
         default:
            return var3;
      }
   }

   public static String printPL(double var0, byte var2, int var3) {
      String var4 = SQUtils.d2String(var0, var3) + "";
      switch (var2) {
         case 10:
         case 40:
            return "$" + var4;
         case 20:
         case 50:
            return var4 + " %";
         case 30:
            return var4 + " pips";
         default:
            return var4;
      }
   }

   public static boolean isValid(byte var0) {
      switch (var0) {
         case 10:
         case 20:
         case 30:
         case 40:
         case 50:
            return true;
         default:
            return false;
      }
   }

   public static String typeToString(byte var0) {
      switch (var0) {
         case 10:
            return "Money";
         case 20:
            return "%";
         case 30:
            return "Pips";
         case 40:
            return "Open $";
         case 50:
            return "Open %";
         default:
            return "?";
      }
   }
}
