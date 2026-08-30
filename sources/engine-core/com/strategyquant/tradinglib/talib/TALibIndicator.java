package com.strategyquant.tradinglib.talib;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.ReturnTypes;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.Variable;
import com.strategyquant.tradinglib.engine.stockpicker.backtester.BacktestData;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.List;
import org.jdom2.Element;

public class TALibIndicator implements IBlock {
   public StrategyBase Strategy = null;
   private Int2IntOpenHashMap customData = null;
   private int returnType = 0;
   private String name;
   private SettingsMap params;
   private String[] paramsKeys;
   private IntSet periodParams;
   private int chartIndex;
   boolean paramsPrepared = false;
   private int paramShift = -1;
   private int paramLine = 0;
   private String paramLineName = null;
   private int paramLongestPeriod = -1;
   private int paramChartIndex = -1;
   private int paramComputedFrom = -1;

   @Override
   public double evaluateBlock() throws TradingException {
      if (this.params == null) {
         throw new TradingException("First parseXml() must be called.");
      }

      if (!this.paramsPrepared) {
         this.prepareParams();
      }

      BacktestData var1 = this.Strategy.Stockpicker.data;
      String var2 = var1.getSymbol(this.chartIndex);
      String var3 = var1.getChartTimeframe(this.paramChartIndex);
      double var4 = 0.0;
      int var7 = this.getCurrentIndex();
      if (this.paramLongestPeriod > 0) {
         int var6 = var7 - this.paramLongestPeriod;
         int var8 = this.Strategy.Stockpicker.data.getChartSymbolData(this.paramChartIndex, var2).getRealDataIndexStart(var3);
         if (var6 < var8) {
            throw new TradingException(String.format("Not enough data to calculate %s indicator value.", this.name), 55);
         }
      }

      int var11 = this.paramShift;
      if (this.Strategy.Stockpicker.strategyTriggeredAt() == 5
         && var11 == 0
         && (TALibIndicators.isHlcIndicator(this.name) || this.paramComputedFrom != -1 && this.paramComputedFrom != 1)) {
         var11 = 1;
         this.Strategy.setStrategyProblem(2048);
      }

      float[][] var9 = this.Strategy
         .Stockpicker
         .TALibIndicators
         .calculate(var2, this.name, this.params, this.Strategy, this.paramChartIndex, var11, this.paramComputedFrom, this.paramLine, this.paramLineName);
      if (var9 != null) {
         int var10 = var7 - var11;
         var4 = var9[this.paramLine][var10];
      }

      if (this.returnType == 3) {
         return var4 == 0.0 ? 0.0 : 1.0;
      } else {
         return var4;
      }
   }

   @Override
   public double evaluateBlock(int var1) throws TradingException {
      if (this.params == null) {
         throw new TradingException("First parseXml() must be called.");
      }

      if (!this.paramsPrepared) {
         this.prepareParams();
      }

      BacktestData var2 = this.Strategy.Stockpicker.data;
      String var3 = var2.getSymbol(this.chartIndex);
      String var4 = var2.getChartTimeframe(this.paramChartIndex);
      double var5 = 0.0;
      int var8 = this.getCurrentIndex();
      if (this.paramLongestPeriod > 0) {
         int var7 = var8 - var1 - this.paramLongestPeriod;
         int var9 = this.Strategy.Stockpicker.data.getChartSymbolData(this.paramChartIndex, var3).getRealDataIndexStart(var4);
         if (var7 < var9) {
            throw new TradingException(String.format("Not enough data to calculate %s indicator value.", this.name), 55);
         }
      }

      int var12 = this.paramShift;
      if (this.Strategy.Stockpicker.strategyTriggeredAt() == 5
         && var12 == 0
         && (TALibIndicators.isHlcIndicator(this.name) || this.paramComputedFrom != -1 && this.paramComputedFrom != 1)) {
         var12 = 1;
         this.Strategy.setStrategyProblem(2048);
      }

      float[][] var10 = this.Strategy
         .Stockpicker
         .TALibIndicators
         .calculate(var3, this.name, this.params, this.Strategy, this.paramChartIndex, var12, this.paramComputedFrom, this.paramLine, this.paramLineName);
      if (var10 != null) {
         int var11 = var8 - var1 - var12;
         var5 = var10[this.paramLine][var11];
      }

      if (this.returnType == 3) {
         return var5 == 0.0 ? 0.0 : 1.0;
      } else {
         return var5;
      }
   }

   private void prepareParams() throws TradingException {
      this.paramLine = this.params.getInt("Line");
      this.paramLineName = this.params.getString("LineName", "Value");
      this.paramShift = this.params.getInt("Shift");
      this.paramChartIndex = this.params.getInt("Chart");
      this.paramComputedFrom = this.params.getInt("ComputedFrom", -1);
      this.paramLongestPeriod = -1;

      for (int var1 = 0; var1 < this.paramsKeys.length; var1++) {
         String var2 = this.paramsKeys[var1];
         if (this.periodParams.contains(var2.hashCode())) {
            int var3 = this.params.getInt(var2);
            if (var3 > this.paramLongestPeriod) {
               this.paramLongestPeriod = var3;
            }
         }
      }

      this.paramsPrepared = true;
   }

   private int getCurrentIndex() {
      return this.Strategy.Stockpicker.getCurrentBar(this.chartIndex);
   }

