package com.strategyquant.datalib.historyData.dto;

import java.sql.Date;

public class TickerDto {
   private boolean eod;
   private Long id;
   private String name;
   private String ticker;
   private Long marketId;
   private String marketName;
   private Date dateFrom;
   private Date dateTo;
   private String type;
   private Long commodityId;
   private Long aliasTickerId;

   public Long getId() {
      return this.id;
   }

   public void setId(Long var1) {
      this.id = var1;
   }

   public String getName() {
      return this.name;
   }

   public void setName(String var1) {
      this.name = var1;
   }

   public String getTicker() {
      return this.ticker;
   }

   public void setTicker(String var1) {
      this.ticker = var1;
   }

   public Long getMarketId() {
      return this.marketId;
   }

   public void setMarketId(Long var1) {
      this.marketId = var1;
   }

   public String getMarketName() {
      return this.marketName;
   }

   public void setMarketName(String var1) {
      this.marketName = var1;
   }

   public boolean isEod() {
      return this.eod;
   }

   public void setEod(boolean var1) {
      this.eod = var1;
   }

   public Date getDateFrom() {
      return this.dateFrom;
   }

   public void setDateFrom(Date var1) {
      this.dateFrom = var1;
   }

   public Date getDateTo() {
      return this.dateTo;
   }

   public void setDateTo(Date var1) {
      this.dateTo = var1;
   }

   public String getType() {
      return this.type;
   }

   public void setType(String var1) {
      this.type = var1;
   }

   public Long getCommodityId() {
      return this.commodityId;
   }

   public void setCommodityId(Long var1) {
      this.commodityId = var1;
   }

   public Long getAliasTickerId() {
      return this.aliasTickerId;
   }

   public void setAliasTickerId(Long var1) {
      this.aliasTickerId = var1;
   }
}
