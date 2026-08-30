/*
 * Copyright (c) 2017-2018, StrategyQuant - All rights reserved.
 *
 * Code in this file was made in a good faith that it is correct and does what it should.
 * If you found a bug in this code OR you have an improvement suggestion OR you want to include
 * your own code snippet into our standard library please contact us at:
 * https://roadmap.strategyquant.com
 *
 * This code can be used only within StrategyQuant products.
 * Every owner of valid (free, trial or commercial) license of any StrategyQuant product
 * is allowed to freely use, copy, modify or make derivative work of this code without limitations,
 * to be used in all StrategyQuant products and share his/her modifications or derivative work
 * with the StrategyQuant community.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES
 * OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package SQ.Internal;

import java.util.Iterator;
import java.util.List;

import org.jdom2.Element;
import org.jdom2.filter.ElementFilter;

import com.strategyquant.datalib.TimeframeManager;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.datalib.UpdateEventTypes;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.ValuesMap;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.snippets.NonexistingCustomClassException;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Directions;
import com.strategyquant.tradinglib.ILiveOrder;
import com.strategyquant.tradinglib.MoneyManagementMethod;
import com.strategyquant.tradinglib.Variable;
import com.strategyquant.tradinglib.Variables;
import com.strategyquant.tradinglib.engine.TradingSetup;
import com.strategyquant.tradinglib.engine.stockpicker.constants.PickerTriggerTypes;
import com.strategyquant.tradinglib.moneymanagement.MoneyManagementMethodsList;
import com.strategyquant.tradinglib.options.parameters.StockpickerOptions;
import com.strategyquant.tradinglib.strategy.EmptyMarketData;
import com.strategyquant.tradinglib.strategy.xml.XmlStrategyException;
import com.strategyquant.tradinglib.engine.stockpicker.Stockpicker;

/**
 * The Class XmlStrategy.
 */
public class XmlStrategy extends Strategy implements ITradingOptionsEvaluator {

	/** The event on init. */
	private StrategyEvent eventOnInit = null;
	
	/** The event on bar update. */
	private StrategyEvent eventOnBarUpdate = null;
	
	/** The transformer. */
	private VariablesTransformer transformer;
	
	/** The has zero shift. */
	private boolean hasZeroShift;
	
	/** The global MM method parsed. */
	private boolean globalMMMethodParsed = false;
	
	/** The continue bar update. */
	private boolean continueBarUpdate = true;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	/**
	 * Instantiates a new xml strategy.
	 *
	 * @param xml the xml
	 * @throws XmlStrategyException the xml strategy exception
	 */
	public XmlStrategy(Element xml) throws XmlStrategyException {
		this(xml, null);
	}

	//------------------------------------------------------------------------

	/**
	 * Instantiates a new xml strategy.
	 *
	 * @param xml the xml
	 * @param strategyName the strategy name
	 * @throws XmlStrategyException the xml strategy exception
	 */
	public XmlStrategy(Element xml, String strategyName) throws XmlStrategyException {
		this.xmlStrategy = xml;
		SQUtils.fixExitmethodAttributes(xmlStrategy);

		this.strategyName = strategyName;
		
		this.variables = new Variables(xmlStrategy);
		this.transformer = new VariablesTransformer(this);
		
		recognizePerformanceParams();
	}

	/**
	 * Instantiates a new xml strategy.
	 *
	 * @throws XmlStrategyException the xml strategy exception
	 */
	public XmlStrategy() throws XmlStrategyException {
		this.MarketData = new EmptyMarketData();
		this.variables = new Variables(null);
		this.transformer = new VariablesTransformer(this);
	}

	//------------------------------------------------------------------------

	/**
	 * Clone.
	 *
	 * @return the xml strategy
	 * @throws CloneNotSupportedException the clone not supported exception
	 */
	public XmlStrategy clone() throws CloneNotSupportedException {
		try {
			return new XmlStrategy(xmlStrategy.clone(), this.strategyName);
			
		} catch (XmlStrategyException e) {
			e.printStackTrace();
			throw new CloneNotSupportedException(String.format("Strategy cannot be cloned. Reason: %s", e.getMessage()));
		}
	}
	
	//------------------------------------------------------------------------

