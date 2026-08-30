package com.strategyquant.datalib.data;

import java.util.HashMap;
import java.util.Map;

public class ExchangeTimezone {
   private static final String DEFAULT = "America/New_York";
   private static ExchangeTimezone instance = new ExchangeTimezone();
   private Map<String, String> map = new HashMap<>();

   public static ExchangeTimezone get() {
      return instance;
   }

   private ExchangeTimezone() {
      this.map.put("AMEX", "America/New_York");
      this.map.put("ASX", "Australia/Sydney");
      this.map.put("CNSX", "America/Toronto");
      this.map.put("NASDAQ", "America/New_York");
      this.map.put("NYSE", "America/New_York");
      this.map.put("OTHEROTC", "America/New_York");
      this.map.put("TSX", "America/Toronto");
      this.map.put("TSX-V", "America/Toronto");
      this.map.put("CBOT", "America/Chicago");
      this.map.put("CME", "America/Chicago");
      this.map.put("COMEX", "America/New_York");
      this.map.put("ICEUS", "America/New_York");
      this.map.put("KCBT", "America/Chicago");
      this.map.put("NYMEX", "America/New_York");
      this.map.put("SMALL", "America/Chicago");
   }

   public String getTimezone(String var1) {
      String var2 = this.map.get(var1);
      return var2 != null ? var2 : "America/New_York";
   }
}
