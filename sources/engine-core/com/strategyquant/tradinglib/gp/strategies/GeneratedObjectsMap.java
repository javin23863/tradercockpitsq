package com.strategyquant.tradinglib.gp.strategies;

import com.strategyquant.lib.utils.SQUUID;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import org.jdom2.Element;

public class GeneratedObjectsMap {
   private Int2ObjectOpenHashMap<GeneratedObject> generatedObjectsMap = new Int2ObjectOpenHashMap();
   private ArrayList<GeneratedObject> generatedObjectsArray = new ArrayList<>();

   public GeneratedObject get(int var1) {
      if (var1 >= this.size()) {
         throw new IllegalArgumentException("Index must be smaller than size!");
      } else {
         return this.generatedObjectsArray.get(var1);
      }
   }

   public int size() {
      return this.generatedObjectsArray.size();
   }

   public void addItem(Element var1) {
      int var2 = SQUUID.randomUUID().hashCode();
      String var3 = Integer.toString(var2);
      var1.setAttribute("gid", var3);
      GeneratedObject var4 = new GeneratedObject(var2, var1);
      this.generatedObjectsMap.put(var2, var4);
      this.generatedObjectsArray.add(var4);
      List var5 = var1.getChildren("Param");

      for (int var6 = 0; var6 < var5.size(); var6++) {
         Element var7 = (Element)var5.get(var6);
         String var8 = var7.getAttributeValue("generated");
         String var9 = var7.getAttributeValue("type");
         if ((var9 == null || !var9.equals("data")) && (var8 == null || var8.equals("random"))) {
            int var10 = SQUUID.randomUUID().hashCode();
            String var11 = Integer.toString(var10);
            var7.setAttribute("gid", var11);
            this.addParameter(var10, var7, var2);
         }
      }
   }

   public void addParam(Element var1) {
      String var2 = var1.getAttributeValue("type");
      if (!var2.equals("data")) {
         String var3 = SQUUID.randomUUID();
         int var4 = var3.hashCode();
         var1.setAttribute("gid", var3);
         this.addParameter(var4, var1, 0);
      }
   }

   private void addParameter(int var1, Element var2, int var3) {
      GeneratedObject var4 = new GeneratedObject(var1, var2, var3);
      this.generatedObjectsMap.put(var1, var4);
      this.generatedObjectsArray.add(var4);
   }

   public void print() {
      System.out.println("--------------------------------------");
      System.out.println(" GENERATED OBJ MAP");
      System.out.println("--------------------------------------");

      for (int var1 = 0; var1 < this.size(); var1++) {
         this.get(var1).print();
      }

      System.out.println("--------------------------------------");
   }

   public void clear() {
      this.generatedObjectsMap.clear();
      this.generatedObjectsArray.clear();
   }
}
