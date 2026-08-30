package com.strategyquant.tradinglib.blocks.random;

import com.strategyquant.lib.SQUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.List;
import org.jdom2.Element;

public class ItemRandomValueRanges {
   private Int2ObjectOpenHashMap<String> map;

   public static ItemRandomValueRanges getRandomValueRanges(Element var0) {
      List var1 = var0.getChildren("Param");
      ItemRandomValueRanges var2 = null;

      for (int var3 = 0; var3 < var1.size(); var3++) {
         Element var4 = (Element)var1.get(var3);
         String var5 = var4.getAttributeValue("generate");
         if (var5 != null && var5.equals("random")) {
            String var6;
            if (var0.getAttributeValue("key").equals("Number") && var4.getAttributeValue("key").equals("#Number#")) {
               String var7 = var4.getText();
               var6 = var4.getAttributeValue("randomValue");
               if (var7.contains(":") && !var6.contains(":")) {
                  var6 = var7;
               }
            } else {
               var6 = var4.getAttributeValue("randomValue");
            }

            if (var6 != null && !var6.equals("") && !var6.equals("default")) {
               String var8 = var4.getAttributeValue("key");
               if (var2 == null) {
                  var2 = new ItemRandomValueRanges();
               }

               var2.add(var8, var6);
            }
         }
      }

      return var2;
   }

   private void add(String var1, String var2) {
      if (this.map == null) {
         this.map = new Int2ObjectOpenHashMap();
      }

      this.map.put(SQUtils.betterHashCode(var1), var2);
   }

   public String getForParam(String var1) {
      return this.map == null ? null : (String)this.map.get(SQUtils.betterHashCode(var1));
   }
}
