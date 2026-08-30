package com.strategyquant.tradinglib;

import com.strategyquant.datalib.DataSeries;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.snippets.CustomClassesLoader;
import com.strategyquant.lib.snippets.CustomClassesReg;
import com.strategyquant.lib.snippets.ICustomClasses;
import com.strategyquant.lib.snippets.SnippetsCompiler;
import com.strategyquant.tradinglib.blocks.AnnotationProcessor;
import com.strategyquant.tradinglib.blocks.BlockCategoryInfo;
import com.strategyquant.tradinglib.blocks.BlockComparatorByOrder;
import com.strategyquant.tradinglib.blocks.CategoryComparatorByOrder;
import com.strategyquant.tradinglib.blocks.CustomBlocks;
import com.strategyquant.tradinglib.blocks.OppositeBlocksConfig;
import com.strategyquant.tradinglib.blocks.OutputAnnotationHandler;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;
import com.strategyquant.tradinglib.exit.ExitMethodsList;
import com.strategyquant.tradinglib.talib.TALibIndicator;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.TreeMap;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Blocks implements ICustomClasses {
   public static final Logger Log = LoggerFactory.getLogger("Blocks");
   private static Blocks instance;
   private Int2ObjectOpenHashMap<IBlock> mapClasses = new Int2ObjectOpenHashMap();
   private HashMap<String, ArrayList<BlockCategoryInfo>> sortedCategories = new HashMap<>();
   private Int2ObjectOpenHashMap<ArrayList<IBlock>> sortedBlocks = new Int2ObjectOpenHashMap();
   private HashMap<String, String> oppositeBlocksCache = null;
   private Comparator<IBlock> comparatorByOrder;
   private Comparator<BlockCategoryInfo> categoryComparatorByOrder;

   private Blocks() {
      CustomClassesReg.add(this);
      this.comparatorByOrder = new BlockComparatorByOrder();
      this.categoryComparatorByOrder = new CategoryComparatorByOrder();
      this.load();
   }

   private void load() {
      this.mapClasses.clear();
      this.sortedCategories.clear();
      this.sortedBlocks.clear();
      this.initClassesMap("Blocks");
      this.initCustomBlocks();

      for (ArrayList var2 : this.sortedCategories.values()) {
         Collections.sort(var2, this.categoryComparatorByOrder);
      }

      OppositeBlocksConfig var3 = new OppositeBlocksConfig();
      this.oppositeBlocksCache = var3.load(MainApp.getDataPath() + "user/settings/OppositeBlocks.csv", this.mapClasses);
   }

   public static void init() throws Exception {
      if (instance != null) {
         throw new Exception("Blocks.init() called more than once!");
      }

      instance = new Blocks();
   }

   private void initClassesMap(String var1) {
      try {
         CustomClassesLoader var2 = new CustomClassesLoader(var1);

         while (var2.hasNext()) {
            String var3 = var2.getNext();
            Object var4 = var2.createInstance(var3);
            if (var4 == null) {
               Log.debug("Class " + var3 + " is abstract, skipping.");
            } else if (var4 instanceof IBlock) {
               IBlock var5 = (IBlock)var4;
               this.mapClasses.put(var5.getClass().getSimpleName().hashCode(), var5);
               this.putClassToSortedMap(var5);
            } else if (Log.isDebugEnabled()) {
               Log.debug("Class: " + var3 + " is not an instance of block");
            }
         }
      } catch (Exception var6) {
         Log.error("An error occured while loading custom classes.", var6);
      }
   }

   private void initCustomBlocks() {
      try {
         CustomClassesLoader var1 = new CustomClassesLoader("Internal");

         while (var1.hasNext()) {
            String var2 = var1.getNext();
            if (var2.contains("CDataIndy") || var2.contains("CBlock")) {
               Object var3 = var1.createInstance(var2);
               if (var3 instanceof IBlock) {
                  IBlock var4 = (IBlock)var3;
                  this.mapClasses.put(var4.getClass().getSimpleName().hashCode(), var4);
               } else if (Log.isDebugEnabled()) {
                  Log.debug("Class: " + var2 + " is not an instance of block");
               }
            }
         }
      } catch (Exception var5) {
         Log.error("An error occured while loading custom classes.", var5);
      }
   }

   private void putClassToSortedMap(IBlock var1) throws BlockDefinitionException {
      String var2 = this.getBlockTypeAsString(var1);
      int var3 = 100;
      int var4 = 100;
      Class var5 = var1.getClass();
      BuildingBlock var6 = var5.getAnnotation(BuildingBlock.class);
      String var7 = this.getCategoryName(var5);
      SortOrder var8 = var5.getAnnotation(SortOrder.class);
      if (var8 != null) {
         var3 = var8.value();
      } else {
         byte var16 = 100;
      }

      CategoryOrder var9 = var5.getAnnotation(CategoryOrder.class);
      if (var9 != null) {
         var4 = var9.value();
      } else {
         var4 = 100;
      }

      if (!this.sortedCategories.containsKey(var2)) {
         this.sortedCategories.put(var2, new ArrayList<>());
      }

      BlockCategoryInfo var10 = this.findOrCreateCategory(this.sortedCategories.get(var2), var7, var4);
      if (var10.categoryOrder < var4) {
         var10.categoryOrder = var4;
      }

      int var11 = var2.hashCode() + var7.hashCode();
      if (!this.sortedBlocks.containsKey(var11)) {
         this.sortedBlocks.put(var11, new ArrayList());
      }

      ArrayList var12 = (ArrayList)this.sortedBlocks.get(var11);
      var12.add(var1);
      IntIterator var13 = this.sortedBlocks.keySet().iterator();

      while (var13.hasNext()) {
         int var14 = (Integer)var13.next();
         var12 = (ArrayList)this.sortedBlocks.get(var14);
         Collections.sort(var12, this.comparatorByOrder);
      }
   }

   private String getCategoryName(Class<?> var1) {
      String var2 = var1.getCanonicalName();
      int var3 = var2.lastIndexOf(".");
      var2 = var2.substring(0, var3);
      var3 = var2.lastIndexOf(".");
      return var2.substring(var3 + 1);
   }

   private BlockCategoryInfo findOrCreateCategory(ArrayList<BlockCategoryInfo> var1, String var2, int var3) {
      int var4 = var2.hashCode();

      for (BlockCategoryInfo var6 : var1) {
         if (var6.categoryNameHash == var4) {
            return var6;
         }
      }

      BlockCategoryInfo var7 = new BlockCategoryInfo(var2, var3);
      var1.add(var7);
      return var7;
   }

   public static IBlock getBlockObject(String var0, StrategyBase var1, Element var2) throws BlockDefinitionException {
      IBlock var3 = null;
      if (var0.startsWith("talib_")) {
         var3 = new TALibIndicator();
      } else {
         var3 = get(var0);
      }

      return var3.newInstance(var1, var2);
   }

   public static IBlock get(String var0) throws BlockDefinitionException {
      return get(var0, null);
   }

   public static boolean exists(String var0) throws BlockDefinitionException {
      if (instance == null) {
         throw new BlockDefinitionException("You have to call Blocks.init() on program startup!");
      } else {
         return instance.mapClasses.containsKey(var0.hashCode());
      }
   }

   private static IBlock get(String var0, StrategyBase var1) throws BlockDefinitionException {
      if (instance == null) {
         throw new BlockDefinitionException("You have to call Blocks.init() on program startup!");
      } else if (var0.startsWith("CDataIndy")) {
         return instance._get("CDataIndy", var1);
      } else {
         return var0.startsWith("CBlock") ? instance._get("CBlock", var1) : instance._get(var0, var1);
      }
   }

   public static Element generateBlockTreeXml(IBlock var0) throws BlockDefinitionException {
      if (instance == null) {
         throw new BlockDefinitionException("You have to call Blocks.init() on program startup!");
      } else {
         return instance._generateBlockTreeXml(var0);
      }
   }

   private IBlock _get(String var1, StrategyBase var2) throws BlockDefinitionException {
      int var3 = var1.hashCode();
      if (!this.mapClasses.containsKey(var3)) {
         throw new BlockDefinitionException("Cannot find block '" + var1 + "'");
      }

      try {
         return ((IBlock)this.mapClasses.get(var3)).clone(false, var2);
      } catch (BlockDefinitionException var5) {
         Log.error("Error during cloning block", var5);
         var5.printStackTrace();
         return null;
      }
   }

   public static void generateBlocksXml(Element var0) throws BlockDefinitionException {
      if (instance == null) {
         throw new BlockDefinitionException("You have to call Blocks.init() on program startup!");
      }

      instance._generateBlocksXml(var0);
   }

   private void _generateBlocksXml(Element var1) throws BlockDefinitionException {
      Element var2 = var1.getChild("Blocks");
      Element var3 = var1.getChild("ParameterSets");

      for (String var5 : this.sortedCategories.keySet()) {
         for (BlockCategoryInfo var8 : this.sortedCategories.get(var5)) {
            int var9 = var5.hashCode() + var8.categoryNameHash;

            for (IBlock var12 : (ArrayList)this.sortedBlocks.get(var9)) {
               Element var13 = this.getBlockTypeElement(var12, var2);
               Element var14 = this.createBlock(var12, var13, var3);
               if (var14 != null) {
                  String var15 = this.getCategoryName(var12, var14);
                  Element var16 = this.getOrCreate(var13, var15, "Category");
                  var16.addContent(var14);
               }
            }
         }
      }
   }

   private Element createBlock(IBlock var1, Element var2, Element var3) throws BlockDefinitionException {
      Class var4 = var1.getClass();
      if (this.isIgnoredBlock(var4)) {
         return null;
      }

      Element var5 = this.createBlockNode(var4, "Item");
      String var6 = this.addReturnTypeAttribute(var5, var2, var4);
      this.addCategoryTypeAttributes(var5, var4, var6);
      this.addNotFirstValueAttribute(var5, var4);
      this.addBlockParameters(var5, var1, var4, false);
      this.addBlockParameterSets(var3, var1, var4);
      return var5;
   }

   private boolean isIgnoredBlock(Class<?> var1) {
      String[] var2 = new String[]{".AND", ".OR", "Values.Price.VolumeDaily"};
      String var3 = var1.getCanonicalName();

      for (String var7 : var2) {
         if (var3.contains(var7)) {
            return true;
         }
      }

      return false;
   }

   private Element _generateBlockTreeXml(IBlock var1) throws BlockDefinitionException {
      Class var2 = var1.getClass();
      if (var2.getSimpleName().equals("CDataIndy")) {
         return var1.getCustomBlockXml(0);
      } else if (var2.getSimpleName().equals("CBlock")) {
         return var1.getCustomBlockXml(1);
      } else if (var1 instanceof TALibIndicator) {
         TALibIndicator var4 = (TALibIndicator)var1;
         return var4.getBlockXml();
      } else {
         Element var3 = this.createBlockNode(var2, "Item");
         this.addBlockParameters(var3, var1, var2, true);
         return var3;
      }
   }

   private void addBlockParameters(Element var1, IBlock var2, Class<?> var3, boolean var4) throws BlockDefinitionException {
      for (Field var8 : var3.getFields()) {
         AnnotationProcessor.wizardGenerateXml(var8, var1, var2, var3, var4);
      }

      OutputAnnotationHandler.wizardGenerateXml(var3, var1, var2, var3, var4);
   }

   private void addBlockParameterSets(Element var1, IBlock var2, Class<?> var3) {
      ParameterSet[] var4 = null;
      ParameterSets var5 = var3.getAnnotation(ParameterSets.class);
      if (var5 != null) {
         var4 = var5.value();
      } else {
         ParameterSet var6 = var3.getAnnotation(ParameterSet.class);
         if (var6 != null) {
            var4 = new ParameterSet[]{var6};
         }
      }

      if (var4 != null) {
         Element var21 = new Element("Item");
         var21.setAttribute("key", var3.getSimpleName());

         for (int var7 = 0; var7 < var4.length; var7++) {
            ParameterSet var8 = var4[var7];
            String var9 = var8.set();
            if (var9 != null && !var9.isEmpty()) {
               String[] var10 = var9.split("\\,");
               String var11 = "";
               Element var12 = new Element("Set");
               var12.setAttribute("weight", "" + var8.weight());
               String var13 = null;

               for (Field var17 : var3.getFields()) {
                  if (var17.getAnnotation(Parameter.class) != null) {
                     String var18 = "*";

                     for (int var19 = 0; var19 < var10.length; var19++) {
                        String[] var20 = var10[var19].split("=");
                        if (var20.length != 2) {
                           Log.error("Incorrect parameter set definition in block '" + var3.getSimpleName() + "': Param values '" + var9 + "'");
                        } else {
                           if (var20[0].equals("ComputedFrom")) {
                              var13 = var20[1];
                           }

                           if (var20[0].equals(var17.getName())) {
                              var18 = var20[1];
                              break;
                           }
                        }
                     }

                     if (var17.getType().isAssignableFrom(DataSeries.class)) {
                        var11 = var11 + var18 + ",";
                        if (var13 == null) {
                           var11 = var11 + "*,";
                        } else {
                           var11 = var11 + var13 + ",";
                        }
                     } else {
                        var11 = var11 + var18 + ",";
                     }
                  }
               }

               if (var11.length() > 0) {
                  var11 = var11.substring(0, var11.length() - 1);
                  var12.setAttribute("values", var11);
                  var21.addContent(var12);
               }
            }
         }

         var1.addContent(var21);
      }
   }

   private String addReturnTypeAttribute(Element var1, Element var2, Class<?> var3) throws BlockDefinitionException {
      String var4 = "none";
      BuildingBlock var5 = var3.getAnnotation(BuildingBlock.class);
      if (var2.getName().equals("Values")) {
         var1.setAttribute("returnType", "number");
         if (var5 != null) {
            var4 = ReturnTypes.translateReturnType(var5.returnType());
         }
      }

      if (var2.getName().equals("Comparisons")) {
         var4 = "boolean";
      }

      if (var2.getName().equals("Conditions")) {
         var4 = "boolean";
      }

      if (var2.getName().equals("Actions")) {
         if (var5 != null && var5.returnType() == 8) {
            var4 = "order";
         } else {
            var4 = "none";
         }
      }

      var1.setAttribute("returnType", var4);
      return var4;
   }

   private void addCategoryTypeAttributes(Element var1, Class<?> var2, String var3) {
      if (this.isIndicator(var2)) {
         if (var3.equals("pricerange")) {
            var1.setAttribute("categoryType", "priceRange");
         } else {
            var1.setAttribute("categoryType", "indicator");
         }

         Indicator var4 = var2.getAnnotation(Indicator.class);
         if (var4 != null) {
            if (var4.oscillator()) {
               var1.setAttribute("isOscillator", "true");
               var1.setAttribute("middleValue", SQUtils.doubleToString(var4.middleValue()));
            }

            var1.setAttribute("indicatorMin", SQUtils.doubleToString(var4.min()));
            var1.setAttribute("indicatorMax", SQUtils.doubleToString(var4.max()));
            var1.setAttribute("indicatorStep", SQUtils.doubleToString(var4.step()));
         }
      } else if (this.isPriceValue(var2, var3)) {
         var1.setAttribute("categoryType", "priceValue");
      } else if (this.isOperator(var2)) {
         var1.setAttribute("categoryType", "operators");
      } else if (this.isSimpleRule(var2)) {
         var1.setAttribute("categoryType", "simpleRules");
      } else {
         var1.setAttribute("categoryType", "other");
      }
   }

   private boolean isSimpleRule(Class<?> var1) {
      return checkContains(var1, "ConditionBlock");
   }

   private boolean isOperator(Class<?> var1) {
      return checkContains(var1, "ComparisonBlock");
   }

   public static boolean checkContains(Class<?> var0, String var1) {
      for (int var2 = 0; var2 < 6; var2++) {
         if (var0.getName().contains(var1)) {
            return true;
         }

         var0 = var0.getSuperclass();
         if (var0 == null) {
            return false;
         }
      }

      return false;
   }

   private boolean isPriceValue(Class<?> var1, String var2) {
      return checkContains(var1, "SQ.Blocks.Price") && var2.equals("price");
   }

   private boolean isIndicator(Class<?> var1) {
      boolean var2 = checkContains(var1, "IndicatorBlock");
      if (var2) {
         return true;
      }

      Indicator var3 = var1.getAnnotation(Indicator.class);
      return var3 != null;
   }

   private void addNotFirstValueAttribute(Element var1, Class<?> var2) {
      NotFirstValue var3 = var2.getAnnotation(NotFirstValue.class);
      if (var3 != null) {
         var1.setAttribute("notFirstValue", "true");
      }
   }

   private Element createBlockNode(Class<?> var1, String var2) {
      Element var3 = new Element(var2);
      String var4 = SQUtils.insertUppercaseSpaces(var1.getSimpleName());
      String var5 = null;
      int var6 = 0;
      String var7 = null;
      Object var8 = null;
      BuildingBlock var9 = var1.getAnnotation(BuildingBlock.class);
      if (var9 != null) {
         if (!var9.name().equals("Null")) {
            var4 = var9.name();
         }

         if (!var9.display().equals("Null")) {
            var5 = var9.display();
         }

         var6 = var9.returnType();
      }

      Help var10 = var1.getAnnotation(Help.class);
      if (var10 != null) {
         var7 = var10.value();
      }

      var3.setAttribute("customSnippet", "" + !SnippetsCompiler.getInstance().isInternalSnippet(var1.getName()));
      var3.setAttribute("key", var1.getSimpleName());
      var3.setAttribute("name", var4);
      if (var5 != null) {
         var3.setAttribute("display", var5);
      }

      if (var6 != 0) {
         try {
            var3.setAttribute("returnType", ReturnTypes.translateReturnType(var6));
         } catch (BlockDefinitionException var14) {
            Log.info("Unknown return type for block {}", var2);
         }
      }

      if (var7 != null) {
         var3.setAttribute("help", var7);
      }

      IgnoreInBuilder var11 = var1.getAnnotation(IgnoreInBuilder.class);
      if (var11 != null) {
         var3.setAttribute("ignoreInBuilder", "true");
      }

      if (var9 != null) {
         String var12 = var9.mainIndicator();
         if (var12.equals("Null")) {
            String[] var13 = var1.getPackage().toString().split("\\.");
            if (var13 != null && var13.length > 1) {
               var12 = var13[var13.length - 1];
            } else {
               var12 = null;
            }
         }

         if (var12 != null) {
            var3.setAttribute("mI", var12);
         }
      }

      ForEngine var15 = var1.getAnnotation(ForEngine.class);
      if (var15 != null && !var15.value().equals("Null")) {
         String var16 = var15.value();
         var3.setAttribute("forEngine", var16);
      }

      MT5ExtendedTemplate var17 = var1.getAnnotation(MT5ExtendedTemplate.class);
      if (var17 != null) {
         var3.setAttribute("mt5ExtendedTemplate", "true");
      }

      return var3;
   }

   private Element getBlockTypeElement(IBlock var1, Element var2) throws BlockDefinitionException {
      String var3 = this.getBlockTypeAsString(var1);
      return this.getOrCreate(var2, var3, var3);
   }

   public static int getBlockType(IBlock var0) throws Exception {
      if (instance == null) {
         throw new Exception("You have to call Blocks.init() on program startup!");
      } else {
         return instance._getBlockType(var0);
      }
   }

   private int _getBlockType(IBlock var1) throws BlockDefinitionException {
      String var2 = SQUtils.getClasses(var1.getClass());
      if (var2.contains("CBlock")) {
         return this.getBlockTypeFromXml(var1);
      } else if (var2.contains("TALibIndicator.Object")) {
         return 4;
      } else if (var2.contains(".IndicatorBlock")) {
         return 4;
      } else if (var2.contains(".ValueBlock")) {
         return 4;
      } else if (var2.contains(".ComparisonBlock")) {
         return 2;
      } else if (var2.contains(".ConditionBlock")) {
         return 3;
      } else if (var2.contains(".ActionBlock")) {
         return 5;
      } else if (var2.contains(".FormulaBlock")) {
         return 6;
      } else if (var2.contains(".ExitMethod")) {
         return 7;
      } else {
         throw new BlockDefinitionException("Block " + var1.toString() + " has unknown instance type!");
      }
   }

   private int getBlockTypeFromXml(IBlock var1) throws BlockDefinitionException {
      try {
         Element var2 = var1.getCustomBlockXml(3);
         if (var2 == null) {
            throw new BlockDefinitionException("Custom Block " + var1.toString() + " has null XML!");
         }

         String var3 = var2.getAttributeValue("type");
         switch (var3) {
            case "Condition":
               return 3;
            case "Action":
               return 5;
            case "Price level":
               return 4;
            case "Value":
               return 4;
            default:
               throw new BlockDefinitionException("Custom Block " + var1.toString() + " has unknown instance type (1)!");
         }
      } catch (BlockDefinitionException var6) {
         throw new BlockDefinitionException("Custom Block " + var1.toString() + " has unknown instance type (2)!", var6);
      }
   }

   public static int getReturnType(IBlock var0) throws Exception {
      if (instance == null) {
         throw new Exception("You have to call Blocks.init() on program startup!");
      } else {
         return instance._getReturnType(var0);
      }
   }

   private int _getReturnType(IBlock var1) {
      BuildingBlock var2 = var1.getClass().getAnnotation(BuildingBlock.class);
      return var2 != null ? var2.returnType() : 0;
   }

   private String getBlockTypeAsString(IBlock var1) throws BlockDefinitionException {
      int var2 = this._getBlockType(var1);
      switch (var2) {
         case 2:
            return "Comparisons";
         case 3:
            return "Conditions";
         case 4:
            return "Values";
         case 5:
            return "Actions";
         default:
            throw new BlockDefinitionException("Block " + var1.toString() + " has unknown instance type!");
      }
   }

   private String getCategoryName(IBlock var1, Element var2) throws BlockDefinitionException {
      String var3 = var2.getAttributeValue("category");
      if (var3 == null) {
         var3 = this.recognizeFromPackagePath(var1);
      } else {
         var2.removeAttribute("category");
      }

      return var3;
   }

   private String recognizeFromPackagePath(IBlock var1) throws BlockDefinitionException {
      String var2 = var1.toString();
      String[] var4 = var2.split("\\.");
      if (var4.length < 3) {
         return null;
      }

      Class var5 = var1.getClass();
      String var3;
      if (checkContains(var5, "IndicatorBlock")) {
         if (var4.length >= 5) {
            var3 = var4[var4.length - 3];
         } else {
            var3 = var4[var4.length - 2];
         }
      } else {
         var3 = var4[var4.length - 2];
      }

      return var3;
   }

   private Element getOrCreate(Element var1, String var2, String var3) {
      String var4 = var2.replace(" ", "");

      for (Element var6 : var1.getChildren(var3)) {
         if (var6.getAttributeValue("key") != null && var6.getAttributeValue("key").equals(var4)) {
            return var6;
         }
      }

      Element var7 = new Element(var3);
      var7.setAttribute("name", SQUtils.insertUppercaseSpaces(var2));
      var7.setAttribute("key", var4);
      var1.addContent(var7);
      return var7;
   }

   public static IBlock[] getAllAvailableBlocks() throws Exception {
      if (instance == null) {
         throw new Exception("You have to call Blocks.init() on program startup!");
      } else {
         return instance._getAllAvailableBlocks();
      }
   }

   private IBlock[] _getAllAvailableBlocks() {
      IBlock[] var1 = new IBlock[this.mapClasses.size()];
      int var2 = 0;
      ObjectIterator var3 = this.mapClasses.values().iterator();

      while (var3.hasNext()) {
         Object var4 = var3.next();
         var1[var2++] = (IBlock)var4;
         if (var2 == var1.length) {
            break;
         }
      }

      return var1;
   }

   public static void generateExitRulesXml(Element var0) throws BlockDefinitionException {
      if (instance == null) {
         throw new BlockDefinitionException("You have to call Blocks.init() on program startup!");
      }

      instance._generateExitRulesXml(var0);
   }

   private void _generateExitRulesXml(Element var1) throws BlockDefinitionException {
      if (var1 != null) {
         for (ExitMethod var3 : ExitMethodsList.get().getAvailableClasses()) {
            var3.createExitMethodXml(var1);
         }
      }
   }

   public void reload() {
      this.load();
   }

   public static TreeMap<String, String> generateOppositeBlocksMap() {
      return instance._generateOppositeBlocksMap();
   }

   private TreeMap<String, String> _generateOppositeBlocksMap() {
      TreeMap var1 = new TreeMap();

      for (String var3 : this.sortedCategories.keySet()) {
         for (BlockCategoryInfo var6 : this.sortedCategories.get(var3)) {
            int var7 = var3.hashCode() + var6.categoryNameHash;

            for (IBlock var10 : (ArrayList)this.sortedBlocks.get(var7)) {
               Class var11 = var10.getClass();
               String var12 = var11.getSimpleName();
               OppositeBlock var14 = var11.getAnnotation(OppositeBlock.class);
               String var13;
               if (var14 != null && !var14.value().equals("Null")) {
                  var13 = var14.value();
               } else {
                  var13 = var12;
               }

               var1.put(var12, var13);
            }
         }
      }

      return var1;
   }

   public static IBlock getOppositeBlock(IBlock var0) throws BlockDefinitionException {
      return instance._getOppositeBlock(var0);
   }

   private IBlock _getOppositeBlock(IBlock var1) throws BlockDefinitionException {
      String var2 = var1.getClass().getSimpleName();
      if (var2.equals("TALibIndicator")) {
         return this._getTALiBOppositeBlock(var1);
      }

      String var3 = null;
      if (this.oppositeBlocksCache.containsKey(var2)) {
         var3 = this.oppositeBlocksCache.get(var2);

         try {
            return this._getOppositeBlockObject(var1, var3);
         } catch (Exception var8) {
            Log.error("Cannot get block opposite to '" + var2 + "' from cache, opposite block '" + var3 + "' cannot be created!", var8);
         }
      }

      OppositeBlock var4 = var1.getClass().getAnnotation(OppositeBlock.class);
      if (var4 != null) {
         var3 = var4.value();

         try {
            return this._getOppositeBlockObject(var1, var3);
         } catch (Exception var7) {
            Log.error("Cannot get block opposite to '" + var2 + "' from snippet annotation, opposite block '" + var3 + "' cannot be created!", var7);
         }
      }

      var3 = var2;

      try {
         return this._getOppositeBlockObject(var1, var3);
      } catch (Exception var6) {
         Log.error("Cannot get block opposite to '" + var2 + "' from same block, opposite block '" + var3 + "' cannot be created!", var6);
         throw new BlockDefinitionException("Cannot create block opposite to '" + var2 + "'");
      }
   }

   public static IBlock getSameBlock(IBlock var0) throws BlockDefinitionException {
      return instance._getSameBlock(var0);
   }

   private IBlock _getSameBlock(IBlock var1) throws BlockDefinitionException {
      String var2 = var1.getClass().getSimpleName();
      if (var2.equals("TALibIndicator")) {
         return this._getTALiBOppositeBlock(var1);
      }

      String var3 = var2;

      try {
         return this._getOppositeBlockObject(var1, var3);
      } catch (Exception var5) {
         Log.error("Cannot get block opposite to '" + var2 + "' from same block, opposite block '" + var3 + "' cannot be created!", var5);
         throw new BlockDefinitionException("Cannot create block opposite to '" + var2 + "'");
      }
   }

   private IBlock _getTALiBOppositeBlock(IBlock var1) {
      TALibIndicator var2 = new TALibIndicator();
      var2.copyFrom((TALibIndicator)var1);
      return var2;
   }

   private IBlock _getOppositeBlockObject(IBlock var1, String var2) throws BlockDefinitionException {
      IBlock var3;
      if (var1 instanceof ExitMethod) {
         var3 = ExitMethodsList.getExitMethodObject(var2, null, null);
      } else {
         var3 = get(var2, var1.getStrategy());
         var3.copyCustomData(var1);
      }

      return var3;
   }

   public static String getOppositeBlockKey(String var0) {
      return instance._getOppositeBlockKey(var0);
   }

   private String _getOppositeBlockKey(String var1) {
      if (this.oppositeBlocksCache.containsKey(var1)) {
         return this.oppositeBlocksCache.get(var1);
      }

      Element var2 = CustomBlocks.getElement(var1);
      if (var2 != null) {
         String var3 = var2.getAttributeValue("oppositeBlockKey");
         if (var3 != null) {
            return var3;
         }
      }

      return var1;
   }

   public static void sortByOrder(ExitMethod[] var0) {
      instance._sortByOrder(var0);
   }

   private void _sortByOrder(ExitMethod[] var1) {
      Arrays.sort(var1, this.comparatorByOrder);
   }
}
