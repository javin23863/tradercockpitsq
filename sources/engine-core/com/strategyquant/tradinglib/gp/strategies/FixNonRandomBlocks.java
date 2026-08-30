package com.strategyquant.tradinglib.gp.strategies;

import com.strategyquant.lib.IRandomGenerator;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.Blocks;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.NegatersList;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.generator.GenerateException;
import com.strategyquant.tradinglib.generator.Negaters;
import com.strategyquant.tradinglib.gp.AbstractFactory;
import com.strategyquant.tradinglib.gp.GPIDs;
import com.strategyquant.tradinglib.gp.IEvolutionaryOperator;
import com.strategyquant.tradinglib.task.settings.buildtype.BuildType;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.ArrayList;
import java.util.List;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FixNonRandomBlocks implements IEvolutionaryOperator<Node> {
   private static final Logger Log = LoggerFactory.getLogger("FixNonRandomBlocks");
   private final BuildType buildType;
   private NegatersList negatersList;

   public FixNonRandomBlocks(BuildType var1) throws GenerateException {
      this.buildType = var1;
      this.negatersList = Negaters.list();
   }

   @Override
   public List<Node> apply(List<Node> var1, IRandomGenerator var2, AbstractFactory<Node> var3, int var4, int var5) throws Exception {
      ArrayList var6 = new ArrayList(var1.size());

      for (int var7 = 0; var7 < var1.size(); var7++) {
         Node var8 = (Node)var1.get(var7);
         if (this.shouldBeFixed(var8)) {
            GPIDs var9 = var8.getGPIDs();
            StrategyBase var10 = null;
            var10 = this.fixNonRandomElements(var10, var8, var9.toString());
            this.fixFuzzyConditions(var10, var8, var9.toString());
         }

         var6.add(var8);
      }

      return var6;
   }

   private boolean shouldBeFixed(Node var1) {
      return !var1.isModified() ? false : this.buildType.entrySymmetry || this.buildType.exitSymmetry;
   }

   private StrategyBase fixNonRandomElements(StrategyBase var1, Node var2, String var3) throws Exception {
      Int2ObjectOpenHashMap var4 = new Int2ObjectOpenHashMap();
      Int2ObjectOpenHashMap var5 = new Int2ObjectOpenHashMap();
      Int2ObjectOpenHashMap var6 = new Int2ObjectOpenHashMap();
      Element var7 = var2.getStrategy();
      this.findAllGeneratedElements(var7, var4, var5, var6);
      IntSet var8 = var4.keySet();
      IntIterator var9 = var8.iterator();

      while (var9.hasNext()) {
         int var10 = (Integer)var9.next();
         ObjectArrayList var11 = (ObjectArrayList)var4.get(var10);
         ObjectListIterator var12 = var11.iterator();

         while (var12.hasNext()) {
            Element var13 = (Element)var12.next();
            String var14 = var13.getAttributeValue("randomId");
            if (var14 != null) {
               if (var5.containsKey(var10)) {
                  ObjectArrayList var15 = (ObjectArrayList)var5.get(var10);
                  ObjectListIterator var16 = var15.iterator();

                  while (var16.hasNext()) {
                     Element var17 = (Element)var16.next();
                     Element var18 = var13.clone();
                     var17 = this.specialHandlingNumberParams(var17, var18);
                     if (this.compatibleBlocks(var17, var18)) {
                        this.replaceElement(var17, var18, "same", var14);
                     }
                  }
               }

               if (var6.containsKey(var10)) {
                  if (var1 == null) {
                     var1 = StrategyBase.createXmlStrategy(var2.getStrategy(), "FixNonRandomBlocksStrategy");
                  }

                  Element var19 = this.negate(var13, var1);
                  if (var19 != null) {
                     this.fixMagicNumber(var19, var13);
                     ObjectArrayList var20 = (ObjectArrayList)var6.get(var10);
                     ObjectListIterator var22 = var20.iterator();

                     while (var22.hasNext()) {
                        Element var23 = (Element)var22.next();
                        var23 = this.specialHandlingNumberParams(var23, var19);
                        if (this.compatibleBlocks(var23, var19)) {
                           this.replaceElement(var23, var19, "negated", var14);
                        }
                     }
                  }
               }
            }
         }
      }

      return var1;
   }

   private Element specialHandlingNumberParams(Element var1, Element var2) {
      String var3 = var1.getName();
      String var4 = var2.getName();
      if (var3.equals("Item") && var4.equals("Param")) {
         String var5 = var2.getAttributeValue("key");
         Element var6 = XMLUtil.getItemParameterNoException(var1, var5);
         return var6 != null ? var6 : var1;
      } else {
         return var1;
      }
   }

   private boolean compatibleBlocks(Element var1, Element var2) {
      String var3 = var1.getName();
      String var4 = var1.getName();
      if (!var3.equals(var4)) {
         return false;
      } else {
         return var3.equals("Item") ? this.compatibleItemBlocks(var1, var2) : this.compatibleParamBlocks(var1, var2);
      }
   }

   private boolean compatibleParamBlocks(Element var1, Element var2) {
      String var3 = var1.getAttributeValue("key");
      String var4 = var2.getAttributeValue("key");
      String var5 = var1.getAttributeValue("type");
      String var6 = var2.getAttributeValue("type");
      if (var5 == null || var6 == null) {
         return false;
      } else {
         return !var3.equals(var4) ? false : var5.equals(var6);
      }
   }

   private boolean compatibleItemBlocks(Element var1, Element var2) {
      String var3 = var1.getAttributeValue("key");
      String var4 = var2.getAttributeValue("key");
      String var5 = var1.getAttributeValue("returnType");
      String var6 = var2.getAttributeValue("returnType");
      if (var5 == null || var6 == null) {
         return (var3.equals("AND") || var3.equals("OR")) && (var4.equals("AND") || var4.equals("OR"));
      } else if (var5.equals(var6)) {
         return true;
      } else {
         return !var5.equals("pricerange") || !var6.equals("price") && !var6.equals("range")
            ? var6.equals("pricerange") && (var5.equals("price") || var5.equals("range"))
            : true;
      }
   }

   private void fixMagicNumber(Element var1, Element var2) throws Exception {
      Element var3 = null;
      if (var2 != null) {
         var3 = XMLUtil.getItemParameter(var2, "#MagicNumber#", false);
      }

      Element var4 = null;
      if (var1 != null) {
         var4 = XMLUtil.getItemParameter(var1, "#MagicNumber#", false);
      }

      if (var3 != null && var4 != null) {
         List var5 = var3.getAttributes();

         for (int var6 = 0; var6 < var5.size(); var6++) {
            Attribute var7 = (Attribute)var5.get(var6);
            var4.setAttribute(var7.clone());
         }

         var4.setText(var3.getText());
      }
   }

   private void replaceElement(Element var1, Element var2, String var3, String var4) throws Exception {
      if (!var1.getName().equals(var2.getName())) {
         if (!var1.getName().equals("Param") || !var2.getName().equals("Item")) {
            return;
         }

         String var5 = var1.getAttributeValue("key");
         if (var5 == null) {
            return;
         }

         var2 = XMLUtil.getItemParameter(var2, var5, false);
         if (var2 == null) {
            return;
         }
      }

      if (var1.getName().equals("Param")) {
         String var8 = var1.getAttributeValue("key");
         if (var8 == null) {
            return;
         }

         String var6 = var2.getAttributeValue("key");
         if (var6 == null) {
            return;
         }

         if (!var6.equals(var8)) {
            return;
         }
      }

      var2 = var2.clone();
      this.fixGeneratedAttribute(var2, var3, var4, true);
      Element var9 = var1.getParentElement();
      if (var9 != null) {
         int var10 = this.findChildIndex(var9, var1);
         if (var10 >= 0) {
            var9.setContent(var10, var2);
         } else {
            throw new Exception("Cannot find child!");
         }
      }
   }

   private int findChildIndex(Element var1, Element var2) {
      List var3 = var1.getChildren();

      for (int var4 = 0; var4 < var3.size(); var4++) {
         Element var5 = (Element)var3.get(var4);
         if (var5 == var2) {
            return var4;
         }
      }

      return -1;
   }

   private void fixGeneratedAttribute(Element var1, String var2, String var3, boolean var4) {
      if (var4) {
         var1.setAttribute("generated", var2);
         var1.setAttribute("randomId", var3);
      } else {
         String var5 = var1.getAttributeValue("randomId");
         String var6 = var1.getAttributeValue("generated");
         if (var5 != null && var6 != null && var5.equals(var3)) {
            var1.setAttribute("generated", var2);
         }
      }

      List var10 = var1.getChildren();

      for (int var11 = 0; var11 < var10.size(); var11++) {
         Element var7 = (Element)var10.get(var11);
         String var8 = var7.getAttributeValue("randomId");
         String var9 = var7.getAttributeValue("generated");
         if (var8 != null && var9 != null && var8.equals(var3)) {
            var7.setAttribute("generated", var2);
         }

         this.fixGeneratedAttribute(var7, var2, var3, false);
      }
   }

   private Element negate(Element var1, StrategyBase var2) throws BlockDefinitionException {
      String var3 = var1.getAttributeValue("key");

      try {
         IBlock var4 = Blocks.getBlockObject(var3, var2, var1);
         IBlock var5 = this.negatersList.negate(var4, var2);
         return Blocks.generateBlockTreeXml(var5);
      } catch (BlockDefinitionException var7) {
         Log.debug("Cannot find block (non serious issue): " + var3);
         return null;
      }
   }

   private void findAllGeneratedElements(
      Element var1,
      Int2ObjectOpenHashMap<ObjectArrayList<Element>> var2,
      Int2ObjectOpenHashMap<ObjectArrayList<Element>> var3,
      Int2ObjectOpenHashMap<ObjectArrayList<Element>> var4
   ) throws Exception {
      List var5 = var1.getChildren();

      for (int var6 = 0; var6 < var5.size(); var6++) {
         Element var7 = (Element)var5.get(var6);
         String var8 = var7.getAttributeValue("generated");
         if (var8 == null) {
            this.findAllGeneratedElements(var7, var2, var3, var4);
         } else {
            String var9 = var7.getAttributeValue("randomId");
            if (var9 == null) {
               var9 = this.getRandomIdFromChildren(var7);
            }

            if (var9 == null) {
               this.findAllGeneratedElements(var7, var2, var3, var4);
            } else if (!this.buildType.exitSymmetry && var9.equals("RandomConditionLongExit")) {
               this.findAllGeneratedElements(var7, var2, var3, var4);
            } else if (!this.buildType.entrySymmetry && var9.equals("RandomConditionLong")) {
               this.findAllGeneratedElements(var7, var2, var3, var4);
            } else {
               int var10 = var9.hashCode();
               if (var8.equals("random")) {
                  ObjectArrayList var11 = (ObjectArrayList)var2.get(var10);
                  if (var11 == null) {
                     var11 = new ObjectArrayList();
                  }

                  var11.add(var7);
                  var2.put(var10, var11);
               } else if (var8.equals("same")) {
                  ObjectArrayList var12 = (ObjectArrayList)var3.get(var10);
                  if (var12 == null) {
                     var12 = new ObjectArrayList();
                  }

                  var12.add(var7);
                  var3.put(var10, var12);
               } else {
                  if (!var8.startsWith("negated")) {
                     throw new Exception(String.format("Unknown generated attribute '%s'", var8));
                  }

                  ObjectArrayList var13 = (ObjectArrayList)var4.get(var10);
                  if (var13 == null) {
                     var13 = new ObjectArrayList();
                  }

                  var13.add(var7);
                  var4.put(var10, var13);
               }

               this.findAllGeneratedElements(var7, var2, var3, var4);
            }
         }
      }
   }

   private String getRandomIdFromChildren(Element var1) {
      Element var2 = XMLUtil.getItemParameterNoException(var1, "#Identification#");
      return var2 != null ? var2.getText() : null;
   }

   private void fixFuzzyConditions(StrategyBase var1, Node var2, String var3) throws Exception {
      Element var4 = var2.getStrategy();
      Int2ObjectOpenHashMap var5 = this.findSignals(var4);
      if (var5 != null) {
         var1 = this.fixLongShortConditions(var1, var4, var5, "33333333-1111-1111-3333-333333333333", "33333333-2222-1111-3333-333333333333");
         var1 = this.fixLongShortConditions(var1, var4, var5, "33333333-1111-2222-3333-333333333333", "33333333-1111-2222-3333-333333333333");
      }
   }

   private StrategyBase fixLongShortConditions(StrategyBase var1, Element var2, Int2ObjectOpenHashMap<Element> var3, String var4, String var5) throws Exception {
      if (var3.containsKey(var4.hashCode()) && var3.containsKey(var5.hashCode())) {
         Element var6 = (Element)var3.get(var4.hashCode());
         Element var7 = (Element)var3.get(var5.hashCode());
         List var8 = var6.getChildren();
         List var9 = var7.getChildren();
         if (var8.size() != var9.size()) {
            return var1;
         }

         for (int var10 = 0; var10 < var8.size(); var10++) {
            Element var11 = (Element)var8.get(var10);
            Element var12 = (Element)var9.get(var10);
            String var13 = var11.getAttributeValue("randomId");
            if (var13 != null) {
               if (var1 == null) {
                  var1 = StrategyBase.createXmlStrategy(var2, "FixNonRandomBlocksStrategy");
               }

               Element var14 = this.negate(var11, var1);
               this.fixMagicNumber(var14, var11);
               this.replaceElement(var12, var14, "negated", var13);
            }
         }

         return var1;
      } else {
         return var1;
      }
   }

   private Int2ObjectOpenHashMap<Element> findSignals(Element var1) {
      Int2ObjectOpenHashMap var2 = new Int2ObjectOpenHashMap();
      List var3 = var1.getChild("Strategy").getChild("Rules").getChild("Events").getChildren("Event");

      for (int var4 = 0; var4 < var3.size(); var4++) {
         Element var5 = (Element)var3.get(var4);
         List var6 = var5.getChildren("Rule");

         for (int var7 = 0; var7 < var6.size(); var7++) {
            Element var8 = (Element)var6.get(var7);
            String var9 = var8.getAttributeValue("type");
            if (var9 != null && var9.equals("SignalFuzzy")) {
               List var10 = var8.getChild("signals").getChildren("signal");

               for (int var11 = 0; var11 < var10.size(); var11++) {
                  Element var12 = (Element)var10.get(var11);
                  String var13 = var12.getAttributeValue("variable");
                  if (var13 != null) {
                     int var14 = var13.hashCode();
                     var2.put(var14, var12);
                  }
               }
            }
         }
      }

      return var2.size() == 0 ? null : var2;
   }

   private List<Element> getFuzzyConditions(Element var1, String var2) {
      return null;
   }
}
