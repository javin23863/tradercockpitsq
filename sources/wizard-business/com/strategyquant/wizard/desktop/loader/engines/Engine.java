package com.strategyquant.wizard.desktop.loader.engines;

public class Engine {
   private String name;
   private String url;

   public Engine(String var1, String var2) {
      this.name = var1;
      this.url = var2;
   }

   public String getName() {
      return this.name;
   }

   public String getUrl() {
      return this.url;
   }

   public String toJson() {
      return "{\"name\":\"" + this.name + "\",\"url\":\"" + this.url + "\"}";
   }
}
