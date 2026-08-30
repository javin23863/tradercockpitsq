package com.strategyquant.datalib.data.io;

import java.io.EOFException;

public class OhlcDataBinReader extends DataBinReader {
   private boolean version4Format = false;
   private int headerByteLength = -1;
   private int dataByteLength = 44;

   @Override
   public int getColumnsCount() {
      return 8;
   }

   @Override
   public boolean readData() throws Exception {
      this.tickData.reset();

      try {
         this.tickData.time = this.reader.readLong();
         this.tickData.open = this.reader.readDouble();
         this.tickData.high = this.reader.readDouble();
         this.tickData.low = this.reader.readDouble();
         this.tickData.close = this.reader.readDouble();
         this.tickData.volume = this.reader.readDouble();
         return true;
      } catch (EOFException var2) {
         return false;
      }
   }

   @Override
   public void readHeader() throws Exception {
      int var1 = 0;
      String var2 = this.reader.readUTF();
      String var3 = this.reader.readUTF();
      String var4 = this.reader.readUTF();
      var1 += var2.length() + var3.length() + var4.length();
      if (var2.equalsIgnoreCase("4.0")) {
         this.version4Format = true;
      }

      if (!var3.equals("D")) {
         throw new Exception("File type is not data.");
      }

      int var5 = this.reader.readInt();
      var1 += 4;

      for (int var8 = 0; var8 < var5; var8++) {
         String var6 = this.reader.readUTF();
         int var7 = this.reader.readInt();
         var1 += var6.length() + 4;
      }

      String var12 = this.reader.readUTF();
      var1 += var12.length();
      this.headerByteLength = var1;
      if (!var12.equals("SnRbTs")) {
         throw new Exception("DAT file is damaged!");
      }
   }
}
