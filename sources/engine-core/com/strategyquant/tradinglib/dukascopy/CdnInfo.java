package com.strategyquant.tradinglib.dukascopy;

import java.util.HashMap;
import java.util.Map;

public class CdnInfo {
   private Map<String, CdnInfo.SymbolInfo> map = new HashMap<>();

   public CdnInfo.SymbolInfo getSymbolInfo(String var1, String var2) {
      return this.map.get(var1 + "_" + var2);
   }

   public void addSymbolInfo(String var1, String var2, String var3, String var4) {
      if (!this.map.containsKey(var1)) {
         this.map.put(var1 + "_" + var2, new CdnInfo.SymbolInfo(var3, var4));
      }
   }

   public static class SymbolInfo {
      private String url;
      private String format;

      public SymbolInfo(String var1, String var2) {
         this.url = var1;
         this.format = var2;
      }

      public String getFormat() {
         return this.format;
      }

      public String getUrl() {
         return this.url;
      }
   }
}
