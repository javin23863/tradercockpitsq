package SQ.Internal;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.ValuesMap;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.snippets.NonexistingCustomClassException;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.ILiveOrder;
import com.strategyquant.tradinglib.MoneyManagementMethod;
import com.strategyquant.tradinglib.Variable;
import com.strategyquant.tradinglib.Variables;
import com.strategyquant.tradinglib.engine.TradingSetup;
import com.strategyquant.tradinglib.engine.stockpicker.Stockpicker;
import com.strategyquant.tradinglib.engine.stockpicker.constants.PickerTriggerTypes;
import com.strategyquant.tradinglib.moneymanagement.MoneyManagementMethodsList;
import com.strategyquant.tradinglib.options.parameters.StockpickerOptions;
import com.strategyquant.tradinglib.strategy.EmptyMarketData;
import com.strategyquant.tradinglib.strategy.xml.XmlStrategyException;
import java.util.List;
import org.jdom2.Element;
import org.jdom2.filter.ElementFilter;
import org.jdom2.util.IteratorIterable;

public class XmlStrategy extends Strategy implements ITradingOptionsEvaluator {
   private StrategyEvent eventOnInit = null;
   private StrategyEvent eventOnBarUpdate = null;
   private VariablesTransformer transformer;
   private boolean hasZeroShift;
   private boolean globalMMMethodParsed = false;
   private boolean continueBarUpdate = true;

   public XmlStrategy(Element var1) throws XmlStrategyException {
      this(var1, null);
   }

   public XmlStrategy(Element var1, String var2) throws XmlStrategyException {
      this.xmlStrategy = var1;
      SQUtils.fixExitmethodAttributes(this.xmlStrategy);
      this.strategyName = var2;
      this.variables = new Variables(this.xmlStrategy);
      this.transformer = new VariablesTransformer(this);
      this.recognizePerformanceParams();
   }

   public XmlStrategy() throws XmlStrategyException {
      this.MarketData = new EmptyMarketData();
      this.variables = new Variables(null);
      this.transformer = new VariablesTransformer(this);
   }

   public XmlStrategy clone() throws CloneNotSupportedException {
      try {
         return new XmlStrategy(this.xmlStrategy.clone(), this.strategyName);
      } catch (XmlStrategyException var2) {
         var2.printStackTrace();
         throw new CloneNotSupportedException(String.format("Strategy cannot be cloned. Reason: %s", var2.getMessage()));
      }
   }

   @Override
   public void callOnInit(TradingSetup var1) throws Exception {
      this.tradingSetup = var1;
      this.Indicators = new Indicators(this);
      this.Indicators.Engine = var1.getEngineId();
      this.setEngine(var1.getEngineId());
      this.initializeFromMarketData(var1.getMarketData());
      this.dismissBadStrategies = var1.getDismissBadStrategies();
      this.warningsBadStrategies = var1.getWarningsBadStrategies();
      this.parseXml();
      this.hasZeroShift = this.checkHasZeroShift(this.xmlStrategy.getChild("Strategy").getChild("Rules").getChild("Events"));
      this.Indicators.setHasZeroShift(this.hasZeroShift);
      this.Initialize();
   }

   public void callOnInit() throws Exception {
      this.parseXml();
   }

   public void Initialize() throws Exception {
      if (this.eventOnInit != null) {
         this.eventOnInit.evaluateEvent(this.UpdateEventType, this);
      }
   }

   public void OnBarUpdate() throws Exception {
      this.evaluateBeforeActionListeners();
      this.continueBarUpdate = this.evaluateOptions();
      if (this.eventOnBarUpdate != null) {
         this.eventOnBarUpdate.evaluateEvent(this.UpdateEventType, this);
      }
   }

   public void Deinitialize() throws Exception {
      if (this.eventOnBarUpdate != null) {
         this.eventOnBarUpdate.deinitialize();
      }
   }

   public String getXmlVariableValue(String var1) throws XmlStrategyException {
      Variable var2 = this.variables.getById(var1);
      if (var2 == null) {
         throw new XmlStrategyException("Variable with ID '" + var1 + "' doesn't exist!");
      } else {
         return var2.getValue();
      }
   }

   private void parseXml() throws XmlStrategyException {
      this.parseStockpickerSettings();
      this.parseCustomBlocks();
      Element var1 = this.xmlStrategy.getChild("Strategy").getChild("Rules").getChild("Events");

      for (Element var3 : var1.getChildren()) {
         StrategyEvent var4 = new StrategyEvent(this, var3);
         if (var4.getEventName().equals("OnInit")) {
            this.eventOnInit = var4;
         } else if (var4.getEventName().equals("OnBarUpdate")) {
            this.eventOnBarUpdate = var4;
         } else if (!var4.getEventName().equals("OnDeinit")) {
            throw new XmlStrategyException(String.format("Unrecognized event '%s'", var4.getEventName()));
         }
      }
   }

