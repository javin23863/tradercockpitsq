package com.strategyquant.plugin.Task.impl.Build;

import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.gridlib.client.GridMessage;
import com.strategyquant.lib.IRandomGenerator;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.random.MersenneTwisterRng;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.TradingOption;
import com.strategyquant.tradinglib.atm.ATMGenerateConfig;
import com.strategyquant.tradinglib.backtestrunner.BacktestResult;
import com.strategyquant.tradinglib.backtestrunner.BacktestRunner;
import com.strategyquant.tradinglib.backtestrunner.BacktestSettings;
import com.strategyquant.tradinglib.generator.StrategyGenerator;
import com.strategyquant.tradinglib.generator.StrategyGeneratorCache;
import com.strategyquant.tradinglib.options.TradingOptions;
import com.strategyquant.tradinglib.options.parameters.StockpickerOptions;
import com.strategyquant.tradinglib.project.ILastEventListener;
import com.strategyquant.tradinglib.project.StopPauseEngine;
import com.strategyquant.tradinglib.results.SpecialValues;
import com.strategyquant.tradinglib.strategy.xml.StrategyFixer;
import com.strategyquant.tradinglib.util.StockpickerGenFixer;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BuilderJob extends GridJob<BacktestResult> {
   private static final Logger Log = LoggerFactory.getLogger("BuilderJob");
   private Element elReplacements;
   private Element elStrategyTemplate;
   protected String strategyName;
   private SettingsMap buildSettings;
   private long randomSeed = -1L;
   private BacktestRunner backtestRunner;
   private int generatorHash;
   private StopPauseEngine stopPauseEngine;
   private boolean stopped = false;
   private ATMGenerateConfig atmGenerateConfig = null;
   private final TradingOptions tradingOptions;

   public BuilderJob(String var1, Map<String, Serializable> var2, ILastEventListener var3, String var4) throws Exception {
      super(var1, 1, var2);
      this.strategyName = (String)var2.get("StrategyName");
      this.elReplacements = (Element)var2.get("Replacements");
      this.elStrategyTemplate = (Element)var2.get("StrategyTemplate");
      this.randomSeed = (Long)((Serializable)var2.get("RandomSeed"));
      this.generatorHash = (Integer)((Serializable)var2.get("GeneratorHash"));
      this.buildSettings = (SettingsMap)var2.get("BuildSettings");
      this.atmGenerateConfig = (ATMGenerateConfig)var2.get("ATMGenerateConfig");
      this.tradingOptions = (TradingOptions)var2.get("TradingOptions");
      this.stopPauseEngine = new StopPauseEngine();
      this.backtestRunner = new BacktestRunner(new BacktestSettings(var2), this.stopPauseEngine, this, true, var3, var4);
   }

   public BacktestResult call() throws Exception {
      if (Log.isDebugEnabled()) {
         Log.debug("--- Job {} started", this.strategyName);
      }

      Element var1 = this.generateStrategy();
      if (!StrategyFixer.checkVersion(var1)) {
         var1 = StrategyFixer.fixStrategy(XMLUtil.xmlToString(var1));
      }

      this.backtestRunner.setStrategy(var1);
      this.stopPauseEngine.start();
      BacktestResult var2 = this.backtestRunner.execute();
      ResultsGroup var3 = var2.getResult();
      if (var3 != null) {
         var3.specialValues().set(SpecialValues.DateGenerated, SQTime.getLocalCurrentTimeInMs());
      }

      if (this.stopped && var3 != null) {
         var3.clear();
      }

      return var2;
   }

   private Element generateStrategy() throws JDOMException, IOException, Exception {
      MersenneTwisterRng var1 = new MersenneTwisterRng(this.randomSeed);
      StrategyGenerator var2 = StrategyGeneratorCache.getGenerator(
         this.generatorHash, var1, this.elStrategyTemplate, this.elReplacements, this.buildSettings, this.atmGenerateConfig
      );

      try {
         Element var3 = var2.generate(this.strategyName);
         int var4 = StockpickerGenFixer.recognizeEngine(var3);
         if (var4 == 1316847364 || var4 == -1816889229) {
            this.addStockpickerFixes(var3, var1);
         }

         return var3;
      } finally {
         if (var2 != null) {
            var2.freeGenerator();
         }
      }
   }

   private void addStockpickerFixes(Element var1, IRandomGenerator var2) throws Exception {
      if (this.tradingOptions != null) {
         boolean var3 = false;
         int var4 = 0;
         int var5 = 0;
         int var6 = 0;
         int var7 = 0;
         int var8 = 0;
         int var9 = 0;

         for (TradingOption var11 : this.tradingOptions) {
            if (var11 instanceof StockpickerOptions) {
               StockpickerOptions var12 = (StockpickerOptions)var11;
               var3 = true;
               var4 = var12.PickerEntryType;
               var5 = var12.PickerExitType;
               var6 = var12.PickerEndOfDayLong;
               var7 = var12.PickerEndOfDayShort;
               var8 = var12.PickerMaxOpenPositionsLong;
               var9 = var12.PickerMaxOpenPositionsShort;
               break;
            }
         }

         if (!var3) {
            return;
         }

         Element var13 = var1.getChild("Strategy");
         StockpickerGenFixer.fixEntryExitTriggers(var2, var13, var4, var5);
         StockpickerGenFixer.fixExits(var2, var13, var6, var7);
         StockpickerGenFixer.fixMaxOpenPositions(var13, var8, var9);
      }
   }

   public void messageReceived(GridMessage var1) {
      if (var1.getMessageID() == 4) {
         this.backtestRunner.stop();
         this.stopped = true;
      }
   }
}
