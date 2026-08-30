package com.strategyquant.datalib.data.io.newDataFormat;

import com.strategyquant.datalib.data.io.ICanSeek;
import java.io.EOFException;

public class OhlcDataBinReaderNew extends DataBinReaderNew implements ICanSeek {
   private int headerByteLength = -1;
   private long totalRecords = -1L;
   private OhlcDataReader ohlcReader = new OhlcDataReader();

   OhlcDataBinReaderNew() {
   }

   @Override
   public int getColumnsCount() {
      return 8;
   }

   @Override
   public boolean readData() throws Exception {
      this.tickData.reset();
      if (!this.dataRemaining()) {
         return false;
      }

      try {
         this.ohlcReader.readData(this.reader, this.tickData);
         this.fixData(this.tickData, false);
         return true;
      } catch (EOFException var2) {
         return false;
      }
   }

   @Override
   public void readHeader() throws Exception {
      int var1 = 6;
      String var2 = this.reader.readUTF();
      boolean var3 = var2.equals("4.1");
      boolean var4 = var2.equals("4.2");
      if (!var3 && !var4) {
         throw new IllegalStateException("Not supported version of file: " + var2);
      }

      if (var3) {
         this.ohlcReader.setVolumeConstant(100.0);
      }

      String var5 = this.reader.readUTF();
      String var6 = this.reader.readUTF();
      var1 += var2.length() + var5.length() + var6.length();
      boolean var7 = var5.equals("C");
      this.setCrypted(var7);
      if (!var5.equals("D") && !var7) {
         throw new Exception("File type is not data.");
      }

      this.totalRecords = this.reader.readLong();
      var1 += 8;
      int var8 = this.reader.readInt();
      var1 += 4;

      for (int var9 = 0; var9 < var8; var9++) {
         String var10 = this.reader.readUTF();
         this.reader.readInt();
         var1 += 2 + var10.length() + 4;
      }

      String var15 = this.reader.readUTF();
      var1 += 2 + var15.length();
      this.headerByteLength = var1;
      if (!var15.equals("SnRbTs")) {
         throw new Exception("DAT file is damaged!");
      }

      this.headerByteLength = this.headerByteLength + this.readModificators();
      this.ohlcReader.setDataStartPosition(this.headerByteLength);
   }

   @Override
   public void seek(int var1) throws Exception {
      this.ohlcReader.seek(this.reader, var1);
   }

   @Override
   public long getTotalRecords() throws Exception {
      if (this.totalRecords == -1L) {
         this.countTotalRecords();
      }

      return this.totalRecords;
   }

   private void countTotalRecords() throws Exception {
      this.seek(0);
      this.totalRecords = 0L;

      do {
         this.totalRecords++;
      } while (this.readData());

      this.seek(0);
   }

   @Override
   public void overrideDecimals(int var1) {
      this.ohlcReader.overrideDecimals(var1);
   }
}