   private void parseStockpickerSettings() throws XmlStrategyException {
      Element var1 = this.xmlStrategy.getChild("Strategy");
      if (XMLUtil.getBooleanAttr(var1, "pickerEditor", false)) {
         this.Stockpicker = new Stockpicker();
         this.Stockpicker.singleAsset = XMLUtil.getBooleanAttr(var1, "singleAsset", false);
         this.Stockpicker.entryType = PickerTriggerTypes.parse(XMLUtil.getStringAttr(var1, "entryTriggeredAt", "OnBarOpen"));
         this.Stockpicker.exitType = PickerTriggerTypes.parse(XMLUtil.getStringAttr(var1, "exitTriggeredAt", "OnBarClose"));
         StockpickerOptions var2 = this.getStockpickerOptions();
         if (var2 != null && !this.isAlgoWizard) {
            this.Stockpicker.entryType = var2.getEntryType(this.Stockpicker.entryType);
            this.Stockpicker.exitType = var2.getExitType(this.Stockpicker.exitType);
         }
      }
   }

   private void parseCustomBlocks() {
      Element var1 = this.xmlStrategy.getChild("Strategy").getChild("CustomBlocks");
      if (var1 != null) {
         List var2 = var1.getChildren("Item");

         for (int var3 = 0; var3 < var2.size(); var3++) {
            Element var4 = (Element)var2.get(var3);
            this.customBlocksMapAdd(var4);
         }
      }
   }

   public MoneyManagementMethod getGlobalMMMethod() {
      if (!this.globalMMMethodParsed) {
         this.globalMMMethodParsed = true;
         Element var1 = this.xmlStrategy.getChild("Strategy").getChild("MoneyManagement");
         if (var1 != null) {
            try {
               this.globalMMMethod = MoneyManagementMethodsList.createFromXml(var1.getChild("Method"));
            } catch (NonexistingCustomClassException var3) {
               this.Log.error("Class for Global MM method doesn't exist", var3);
            }
         }
      }

      return this.globalMMMethod;
   }

   private boolean checkHasZeroShift(Element var1) {
      IteratorIterable var2 = var1.getDescendants(new ElementFilter("Param"));

      while (var2.hasNext()) {
         Element var3 = (Element)var2.next();
         String var4 = var3.getAttributeValue("key");
         if (var4 != null && var4.equals("#Shift#")) {
            String var5 = var3.getValue();
            if (var5.length() == 1 && var5.equals("0")) {
               return true;
            }
         }
      }

      return false;
   }

   public Variables variables() {
      return this.variables;
   }

   public double getGlobalPT(ILiveOrder var1) throws TradingException {
      return this.getGlobalSLPT("PT", var1);
   }

   private String getSLPTTag(String var1, int var2) {
      Element var3 = this.xmlStrategy.getChild("Strategy").getChild("GlobalSLPT");
      boolean var4 = Boolean.parseBoolean(var3.getChild("useSameSLPTforBothDirections").getText());
      String var5;
      if (var2 == 1) {
         var5 = var4 ? "global" : "globalLong";
      } else {
         var5 = var4 ? "global" : "globalShort";
      }

      return var5 + var1;
   }

   public double getGlobalSL(ILiveOrder var1) throws TradingException {
      return this.getGlobalSLPT("SL", var1);
   }

   public double getGlobalSLPT(String var1, ILiveOrder var2) throws TradingException {
      String var3 = this.getSLPTTag(var1, var2.getDirection());
      Element var6 = this.xmlStrategy.getChild("Strategy").getChild("GlobalSLPT").getChild("values").getChild(var3).getChild("values");
      if (var6.getAttributeValue("type").equals("fixed")) {
         double var11 = Double.parseDouble(((Element)var6.getChildren("value").get(0)).getText());
         return this.convertPipsToRealPrice(var2.getSymbol(), var11);
      } else if (var6.getAttributeValue("type").equals("atr")) {
         double var12 = Double.parseDouble(((Element)var6.getChildren("value").get(0)).getText());
         int var9 = Integer.parseInt(((Element)var6.getChildren("value").get(1)).getText());
         ChartData var10 = this.MarketData.Chart(var2.getSymbol());
         return var12 * SQUtils.round(this.getATRValue(var10, var9, 1), 6);
      } else if (var6.getAttributeValue("type").equals("variable")) {
         String var7 = ((Element)var6.getChildren("value").get(0)).getText();
         double var4 = this.variables().getById(var7).getValueAsDouble();
         return this.convertPipsToRealPrice(var2.getSymbol(), var4);
      } else {
         throw new TradingException("Not implemented yet - type: " + var6.getAttributeValue("type") + "!");
      }
   }

