package com.strategyquant.plugin.DataSource.impl.Dukascopy;

public class DukasExport {
   private long dateFrom;
   private long dateTo;
   private String targetFolder;
   private String symbol;
   private String filenamePrefix;

   public long getDateFrom() {
      return this.dateFrom;
   }

   public void setDateFrom(long var1) {
      this.dateFrom = var1;
   }

   public long getDateTo() {
      return this.dateTo;
   }

   public void setDateTo(long var1) {
      this.dateTo = var1;
   }

   public String getTargetFolder() {
      return this.targetFolder;
   }

   public void setTargetFolder(String var1) {
      this.targetFolder = var1;
   }

   public String getSymbol() {
      return this.symbol;
   }

   public void setSymbol(String var1) {
      this.symbol = var1;
   }

   public String getFilenamePrefix() {
      return this.filenamePrefix;
   }

   public void setFilenamePrefix(String var1) {
      this.filenamePrefix = var1;
   }
}
