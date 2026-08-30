package com.strategyquant.tradinglib.engine.stockpicker.data.stockGroups;

import com.strategyquant.lib.L;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StockGroupDataFile {
   public static final Logger Log = LoggerFactory.getLogger(StockGroupDataFile.class);

   public void save(String var1, LoadedStockGroupData var2) throws Exception {
      Log.info(String.format("Saving picker data to cache file '%s'.", var1));
      DataOutputStream var3 = null;
      File var4 = null;

      try {
         var4 = new File(var1);
         var4.getParentFile().mkdirs();
         var3 = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(var1)));
         FSTObjectOutput var5 = new FSTObjectOutput(var3);
         var5.writeObject(var2, new Class[]{LoadedStockGroupData.class});
         var5.close();
      } catch (Exception var13) {
         if (var4 != null) {
            var4.delete();
         }

         Log.error(String.format("Error while loading picker data from cache file '%s'.", var1), var13);
         throw new Exception(L.t("Error while saving picker data to cache file '%s'. ", new Object[]{var1}) + var13.getMessage());
      } finally {
         if (var3 != null) {
            try {
               var3.close();
            } catch (IOException var12) {
            }
         }
      }
   }

   public LoadedStockGroupData load(String var1) throws Exception {
      Log.info(String.format("Loading picker data from cache file '%s'.", var1));
      DataInputStream var2 = null;
      LoadedStockGroupData var3 = null;

      try {
         File var4 = new File(var1);
         if (!var4.exists()) {
            throw new Exception(L.t("File '%s' doesn't exist.", new Object[]{var1}));
         }

         var2 = new DataInputStream(new BufferedInputStream(new FileInputStream(var1)));
         FSTObjectInput var5 = new FSTObjectInput(var2);
         var3 = (LoadedStockGroupData)var5.readObject(new Class[]{LoadedStockGroupData.class});
         var5.close();
      } catch (Exception var13) {
         Log.error(String.format("Error while loading picker data from cache file '%s'.", var1), var13);
         throw new Exception(L.t("Error while loading picker data from cache file '%s'. ", new Object[]{var1}) + var13.getMessage());
      } finally {
         if (var2 != null) {
            try {
               var2.close();
            } catch (IOException var12) {
            }
         }
      }

      return var3;
   }
}
