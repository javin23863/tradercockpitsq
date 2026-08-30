package com.strategyquant.datalib.historyData;

public class TickerFilterDto {
   private String[] names;
   private Long marketId;
   private boolean searchInTicker;
   private boolean searchInName;
   private boolean exactMatch;
   private boolean onlyContFutures;

   public Long getMarketId() {
      return this.marketId;
   }

   public void setMarketId(Long var1) {
      this.marketId = var1;
   }

   public String[] getNames() {
      return this.names;
   }

   public void setNames(String[] var1) {
      this.names = var1;
   }

   public boolean isSearchInTicker() {
      return this.searchInTicker;
   }

   public void setSearchInTicker(boolean var1) {
      this.searchInTicker = var1;
   }

   public boolean isSearchInName() {
      return this.searchInName;
   }

   public void setSearchInName(boolean var1) {
      this.searchInName = var1;
   }

   public boolean isOnlyContFutures() {
      return this.onlyContFutures;
   }

   public void setOnlyContFutures(boolean var1) {
      this.onlyContFutures = var1;
   }

   public boolean isExactMatch() {
      return this.exactMatch;
   }

   public void setExactMatch(boolean var1) {
      this.exactMatch = var1;
   }
}
