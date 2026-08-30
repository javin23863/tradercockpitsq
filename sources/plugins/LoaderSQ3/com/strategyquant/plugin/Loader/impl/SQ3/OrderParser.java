package com.strategyquant.plugin.Loader.impl.SQ3;

import com.strategyquant.lib.time.SQTimeOld;
import com.strategyquant.tradinglib.Order;
import org.jdom2.Element;

public class OrderParser {
   public static Order parse(Element var0, String var1) {
      Order var2 = new Order();
      if (var1 != null && var1.equalsIgnoreCase("2.0")) {
         var2.Ticket = getInt(var0, "ticket", 0);
         var2.Type = getByte(var0, "type", 0);
         var2.OriginalType = getByte(var0, "originalType", 0);
         var2.Size = getFloat(var0, "volume", 0.0F);
         var2.OpenPrice = getFloat(var0, "openPrice", 0.0F);
         var2.ClosePrice = getFloat(var0, "closePrice", 0.0F);
         var2.StopLoss = getFloat(var0, "stoploss", 0.0F);
         var2.TakeProfit = getFloat(var0, "takeprofit", 0.0F);
         var2.OpenTime = getLong(var0, "openTime", 0);
         var2.CloseTime = getLong(var0, "closeTime", 0);
         var2.CloseType = getByte(var0, "closeType", 0);
         var2.BarsInTrade = getShort(var0, "barsInTrade", 0);
         var2.PL = getFloat(var0, "pl", 0.0F);
         var2.SampleType = setSampleType(getBool(var0, "outOfSample", false), getBool(var0, "inSampleValidation", false));
         var2.MAE = getFloat(var0, "mae", 0.0F);
         var2.MFE = getFloat(var0, "mfe", 0.0F);
         var2.DD = getFloat(var0, "dd", 0.0F);
         var2.PctDD = getFloat(var0, "pctDD", 0.0F);
         var2.PipsDD = getFloat(var0, "pipsDD", 0.0F);
         var2.CommSwap = -getFloat(var0, "commSwap", 0.0F);
      } else {
         var2.Ticket = getInt(var0, "ticket", 0);
         var2.Type = getByteFromInt(var0, "type", 0);
         var2.OriginalType = getByteFromInt(var0, "originalType", 0);
         var2.Size = getFloat(var0, "volume", 0.0F);
         var2.OpenPrice = getFloat(var0, "openPrice", 0.0F);
         var2.ClosePrice = getFloat(var0, "closePrice", 0.0F);
         var2.StopLoss = getFloat(var0, "stoploss", 0.0F);
         var2.TakeProfit = getFloat(var0, "takeprofit", 0.0F);
         var2.OpenTime = getLong(var0, "openTime", 0);
         var2.CloseTime = getLong(var0, "closeTime", 0);
         var2.CloseType = getByteFromInt(var0, "closeType", 0);
         var2.BarsInTrade = getShortFromInt(var0, "barsInTrade", 0);
         var2.PL = getFloat(var0, "pl", 0.0F);
         var2.SampleType = setSampleType(getBool(var0, "outOfSample", false), getBool(var0, "inSampleValidation", false));
         var2.MAE = getFloat(var0, "mae", 0.0F);
         var2.MFE = getFloat(var0, "mfe", 0.0F);
      }

      int var3 = new SQTimeOld().getDateTime();
      if (var2.OpenTime < var3) {
         var2.OpenTime *= 1000L;
      }

      if (var2.CloseTime < var3) {
         var2.CloseTime *= 1000L;
      }

      if (var2.CloseType != 0 && var2.CloseType != 6 && var2.CloseType != 9 && var2.CloseType != 8) {
         long var4 = (var2.CloseTime - var2.OpenTime) / 1000L;
         var2.Duration = (int)(var4 < 2147483647L ? var4 : 2147483647L);
      } else {
         var2.Duration = 0;
      }

      return var2;
   }

   private static byte setSampleType(boolean var0, boolean var1) {
      return (byte)(var0 ? 20 : 10);
   }

   public static short getShortFromInt(Element var0, String var1, int var2) {
      return (short)getInt(var0, var1, var2);
   }

   public static byte getByteFromInt(Element var0, String var1, int var2) {
      return (byte)getInt(var0, var1, var2);
   }

   public static short getShort(Element var0, String var1, int var2) {
      Element var3 = var0.getChild(var1);
      return var3 == null ? (short)var2 : Short.parseShort(var3.getValue());
   }

   public static byte getByte(Element var0, String var1, int var2) {
      Element var3 = var0.getChild(var1);
      return var3 == null ? (byte)var2 : Byte.parseByte(var3.getValue());
   }

   public static int getInt(Element var0, String var1, int var2) {
      Element var3 = var0.getChild(var1);
      return var3 == null ? var2 : Integer.parseInt(var3.getValue());
   }

   public static long getLong(Element var0, String var1, int var2) {
      Element var3 = var0.getChild(var1);
      return var3 == null ? var2 : Long.parseLong(var3.getValue());
   }

   public static float getFloat(Element var0, String var1, float var2) {
      Element var3 = var0.getChild(var1);
      return var3 == null ? var2 : Float.parseFloat(var3.getValue());
   }

   public static double getDouble(Element var0, String var1, float var2) {
      Element var3 = var0.getChild(var1);
      return var3 == null ? var2 : Double.parseDouble(var3.getValue());
   }

   public static boolean getBool(Element var0, String var1, boolean var2) {
      Element var3 = var0.getChild(var1);
      return var3 == null ? var2 : Boolean.parseBoolean(var3.getValue());
   }
}
