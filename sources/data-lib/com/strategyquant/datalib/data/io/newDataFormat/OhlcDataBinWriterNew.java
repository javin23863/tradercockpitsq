package com.strategyquant.datalib.data.io.newDataFormat;

import com.strategyquant.datalib.data.io.VersatileData;
import java.io.IOException;
import java.io.RandomAccessFile;

public class OhlcDataBinWriterNew extends DataBinWriterNew {
   private OhlcDataWriter ohlcWriter = new OhlcDataWriter();

   OhlcDataBinWriterNew(String var1) {
      super(var1);
   }

   @Override
   public int getColumnsCount() {
      return 8;
   }

   @Override
   public void writeData(VersatileData var1) throws Exception {
      this.ohlcWriter.writeData(this.writer, this.modifyDataBeforeSave(var1, false));
   }

   @Override
   public void reset() {
      this.ohlcWriter.reset();
   }

   @Override
   public void close() throws Exception {
      super.close();
      this.updateCountInHeader();
   }

   private void updateCountInHeader() throws IOException {
      long var1 = this.ohlcWriter.getSavedCount();
      RandomAccessFile var3 = new RandomAccessFile(this.getFileName(), "rw");
      long var4 = 2 + "4.2".length() + 2 + "ABCDEFGH".length() + 2 + "D".length();
      var3.seek(var4);
      var3.writeLong(var1);
      var3.close();
   }

   @Override
   public void overrideDecimals(int var1) {
      this.ohlcWriter.overrideDecimals(var1);
   }
}
