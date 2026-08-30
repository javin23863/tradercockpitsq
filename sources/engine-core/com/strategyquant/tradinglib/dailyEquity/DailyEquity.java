package com.strategyquant.tradinglib.dailyEquity;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.tradinglib.Result;
import it.unimi.dsi.fastutil.longs.Long2FloatRBTreeMap;
import it.unimi.dsi.fastutil.longs.LongBidirectionalIterator;
import it.unimi.dsi.fastutil.longs.Long2FloatMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.PrintWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DailyEquity {
   private static final Logger Log = LoggerFactory.getLogger("DailyEquitySaver");
   private static final String valueDelimiter = ";";
   private static final String lineDelimiter = "\n";

   public static File createTempFileOldUnused(Result var0) {
      Long2FloatRBTreeMap var1 = var0.getWorstDailyEquity();
      if (var1 == null) {
         return null;
      }

      PrintWriter var2 = null;
      String var3 = MainApp.getDataPath() + "/internal/tmp/dailyEquity";
      SQUtils.ensureDirExists(var3);

      try {
         File var4 = File.createTempFile("sq4-dailyEquity-", ".csv", new File(var3));
         var2 = new PrintWriter(var4);
         LongBidirectionalIterator var5 = var1.keySet().iterator();

         while (var5.hasNext()) {
            long var6 = (Long)var5.next();
            double var8 = var1.get(var6);
            var2.print(var6 + ";" + var8 + "\n");
         }

         var2.flush();
         var2.close();
         return var4;
      } catch (Exception var10) {
         if (var2 != null) {
            var2.close();
         }

         Log.error("Writing daily equity values to file '" + var3 + "' failed. ", var10);
         return null;
      }
   }

   public static void loadCsv(Result var0, InputStream var1) {
      try {
         String var2 = SQUtils.inputStreamToString(var1);
         String[] var3 = var2.split("\n");
         Long2FloatRBTreeMap var4 = new Long2FloatRBTreeMap();

         for (int var5 = 0; var5 < var3.length; var5++) {
            String var6 = var3[var5];
            String[] var7 = var6.split(";");
            if (var7.length == 2) {
               var4.addTo(Long.parseLong(var7[0]), (float)Double.parseDouble(var7[1]));
            }
         }

         var0.setWorstDailyEquity(var4);
      } catch (Exception var8) {
         Log.error("Reading daily equity values of result '" + var0.getResultKey() + "' failed. ", var8);
      }
   }

   public static void serialize(Result var0, ObjectOutput var1) {
      Long2FloatRBTreeMap var2 = var0.getWorstDailyEquity();

      try {
         var1.writeInt(var2 == null ? 0 : var2.size());
         if (var2 == null) {
            return;
         }

         ObjectBidirectionalIterator var3 = var2.long2FloatEntrySet().iterator();

         while (var3.hasNext()) {
            Entry var4 = (Entry)var3.next();
            long var5 = var4.getLongKey();
            double var7 = var4.getFloatValue();
            var1.writeLong(var5);
            var1.writeDouble(var7);
         }
      } catch (Exception var10) {
         Log.error("Writing daily equity values to binary file failed. ", var10);
      }

      try {
         var1.flush();
      } catch (IOException var9) {
         Log.error("Error flushing", var9);
      }
   }

   public static void load(Result var0, InputStream var1) {
      try {
         ObjectInputStream var2 = new ObjectInputStream(var1);
         int var3 = var2.readInt();
         if (var3 == 0) {
            return;
         }

         Long2FloatRBTreeMap var4 = new Long2FloatRBTreeMap();

         for (int var5 = 0; var5 < var3; var5++) {
            long var6 = var2.readLong();
            double var8 = var2.readDouble();
            var4.addTo(var6, (float)var8);
         }

         var0.setWorstDailyEquity(var4);
         var2.close();
      } catch (Exception var10) {
         Log.error("Reading daily equity values of result '" + var0.getResultKey() + "' failed. ", var10);
      }
   }
}
