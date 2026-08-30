package com.strategyquant.datalib.data.io.newDataFormat;

import com.strategyquant.datalib.data.io.FuturesVersatileData;
import com.strategyquant.datalib.data.io.VersatileData;
import java.io.IOException;
import java.io.RandomAccessFile;

public class FuturesDataBinWriterNew extends DataBinWriterNew {
   private FuturesDataWriter futuresWriter = new FuturesDataWriter();
   private FuturesVersatileData modifiedVersatileData;

   public FuturesDataBinWriterNew(String var1) {
      super(var1);
   }

   @Override
   public int getColumnsCount() {
      return 8;
   }

   @Override
   public void writeData(VersatileData var1) throws Exception {
      this.futuresWriter.writeData(this.writer, this.modifyDataBeforeSave((FuturesVersatileData)var1, false));
   }

   protected FuturesVersatileData modifyDataBeforeSave(FuturesVersatileData var1, boolean var2) {
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
      this.modifiedVersatileData.openInterest = var1.openInterest + this.modificators[0];
      return this.modifiedVersatileData;
   }

   @Override
   protected void writeModificatorsToHeader() throws Exception {
      super.writeModificatorsToHeader();
      if (this.isCrypted()) {
         this.modifiedVersatileData = new FuturesVersatileData();
      }
   }

   @Override
   public void reset() {
      this.futuresWriter.reset();
   }

   @Override
   public void close() throws Exception {
      super.close();
      this.updateCountInHeader();
   }

   private void updateCountInHeader() throws IOException {
      long var1 = this.futuresWriter.getSavedCount();
      RandomAccessFile var3 = new RandomAccessFile(this.getFileName(), "rw");
      long var4 = 2 + "4.2".length() + 2 + "ABCDEFGH".length() + 2 + "D".length();
      var3.seek(var4);
      var3.writeLong(var1);
      var3.close();
   }

   @Override
   public void overrideDecimals(int var1) {
      this.futuresWriter.overrideDecimals(var1);
   }
}
