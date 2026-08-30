package com.strategyquant.gridlib;

import java.io.Serializable;

public class FileInfo implements Serializable {
   private static final long serialVersionUID = 1L;
   private byte[] data;

   public FileInfo() {
   }

   public FileInfo(byte[] var1) {
      this.data = var1;
   }

   public byte[] getData() {
      return this.data;
   }

   public void setData(byte[] var1) {
      this.data = var1;
   }
}