	/**
	 * Call on init.
	 *
	 * @param tradingSetup the trading setup
	 * @throws Exception the exception
	 */
	public void callOnInit(TradingSetup tradingSetup) throws Exception {
		this.tradingSetup = tradingSetup;

		Indicators = new Indicators(this);
		Indicators.Engine = tradingSetup.getEngineId();

		setEngine(tradingSetup.getEngineId());
		
		initializeFromMarketData(tradingSetup.getMarketData());

		dismissBadStrategies = tradingSetup.getDismissBadStrategies();
		warningsBadStrategies = tradingSetup.getWarningsBadStrategies();

		parseXml();

		hasZeroShift = checkHasZeroShift(xmlStrategy.getChild("Strategy").getChild("Rules").getChild("Events"));
		
		Indicators.setHasZeroShift(hasZeroShift);
		
		Initialize();
	}
	
	//------------------------------------------------------------------------

	/**
	 * Call on init.
	 *
	 * @throws Exception the exception
	 */
	public void callOnInit() throws Exception {
		parseXml();
	}

	//------------------------------------------------------------------------

	/**
	 * Initialize.
	 *
	 * @throws Exception the exception
	 */
	@Override
	public void Initialize() throws Exception {
		if(eventOnInit != null) {
			eventOnInit.evaluateEvent(UpdateEventType, this);
		}
	}

	//------------------------------------------------------------------------

	/**
	 * On bar update.
	 *
	 * @throws Exception the exception
	 */
	@Override
	public void OnBarUpdate() throws Exception {
		
		//double atr = this.MarketData.Chart(0).CloseD(1);
		//Log.info("Processing bar: {}, EMA: {}", SQTime.toDateMinuteString(this.MarketData.Chart(0).Time(0)), atr);

		evaluateBeforeActionListeners();

		continueBarUpdate = evaluateOptions();
		
		if(eventOnBarUpdate != null) {
			eventOnBarUpdate.evaluateEvent(UpdateEventType, this);
		}
	}

	//------------------------------------------------------------------------

	/**
	 * Deinitialize.
	 *
	 * @throws Exception the exception
	 */
	@Override
	public void Deinitialize() throws Exception {
		if(eventOnBarUpdate != null) {
			eventOnBarUpdate.deinitialize();
		}
	}

	//------------------------------------------------------------------------

	/**
	 * Gets the xml variable value.
	 *
	 * @param variableId the variable id
	 * @return the xml variable value
	 * @throws XmlStrategyException the xml strategy exception
	 */
	public String getXmlVariableValue(String variableId) throws XmlStrategyException {

		Variable var = variables.getById(variableId);
		if(var == null) {
			throw new XmlStrategyException("Variable with ID '"+variableId+"' doesn't exist!");
		}

		return var.getValue();
	}

	//------------------------------------------------------------------------

	/**
	 * Parses the xml.
	 *
	 * @throws XmlStrategyException the xml strategy exception
	 */
	private void parseXml() throws XmlStrategyException {
		parseStockpickerSettings();
		
		parseCustomBlocks();
		
		Element elEvents = xmlStrategy.getChild("Strategy").getChild("Rules").getChild("Events");

		for(Element elEvent : elEvents.getChildren()) {
			StrategyEvent event = new StrategyEvent(this, elEvent);

			if(event.getEventName().equals("OnInit")) {
				eventOnInit = event;
			} else if(event.getEventName().equals("OnBarUpdate")) {
				eventOnBarUpdate = event;
			} else if(event.getEventName().equals("OnDeinit")) {
				// do nothing
			} else {
				throw new XmlStrategyException(String.format("Unrecognized event '%s'", event.getEventName()));
			}
		}
	}

	//------------------------------------------------------------------------

