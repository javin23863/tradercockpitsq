package com.strategyquant.datalib.data.io;

import com.strategyquant.datalib.DataInfo;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import javax.swing.JProgressBar;

public abstract class DataBinReader {
   protected ImportDataInfo importInfo;
   protected JProgressBar progressBar;
   protected DataInfo dataInfo;
   protected String fileName;
   protected DataInputStream reader = null;
   public final VersatileData tickData = new VersatileData();

   public static DataBinReader getInstance(int var0, int var1) {
      return var0 == 2 ? new TickDataBinReader() : new OhlcDataBinReader();
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
      this.reader = new DataInputStream(new BufferedInputStream(new FileInputStream(this.fileName)));
      this.readHeader();
   }

   public void closeFile() throws Exception {
      try {
         this.reader.close();
      } catch (IOException var2) {
         throw new Exception("Cannot close file");
      }
   }

   public boolean isDataCorrect() {
      return true;
   }

   public abstract void readHeader() throws Exception;

   public abstract int getColumnsCount();

   public abstract boolean readData() throws Exception;
}
