package com.strategyquant.datalib.data.io;

public class MassImportDataInfo {
   private String timezone;
   private int barType;
   private String path;
   private String format;
   private MassImportDataInfo.OverwriteStrategy overwriteStrategy;
   private String instrument;
   private String connection;
   private String postfix;
   private String dateFormat;
   private boolean createStockGroup;
   private String timeframe;

   public String getPath() {
      return this.path;
   }

   public void setPath(String var1) {
      this.path = var1;
   }

   public String getFormat() {
      return this.format;
   }

   public void setFormat(String var1) {
      this.format = var1;
   }

   public MassImportDataInfo.OverwriteStrategy getOverwriteStrategy() {
      return this.overwriteStrategy;
   }

   public void setOverwriteStrategy(MassImportDataInfo.OverwriteStrategy var1) {
      this.overwriteStrategy = var1;
   }

   public int getBarType() {
      return this.barType;
   }

   public void setBarType(int var1) {
      this.barType = var1;
   }

   public String getInstrument() {
      return this.instrument;
   }

   public void setInstrument(String var1) {
      this.instrument = var1;
   }

   public String getTimezone() {
      return this.timezone;
   }

   public void setTimezone(String var1) {
      this.timezone = var1;
   }

   public String getConnection() {
      return this.connection;
   }

   public void setConnection(String var1) {
      this.connection = var1;
   }

   public String getPostfix() {
      return this.postfix;
   }

   public void setPostfix(String var1) {
      this.postfix = var1;
   }

   public boolean isCreateStockGroup() {
      return this.createStockGroup;
   }

   public void setCreateStockGroup(boolean var1) {
      this.createStockGroup = var1;
   }

   public String getDateFormat() {
      return this.dateFormat;
   }

   public void setDateFormat(String var1) {
      this.dateFormat = var1;
   }

   public String getTimeframe() {
      return this.timeframe;
   }

   public void setTimeframe(String var1) {
      this.timeframe = var1;
   }

   public enum OverwriteStrategy {
      overwrite,
      skip,
      create;
   }
}
