package com.strategyquant.datalib.broker;

public class BrokerDto {
   private String name;
   private String desc;
   private int customizedStocks;
   private int customizedInstruments;
   private int customizedSessions;
   private Integer id;
   private boolean system;
   private boolean mtUse;
   private String mtTimezone;
   private String postfix;
   private boolean stockPickerUse;

   public int getCustomizedStocks() {
      return this.customizedStocks;
   }

   public void setCustomizedStocks(int var1) {
      this.customizedStocks = var1;
   }

   public int getCustomizedInstruments() {
      return this.customizedInstruments;
   }

   public void setCustomizedInstruments(int var1) {
      this.customizedInstruments = var1;
   }

   public boolean isMtUse() {
      return this.mtUse;
   }

   public void setMtUse(boolean var1) {
      this.mtUse = var1;
   }

   public String getMtTimezone() {
      return this.mtTimezone;
   }

   public void setMtTimezone(String var1) {
      this.mtTimezone = var1;
   }

   public String getPostfix() {
      return this.postfix;
   }

   public void setPostfix(String var1) {
      this.postfix = var1;
   }

   public boolean isStockPickerUse() {
      return this.stockPickerUse;
   }

   public void setStockPickerUse(boolean var1) {
      this.stockPickerUse = var1;
   }

   public String getName() {
      return this.name;
   }

   public void setName(String var1) {
      this.name = var1;
   }

   public String getDesc() {
      return this.desc;
   }

   public void setDesc(String var1) {
      this.desc = var1;
   }

   public Integer getId() {
      return this.id;
   }

   public void setId(Integer var1) {
      this.id = var1;
   }

   public boolean isSystem() {
      return this.system;
   }

   public void setSystem(boolean var1) {
      this.system = var1;
   }

   public int getCustomizedSessions() {
      return this.customizedSessions;
   }

   public void setCustomizedSessions(int var1) {
      this.customizedSessions = var1;
   }
}
