package com.strategyquant.datalib.historyData.dto;

public enum TickerKind {
   FUTURES(1L, "futures"),
   STOCK(2L, "equities");

   private Long id;
   private String forDownload;

   TickerKind(Long nullxx, String nullxxx) {
      this.id = nullxx;
      this.forDownload = nullxxx;
   }

   public Long getId() {
      return this.id;
   }

   public static TickerKind tryEvalTickerFromName(String var0) {
      for (int var1 = 0; var1 < var0.length(); var1++) {
         char var2 = var0.charAt(var1);
         if (Character.isDigit(var2)) {
            return FUTURES;
         }
      }

      return STOCK;
   }

   public static TickerKind forId(Long var0) {
      for (TickerKind var4 : values()) {
         if (var4.getId().equals(var0)) {
            return var4;
         }
      }

      return null;
   }

   public String forDownload() {
      return this.forDownload;
   }
}