	private void parseStockpickerSettings() throws XmlStrategyException {
		Element elStrategy = xmlStrategy.getChild("Strategy");
		
		if(XMLUtil.getBooleanAttr(elStrategy, "pickerEditor", false)) {
			Stockpicker = new Stockpicker();
			Stockpicker.singleAsset = XMLUtil.getBooleanAttr(elStrategy, "singleAsset", false);
			
			Stockpicker.entryType = PickerTriggerTypes.parse(XMLUtil.getStringAttr(elStrategy, "entryTriggeredAt", "OnBarOpen"));
			Stockpicker.exitType = PickerTriggerTypes.parse(XMLUtil.getStringAttr(elStrategy, "exitTriggeredAt", "OnBarClose"));
			
			StockpickerOptions options = getStockpickerOptions();
			if(options != null && !isAlgoWizard) {
				Stockpicker.entryType = options.getEntryType(Stockpicker.entryType);
				Stockpicker.exitType = options.getExitType(Stockpicker.exitType);
			}
		}
	}


	//------------------------------------------------------------------------

	/**
	 * Parses the custom blocks.
	 */
	private void parseCustomBlocks() {
		Element elCBs = xmlStrategy.getChild("Strategy").getChild("CustomBlocks");
		if(elCBs != null) {
			List<Element> children = elCBs.getChildren("Item");
			
			for(int i=0; i<children.size(); i++) {
				Element elCBItem = children.get(i);
				
				this.customBlocksMapAdd(elCBItem);
			}
		}
		
	}

	//------------------------------------------------------------------------

	/**
	 * Gets the global MM method.
	 *
	 * @return the global MM method
	 */
	public MoneyManagementMethod getGlobalMMMethod() {
		
		if(!globalMMMethodParsed) {
			globalMMMethodParsed = true;

			Element elGlobalMM = xmlStrategy.getChild("Strategy").getChild("MoneyManagement");
			if(elGlobalMM != null) {
				try {
					globalMMMethod = MoneyManagementMethodsList.createFromXml(elGlobalMM.getChild("Method"));

				} catch (NonexistingCustomClassException e) {
					Log.error("Class for Global MM method doesn't exist", e);
				}
			}
		}
		
		return globalMMMethod;
	}

	//------------------------------------------------------------------------

	/**
	 * Check has zero shift.
	 *
	 * @param el the el
	 * @return true, if successful
	 */
	private boolean checkHasZeroShift(Element el) {
		
		Iterator<?> iterator = el.getDescendants(new ElementFilter("Param"));
		while (iterator.hasNext()) {
			Element elParam = (Element) iterator.next();
			
			String paramName = elParam.getAttributeValue("key");
			if(paramName != null && paramName.equals("#Shift#")) {
				String val = elParam.getValue();
				if(val.length() == 1) {
					if(val.equals("0")) {
						return true;
					}
				}
			}
		}
/*		
		if(el.getName().equals("Param")) {
			// it is parameter, check if it is Shift
			}
			
			return false;
		} 
		
		List<Element> children = el.getChildren();
		if(children.size() > 0) {
			for(int i=0; i<children.size(); i++) {
				Element elChild = children.get(i);
				
				if(checkHasZeroShift(elChild)) {
					return true;
				}
			}
		}
*/
		return false;
	}
	
	//------------------------------------------------------------------------

	/**
	 * Variables.
	 *
	 * @return the variables
	 */
	public Variables variables() {
		return variables;
	}

	//------------------------------------------------------------------------

	/**
	 * Gets the global PT.
	 *
	 * @param order the order
	 * @return the global PT
	 * @throws TradingException the trading exception
	 */
	public double getGlobalPT(ILiveOrder order) throws TradingException {
		return getGlobalSLPT("PT", order);
	}

	//------------------------------------------------------------------------

	/**
	 * Gets the SLPT tag.
	 *
	 * @param SLorPT the s lor PT
	 * @param direction the direction
	 * @return the SLPT tag
	 */
	private String getSLPTTag(String SLorPT, int direction) {
		Element elGlobalSLPT = xmlStrategy.getChild("Strategy").getChild("GlobalSLPT");

		boolean useSameSLPTforBothDirections = Boolean.parseBoolean(elGlobalSLPT.getChild("useSameSLPTforBothDirections").getText());

		String tagName;
		if(direction == Directions.Long) {
			tagName = (useSameSLPTforBothDirections ? "global" : "globalLong") ;
		} else {
			tagName = (useSameSLPTforBothDirections ? "global" : "globalShort") ;
		}

		return tagName + SLorPT;
	}

	//------------------------------------------------------------------------

