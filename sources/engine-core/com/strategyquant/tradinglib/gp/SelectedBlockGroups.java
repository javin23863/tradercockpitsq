package com.strategyquant.tradinglib.gp;

import com.strategyquant.tradinglib.Blocks;
import com.strategyquant.tradinglib.gp.strategies.BuildSettings;
import java.util.ArrayList;
import java.util.List;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SelectedBlockGroups {
   public static final Logger Log = LoggerFactory.getLogger("SelectedBlockGroups");
   private ArrayList<String> entryBlocks = new ArrayList<>();
   private ArrayList<String> stopLimitLevelBlocks = new ArrayList<>();

   public SelectedBlockGroups(BuildSettings var1) {
      this.fillAllBlocks(var1.getReplacements(), false);
      this.fillNegatedBlocks();
   }

   private void fillNegatedBlocks() {
      for (int var1 = 0; var1 < this.entryBlocks.size(); var1++) {
         String var2 = this.entryBlocks.get(var1);
         String var3 = Blocks.getOppositeBlockKey(var2);
         if (var3 != null && !this.entryBlocks.contains(var3)) {
            this.entryBlocks.add(var3);
         }
      }

      for (int var4 = 0; var4 < this.stopLimitLevelBlocks.size(); var4++) {
         String var5 = this.stopLimitLevelBlocks.get(var4);
         String var6 = Blocks.getOppositeBlockKey(var5);
         if (var6 != null && !this.stopLimitLevelBlocks.contains(var6)) {
            this.stopLimitLevelBlocks.add(var6);
         }
      }
   }

   private void fillAllBlocks(Element var1, boolean var2) {
      if (var1.getName().equals("StopLimitLevels") || var1.getName().equals("StopLimitRanges")) {
         var2 = true;
      }

      List var3 = var1.getChildren();

      for (int var4 = 0; var4 < var3.size(); var4++) {
         Element var5 = (Element)var3.get(var4);
         String var6 = var5.getName();
         if (var6.equals("Block")) {
            String var7 = var5.getAttributeValue("key");
            if (var7 != null) {
               if (var2) {
                  if (!this.stopLimitLevelBlocks.contains(var7)) {
                     this.stopLimitLevelBlocks.add(var7);
                  }
               } else if (!this.entryBlocks.contains(var7)) {
                  this.entryBlocks.add(var7);
               }
            }
         }

         this.fillAllBlocks(var5, var2);
      }
   }

   public boolean passed(Element var1, boolean var2) {
      if (var1.getName().equals("Formula")) {
         String var3 = var1.getAttributeValue("key");
         if (var3.equals("SQ.Formulas.Price.UseFormula")) {
            var2 = true;
         }
      }

      List var9 = var1.getChildren();

      for (int var4 = 0; var4 < var9.size(); var4++) {
         Element var5 = (Element)var9.get(var4);
         String var6 = var5.getName();
         if (var6.equals("Item")) {
            String var7 = var5.getAttributeValue("generated");
            if (var7 != null) {
               String var8 = var5.getAttributeValue("key");
               if (var8 != null && !var8.equals("Boolean") && !var8.equals("AND") && !var8.equals("OR")) {
                  if (var2) {
                     if (!this.stopLimitLevelBlocks.contains(var8)) {
                        return false;
                     }
                  } else if (!this.entryBlocks.contains(var8)) {
                     return false;
                  }
               }
            }
         }

         if (!this.passed(var5, var2)) {
            return false;
         }
      }

      return true;
   }
}