   public void transformToVariables(boolean var1) throws Exception {
      this.transformToVariables(var1, null);
   }

   public void transformToVariables(boolean var1, ValuesMap var2) throws Exception {
      if (var1 && !this.transformer.isSymmetryEnabled()) {
         var1 = false;
      }

      this.transformer.transformToVariables(var1, var2);
   }

   public void transformToNumbers() throws Exception {
      this.transformer.backToNumbers();
   }

   public void setParameters(Combination var1) throws Exception {
      for (String var3 : var1.keySet()) {
         try {
            this.variables.get(var3).setValue(var1.get(var3));
         } catch (Exception var5) {
            throw new Exception("Error while setting value of parameter '" + var3 + "'. ", var5);
         }
      }
   }

   public boolean isSymmetryEnabled() {
      return this.transformer.isSymmetryEnabled();
   }

   public boolean hasOnTickRule() {
      return this.hasOnTickRule;
   }

   public boolean hasDailyDataBlock() {
      return this.hasDailyDataBlock;
   }

   public boolean hasWeeklyDataBlock() {
      return this.hasWeeklyDataBlock;
   }

   public boolean hasMonthlyDataBlock() {
      return this.hasMonthlyDataBlock;
   }

   private void recognizePerformanceParams() {
      this.hasOnTickRule = false;
      this.hasDailyDataBlock = false;
      this.hasWeeklyDataBlock = false;
      this.hasMonthlyDataBlock = false;
      this.goThroughXMLRecognizePerformanceParams(this.xmlStrategy);
   }

   private void goThroughXMLRecognizePerformanceParams(Element var1) {
      if (!this.hasOnTickRule || !this.hasDailyDataBlock || !this.hasWeeklyDataBlock || !this.hasMonthlyDataBlock) {
         List var2 = var1.getChildren();

         for (int var3 = 0; var3 < var2.size(); var3++) {
            Element var4 = (Element)var2.get(var3);
            if (!this.hasOnTickRule) {
               String var5 = var4.getAttributeValue("everyTick");
               if (var5 != null && var5.equals("true")) {
                  this.hasOnTickRule = true;
               }
            }

            if (!this.hasDailyDataBlock || !this.hasWeeklyDataBlock || !this.hasMonthlyDataBlock) {
               String var7 = var4.getAttributeValue("key");
               if (var7 != null) {
                  if (var7.contains("OpenD") || var7.contains("HighD") || var7.contains("LowD") || var7.contains("CloseD")) {
                     this.hasDailyDataBlock = true;
                  } else if (var7.contains("OpenW") || var7.contains("HighW") || var7.contains("LowW") || var7.contains("CloseW")) {
                     this.hasWeeklyDataBlock = true;
                  } else if (var7.contains("OpenM") || var7.contains("HighM") || var7.contains("LowM") || var7.contains("CloseM")) {
                     this.hasMonthlyDataBlock = true;
                  }
               }

               String var6 = var4.getAttributeValue("chartTF");
               if (var6 != null) {
                  if (var6.contains("D1")) {
                     this.hasDailyDataBlock = true;
                  } else if (var6.contains("W1") || var6.contains("Weekly")) {
                     this.hasWeeklyDataBlock = true;
                  } else if (var6.contains("M1") || var6.contains("Monthly")) {
                     this.hasMonthlyDataBlock = true;
                  }
               }
            }

            if (this.hasOnTickRule && this.hasDailyDataBlock && this.hasWeeklyDataBlock && this.hasMonthlyDataBlock) {
               return;
            }

            this.goThroughXMLRecognizePerformanceParams(var4);
         }
      }
   }

   @Override
   public boolean continueBarUpdate() throws Exception {
      this.continueBarUpdate = this.continueBarUpdate && this.evaluateOptions();
      return this.continueBarUpdate;
   }

   public int getBarEventType() {
      return this.tradingSetup == null ? 2 : this.tradingSetup.getBarEventType();
   }

   public void evaluateEntryExit(byte var1) throws Exception {
      this.Stockpicker.strategyTriggeredAt = var1;
      if (this.eventOnBarUpdate != null) {
         this.eventOnBarUpdate.evaluateEvent(this.UpdateEventType, this);
      }
   }
}