	/**
	 * Gets the global SL.
	 *
	 * @param order the order
	 * @return the global SL
	 * @throws TradingException the trading exception
	 */
	public double getGlobalSL(ILiveOrder order) throws TradingException {
		return getGlobalSLPT("SL", order);
	}

	//------------------------------------------------------------------------

	/**
	 * Gets the global SLPT.
	 *
	 * @param SLorPT the s lor PT
	 * @param order the order
	 * @return the global SLPT
	 * @throws TradingException the trading exception
	 */
	public double getGlobalSLPT(String SLorPT, ILiveOrder order) throws TradingException {
		String tagName = getSLPTTag(SLorPT, order.getDirection());

		double value;

		Element elValues = xmlStrategy.getChild("Strategy").getChild("GlobalSLPT").getChild("values").getChild(tagName).getChild("values");
		if(elValues.getAttributeValue("type").equals("fixed")) {
			// it is fixed SL/PT
			value = Double.parseDouble(elValues.getChildren("value").get(0).getText());
			return convertPipsToRealPrice(order.getSymbol(), value);
		}

		if(elValues.getAttributeValue("type").equals("atr")) {
			// it is ATR-based SL/PT
			double Value = Double.parseDouble(elValues.getChildren("value").get(0).getText());
			int AtrPeriod = Integer.parseInt(elValues.getChildren("value").get(1).getText());

			ChartData chartData = MarketData.Chart(order.getSymbol());

			return Value * SQUtils.round(getATRValue(chartData, AtrPeriod, 1), 6);
		}

		if(elValues.getAttributeValue("type").equals("variable")) {
			// it is fixed SL/PT stored in variable
			String variableId = elValues.getChildren("value").get(0).getText();

			value = this.variables().getById(variableId).getValueAsDouble();
			return convertPipsToRealPrice(order.getSymbol(), value);
		}

		throw new TradingException("Not implemented yet - type: "+elValues.getAttributeValue("type")+"!");
	}

	//------------------------------------------------------------------------

	/**
	 * Transform to variables.
	 *
	 * @param symmetric the symmetric
	 * @throws Exception the exception
	 */
	public void transformToVariables(boolean symmetric) throws Exception {
		transformToVariables(symmetric, null);
	}
	
	//------------------------------------------------------------------------

	/**
	 * Transform to variables.
	 *
	 * @param symmetric the symmetric
	 * @param parameterTypes the parameter types
	 * @throws Exception the exception
	 */
	public void transformToVariables(boolean symmetric, ValuesMap parameterTypes) throws Exception{
		if(symmetric && !transformer.isSymmetryEnabled()){
			//Log.info("Selected strategy doesn't have symmetric rules");
			symmetric = false;
		}
		
		transformer.transformToVariables(symmetric, parameterTypes);
	}

	//------------------------------------------------------------------------

	/**
	 * Transform to numbers.
	 *
	 * @throws Exception the exception
	 */
	public void transformToNumbers() throws Exception{
		transformer.backToNumbers();
	}

	//------------------------------------------------------------------------

	/**
	 * Sets the parameters.
	 *
	 * @param combination the new parameters
	 * @throws Exception the exception
	 */
	public void setParameters(Combination combination) throws Exception{
		for(String paramName : combination.keySet()){
			try {
				variables.get(paramName).setValue(combination.get(paramName));
			} catch(Exception e){
				throw new Exception("Error while setting value of parameter '" + paramName + "'. ", e);
			}
		}
	}

	//------------------------------------------------------------------------
	
	/**
	 * Checks if is symmetry enabled.
	 *
	 * @return true, if is symmetry enabled
	 */
	public boolean isSymmetryEnabled(){
		return transformer.isSymmetryEnabled();
	}
/*
	@Override
	public String getStrategyName(){
		if(strategyName == null){
			xmlStrategy.
		}
		
		return strategyName;
	}
	*/

	//------------------------------------------------------------------------

	/**
 * Checks for on tick rule.
 *
 * @return true, if successful
 */
@Override
	public boolean hasOnTickRule() {
		return hasOnTickRule;
	}

	//------------------------------------------------------------------------

