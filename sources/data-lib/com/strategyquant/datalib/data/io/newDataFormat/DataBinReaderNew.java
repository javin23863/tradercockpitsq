package com.strategyquant.datalib.data.io.newDataFormat;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.InstrumentInfo;
import com.strategyquant.datalib.data.io.ImportDataInfo;
import com.strategyquant.datalib.data.io.VersatileData;
import java.io.IOException;
import javax.swing.JProgressBar;

public abstract class DataBinReaderNew extends DataManipulatorNew {
   protected ImportDataInfo importInfo;
   protected JProgressBar progressBar;
   protected DataInfo dataInfo;
   protected String fileName;
   protected IRandomAccessReader reader = null;
   public final VersatileData tickData = new VersatileData();
   private byte[] modificators;
   private int timeModificator;

   public static DataBinReaderNew getInstance(int var0, InstrumentInfo var1) {
      DataBinReaderNew var2 = null;
      if (var0 == 2) {
         var2 = new TickDataBinReaderNew();
      } else {
         var2 = new OhlcDataBinReaderNew();
      }

      setDecimals(var1, var2);
      return var2;
   }

   public void setParams(ImportDataInfo var1, DataInfo var2, JProgressBar var3) {
      this.importInfo = var1;
      this.progressBar = var3;
      this.dataInfo = var2;
   }

   public void setFileName(String var1) {
      this.fileName = var1;
   }

   public void openFile() throws Exception {
      this.reader = new RandomAccessReaderFile(this.fileName);
      ((RandomAccessReaderFile)this.reader).openFile();
      this.readHeader();
   }

   public void setData(byte[] var1) throws Exception {
      this.reader = new RandomAccessReaderArray(var1);
      this.readHeader();
   }

   protected int readModificators() throws Exception {
      if (this.isCrypted()) {
         int var1 = this.reader.readInt();
         byte[] var2 = new byte[var1];
         this.reader.readBytes(var2);
         this.modificators = this.decryptModificators(var2);
         this.timeModificator = this.modificators[0] + this.modificators[1] + this.modificators[2] + this.modificators[3];
         return 4 + var1;
      } else {
         return 0;
      }
   }

   protected void fixData(VersatileData var1, boolean var2) {
      if (this.isCrypted()) {
         if (var2) {
            var1.ask = var1.ask - this.modificators[0];
            var1.bid = var1.bid - this.modificators[1];
         } else {
            var1.open = var1.open - this.modificators[0];
            var1.high = var1.high - this.modificators[1];
            var1.low = var1.low - this.modificators[2];
            var1.close = var1.close - this.modificators[3];
         }

         var1.time = var1.time - this.timeModificator;
         var1.volume = var1.volume - this.modificators[0];
      }
   }

   public void closeFile() throws Exception {
      try {
         if (this.reader instanceof RandomAccessReaderFile) {
            ((RandomAccessReaderFile)this.reader).closeFile();
         }
      } catch (IOException var2) {
         throw new Exception("Cannot close file");
      }
   }

   public boolean isDataCorrect() {
      return true;
   }

   public boolean dataRemaining() {
      return this.reader.dataRemaining();
   }

   public abstract void readHeader() throws Exception;

   public abstract int getColumnsCount();

   public abstract boolean readData() throws Exception;

   public abstract void seek(int var1) throws Exception;

   public abstract long getTotalRecords() throws Exception;
}
