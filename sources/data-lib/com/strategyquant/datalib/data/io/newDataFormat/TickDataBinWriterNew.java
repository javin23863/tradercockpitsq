package com.strategyquant.datalib.data.io.newDataFormat;

import com.strategyquant.datalib.data.io.VersatileData;

public class TickDataBinWriterNew extends DataBinWriterNew {
   TickDataWriter tickWriter = new TickDataWriter();

   TickDataBinWriterNew(String var1) {
      super(var1);
   }

   @Override
   public int getColumnsCount() {
      return 0;
   }

   @Override
   public void writeData(VersatileData var1) throws Exception {
      this.tickWriter.writeData(this.writer, this.modifyDataBeforeSave(var1, true));
   }

   @Override
   public void reset() {
      this.tickWriter.reset();
   }

   @Override
   public void overrideDecimals(int var1) {
      this.tickWriter.overrideDecimals(var1);
   }
}
