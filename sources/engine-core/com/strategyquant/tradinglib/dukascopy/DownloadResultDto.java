package com.strategyquant.tradinglib.dukascopy;

import java.io.Serializable;

public class DownloadResultDto implements Serializable {
   private static final long serialVersionUID = 1L;
   private String filename;
   private byte[] data;
   private int httpCode;
   private long duration;
   private boolean checksumFailed;
   private long downloadedSize;
   private String errorMessage;

   public static DownloadResultDto failed(int var0) {
      DownloadResultDto var1 = new DownloadResultDto();
      var1.httpCode = var0;
      return var1;
   }

   public static DownloadResultDto failed(int var0, long var1, long var3) {
      DownloadResultDto var5 = new DownloadResultDto();
      var5.httpCode = var0;
      var5.duration = var3;
      var5.downloadedSize = var1;
      return var5;
   }

   public static DownloadResultDto success(byte[] var0, long var1) {
      DownloadResultDto var3 = new DownloadResultDto();
      var3.httpCode = 200;
      var3.data = var0;
      var3.duration = var1;
      var3.downloadedSize = var0 == null ? 0L : var0.length;
      return var3;
   }

   public static DownloadResultDto success(String var0, long var1, long var3) {
      DownloadResultDto var5 = new DownloadResultDto();
      var5.httpCode = 200;
      var5.duration = var3;
      var5.filename = var0;
      var5.downloadedSize = var1;
      return var5;
   }

   public static DownloadResultDto checksumFailed() {
      DownloadResultDto var0 = new DownloadResultDto();
      var0.checksumFailed = true;
      return var0;
   }

   public long getDownloadedSize() {
      return this.downloadedSize;
   }

   public void setDownloadedBytes(long var1) {
      this.downloadedSize = var1;
   }

   public void setFilename(String var1) {
      this.filename = var1;
   }

   public String getFilename() {
      return this.filename;
   }

   public byte[] getData() {
      return this.data;
   }

   public void setData(byte[] var1) {
      this.data = var1;
   }

   public int getHttpCode() {
      return this.httpCode;
   }

   public void setHttpCode(int var1) {
      this.httpCode = var1;
   }

   public boolean isChecksumFailed() {
      return this.checksumFailed;
   }

   public void setChecksumFailed(boolean var1) {
      this.checksumFailed = var1;
   }

   public long getDuration() {
      return this.duration;
   }

   public void setDuration(long var1) {
      this.duration = var1;
   }

   public String getErrorMessage() {
      return this.errorMessage;
   }

   public void setErrorMessage(String var1) {
      this.errorMessage = var1;
   }
}
