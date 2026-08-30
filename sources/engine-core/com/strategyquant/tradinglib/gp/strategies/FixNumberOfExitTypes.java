package com.strategyquant.tradinglib.gp.strategies;

import com.strategyquant.lib.IRandomGenerator;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.generator.GenerateException;
import com.strategyquant.tradinglib.gp.AbstractFactory;
import com.strategyquant.tradinglib.gp.IEvolutionaryOperator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FixNumberOfExitTypes implements IEvolutionaryOperator<Node> {
   private static final Logger Log = LoggerFactory.getLogger("FixNumberOfExitTypes");
   private final int maxExitTypes;
   private final boolean slRequired;
   private final boolean ptRequired;
   private final boolean exitSymmetry;
   private final ArrayList<String> requiredExitsList;

   public FixNumberOfExitTypes(SettingsMap var1, Element var2) throws GenerateException {
      this.maxExitTypes = (Integer)var1.get("MaxExitTypes", 0);
      this.slRequired = (Boolean)var1.get("SLRequired");
      this.ptRequired = (Boolean)var1.get("PTRequired");
      this.exitSymmetry = (Boolean)var1.get("ExitSymmetry");
      this.requiredExitsList = new ArrayList<>();
      this.analyzeRequiredExits(var2, this.requiredExitsList);
   }

   private void analyzeRequiredExits(Element var1, ArrayList<String> var2) {
      for (Element var5 : var1.getChildren()) {
         String var6 = var5.getName();
         if (var6.equals("Formula")) {
            String var7 = var5.getAttributeValue("exitMethod");
            if (var7 != null && var7.equals("true")) {
               String var8 = var5.getAttributeValue("probability");
               if (var8 != null && var8.equals("100")) {
                  String var9 = var5.getAttributeValue("key");
                  if (!var2.contains(var9)) {
                     var2.add(var9);
                  }
               }
            }
         }

         this.analyzeRequiredExits(var5, var2);
      }
   }

   @Override
   public List<Node> apply(List<Node> var1, IRandomGenerator var2, AbstractFactory<Node> var3, int var4, int var5) throws Exception {
      for (int var6 = 0; var6 < var1.size(); var6++) {
         Node var7 = (Node)var1.get(var6);
         this.fixStrategy(var7.getStrategy());
      }

      return var1;
   }

   public boolean fixStrategy(Element var1) throws GenerateException {
      if (this.maxExitTypes <= 0) {
         return false;
      }

      Element var2 = var1.getChild("Strategy");
      Element var3 = var2.getChild("Rules");
      ArrayList var4 = new ArrayList();
      this.findEntryOrders(var3, var4);
      boolean var5 = false;
      ArrayList var6 = new ArrayList();

      for (int var7 = 0; var7 < var4.size(); var7++) {
         Element var8 = (Element)var4.get(var7);
         ArrayList var9 = new ArrayList();
         int var10 = this.countAllUsedExitTypes(var8, var9);
         int var11 = var10 - this.maxExitTypes;
         if (var11 > 0) {
            var5 = true;
            if (var7 == 0) {
               this.removeExits(var11, var9, var6, false);
            } else {
               this.removeExits(var11, var9, var6, this.exitSymmetry);
            }

            var9.clear();
            var10 = this.countAllUsedExitTypes(var8, var9);
            if (var10 > this.maxExitTypes) {
               boolean var12 = true;
            }
         }
      }

      return var5;
   }

   private void removeExits(int var1, ArrayList<Element> var2, ArrayList<String> var3, boolean var4) {
      if (var4) {
         var2 = this.prepareFromRemovedKeys(var2, var3);
      } else {
         Collections.shuffle(var2);
      }

      for (int var5 = 0; var5 < var2.size(); var5++) {
         Element var6 = (Element)var2.get(var5);
         String var7 = this.removeOneExit(var6);
         if (var7 != null) {
            var1--;
            var3.add(var7);
         }

         if (var1 <= 0) {
            break;
         }
      }
   }

   private ArrayList<Element> prepareFromRemovedKeys(ArrayList<Element> var1, ArrayList<String> var2) {
      ArrayList var3 = new ArrayList();

      for (int var4 = 0; var4 < var1.size(); var4++) {
         Element var5 = (Element)var1.get(var4);
         String var6 = var5.getAttributeValue("key");
         if (var2.contains(var6)) {
            var3.add(var5);
         }
      }

      return var3;
   }

   private String removeOneExit(Element var1) {
      String var2 = var1.getAttributeValue("key");
      if (this.slRequired && var2.equals("#StopLoss.StopLoss#")) {
         return null;
      }

      if (this.ptRequired && var2.equals("#ProfitTarget.ProfitTarget#")) {
         return null;
      }

      if (this.requiredExitsList.contains(var2)) {
         return null;
      }

      String var3 = var1.getAttributeValue("controlType");
      if (var3.equals("jspinnerVar")) {
         var1.setText("0");
         return var2;
      }

      Element var4 = var1.getChild("Formula");
      if (var4 != null) {
         String var5 = var4.getAttributeValue("key");
         if (!var5.contains("None") && this.removeFormulaExit(var4)) {
            return var2;
         }
      }

      return null;
   }

   private boolean removeFormulaExit(Element var1) {
      String var2 = var1.getAttributeValue("key");
      int var3 = var2.lastIndexOf(".");
      var2 = var2.substring(0, var3);
      var2 = var2 + ".None";
      var1.setAttribute("key", var2);
      var1.removeContent();
      return true;
   }

   private int countAllUsedExitTypes(Element var1, ArrayList<Element> var2) {
      boolean var3 = false;
      List var4 = var1.getChildren("Param");

      for (int var5 = 0; var5 < var4.size(); var5++) {
         Element var6 = (Element)var4.get(var5);
         String var7 = var6.getAttributeValue("exitMethod");
         if (var7 != null && var7.equals("true") && !this.isDependentOn(var6)) {
            String var8 = var6.getAttributeValue("controlType");
            if (var8.equals("jspinnerVar")) {
               String var9 = var6.getText();

               try {
                  int var10 = Integer.parseInt(var9);
                  if (var10 > 0) {
                     var2.add(var6);
                  }
               } catch (NumberFormatException var11) {
               }
            } else {
               Element var12 = var6.getChild("Formula");
               if (var12 != null) {
                  String var13 = var12.getAttributeValue("key");
                  if (!var13.contains("None")) {
                     var2.add(var6);
                  }
               }
            }
         }
      }

      return var2.size();
   }

   private boolean isDependentOn(Element var1) {
      String var2 = var1.getAttributeValue("dependentOn");
      if (var2 != null) {
         return true;
      }

      String var3 = var1.getAttributeValue("key");
      return var3.equals("#MoveSL2BE.SL2BEAddPips#") || var3.equals("#TrailingStop.TrailingActivation#");
   }

   private void findEntryOrders(Element var1, ArrayList<Element> var2) {
      List var3 = var1.getChildren();

      for (int var4 = 0; var4 < var3.size(); var4++) {
         Element var5 = (Element)var3.get(var4);
         String var6 = var5.getName();
         if (var6.equals("Item")) {
            String var7 = var5.getAttributeValue("returnType");
            if (var7 != null && var7.equals("order")) {
               var2.add(var5);
            }
         }

         this.findEntryOrders(var5, var2);
      }
   }
}
