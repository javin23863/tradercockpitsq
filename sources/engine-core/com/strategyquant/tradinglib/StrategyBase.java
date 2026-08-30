package com.strategyquant.tradinglib;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.InstrumentInfo;
import com.strategyquant.datalib.TickEvent;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.ValuesMap;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.snippets.CustomClassesLoader;
import com.strategyquant.lib.snippets.NonexistingCustomClassException;
import com.strategyquant.tradinglib.engine.TradingSetup;
import com.strategyquant.tradinglib.engine.stockpicker.Stockpicker;
import com.strategyquant.tradinglib.event.ITradingEvent;
import com.strategyquant.tradinglib.exception.StrategyStoppedException;
import com.strategyquant.tradinglib.explore.Explore;
import com.strategyquant.tradinglib.generator.StrategyGenerator;
import com.strategyquant.tradinglib.indicator.IndicatorsCache;
import com.strategyquant.tradinglib.indicator.IndicatorsObj;
import com.strategyquant.tradinglib.optimization.IStrategyModifier;
import com.strategyquant.tradinglib.optimization.OptimizationSettings;
import com.strategyquant.tradinglib.optimization.StrategyParamData;
import com.strategyquant.tradinglib.options.parameters.StockpickerOptions;
import com.strategyquant.tradinglib.simulator.Engines;
import com.strategyquant.tradinglib.strategy.MarketData;
import com.strategyquant.tradinglib.strategy.StrategiesList;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class StrategyBase extends ChartData {
   public static final Logger DefaultLog = LoggerFactory.getLogger("StrategyBase");
   private final String reasonNo = "No reason specified";
   protected TradingSetup tradingSetup;
   public int UpdatedChart = -1;
   public boolean MultipleChartsUpdated = false;
   public int UpdateEventType = 0;
   public Trader Trader;
   private int[] tradersMap = null;
   private Trader[] tradersArray = null;
   protected Logger Log = null;
   protected String strategyName;
   private String setupName;
   private SettingsMap settings;
   protected Variables variables = null;
   protected Element xmlStrategy;
   protected TradingOption[] tradingOptions;
   private boolean wasUsed = false;
   private int badStrategyChecks = 0;
   protected int dismissBadStrategies;
   protected boolean warningsBadStrategies;
   private int strategyProblems = 0;
   private long progressUpdateTime = -1L;
   private long progressTotalHistoryFrom;
   private long progressTotalHistoryTo;
   private double progressDiff;
   private int progressLastBarChecked;
   private static IStrategyModifier strategyModifier = null;
   private long strategyCheckDate = -1L;
   protected MoneyManagementMethod globalMMMethod = null;
   private boolean firstCallEvaluateOptions = true;
   private int updateProgressTicks = 0;
   private boolean applyExitsAtTheEndOfRule = false;
   protected TradingOption endOfdayExit;
   private int engine;
   private HashMap<String, Element> cbMap;
   private ATM atm = null;
   public static final int ValueNotDefined = -999999;
   public double accountBalance = -999999.0;
   public double accountEquity = -999999.0;
   public double initialCapital = -999999.0;
   private int ambigouousTrades = -1;
   public boolean isAlgoWizard = false;
   public Stockpicker Stockpicker = null;
   public Explore Explore;
   public final SettingsMap specialValues = new SettingsMap();

   public StrategyBase() {
      this.Log = LoggerFactory.getLogger("Strategy[" + this.getClass().getSimpleName() + "]");
   }

   @Override
   protected void initializeFromMarketData(MarketData var1) {
      super.initializeFromMarketData(var1);

      for (int var2 = 0; var2 < this.tradersArray.length; var2++) {
         this.tradersArray[var2].initialize(var1);
      }

      this.badStrategyChecks = 0;
      this.wasUsed = true;
   }

   public void setTradeControllers(String var1, Trader[] var2) {
      if (this.tradersArray != null) {
         for (int var3 = 0; var3 < this.tradersArray.length; var3++) {
            this.tradersArray[var3].destroy();
         }
      }

      this.tradersArray = var2;
      this.tradersMap = new int[this.tradersArray.length];
      this.initializeTradersMap();
      this.Trader = this.Trader(var1);
   }

   protected void initializeTradeControllers() {
      for (int var1 = 0; var1 < this.tradersArray.length; var1++) {
         this.tradersArray[var1].initialize(this.MarketData);
      }
   }

   public void updateTradeControllers() {
      if (this.MarketData.Chart(0).isUpdated() && (this.MarketData.Chart(0).EventType == 2 || this.MarketData.Chart(0).EventType == 3)) {
         for (int var1 = 0; var1 < this.tradersArray.length; var1++) {
            this.tradersArray[var1].eventBarUpdated(this.MarketData.Chart(0).EventType);
         }
      }

      if (this.MarketData.Chart(0).EventType == 5) {
         throw new IllegalArgumentException(
            "This should't happen - Trade controller will not work correctly if it is not notified on every bar open and close!!! Fix it!"
         );
      }
   }

   public abstract void callOnInit(TradingSetup var1) throws Exception;

   public void callOnInit() throws Exception {
   }

   protected void setUpdateEventType(int var1) throws TradingException {
      if (!this.tradingSetup.isInInit()) {
         throw new TradingException("You cannot call Strategy.setEventType() outside the Strategy.onInit() method !");
      }

      if (var1 == 2 || var1 == 3 || var1 == 4 || var1 == 6) {
         this.requestedEventType = var1;
      }
   }

   public int getEventType() {
      return this.requestedEventType;
   }

   public abstract void Initialize() throws Exception;

   public abstract void OnBarUpdate() throws Exception;

   public void Deinitialize() throws Exception {
   }

   public void OnEvent(ITradingEvent var1) throws Exception {
   }

   public StrategyBase clone() throws CloneNotSupportedException {
      try {
         Object var1 = this.getClass().newInstance();
         return (StrategyBase)var1;
      } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | SecurityException var2) {
         DefaultLog.info(
            String.format("Strategy %s cannot be cloned using its constructor. Does it have a contructor with parameters?", this.getClass().getSimpleName())
         );
         throw new CloneNotSupportedException(
            String.format("Strategy cannot be cloned using its constructor. Does it have a contructor with parameters? Reason: %s", var2.getMessage())
         );
      }
   }

   protected void setReservedBars(int var1) throws TradingException {
      if (!this.tradingSetup.isInInit()) {
         throw new TradingException("You cannot call Strategy.setReservedBars() outside the Strategy.onInit() method !");
      }

      this.tradingSetup.setReservedBars(var1);
   }

   public boolean hasTrader(String var1) {
      int var2 = var1.hashCode();

      for (int var3 = 0; var3 < this.tradersMap.length; var3++) {
         if (this.tradersMap[var3] == var2) {
            return true;
         }
      }

      return false;
   }

   public Trader Trader(String var1) {
      int var2 = var1.hashCode();

      for (int var3 = 0; var3 < this.tradersMap.length; var3++) {
         if (this.tradersMap[var3] == var2) {
            return this.tradersArray[var3];
         }
      }

      throw new IllegalArgumentException("Connection '" + var1 + "' doesn't exist!");
   }

   protected void StopStrategy() throws StrategyStoppedException {
      this.StopStrategy("No reason specified");
   }

   protected void StopStrategy(String var1) throws StrategyStoppedException {
      throw new StrategyStoppedException(var1);
   }

   public String getStrategyName() {
      if (this.strategyName == null) {
         this.strategyName = this.getClass().getSimpleName().intern();
      }

      return this.strategyName;
   }

   public void setStrategyName(String var1) {
      this.strategyName = var1;
   }

   public void setSetupName(String var1) {
      this.setupName = var1.intern();
   }

   public String getSetupName() {
      return this.setupName;
   }

   public void addTrader(String var1, Trader var2) {
      if (this.tradersArray != null) {
         for (int var3 = 0; var3 < this.tradersMap.length; var3++) {
            if (this.tradersMap[var3] == var1.hashCode()) {
               this.tradersArray[var3] = var2;
               return;
            }
         }
      }

      Trader[] var5;
      if (this.tradersArray == null) {
         var5 = new Trader[1];
      } else {
         var5 = new Trader[this.tradersArray.length + 1];
      }

      for (int var4 = 0; var4 < this.tradersArray.length; var4++) {
         var5[var4] = this.tradersArray[var4];
      }

      var5[this.tradersArray.length] = var2;
      this.tradersArray = var5;
      this.initializeTradersMap();
   }

   private void initializeTradersMap() {
      this.tradersMap = new int[this.tradersArray.length];

      for (int var1 = 0; var1 < this.tradersArray.length; var1++) {
         Trader var2 = this.tradersArray[var1];
         var2.setStrategy(this);
         this.tradersMap[var1] = var2.getName().hashCode();
      }
   }

   public void setSettings(SettingsMap var1) {
      this.applyExitsAtTheEndOfRule = var1.getBoolean("ApplyExitsAtTheEndOfRule", false);
      this.settings = var1;
      if (this.tradersArray != null) {
         for (int var2 = 0; var2 < this.tradersArray.length; var2++) {
            this.tradersArray[var2].setSettings(var1);
         }
      }
   }

   public SettingsMap getSettings() {
      return this.settings;
   }

   public StrategyBase newInstance() throws NoSuchMethodException, SecurityException {
      Constructor var1 = this.getClass().getConstructor();
      return (StrategyBase)SQUtils.invokeUnchecked(var1, new Object[0]);
   }

   public Variables variables() {
      if (this.variables == null) {
         this.initVariablesFromClass();
      }

      return this.variables;
   }

   private void initVariablesFromClass() {
      this.variables = new Variables(this);
   }

   public void destroyHistoryTrades() {
      if (this.tradersArray != null) {
         for (int var1 = 0; var1 < this.tradersArray.length; var1++) {
            this.tradersArray[var1].destroy();
         }
      }
   }

   public IndicatorsCache getIndicatorsCache() {
      return null;
   }

   public double convertPipsToRealPrice(String var1, double var2) throws TradingException {
      InstrumentInfo var4;
      if (this.isStockpicker()) {
         var4 = this.SymbolInfo;
      } else {
         var4 = this.MarketData.getInstrumentInfo(var1);
      }

      if (var4 == null) {
         DataInfo var5 = DataManager.getDataInfo("History", var1);
         if (var5 == null || var5.symbolInfo == null) {
            throw new TradingException(String.format("Instrument info for symbol '%s doens't exist", var1));
         }

         var4 = var5.symbolInfo;
      }

      return SQUtils.round(var2 * var4.tickSize, 6);
   }

   public double convertRealPriceToPips(String var1, double var2) throws TradingException {
      InstrumentInfo var4;
      if (this.isStockpicker()) {
         var4 = this.SymbolInfo;
      } else {
         var4 = this.MarketData.getInstrumentInfo(var1);
      }

      if (var4 == null) {
         DataInfo var5 = DataManager.getDataInfo("History", var1);
         if (var5 == null || var5.symbolInfo == null) {
            throw new TradingException(String.format("Instrument info for symbol '%s doesn't exist", var1));
         }

         var4 = var5.symbolInfo;
      }

      return SQUtils.round(var2 / var4.tickSize, 2);
   }

   public Element getStrategyXml() {
      return this.xmlStrategy;
   }

   protected void updateStrategyXML(Element var1) {
      this.xmlStrategy = var1;
   }

   public void setTradingOptions(ArrayList<TradingOption> var1) {
      this.tradingOptions = this.transformToPrimitiveArray(var1);
   }

   private TradingOption[] transformToPrimitiveArray(ArrayList<TradingOption> var1) {
      if (var1 == null) {
         return null;
      }

      TradingOption[] var2 = new TradingOption[var1.size()];
      int var3 = 0;

      for (TradingOption var5 : var1) {
         var2[var3++] = var5;
      }

      return var2;
   }

   protected boolean evaluateOptions() throws Exception {
      if (this.tradingOptions != null && this.tradingOptions.length != 0) {
         if (this.firstCallEvaluateOptions) {
            this.firstCallEvaluateOptions = false;
            this.initOptions();
         }

         boolean var1 = true;

         for (int var2 = 0; var2 < this.tradingOptions.length; var2++) {
            if (!this.tradingOptions[var2].OnBarUpdate(this)) {
               var1 = false;
            }
         }

         return var1;
      } else {
         return true;
      }
   }

   public void callOptionsOnTick(TickEvent var1) throws Exception {
      if (this.tradingOptions != null && this.tradingOptions.length != 0) {
         if (this.firstCallEvaluateOptions) {
            this.firstCallEvaluateOptions = false;
            this.initOptions();
         }

         for (int var2 = 0; var2 < this.tradingOptions.length; var2++) {
            this.tradingOptions[var2].OnTick(this, var1, true);
         }
      }
   }

   private void initOptions() {
      if (this.tradingOptions != null) {
         int var1 = 0;

         for (int var2 = 0; var2 < this.tradingOptions.length; var2++) {
            if (!this.tradingOptions[var2].isUsedInTrading()) {
               this.tradingOptions[var2] = null;
            } else {
               var1++;
               if (this.tradingOptions[var2].getClass().getSimpleName().equals("ExitAtEndOfDay")) {
                  this.endOfdayExit = this.tradingOptions[var2];
               }
            }
         }

         if (var1 != this.tradingOptions.length) {
            TradingOption[] var5 = new TradingOption[var1];
            int var3 = 0;

            for (int var4 = 0; var4 < this.tradingOptions.length; var4++) {
               if (this.tradingOptions[var4] != null) {
                  var5[var3++] = this.tradingOptions[var4];
               }
            }

            this.tradingOptions = var5;
         }
      }
   }

   public boolean wasUsed() {
      return this.wasUsed;
   }

   public void checkBadStrategy() throws BadStrategyException, TradingException {
      if (this.strategyCheckDate < 0L) {
         this.strategyCheckDate = this.tradingSetup.getStrategyCheckDate();
      }

      if (this.strategyCheckDate > 0L) {
         boolean var2 = this.MarketData.TimeCurrent() > this.strategyCheckDate;
         if (!var2 && this.badStrategyChecks % 10 == 0) {
            this.badStrategyChecks++;
            OrdersList var1 = this.Trader.getHistoryOrders();
            if (var1 != null && var1.size() > 200) {
               var2 = true;
            }
         }

         if (var2) {
            OrdersList var6 = this.Trader.getHistoryOrders();
            this.ambigouousTrades = this.Trader.getAmbiguousTrades();
            if (var6 != null) {
               this.strategyCheckDate = 0L;
               if (this.checkBadHistoryTrades(var6, this.ambigouousTrades)) {
                  return;
               }
            }

            int var3 = (int)(this.MarketData.Chart(0).Bars() * 0.2);

            for (int var4 = 0; var4 < this.Trader.getOpenOrdersCount(false); var4++) {
               ILiveOrder var5 = this.Trader.getOpenOrder(var4, false);
               if (var5.isSuccessful() && !var5.isPendingOrder() && var5.getBarsInTrade() > var3) {
                  this.strategyProblems = this.strategyProblems | BadStrategyException.setOrThrow(this.dismissBadStrategies, this.warningsBadStrategies, 64);
               }
            }
         }
      }
   }

   private boolean checkBadHistoryTrades(OrdersList var1, int var2) throws BadStrategyException, TradingException {
      if (var1.size() == 0) {
         int var3 = BadStrategyException.setOrThrow(this.dismissBadStrategies, this.warningsBadStrategies, 1);
         if (var3 == 1) {
            this.strategyProblems |= var3;
            return true;
         }
      }

      if (var1.size() < 20) {
         int var13 = BadStrategyException.setOrThrow(this.dismissBadStrategies, this.warningsBadStrategies, 2);
         if (var13 == 2) {
            this.strategyProblems |= var13;
            return true;
         }
      }

      int var4 = 0;
      int var5 = 0;
      int var6 = 0;
      int var7 = 0;
      int var8 = 0;

      for (int var9 = 0; var9 < var1.size(); var9++) {
         Order var10 = var1.get(var9);
         if (var10.isFilledOrder()) {
            var5++;
            if (var10.CloseType != 18) {
               var4++;
               if (var10.PL == 0.0F) {
                  var6++;
               }

               long var11 = var10.CloseTime - var10.OpenTime;
               if (var11 == 0L && var10.OpenPrice == var10.ClosePrice) {
                  var7++;
               }

               if (var10.BarsInTrade == 0) {
                  var8++;
               }
            }
         }
      }

      if (var4 < var5 * 0.1) {
         this.strategyProblems = this.strategyProblems | BadStrategyException.setOrThrow(this.dismissBadStrategies, this.warningsBadStrategies, 256);
      }

      int var14 = var4 / 10;
      if (var14 > 100) {
         var14 = 100;
      }

      if (var6 > var14) {
         this.strategyProblems = this.strategyProblems | BadStrategyException.setOrThrow(this.dismissBadStrategies, this.warningsBadStrategies, 4);
      }

      if (var7 > var14) {
         this.strategyProblems = this.strategyProblems | BadStrategyException.setOrThrow(this.dismissBadStrategies, this.warningsBadStrategies, 16);
      }

      if (var8 > var14) {
         this.strategyProblems = this.strategyProblems | BadStrategyException.setOrThrow(this.dismissBadStrategies, this.warningsBadStrategies, 8);
      }

      var14 = var4 / 20;
      if (var14 > 50) {
         var14 = 50;
      }

      if (var2 > var14) {
         this.strategyProblems = this.strategyProblems | BadStrategyException.setOrThrow(this.dismissBadStrategies, this.warningsBadStrategies, 1024);
      }

      return true;
   }

   public static void init() throws Exception {
      if (strategyModifier != null) {
         throw new Exception("StrategyBase.init() called more than once!");
      }

      try {
         CustomClassesLoader var0 = new CustomClassesLoader("Internal");

         while (var0.hasNext()) {
            String var1 = var0.getNext();
            if (var1.equals("SQ.Internal.XmlStrategyModifier")) {
               Object var2 = var0.createInstance(var1);
               if (var2 instanceof IStrategyModifier) {
                  strategyModifier = (IStrategyModifier)var2;
               }
            }
         }

         if (strategyModifier == null) {
            throw new Exception("Loading of StrategyModifier class was unsuccessful in StrategyBase.init()!");
         }
      } catch (Exception var3) {
         throw var3;
      }
   }

   public static void reload() throws Exception {
      strategyModifier = null;
      init();
   }

   public void transformToVariables(boolean var1) throws Exception {
      this.transformToVariables(var1, null);
   }

   public void transformToVariables(boolean var1, ValuesMap var2) throws Exception {
      throw new Exception("Not implemented in StrategyBase!");
   }

   public void transformToNumbers() throws Exception {
      throw new Exception("Not implemented in StrategyBase!");
   }

   public static StrategyBase createXmlStrategy(Element var0) throws Exception {
      return createXmlStrategy(var0, null);
   }

   public static StrategyBase createXmlStrategy(Element var0, String var1) throws Exception {
      if (strategyModifier == null) {
         throw new Exception("StrategyBase->strategyModifier uninitialized! You have to call StrategyBase.init() during program startup!");
      } else {
         return strategyModifier.createStrategyFromXml(var0, var1);
      }
   }

   public static StrategyParamData createStrategyVariation(StrategyBase var0, OptimizationSettings var1, short[] var2) throws Exception {
      if (strategyModifier == null) {
         throw new Exception("StrategyBase->strategyModifier uninitialized! You have to call StrategyBase.init() during program startup!");
      } else {
         return strategyModifier.createStrategyVariation(var0, var1, var2);
      }
   }

   public boolean isSymmetryEnabled() {
      return false;
   }

   protected void evaluateBeforeActionListeners() throws TradingException {
      if (this.Trader != null) {
         this.Trader.evaluateActionListeners(this.UpdateEventType, this);
      }
   }

   public static IndicatorsObj createIndicatorsObj(int var0) throws Exception {
      if (strategyModifier == null) {
         throw new Exception("StrategyBase->strategyModifier uninitialized! You have to call StrategyBase.init() during program startup!");
      }

      String var1 = "<StrategyFile><Strategy><Variables/></Strategy></StrategyFile>";
      StrategyBase var2 = createXmlStrategy(XMLUtil.stringToElement(var1));
      var2.setEngine(var0);
      return strategyModifier.createIndicatorsObject(var2);
   }

   public int getStrategyProblems() {
      return this.strategyProblems;
   }

   public void setStrategyProblem(int var1) {
      this.strategyProblems |= var1;
   }

   public void updateJobProgress() throws TradingException {
      if (this.updateProgressTicks < 100) {
         this.updateProgressTicks++;
      } else {
         this.updateProgressTicks = 0;
         long var1 = 500L;
         long var3 = System.currentTimeMillis();
         if (this.progressUpdateTime < 0L) {
            this.progressUpdateTime = var3 + var1;
            ChartDef var10 = this.tradingSetup.getChartSetup().getCharts().get(0);
            this.progressTotalHistoryFrom = var10.getHistoryFrom();
            this.progressTotalHistoryTo = var10.getHistoryTo();
            this.progressDiff = this.progressTotalHistoryTo - this.progressTotalHistoryFrom;
            this.progressLastBarChecked = this.MarketData.Chart(0).Bars();
         } else {
            if (this.MarketData.Chart(0).Bars() > this.progressLastBarChecked && var3 >= this.progressUpdateTime) {
               long var5 = this.MarketData.TimeCurrent();
               var5 -= this.progressTotalHistoryFrom;
               double var7 = var5 / this.progressDiff * 100.0;
               this.tradingSetup.notifyAboutProgress((int)var7);
               this.progressUpdateTime = var3 + var1;
            }
         }
      }
   }

   public MoneyManagementMethod getGlobalMMMethod() {
      return this.globalMMMethod;
   }

   public void setGlobalMMMethod(MoneyManagementMethod var1) {
      this.globalMMMethod = var1;
   }

   public boolean hasOnTickRule() {
      return true;
   }

   public boolean hasDailyDataBlock() {
      return true;
   }

   public boolean hasWeeklyDataBlock() {
      return true;
   }

   public boolean hasMonthlyDataBlock() {
      return true;
   }

   public static StrategyBase getStrategy(SettingsMap var0) throws TradingException {
      if (var0.containsKey("StrategyObject")) {
         if (!(var0.get("StrategyObject") instanceof StrategyBase)) {
            throw new TradingException(
               "Setting 'TradingSetup.StrategyObject was set, but it has incorrect value! It must be an instance of StrategyBase object."
            );
         } else {
            return (StrategyBase)var0.get("StrategyObject");
         }
      } else if (var0.containsKey("StrategyClass")) {
         if (!(var0.get("StrategyClass") instanceof String)) {
            throw new TradingException(
               "Setting 'TradingSetup.StrategyClass was set, but it has incorrect value! It must be a string with name of existing strategy."
            );
         }

         String var1 = (String)var0.get("StrategyClass");

         try {
            return StrategiesList.create(var1);
         } catch (NonexistingCustomClassException var3) {
            throw new TradingException("Cannot create an instance of '" + var1 + "' class.");
         }
      } else {
         throw new TradingException("Setting 'TradingSetup.StrategyClass' is not set.");
      }
   }

   public boolean getApplyExitsAtTheEndOfRule() {
      return this.applyExitsAtTheEndOfRule;
   }

   protected void setEngine(int var1) {
      this.engine = var1;
   }

   public int getEngine() {
      return this.engine;
   }

   public boolean isEngineDefined() {
      return this.engine != 0;
   }

   public boolean isTradestationEngine() {
      return this.engine == 56756755 || this.engine == 938213070;
   }

   public void callExitEOD(TickEvent var1) throws Exception {
      if (Engines.isTradestationEngine(this.engine)) {
         if (this.firstCallEvaluateOptions) {
            this.firstCallEvaluateOptions = false;
            this.initOptions();
         }

         if (this.endOfdayExit != null) {
            this.endOfdayExit.OnTick(this, var1, false);
         }
      }
   }

   public void customBlocksMapAdd(Element var1) {
      String var2 = var1.getAttributeValue("key");
      if (this.cbMap == null) {
         this.cbMap = new HashMap<>();
      }

      this.cbMap.put(var2, var1);
   }

   public Element customBlocksMapGet(String var1) {
      return this.cbMap == null ? null : this.cbMap.get(var1);
   }

   public void addBlocksToXML(Element var1) {
      StrategyGenerator.addBlocksToXML(var1, this.cbMap);
   }

   public void setATM(ATM var1) throws Exception {
      int var2 = var1.getType();
      if (var2 == 0) {
         this.atm = null;
         this.updateStrategyXML(ATM.setSourceAndEnable(this.getStrategyXml(), null, false));
      } else if (var2 == 2) {
         this.atm = var1;
         this.updateStrategyXML(ATM.setConfigATMToStrategy(var1, this.getStrategyXml(), true));
      } else {
         this.atm = ATM.createFromStrategy(this.getStrategyXml());
         this.updateStrategyXML(ATM.setSourceAndEnable(this.getStrategyXml(), "strategy", this.atm != null && this.atm.isUsed()));
      }
   }

   public ATM getATM() {
      return this.atm;
   }

   public abstract double getATRValue(ChartData var1, int var2, int var3) throws TradingException;

   public double getInitialBalance() {
      return this.initialCapital != -999999.0 ? this.initialCapital : (Double)this.settings.get("MoneyManagement.InitialCapital");
   }

   public double getAccountBalance() {
      return this.accountBalance != -999999.0 ? this.accountBalance : this.Trader.getAccountBalance();
   }

   public double getAccountEquity() {
      return this.accountEquity != -999999.0 ? this.accountEquity : this.Trader.getAccountEquity();
   }

   public boolean isStockpicker() {
      return this.Stockpicker != null;
   }

   public void evaluateEntryExit(byte var1) throws Exception {
   }

   public StockpickerOptions getStockpickerOptions() {
      StockpickerOptions var1 = null;
      if (this.tradingOptions != null) {
         for (int var2 = 0; var2 < this.tradingOptions.length; var2++) {
            TradingOption var3 = this.tradingOptions[var2];
            if (var3.getClass().getSimpleName().equals("StockpickerOptions")) {
               var1 = (StockpickerOptions)var3;
               break;
            }
         }
      }

      return var1;
   }

   public int getAmbiguousTrades() {
      return this.ambigouousTrades;
   }

   public TradingOption[] getTradingOptions() {
      return this.tradingOptions;
   }

   public SettingsMap getSpecialValues() {
      return this.specialValues;
   }
}
