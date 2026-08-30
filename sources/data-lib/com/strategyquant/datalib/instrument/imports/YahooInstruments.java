package com.strategyquant.datalib.instrument.imports;

import com.strategyquant.datalib.instrument.InstrumentManager;
import java.io.BufferedReader;
import java.io.FileReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YahooInstruments {
   public static final Logger Log = LoggerFactory.getLogger(YahooInstruments.class);

   public void checkData(String var1) throws Exception {
      BufferedReader var2 = null;
      String var3 = "";

      try {
         var2 = new BufferedReader(new FileReader(var1));

         while ((var3 = var2.readLine()) != null) {
            String[] var4 = var3.split(",", -1);
            String var5 = var4[0].replaceAll("\\\"", "");
            String var6 = var4[1].replaceAll("\\\"", "");
            String var7 = var4[2].replaceAll("\\\"", "");
         }
      } catch (Exception var15) {
         throw new Exception("File contains incorrect data. Line: " + var3);
      } finally {
         if (var2 != null) {
            try {
               var2.close();
            } catch (Exception var14) {
               Log.error("Exc.", var14);
            }
         }
      }
   }

   public void importData(byte var1, String var2) throws Exception {
      BufferedReader var3 = null;
      String var4 = "";

      try {
         var3 = new BufferedReader(new FileReader(var2));
         var3.readLine();

         while ((var4 = var3.readLine()) != null) {
            String[] var5 = var4.split(",", -1);
            String var6 = var5[0].replaceAll("\\\"", "").trim();
            String var7 = var5[1].replaceAll("\\\"", "").trim();
            String var8 = var5[2].replaceAll("\\\"", "").trim();
            if (!InstrumentManager.checkInstrumentExists(var6)) {
               InstrumentManager.addInstrument(
                  var6, var7, 1.0, 0.01, 0.01, 0.0, 0.0, "<Method type=\"None\" use=\"true\"><Params/></Method>", var1, var8, null, null, null, 1.0, 0.0
               );
               Log.info("Added instrument " + var6 + " | " + var7 + " | " + var8);
            }
         }
      } catch (Exception var16) {
         throw var16;
      } finally {
         if (var3 != null) {
            try {
               var3.close();
            } catch (Exception var15) {
               Log.error("Exc.", var15);
            }
         }
      }
   }
}
