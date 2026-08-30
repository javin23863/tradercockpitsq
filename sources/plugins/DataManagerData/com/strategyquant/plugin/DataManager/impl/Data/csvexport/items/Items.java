package com.strategyquant.plugin.DataManager.impl.Data.csvexport.items;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;

public class Items {
   private static Items instance;
   public List<AbstractItem> available = new ArrayList<>();

   public static Items get() {
      if (instance == null) {
         instance = new Items();
      }

      return instance;
   }

   private Items() {
      this.available.add(new Date());
      this.available.add(new Time());
      this.available.add(new DateTime());
      this.available.add(new OpenPrice());
      this.available.add(new HighPrice());
      this.available.add(new LowPrice());
      this.available.add(new ClosePrice());
      this.available.add(new AskPrice());
      this.available.add(new BidPrice());
      this.available.add(new Volume());
      this.available.add(new Spread());
      this.available.add(new Symbol());
      this.available.add(new Comma());
      this.available.add(new Semicolon());
      this.available.add(new Tab());
   }

   public AbstractItem findByKey(String var1) {
      for (int var2 = 0; var2 < this.available.size(); var2++) {
         if (var1.equals(this.available.get(var2).key)) {
            return this.available.get(var2);
         }
      }

      return null;
   }

   public JSONArray toJson() {
      JSONArray var1 = new JSONArray();

      for (AbstractItem var3 : this.available) {
         var1.put(var3.toJson());
      }

      return var1;
   }
}
