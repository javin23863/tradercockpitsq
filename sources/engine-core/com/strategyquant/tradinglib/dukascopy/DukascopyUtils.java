package com.strategyquant.tradinglib.dukascopy;

import SevenZip.Compression.LZMA.Decoder;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.data.io.newDataFormat.DataBinReaderNew;
import com.strategyquant.datalib.data.io.newDataFormat.DataBinWriterNew;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.utils.Pair;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DukascopyUtils {
   public static final Logger Log = LoggerFactory.getLogger(DukascopyUtils.class);
   private static Decoder decoder = new Decoder();

   public static synchronized ByteBuffer decodeBi5Data(InputStream var0) {
      try {
         ByteArrayOutputStream var1 = new ByteArrayOutputStream();
         long var2 = 0L;
         byte var4 = 5;
         byte[] var5 = new byte[var4];
         if (var0.read(var5, 0, var4) != var4) {
            return null;
         }

         if (!decoder.SetDecoderProperties(var5)) {
            throw new Exception(L.t("Incorrect stream properties", new Object[0]));
         }

         for (int var6 = 0; var6 < 8; var6++) {
            int var7 = var0.read();
            if (var7 < 0) {
               throw new Exception(L.t("Can't read stream size", new Object[0]));
            }

            var2 |= (long)var7 << 8 * var6;
         }

         if (!decoder.Code(var0, var1, var2)) {
            throw new Exception(L.t("Error in data stream", new Object[0]));
         }

         byte[] var9 = var1.toByteArray();
         return ByteBuffer.wrap(var9);
      } catch (Exception var8) {
         Log.error("Failed to decode .bi5 data. Exc.", var8);
         throw new RuntimeException(L.t("Failed to decode .bi5 data. Exc.", new Object[0]), var8);
      }
   }

   public static boolean deleteCopyFile(String var0, String var1) {
      String var2 = DataManager.getDataFileName("History", var0, var1, "No Session");
      String var3 = var2 + ".copy";
      return new File(var3).delete();
   }

   public static void revertCopyFile(String var0, String var1) {
      String var2 = DataManager.getDataFileName("History", var0, var1, "No Session");
      String var3 = var2 + ".copy";
      File var4 = new File(var3);
      if (var4.exists()) {
         File var5 = new File(var2);
         var5.delete();
         var4.renameTo(var5);
      }
   }

   public static Pair<DataBinReaderNew, DataBinWriterNew> prepareFiles(ImportInfo var0, String var1) throws Exception {
      String var2 = DataManager.getDataFileName("History", var0.symbol, var1, "No Session");
      String var3 = var2 + ".copy";
      File var4 = new File(var2);
      File var5 = new File(var3);
      int var6 = var1.equals("TICK") ? 2 : 1;
      DataBinReaderNew var7 = null;
      if (var4.exists()) {
         if (var4.length() == 0L) {
            var4.delete();
         } else {
            try {
               Files.copy(var4.toPath(), var5.toPath(), StandardCopyOption.REPLACE_EXISTING);
               var7 = DataBinReaderNew.getInstance(var6, var0.instrumentInfo);
               var7.setFileName(var3);
               var7.openFile();
            } catch (Exception var11) {
               Log.error("Cannot make a copy of existing symbol data file. ", var11);
               throw new Exception(L.t("Error - Cannot make a copy of existing symbol data file.", new Object[0]), var11);
            }
         }
      }

      DataBinWriterNew var8;
      try {
         var8 = DataBinWriterNew.getInstance(var6, MainApp.getDataPath(), var0.instrumentInfo);
         var8.setFileName(var2);
         var8.open();
      } catch (Exception var10) {
         Log.error("Cannot open symbol data file for writing. ", var10);
         throw new Exception(L.t("Error - Cannot open symbol data file for writing.", new Object[0]));
      }

      return new Pair(var7, var8);
   }

   public static String generateCdnYearUrl(String var0, int var1, String var2) {
      return var0 + "/" + var2 + "/" + var1 + ".zip";
   }

   public static String generateCdnMonthUrl(String var0, int var1, int var2, String var3) {
      String var4 = String.format("%d_%02d", var1, var2);
      return var0 + "/" + var3 + "/" + var4 + ".zip";
   }

   public static String generateTickDukascopyUrl(long var0, String var2) {
      int var3 = SQTime.getFullYear(var0);
      int var4 = SQTime.getMonth(var0);
      int var5 = SQTime.getDay(var0);
      int var6 = SQTime.getHour(var0);
      String var7 = String.format("%d/%02d/%02d/%02d", var3, var4, var5, var6);
      return "http://datafeed.dukascopy.com/datafeed/" + var2 + "/" + var7 + "h_ticks.bi5";
   }

   public static String generateM1DukascopyUrl(long var0, String var2) {
      int var3 = SQTime.getFullYear(var0);
      int var4 = SQTime.getMonth(var0);
      int var5 = SQTime.getDay(var0);
      String var6 = String.format("%d/%02d/%02d", var3, var4, var5);
      return "https://datafeed.dukascopy.com/datafeed/" + var2 + "/" + var6 + "/BID_candles_min_1.bi5";
   }
}
