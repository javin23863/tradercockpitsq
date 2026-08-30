package com.strategyquant.tradinglib.strategy;

import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.settings.IXMLAble;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.SampleTypes;
import java.io.Serializable;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import org.jdom2.Element;

public class OutOfSample implements IXMLAble, Serializable {
   private long[] arrDateFrom = null;
   private long[] arrDateTo = null;
   private byte[] arrSampleType = null;
   private boolean hasNoTrade = false;

   public void addRange(Element var1) {
      long var2 = this.stringToTime(var1.getAttributeValue("dateFrom").trim());
      long var4 = this.stringToTime(var1.getAttributeValue("dateTo").trim());
      String var6 = var1.getAttributeValue("type");
      byte var7;
      if (var6 == null) {
         var7 = 20;
      } else if (var6.equals("notrade")) {
         var7 = 99;
         this.hasNoTrade = true;
      } else if (var6.equals("isv")) {
         var7 = 40;
      } else {
         var7 = 20;
      }

      this.addRange(var2, var4, var7);
   }

   private long stringToTime(String var1) {
      return SQTime.parseToMilis(var1, "dd.MM.yyyy");
   }

   public void addRange(long var1, long var3, byte var5) {
      if (this.arrDateFrom == null) {
         this.arrDateFrom = new long[1];
         this.arrDateTo = new long[1];
         this.arrSampleType = new byte[1];
      } else {
         long[] var6 = Arrays.copyOf(this.arrDateFrom, this.arrDateFrom.length + 1);
         long[] var7 = Arrays.copyOf(this.arrDateTo, this.arrDateTo.length + 1);
         byte[] var8 = Arrays.copyOf(this.arrSampleType, this.arrSampleType.length + 1);
         this.arrDateFrom = var6;
         this.arrDateTo = var7;
         this.arrSampleType = var8;
      }

      int var11 = this.arrDateFrom.length - 1;
      this.arrDateFrom[var11] = var1;
      this.arrDateTo[var11] = var3;
      this.arrSampleType[var11] = var5;
      int var12 = 0;
      int var13 = 0;

      for (int var9 = 0; var9 < this.arrSampleType.length - 1; var9++) {
         byte var10 = this.arrSampleType[var9];
         if (SampleTypes.isOOS(var10)) {
            var12++;
         }

         if (SampleTypes.isISV(var10)) {
            var13++;
         }
      }

      if (SampleTypes.isOOS(var5)) {
         this.arrSampleType[var11] = getOOSPart(var12 + 1);
      } else if (SampleTypes.isISV(var5)) {
         this.arrSampleType[var11] = getISVPart(var13 + 1);
      }
   }

   public Element getXML() {
      Element var1 = new Element("OutOfSample");
      if (this.arrDateFrom != null) {
         for (int var2 = 0; var2 < this.arrDateFrom.length; var2++) {
            Element var3 = new Element("Range");
            XMLUtil.trySetAttr(var3, "dateFrom", SQTime.toString(this.arrDateFrom[var2], "d.M.yyyy"));
            XMLUtil.trySetAttr(var3, "dateTo", SQTime.toString(this.arrDateTo[var2], "d.M.yyyy"));
            String var4 = this.typeToString(this.arrSampleType[var2]);
            if (var4 != null) {
               XMLUtil.trySetAttr(var3, "type", var4);
            }

            var1.addContent(var3);
         }
      }

      return var1;
   }

   private String typeToString(byte var1) {
      switch (var1) {
         case 40:
         case 41:
         case 42:
         case 43:
         case 44:
         case 45:
         case 46:
         case 47:
         case 48:
         case 49:
         case 50:
            return "isv";
         case 99:
            return "notrade";
         default:
            return null;
      }
   }

   public void setFromXML(Element var1) throws ParseException {
      List var2 = var1.getChildren("Range");
      this.hasNoTrade = false;
      if (var2 != null && var2.size() != 0) {
         if (this.arrDateFrom == null || this.arrDateFrom.length != var2.size()) {
            this.arrDateFrom = new long[var2.size()];
            this.arrDateTo = new long[var2.size()];
            this.arrSampleType = new byte[var2.size()];
         }

         for (int var3 = 0; var3 < var2.size(); var3++) {
            Element var4 = (Element)var2.get(var3);
            String var5 = var4.getAttributeValue("dateFrom");
            String var6 = var4.getAttributeValue("dateTo");
            if (var5.indexOf(".") > 2) {
               this.arrDateFrom[var3] = SQTime.parseDateToMilis(var5);
               this.arrDateTo[var3] = SQTime.parseDateToMilis(var6);
               this.arrDateFrom[var3] = SQTime.correctDayStart(this.arrDateFrom[var3]);
               this.arrDateTo[var3] = SQTime.correctDayEnd(this.arrDateTo[var3]);
            } else {
               this.arrDateFrom[var3] = SQTime.parseToMilis(var5, "d.M.yyyy");
               this.arrDateTo[var3] = SQTime.parseToMilis(var6, "d.M.yyyy");
            }

            String var7 = var4.getAttributeValue("type");
            if (var7 == null) {
               this.arrSampleType[var3] = 20;
            } else if (var7.equals("notrade")) {
               this.arrSampleType[var3] = 99;
               this.hasNoTrade = true;
            } else if (var7.equals("isv")) {
               this.arrSampleType[var3] = 40;
            } else {
               this.arrSampleType[var3] = 20;
            }
         }
      } else {
         this.arrDateFrom = null;
         this.arrDateTo = null;
      }

      if (this.arrSampleType != null) {
         int var8 = 0;
         int var9 = 0;

         for (int var10 = 0; var10 < this.arrSampleType.length; var10++) {
            byte var11 = this.arrSampleType[var10];
            if (var11 == 40) {
               this.arrSampleType[var10] = getISVPart(++var9);
            } else if (var11 == 20) {
               this.arrSampleType[var10] = getOOSPart(++var8);
            }
         }
      }
   }

