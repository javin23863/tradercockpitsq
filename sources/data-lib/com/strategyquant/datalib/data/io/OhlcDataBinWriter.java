package com.strategyquant.datalib.data.io;

import com.strategyquant.datalib.InstrumentInfo;
import java.io.IOException;

public class OhlcDataBinWriter extends DataBinWriter {
   public OhlcDataBinWriter(String var1) {
      super(var1);
   }

   public OhlcDataBinWriter(String var1, InstrumentInfo var2, String var3) {
      super(var1);
      this.setParams(null, var2, null);
      String var4;
      if (var2.filename != null && var2.filename != "") {
         var4 = var2.filename;
      } else {
         var4 = var2.instrument;
      }

      this.setFileName(var1 + "/" + var4 + "_" + var3 + ".dat");
   }

   @Override
   public int getColumnsCount() {
      return 8;
   }

   @Override
   protected void writeHeader() throws Exception {
      this.writer.writeUTF("4.0");
      this.writer.writeUTF("D");
      this.writer.writeUTF("ABCDEFGH");
      this.writer.writeInt(this.getColumnsCount());

      for (int var1 = 0; var1 < this.getColumnsCount(); var1++) {
         this.writer.writeUTF("_");
         this.writer.writeInt(0);
      }

      this.writer.writeUTF("SnRbTs");
   }

   @Override
   public void writeData(VersatileData var1) throws IOException {
      this.write(var1.time, var1.open, var1.high, var1.low, var1.close, var1.volume);
   }

   private void write(long var1, double var3, double var5, double var7, double var9, double var11) throws IOException {
      this.writer.writeLong(var1);
      this.writer.writeDouble(var3);
      this.writer.writeDouble(var5);
      this.writer.writeDouble(var7);
      this.writer.writeDouble(var9);
      this.writer.writeDouble(var11);
   }
}
