package com.strategyquant.datalib.data.io.newDataFormat;

import com.strategyquant.datalib.InstrumentInfo;
import com.strategyquant.datalib.data.io.VersatileData;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DataBinWriterNew extends DataManipulatorNew {
   public static final Logger Log = LoggerFactory.getLogger("DataBinWriterNew");
   private String fileName;
   protected DataOutputStream writer = null;
   private String dataPath = "";
   protected byte[] modificators;
   private VersatileData modifiedVersatileData;
   protected int timeModificator;

   public DataBinWriterNew(String var1) {
      this.dataPath = var1;
   }

   protected void writeModificatorsToHeader() throws Exception {
      if (this.isCrypted()) {
         this.modificators = this.generateModificators();
         byte[] var1 = this.encryptModificators(this.modificators);
         this.writer.writeInt(var1.length);
         this.writer.write(var1);
         this.modifiedVersatileData = new VersatileData();
         this.timeModificator = this.modificators[0] + this.modificators[1] + this.modificators[2] + this.modificators[3];
      }
   }

   protected VersatileData modifyDataBeforeSave(VersatileData var1, boolean var2) {
      if (!this.isCrypted()) {
         return var1;
      }

      if (var2) {
         this.modifiedVersatileData.ask = var1.ask + this.modificators[0];
         this.modifiedVersatileData.bid = var1.bid + this.modificators[1];
      } else {
         this.modifiedVersatileData.open = var1.open + this.modificators[0];
         this.modifiedVersatileData.high = var1.high + this.modificators[1];
         this.modifiedVersatileData.low = var1.low + this.modificators[2];
         this.modifiedVersatileData.close = var1.close + this.modificators[3];
      }

      this.modifiedVersatileData.time = var1.time + this.timeModificator;
      this.modifiedVersatileData.volume = var1.volume + this.modificators[0];
      return this.modifiedVersatileData;
   }

   protected void writeHeader() throws Exception {
      this.writer.writeUTF("4.2");
      this.writer.writeUTF(this.isCrypted() ? "C" : "D");
      this.writer.writeUTF("ABCDEFGH");
      this.writer.writeLong(0L);
      this.writer.writeInt(this.getColumnsCount());

      for (int var1 = 0; var1 < this.getColumnsCount(); var1++) {
         this.writer.writeUTF("_");
         this.writer.writeInt(0);
      }

      this.writer.writeUTF("SnRbTs");
      this.writeModificatorsToHeader();
   }

   protected byte[] generateModificators() {
      return new byte[]{(byte)(Math.random() * 100.0), (byte)(Math.random() * 150.0), (byte)(Math.random() * 111.0), (byte)(Math.random() * 49.0)};
   }

   public static DataBinWriterNew getInstance(int var0, String var1, InstrumentInfo var2) {
      DataBinWriterNew var3 = null;
      if (var0 == 2) {
         var3 = new TickDataBinWriterNew(var1);
      } else {
         var3 = new OhlcDataBinWriterNew(var1);
      }

      setDecimals(var2, var3);
      return var3;
   }

   public static DataBinWriterNew getCryptedInstance(int var0, String var1, InstrumentInfo var2) {
      DataBinWriterNew var3 = null;
      if (var0 == 2) {
         var3 = new TickDataBinWriterNew(var1);
      } else {
         var3 = new OhlcDataBinWriterNew(var1);
      }

      var3.setCrypted(true);
      setDecimals(var2, var3);
      return var3;
   }

   public void setFileName(String var1) {
      this.fileName = var1;
   }

   public String getFileName() {
      return this.fileName;
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
      if (this.writer != null) {
         try {
            this.writer.flush();
            this.writer.close();
            this.writer = null;
            this.reset();
         } catch (IOException var2) {
            throw new Exception("Cannot close file");
         }
      }
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

   public abstract void writeData(VersatileData var1) throws Exception;

   public abstract int getColumnsCount();

   public abstract void reset();
}