   public void clear() {
      this.arrDateFrom = null;
      this.arrDateTo = null;
   }

   @Override
   public String toString() {
      StringBuilder var1 = new StringBuilder("");
      if (this.arrDateFrom != null) {
         for (int var2 = 0; var2 < this.arrDateFrom.length; var2++) {
            if (var2 > 0) {
               var1.append(", ");
            }

            var1.append("oos");
            var1.append(var2);
            var1.append(SQTime.toString(this.arrDateFrom[var2], "dd.MM.yyyy"));
            var1.append(" - ");
            var1.append(SQTime.toString(this.arrDateTo[var2], "dd.MM.yyyy"));
         }
      }

      return var1.toString();
   }

   public boolean isEmpty() {
      return this.arrDateFrom == null;
   }

   public long getDateFrom(int var1) {
      return this.arrDateFrom != null && var1 < this.arrDateFrom.length ? this.arrDateFrom[var1] : 0L;
   }

   public long getDateTo(int var1) {
      return this.arrDateTo != null && var1 < this.arrDateTo.length ? this.arrDateTo[var1] : 0L;
   }

   public byte getSampleType(int var1) {
      return this.arrSampleType != null && var1 < this.arrSampleType.length ? this.arrSampleType[var1] : 127;
   }

   public void setDateFrom(int var1, long var2) {
      if (this.arrDateFrom != null) {
         this.arrDateFrom[var1] = var2;
      }
   }

   public void setDateTo(int var1, long var2) {
      if (this.arrDateTo != null) {
         this.arrDateTo[var1] = var2;
      }
   }

   public static boolean compare(OutOfSample var0, OutOfSample var1) {
      if (var0.arrDateFrom == null && var1.arrDateFrom == null) {
         return true;
      }

      if (var0.arrDateFrom != null && var1.arrDateFrom != null) {
         if (var0.arrDateFrom.length != var1.arrDateFrom.length) {
            return false;
         }

         for (int var2 = 0; var2 < var0.arrDateFrom.length; var2++) {
            if (var0.arrDateFrom[var2] != var1.arrDateFrom[var2]) {
               return false;
            }

            if (var0.arrDateTo[var2] != var1.arrDateTo[var2]) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public int getRangesCount() {
      return this.arrDateFrom == null ? 0 : this.arrDateFrom.length;
   }

   public byte getSampleType(Order var1) {
      if (this.arrDateFrom == null) {
         return 11;
      }

      for (int var2 = 0; var2 < this.arrDateFrom.length; var2++) {
         if (this.arrDateFrom[var2] != 0L && this.arrDateTo[var2] != 0L && this.arrDateFrom[var2] < var1.OpenTime && var1.OpenTime <= this.arrDateTo[var2]) {
            return this.arrSampleType[var2];
         }
      }

      return 11;
   }

   public static byte getISVPart(int var0) {
      switch (var0) {
         case 1:
            return 41;
         case 2:
            return 42;
         case 3:
            return 43;
         case 4:
            return 44;
         case 5:
            return 45;
         case 6:
            return 46;
         case 7:
            return 47;
         case 8:
            return 48;
         case 9:
            return 49;
         case 10:
            return 50;
         default:
            return 11;
      }
   }

   public static byte getOOSPart(int var0) {
      switch (var0) {
         case 1:
            return 21;
         case 2:
            return 22;
         case 3:
            return 23;
         case 4:
            return 24;
         case 5:
            return 25;
         case 6:
            return 26;
         case 7:
            return 27;
         case 8:
            return 28;
         case 9:
            return 29;
         case 10:
            return 30;
         default:
            return 21;
      }
   }

   public boolean hasNoTrade() {
      return this.hasNoTrade;
   }

   public boolean isNoTrade(long var1) {
      if (this.arrDateFrom == null) {
         return false;
      }

      for (int var3 = 0; var3 < this.arrDateFrom.length; var3++) {
         if (this.arrSampleType[var3] == 99
            && this.arrDateFrom[var3] != 0L
            && this.arrDateTo[var3] != 0L
            && var1 >= this.arrDateFrom[var3]
            && var1 <= this.arrDateTo[var3]) {
            return true;
         }
      }

      return false;
   }

   public byte getPartType(int var1) {
      return this.arrSampleType != null && var1 < this.arrSampleType.length ? this.arrSampleType[var1] : 20;
   }

   public int getPartsCount(byte var1) {
      if (this.arrSampleType == null) {
         return 0;
      }

      int var2 = 0;

      for (int var3 = 0; var3 < this.arrSampleType.length; var3++) {
         if (var1 == 40 && SampleTypes.isISV(this.arrSampleType[var3])) {
            var2++;
         } else if (var1 == 20 && SampleTypes.isOOS(this.arrSampleType[var3])) {
            var2++;
         }
      }

      return var2;
   }
}
