package com.strategyquant.datalib.data.io;

import java.io.IOException;

public class TickDataBinWriter extends DataBinWriter {
   public TickDataBinWriter(String var1) {
      super(var1);
   }

   @Override
   public int getColumnsCount() {
      return 0;
   }

   @Override
   public void writeHeader() throws Exception {
   }

   @Override
   public void writeData(VersatileData var1) throws IOException {
      this.writer.writeLong(var1.time);
      this.writer.writeDouble(var1.ask);
      this.writer.writeDouble(var1.bid);
      this.writer.writeDouble(var1.volume);
   }
}
