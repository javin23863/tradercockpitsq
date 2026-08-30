package com.strategyquant.datalib.data.io.newDataFormat;

import java.io.EOFException;

public class TickDataBinReaderNew extends DataBinReaderNew {
   private TickDataReader tickReader;
   private long totalRecords = -1L;
   private int headerByteLength;

   TickDataBinReaderNew() {
      this.tickReader = new TickDataReader();
   }

   @Override
   public int getColumnsCount() {
      return 0;
   }

   @Override
   public boolean readData() throws Exception {
      this.tickData.reset();
      if (!this.dataRemaining()) {
         return false;
      }

      try {
         this.tickReader.readData(this.reader, this.tickData);
         this.fixData(this.tickData, true);
         return true;
      } catch (EOFException var2) {
         return false;
      }
   }

   @Override
   public void readHeader() throws Exception {
      try {
         int var1 = 6;
         String var2 = this.reader.readUTF();
         if (!var2.equals("4.2")) {
            throw new IllegalStateException("Not supported version of file: " + var2);
         }

         String var3 = this.reader.readUTF();
         String var4 = this.reader.readUTF();
         var1 += var2.length() + var3.length() + var4.length();
         boolean var5 = var3.equals("C");
         this.setCrypted(var5);
         if (!var3.equals("D") && !var5) {
            throw new Exception("File type is not data.");
         }

         this.totalRecords = this.reader.readLong();
         var1 += 8;
         int var6 = this.reader.readInt();
         var1 += 4;

         for (int var7 = 0; var7 < var6; var7++) {
            String var8 = this.reader.readUTF();
            this.reader.readInt();
            var1 += 2 + var8.length() + 4;
         }

         String var14 = this.reader.readUTF();
         var1 += 2 + var14.length();
         this.headerByteLength = var1;
         if (!var14.equals("SnRbTs")) {
            throw new Exception("DAT file is damaged!");
         }

         this.headerByteLength = this.headerByteLength + this.readModificators();
         this.tickReader.setDataStartPosition(this.headerByteLength);
      } catch (Exception var9) {
         this.tickReader.setDataStartPosition(0L);
         this.tickReader.setVolumeConstant(100.0);
      }
   }

   @Override
   public long getTotalRecords() throws Exception {
      return this.totalRecords;
   }

   @Override
   public void seek(int var1) throws Exception {
      this.tickReader.seek(this.reader, var1);
   }

   @Override
   public void overrideDecimals(int var1) {
      this.tickReader.overrideDecimals(var1);
   }
}