	/**
	 * Checks for daily data block.
	 *
	 * @return true, if successful
	 */
	@Override
	public boolean hasDailyDataBlock() {
		return hasDailyDataBlock;
	}

	//------------------------------------------------------------------------

	/**
	 * Checks for weekly data block.
	 *
	 * @return true, if successful
	 */
	@Override
	public boolean hasWeeklyDataBlock() {
		return hasWeeklyDataBlock;
	}

	//------------------------------------------------------------------------

	/**
	 * Checks for monthly data block.
	 *
	 * @return true, if successful
	 */
	@Override
	public boolean hasMonthlyDataBlock() {
		return hasMonthlyDataBlock;
	}
	
	//------------------------------------------------------------------------

	/**
	 * Recognize performance params.
	 */
	private void recognizePerformanceParams() {
		hasOnTickRule = false;
		hasDailyDataBlock = false;
		hasWeeklyDataBlock = false;
		hasMonthlyDataBlock = false;

		goThroughXMLRecognizePerformanceParams(xmlStrategy);
	}
	
	//------------------------------------------------------------------------

	/**
	 * Go through XML recognize performance params.
	 *
	 * @param elParent the el parent
	 */
	private void goThroughXMLRecognizePerformanceParams(Element elParent) {
		if(hasOnTickRule && hasDailyDataBlock && hasWeeklyDataBlock && hasMonthlyDataBlock) {
			return;
		}
		
		List<Element> children = elParent.getChildren();
		
		for(int i=0; i<children.size(); i++) {
			Element elChild = children.get(i);
			
			if(!hasOnTickRule) {
				String everyTick = elChild.getAttributeValue("everyTick");
				if(everyTick != null && everyTick.equals("true")) {
					hasOnTickRule = true;	
				}
			}
			
			if(!hasDailyDataBlock || !hasWeeklyDataBlock || !hasMonthlyDataBlock) {
				String key = elChild.getAttributeValue("key");
				if(key != null) {
					if(key.contains("OpenD") || key.contains("HighD") || key.contains("LowD") || key.contains("CloseD")) {
						hasDailyDataBlock = true;	
					
					} else if(key.contains("OpenW") || key.contains("HighW") || key.contains("LowW") || key.contains("CloseW")) {
						hasWeeklyDataBlock = true;	

					} else if(key.contains("OpenM") || key.contains("HighM") || key.contains("LowM") || key.contains("CloseM")) {
						hasMonthlyDataBlock = true;	
					}
				}
				
				String chartTF = elChild.getAttributeValue("chartTF");
				if(chartTF != null) {
					if(chartTF.contains("D1")) {
						hasDailyDataBlock = true;	
					
					} else if(chartTF.contains("W1") || chartTF.contains(TimeframeManager.TF_WEEKLY)) {
						hasWeeklyDataBlock = true;	

					} else if(chartTF.contains("M1") || chartTF.contains(TimeframeManager.TF_MONTHLY)) {
						hasMonthlyDataBlock = true;	
					}
				}
			}
			
			if(hasOnTickRule && hasDailyDataBlock && hasWeeklyDataBlock && hasMonthlyDataBlock) {
				return;
			}

			goThroughXMLRecognizePerformanceParams(elChild);
		}
	}

	//------------------------------------------------------------------------

	/**
	 * Continue bar update.
	 *
	 * @return true, if successful
	 * @throws Exception the exception
	 */
	@Override
	public boolean continueBarUpdate() throws Exception {		
		continueBarUpdate = continueBarUpdate && evaluateOptions();
		return continueBarUpdate;
	}

	//------------------------------------------------------------------------

	/**
	 * Gets the bar event type.
	 *
	 * @return the bar event type
	 */
	public int getBarEventType() {
		if(tradingSetup==null) {
			return UpdateEventTypes.BarOpen;
		}
		
		return tradingSetup.getBarEventType();
	}

	/**
	 * Stockpicker, evaluates entry/exit signals.
	 *
	 */
	@Override
	public void evaluateEntryExit(byte triggeredAt) throws Exception {
		this.Stockpicker.strategyTriggeredAt = triggeredAt;
		
		if(eventOnBarUpdate != null) {
			eventOnBarUpdate.evaluateEvent(UpdateEventType, this);
		}
	}

}