   @Override
   public IBlock newInstance(StrategyBase var1, Element var2) throws BlockDefinitionException {
      IBlock var3 = this.clone(true, var1);
      this.parseXml(var2);
      return var3;
   }

   private void parseXml(Element var1) throws BlockDefinitionException {
      if (!var1.getName().equals("Item") && !var1.getName().equals("Formula")) {
         throw new BlockDefinitionException("@Parameter must be used only on Item or Formula node!");
      }

      this.params = new SettingsMap();
      this.periodParams = new IntArraySet();
      this.name = var1.getAttributeValue("key");
      this.name = this.name.replaceFirst("talib_", "");
      String var2 = var1.getAttributeValue("returnType");
      this.returnType = ReturnTypes.translate(var2);
      List var3 = var1.getChildren();

      for (int var4 = 0; var4 < var3.size(); var4++) {
         Element var5 = (Element)var3.get(var4);
         String var6 = var5.getAttributeValue("type");
         String var7 = var5.getValue();
         String var8 = var5.getAttributeValue("key");
         var8 = var8.replace("#", "");
         String var9 = var5.getAttributeValue("variable");
         if (var9 != null && var9.equals("true")) {
            Variable var10 = this.Strategy.variables().getById(var7);
            if (var10 != null) {
               var7 = var10.getValue();
            }
         }

         boolean var16 = TALibIndicators.isPeriod(var8);
         if (var6.equals("int") || var6.equals("data") || var6.equals("period")) {
            int var11 = Integer.parseInt(var7);
            if (var16 && var11 < 2) {
               var11 = 2;
            }

            this.params.set(var8, var11);
         } else if (var6.equals("long")) {
            this.params.set(var8, Long.parseLong(var7));
         } else if (var6.equals("double")) {
            this.params.set(var8, Double.parseDouble(var7));
         } else if (var6.equals("boolean")) {
            this.params.set(var8, Boolean.parseBoolean(var7));
         } else if (var6.equals("java.lang.String")) {
            this.params.set(var8, var7);
         }

         if (var16 || var8.equals("Shift")) {
            this.periodParams.add(var8.hashCode());
         }

         if (var8.equals("Line")) {
            int var17 = Integer.parseInt(var7);
            String var12 = var5.getAttributeValue("values");
            if (var12 != null) {
               String[] var13 = var12.split(",");
               String var14 = var13[var17].split("=")[0].trim();
               this.params.set("LineName", var14);
            }
         }
      }

      this.paramsKeys = this.params.getAllKeys();
      this.chartIndex = this.params.getInt("Chart");
   }

   @Override
   public StrategyBase getStrategy() {
      return this.Strategy;
   }

   @Override
   public IBlock clone(boolean var1, StrategyBase var2) throws BlockDefinitionException {
      this.Strategy = var2;
      return this;
   }

   @Override
   public void setCustomData(String var1, int var2) {
      if (this.customData == null) {
         this.customData = new Int2IntOpenHashMap();
      }

      this.customData.put(var1.hashCode(), var2);
   }

   @Override
   public int getCustomData(String var1) {
      return this.customData != null && this.customData.containsKey(var1.hashCode()) ? this.customData.get(var1.hashCode()) : Integer.MIN_VALUE;
   }

   @Override
   public void copyCustomData(IBlock var1) {
      if (var1 instanceof TALibIndicator) {
         TALibIndicator var2 = (TALibIndicator)var1;
         if (var2.customData != null) {
            if (this.customData == null) {
               this.customData = new Int2IntOpenHashMap();
            }

            this.customData.putAll(var2.customData);
         }
      }
   }

   @Override
   public int getReturnType() {
      return this.returnType;
   }

   @Override
   public void setReturnType(int var1) {
      this.returnType = var1;
   }

   @Override
   public Element getCustomBlockXml(int var1) throws BlockDefinitionException {
      throw new BlockDefinitionException("Not implemented for this!");
   }

   public void copyFrom(TALibIndicator var1) {
      this.name = var1.name;
      this.returnType = var1.returnType;
      this.params = var1.params.clone();
      this.copyCustomData(var1);
   }

   public Element getBlockXml() throws BlockDefinitionException {
      Element var1 = TALibBlocks.getBlock("talib_" + this.name);
      var1 = var1.clone();
      List var2 = var1.getChildren("Param");

      for (int var3 = 0; var3 < var2.size(); var3++) {
         Element var4 = (Element)var2.get(var3);
         String var5 = var4.getAttributeValue("type");
         String var6 = var4.getAttributeValue("key");
         var6 = var6.replace("#", "");
         if (!this.params.containsKey(var6)) {
            throw new BlockDefinitionException("Param '" + var6 + "' not found in block '" + this.name + "'");
         }

         String var7;
         if (var5.equals("int") || var5.equals("data") || var5.equals("period")) {
            var7 = Integer.toString(this.params.getInt(var6));
         } else if (var5.equals("long")) {
            var7 = Long.toString(this.params.getLong(var6));
         } else if (var5.equals("double")) {
            var7 = Double.toString(this.params.getDouble(var6));
         } else if (var5.equals("boolean")) {
            var7 = Boolean.toString(this.params.getBoolean(var6));
         } else {
            if (!var5.equals("java.lang.String")) {
               throw new BlockDefinitionException("Param '" + var6 + "' in block '" + this.name + "' has unknown type: '" + var5 + "'");
            }

            var7 = this.params.getString(var6);
         }

         var4.setText(var7);
      }

      return var1;
   }
}
