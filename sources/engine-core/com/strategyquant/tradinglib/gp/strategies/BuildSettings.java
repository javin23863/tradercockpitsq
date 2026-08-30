package com.strategyquant.tradinglib.gp.strategies;

import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.atm.ATMGenerateConfig;
import com.strategyquant.tradinglib.generator.StrategyGeneratorCache;
import com.strategyquant.tradinglib.options.TradingOptions;
import org.jdom2.Element;

public class BuildSettings {
   private Element elReplacements;
   private Element elStrategyTemplate;
   private SettingsMap buildSettings;
   private int generatorHash;
   private ATMGenerateConfig atmGenerateConfig;
   public TradingOptions tradingOptions = null;

   public BuildSettings(Element var1, Element var2, SettingsMap var3, ATMGenerateConfig var4) {
      this.elStrategyTemplate = var1;
      this.elReplacements = var2;
      this.buildSettings = var3;
      this.atmGenerateConfig = var4;
      this.generatorHash = StrategyGeneratorCache.getHash(var1, var2, var3, var4);
   }

   private BuildSettings() {
   }

   public BuildSettings clone() {
      BuildSettings var1 = new BuildSettings();
      var1.elReplacements = this.elReplacements;
      var1.elStrategyTemplate = this.elStrategyTemplate;
      var1.buildSettings = this.buildSettings.clone();
      var1.atmGenerateConfig = this.atmGenerateConfig;
      var1.generatorHash = this.generatorHash;
      var1.tradingOptions = this.tradingOptions;
      return var1;
   }

   public int getGeneratorHash() {
      return this.generatorHash;
   }

   public Element getStrategyTemplate() {
      return this.elStrategyTemplate;
   }

   public Element getReplacements() {
      return this.elReplacements;
   }

   public SettingsMap getBuildSettingsMap() {
      return this.buildSettings;
   }

   public ATMGenerateConfig getATMGenerateConfig() {
      return this.atmGenerateConfig;
   }
}
