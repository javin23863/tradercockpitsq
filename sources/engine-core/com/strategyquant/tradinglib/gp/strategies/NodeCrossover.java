package com.strategyquant.tradinglib.gp.strategies;

import com.strategyquant.lib.IRandomGenerator;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.tradinglib.blocks.random.RandomValueConfig;
import com.strategyquant.tradinglib.gp.AbstractFactory;
import com.strategyquant.tradinglib.gp.GPIDs;
import com.strategyquant.tradinglib.gp.IEvolutionaryOperator;
import com.strategyquant.tradinglib.gp.SelectedBlockGroups;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeCrossover implements IEvolutionaryOperator<Node> {
   public static final Logger Log = LoggerFactory.getLogger("NodeCrossover");
   private final double crossoverProbability;
   private final int crossoverPoints;
   private boolean LimitSLPTRRR;
   private double LimitSLPTRRRFromCoef;
   private double LimitSLPTRRRToCoef;
   private SelectedBlockGroups selectedBlockGroups;

   public NodeCrossover(double var1, int var3, BuildSettings var4, SelectedBlockGroups var5) {
      this.crossoverProbability = var1;
      this.crossoverPoints = var3;
      this.selectedBlockGroups = var5;
      SettingsMap var6 = var4.getBuildSettingsMap();
      this.LimitSLPTRRR = var6.getBoolean("LimitSLPTRRR");
      if (this.LimitSLPTRRR) {
         double var7 = var6.getInt("LimitSLPTRRRFrom");
         double var9 = var6.getInt("LimitSLPTRRRTo");
         this.LimitSLPTRRRFromCoef = 1.0 / (var7 / 100.0);
         this.LimitSLPTRRRToCoef = 1.0 / (var9 / 100.0);
      }
   }

   @Override
   public List<Node> apply(List<Node> var1, IRandomGenerator var2, AbstractFactory<Node> var3, int var4, int var5) throws Exception {
      List var6 = new ArrayList(var1.size());
      Int2IntOpenHashMap var7 = new Int2IntOpenHashMap();
      int var8 = var1.size();
      NodeFactory var9 = (NodeFactory)var3;
      int var10 = 3;

      do {
         var10--;
         var6 = this._apply(var1, var2, var6, var4, var5, var9);
         if (var10 > 0) {
            this.removeDuplicates(var6, var8, var7);
         }
      } while (var6.size() < var8 && var10 > 0);

      return var6;
   }

   private void removeDuplicates(List<Node> var1, int var2, Int2IntOpenHashMap var3) {
      int var4 = (int)(var2 * 0.2);
      var4 = Math.min(10, var4);
      var4 = Math.max(2, var4);
      Iterator var5 = var1.iterator();

      while (var5.hasNext()) {
         Node var6 = (Node)var5.next();
         int var7 = var6.getHash();
         int var8 = 0;
         if (var3.containsKey(var7)) {
            var8 = var3.get(var7);
         }

         if (var8 >= var4) {
            var5.remove();
         } else {
            var3.put(var7, ++var8);
         }
      }
   }

   public List<Node> _apply(List<Node> var1, IRandomGenerator var2, List<Node> var3, int var4, int var5, NodeFactory var6) throws Exception {
      ArrayList var7 = new ArrayList(var1);
      Collections.shuffle(var7);
      int var8 = var7.size();
      Iterator var9 = var7.iterator();

      while (var9.hasNext() && var3.size() < var8) {
         Node var10 = (Node)var9.next();
         if (var9.hasNext()) {
            Node var11 = (Node)var9.next();
            int var12 = 0;
            if (var2.probability(this.crossoverProbability)) {
               var12 = 1 + var2.nextInt(this.crossoverPoints);
            }

            if (var12 > 0) {
               List var13 = this.mate(var10, var11, var12, var2, var4, var5, var6);
               var3.addAll(var13);
            } else {
               var3.add(var10);
               var3.add(var11);
            }
         } else {
            var3.add(var10);
         }
      }

      if (var3.size() > var8) {
         var3.remove(var3.size() - 1);
      }

      return var3;
   }

   public List<Node> mate(Node var1, Node var2, int var3, IRandomGenerator var4, int var5, int var6, NodeFactory var7) throws Exception {
      byte var8 = 0;
      ArrayList var9 = new ArrayList(2);
      Element var10 = var1.getStrategy().clone();
      Element var11 = var2.getStrategy().clone();
      int var12 = var1.getHash();
      int var13 = var2.getHash();
      GPIDs var14 = var1.getGPIDs();
      GPIDs var15 = var1.getGPIDs();
      boolean var16 = false;
      if (this.makeCrossover(var10, var11, var3, var4, var8, var7)) {
         var16 = true;
      }

      Node var17;
      Node var18;
      if (!var16) {
         var17 = var1.clone();
         var18 = var2.clone();
      } else {
         int var19 = XMLUtil.xmlToStringRaw(var10).hashCode();
         int var20 = XMLUtil.xmlToStringRaw(var11).hashCode();
         String var21 = var1.getGPIDs().toShortString();
         String var22 = var2.getGPIDs().toShortString();
         if (var19 == var12) {
            var17 = var1.clone();
         } else if (var19 == var13) {
            var17 = var1.clone();
         } else {
            var17 = new Node(var10);
            GPIDs var23 = new GPIDs();
            var23.islandIndex = var5;
            var23.generationIndex = var6;
            var23.generationType = "Crossover";
            var23.parent1 = var21;
            var23.parent2 = var22;
            var17.setGPIDs(var23);
            var17.setModified();
         }

         if (var20 == var12) {
            var18 = var1.clone();
         } else if (var20 == var13) {
            var18 = var1.clone();
         } else {
            var18 = new Node(var11);
            GPIDs var24 = new GPIDs();
            var24.islandIndex = var5;
            var24.generationIndex = var6;
            var24.generationType = "Crossover";
            var24.parent1 = var21;
            var24.parent2 = var22;
            var18.setGPIDs(var24);
            var18.setModified();
         }
      }

      var9.add(var17);
      var9.add(var18);
      return var9;
   }

   private boolean checkBadFormula(Element var1) {
      ArrayList var2 = new ArrayList();
      XMLUtil.findAllWithKey(var1, "Param", "#StopLoss.StopLoss#", var2);

      for (int var3 = 0; var3 < var2.size(); var3++) {
         Element var4 = (Element)var2.get(var3);
         if (var4.getChildren().size() == 0) {
            return true;
         }
      }

      return false;
   }

   private boolean makeCrossover(Element var1, Element var2, int var3, IRandomGenerator var4, int var5, NodeFactory var6) throws Exception {
      ArrayList var7 = new ArrayList();
      ArrayList var8 = new ArrayList();
      this.recognizeReplacableElements(var1, var7, false);
      this.recognizeReplacableElements(var2, var8, false);
      if (!var7.isEmpty() && !var8.isEmpty()) {
         int var9 = var7.size() * 2;
         int var10 = 0;
         int var11 = 0;

         while (true) {
            var5++;
            var11++;
            var9--;
            if (var9 <= 0 || var10 >= var3 || var7.size() == 0 || var8.size() == 0) {
               if (var10 <= 0) {
                  return false;
               } else if (GenEvoChecker.fixZeroPeriodsAndExits(var1) == null) {
                  return false;
               } else if (GenEvoChecker.fixZeroPeriodsAndExits(var2) == null) {
                  return false;
               } else if (!GenEvoChecker.passedRRRCheck(var1, this.LimitSLPTRRR, this.LimitSLPTRRRToCoef, this.LimitSLPTRRRFromCoef)) {
                  return false;
               } else if (!GenEvoChecker.passedRRRCheck(var2, this.LimitSLPTRRR, this.LimitSLPTRRRToCoef, this.LimitSLPTRRRFromCoef)) {
                  return false;
               } else {
                  return !this.selectedBlockGroups.passed(var1, false) ? false : this.selectedBlockGroups.passed(var2, false);
               }
            }

            int var12 = var4.nextInt(var7.size());
            NodeElement var13 = (NodeElement)var7.get(var12);
            ArrayList var14 = this.findCompatibleElements(var13, var8);
            if (var14 == null) {
               this.removeFromReplacable(var7, var13);
            } else {
               int var15 = var4.nextInt(var14.size());
               NodeElement var16 = (NodeElement)var14.get(var15);
               this.replaceElements(var13, var16);
               var10++;
               this.removeFromReplacable(var7, var13);
               this.removeFromReplacable(var8, var16);
            }
         }
      } else {
         return false;
      }
   }

   private void logReplacableElements(ArrayList<NodeElement> var1, String var2) {
      String var3 = "";

      for (NodeElement var5 : var1) {
         var3 = var3 + "------------------------------------\n";
         var3 = var3 + XMLUtil.elementToString(var5.element);
         var3 = var3 + "------------------------------------\n";
      }

      SQUtils.stringToFile(MainApp.getDataPath() + "tests/tmp/generated/" + var2, var3);
   }

   private void removeFromReplacable(ArrayList<NodeElement> var1, NodeElement var2) {
      var1.remove(var2);
      Iterator var3 = var1.iterator();

      while (var3.hasNext()) {
         NodeElement var4 = (NodeElement)var3.next();
         if (var4 == var2) {
            var3.remove();
         } else if (var4.elementParent == var2.element) {
            var3.remove();
         }
      }
   }

   private void replaceElements(NodeElement var1, NodeElement var2) {
      NodeElement var3 = new NodeElement(var1.element.clone(), var1.elementParent);
      this.replaceElement(var1, var2);
      this.replaceElement(var2, var3);
   }

   private void replaceElement(NodeElement var1, NodeElement var2) {
      List var3 = var1.element.getAttributes();
      int var4 = var3.size();

      for (int var5 = 0; var5 < var4; var5++) {
         String var6 = ((Attribute)var3.get(0)).getName();
         if (!var6.equals("generated") && !var6.equals("randomId")) {
            var1.element.removeAttribute(var6);
         }
      }

      var3 = var2.element.getAttributes();
      var4 = var3.size();

      for (int var9 = 0; var9 < var4; var9++) {
         String var11 = ((Attribute)var3.get(var9)).getName();
         if (!var11.equals("generated") && !var11.equals("randomId")) {
            var1.element.setAttribute(var11, ((Attribute)var3.get(var9)).getValue());
         }
      }

      var1.element.removeContent();
      List var10 = var2.element.getChildren();
      if (var10.size() > 0) {
         while (var10.size() > 0) {
            var1.element.addContent(((Element)var10.get(0)).detach());
         }
      } else if (var1.element.getName().equals("Param")) {
         var1.element.setText(var2.element.getText());
      }
   }

   private void recognizeReplacableElements(Element var1, ArrayList<NodeElement> var2, boolean var3) {
      String var4 = var1.getName();
      if (var4.equals("Item")) {
         String var5 = var1.getAttributeValue("generated");
         if (var5 != null && var5.equals("random")) {
            String var6 = var1.getAttributeValue("returnType");
            if (var6 != null && (var6.equals("boolean") || var6.equals("price") || var6.equals("number"))) {
               String var7 = var1.getAttributeValue("key");
               var2.add(new NodeElement(var1));
               var3 = true;
            }
         }
      } else if (var4.equals("Param")) {
         String var9 = var1.getAttributeValue("generated");
         if (var3 || var9 != null && var9.equals("random")) {
            String var11 = var1.getAttributeValue("key");
            if (!var11.equals("#Shift#") && !var11.equals("#Direction#") && !var11.contains("Magic")) {
               String var13 = var1.getAttributeValue("type");
               if (var13 != null && (var13.equals("int") || var13.equals("double") || var13.equals("boolean"))) {
                  String var8 = var1.getAttributeValue("variable");
                  if (var8 == null) {
                     var2.add(new NodeElement(var1));
                  }
               }
            }
         }
      }

      List var10 = var1.getChildren();
      if (var10 != null && var10.size() > 0) {
         for (int var12 = 0; var12 < var10.size(); var12++) {
            Element var14 = (Element)var10.get(var12);
            this.recognizeReplacableElements(var14, var2, var3);
         }
      }
   }

   private ArrayList<NodeElement> findCompatibleElements(NodeElement var1, ArrayList<NodeElement> var2) {
      ArrayList var3 = null;
      boolean var4 = var1.element.getName().equals("Item");
      String var5 = var1.element.getAttributeValue("key");

      for (int var6 = 0; var6 < var2.size(); var6++) {
         NodeElement var7 = (NodeElement)var2.get(var6);
         if (var4) {
            if (var7.element.getName().equals("Item")) {
               String var8 = var1.element.getAttributeValue("returnType");
               String var9 = var7.element.getAttributeValue("returnType");
               if (var8.equals(var9)) {
                  var3 = this.addToCompatibleElements(var3, var7);
               }
            }
         } else {
            String var13 = var7.element.getAttributeValue("key");
            if (var5.equals(var13)) {
               String var14 = var1.element.getAttributeValue("type");
               String var10 = var7.element.getAttributeValue("type");
               if (var14.equals(var10)) {
                  String var11 = var1.element.getAttributeValue("randomId");
                  String var12 = var7.element.getAttributeValue("randomId");
                  if ((var11 == null || var12 != null && var11.equals(var12)) && this.randomRangedValuesMatch(var1.element, var7.element, var14)) {
                     var3 = this.addToCompatibleElements(var3, var7);
                  }
               }
            }
         }
      }

      return var3;
   }

   private boolean randomRangedValuesMatch(Element var1, Element var2, String var3) {
      String var4 = var1.getAttributeValue("randomValue");
      if (var4 != null && !var4.isBlank()) {
         try {
            double var5 = Double.MIN_VALUE;
            RandomValueConfig var7 = null;
            String var8 = var2.getText();
            if (var8 != null && !var8.isBlank()) {
               if (var3.equals("double")) {
                  var5 = Double.parseDouble(var8);
                  var7 = RandomValueConfig.parseRandomValue(var4, true);
               } else if (var3.equals("int")) {
                  var5 = Integer.parseInt(var8);
                  var7 = RandomValueConfig.parseRandomValue(var4, true);
               }
            }

            if (var7 != null && var5 != Double.MIN_VALUE) {
               boolean var9 = var7.isInValidRange(var5);
               if (!var9) {
               }

               return var9;
            }
         } catch (Exception var10) {
            Log.error("Exception ", var10);
         }

         return true;
      } else {
         return true;
      }
   }

   private ArrayList<NodeElement> addToCompatibleElements(ArrayList<NodeElement> var1, NodeElement var2) {
      if (var1 == null) {
         var1 = new ArrayList();
      }

      var1.add(var2);
      return var1;
   }
}
