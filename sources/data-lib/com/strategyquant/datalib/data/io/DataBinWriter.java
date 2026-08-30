package com.strategyquant.datalib.data.io;

import com.strategyquant.datalib.InstrumentInfo;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.swing.JProgressBar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DataBinWriter {
   public static final Logger Log = LoggerFactory.getLogger("DataBinWriter");
   private ImportDataInfo importInfo;
   private JProgressBar progressBar;
   private InstrumentInfo symbolInfo;
   private String fileName;
   protected DataOutputStream writer = null;
   private String dataPath = "";

   public DataBinWriter(String var1) {
      this.dataPath = var1;
   }

   public void setParams(ImportDataInfo var1, InstrumentInfo var2, JProgressBar var3) {
      this.importInfo = var1;
      this.progressBar = var3;
      this.symbolInfo = var2;
   }

   public void setFileName(String var1) {
      this.fileName = var1;
   }

   public void open() throws Exception {
      try {
         File var1 = new File(this.fileName);
         if (!var1.exists()) {
            var1.getParentFile().mkdirs();
         }

         this.writer = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(this.fileName)));
         this.writeHeader();
      } catch (FileNotFoundException var2) {
         throw new Exception("File not found: " + this.fileName);
      }
   }

   public void close() throws Exception {
      try {
         this.writer.close();
         this.writer.flush();
         this.writer = null;
      } catch (IOException var2) {
         throw new Exception("Cannot close file");
      }
   }

   public static DataBinWriter getInstance(int var0, String var1, InstrumentInfo var2) {
      return var0 == 2 ? new TickDataBinWriter(var1) : new OhlcDataBinWriter(var1);
   }

   public void renameTempFile(String var1) throws Exception {
      String var2 = this.dataPath + "temp/temp.dat";
      File var3 = new File(var2);
      if (!var3.exists()) {
         throw new Exception("Temp file 'temp.dat' doesn't exist, nothing to rename!");
      }

      File var4 = new File(var1);
      if (var4.exists() && !var4.delete()) {
         throw new Exception("Old data file '" + var1 + "' already exists and cannot be deleted, cannot import data!");
      }

      if (!var3.renameTo(var4)) {
         throw new Exception("File cannot be renamed, cannot merge data!");
      }

      Log.info("Data file renamed to " + var1);
   }

   protected abstract void writeHeader() throws Exception;

   public abstract void writeData(VersatileData var1) throws Exception;

   public abstract int getColumnsCount();
}
