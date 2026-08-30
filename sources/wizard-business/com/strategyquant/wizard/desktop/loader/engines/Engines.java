package com.strategyquant.wizard.desktop.loader.engines;

import java.util.LinkedList;
import java.util.List;

public class Engines {
   private List<Engine> engines = new LinkedList<>();

   public void addEngine(Engine var1) {
      this.engines.add(var1);
   }

   public String toJson() {
      StringBuilder var1 = new StringBuilder();
      var1.append("{\"engines\":[");
      boolean var2 = true;

      for (Engine var4 : this.engines) {
         if (!var2) {
            var1.append(",");
         }

         var1.append(var4.toJson());
         var2 = false;
      }

      var1.append("]}");
      return var1.toString();
   }
}
