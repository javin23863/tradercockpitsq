package com.strategyquant.plugin.DataManager.impl.CustomData.job;

import com.strategyquant.datalib.customData.CustomData;
import com.strategyquant.datalib.customData.CustomDataInfo;
import com.strategyquant.datalib.data.io.AbstractDataCsvLoader;
import com.strategyquant.datalib.data.io.ImportDataInfo;
import com.strategyquant.datalib.data.io.columns.DefaultCol;
import com.strategyquant.lib.L;
import com.strategyquant.lib.utils.IProgressListener;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomDataCsvLoader extends AbstractDataCsvLoader {
   public static final Logger Log = LoggerFactory.getLogger("CustomDataCsvLoader");
   public CustomData loadedData;
   public long fromDate = -1L;
   public long toDate = -1L;

   public CustomDataCsvLoader(IProgressListener var1, CustomDataInfo var2, ImportDataInfo var3) {
      this.listener = var1;
      this.importInfo = var3;
      this.loadedData = new CustomData(var2.values);
   }

   public boolean readData() throws Exception {
      if (this.rows == 0 && this.importInfo.skipRows > 0) {
         for (int var2 = 0; var2 < this.importInfo.skipRows; var2++) {
            this.readLine();
         }
      }

      String var1 = this.reader.readLine();
      if (var1 != null && !var1.equals("")) {
         this.loadedRows++;
         Object[] var8 = null;
         int var3 = this.importInfo.columnTypes.size();
         String[] var4 = new String[var3];

         try {
            var8 = var1.split(this.importInfo.separator);
            if (this.importInfo.skipCols > 0) {
               if (var8.length - this.importInfo.skipCols <= 0) {
                  throw new Exception(L.t("There are less columns than specified in Skip Columns parameter!", new Object[0]));
               }

               if (var8.length - this.importInfo.skipCols < var3) {
                  throw new Exception(
                     L.t("There are not enough data columns in the file! Found: %d, Required: %d", new Object[]{var8.length - this.importInfo.skipCols, var3})
                  );
               }

               var4 = Arrays.copyOfRange(var8, this.importInfo.skipCols, var8.length);
            } else {
               var4 = var8;
            }

            Object[] var5 = this.parseLine(var4, this.importInfo);
            this.parseCustomData(var5);
            if (this.loadedData.time == Long.MIN_VALUE) {
               throw new Exception(L.t("Cannot recognize date and time!", new Object[0]));
            }

            if (this.loadedData.time > 0L && this.loadedData.time < this.previousTime) {
               if (this.importInfo.errorHandling != 1) {
                  throw new Exception("Invalid time consecution. Each row must have same or latter time than the previous one");
               }

               Log.error(
                  String.format(
                     "Error parsing on row: %d, line contents: %s, message: %s",
                     this.rows,
                     var1,
                     "Invalid time consecution. Each row must have same or latter time than the previous one"
                  )
               );
            }

            if (this.fromDate == -1L) {
               this.fromDate = this.loadedData.time;
            }

            this.toDate = this.loadedData.time;
            if (this.loadedRows < 100000) {
               this.tfRecognizer.processTime(this.loadedData.time);
            }

            this.previousTime = this.loadedData.time;
            return true;
         } catch (Exception var7) {
            Log.error("Error parsing on line: " + this.rows + ", message: " + var7.getMessage(), var7);
            throw new Exception(L.t("Error parsing on line: %d, message: %s", new Object[]{this.rows, var7.getMessage()}));
         }
      } else {
         return false;
      }
   }

   private void parseCustomData(Object[] var1) {
      this.loadedData.reset();
      int var2 = 0;

      for (int var3 = 0; var3 < this.importInfo.columnTypes.size(); var3++) {
         DefaultCol var4 = (DefaultCol)this.importInfo.columnTypes.get(var3);
         switch (var4.getDataType()) {
            case 9:
               this.loadedData.time = (Long)var1[var3];
               break;
            case 9999:
               double var5 = (Double)var1[var3];
               this.loadedData.values[var2] = var5;
               var2++;
         }
      }
   }

   public void restart() {
   }
}
