package SQ.Internal.RulesImpl;

import java.util.List;

import org.jdom2.Element;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.Directions;
import com.strategyquant.tradinglib.IFormula;
import com.strategyquant.tradinglib.Variable;
import com.strategyquant.tradinglib.engine.stockpicker.backtester.BacktestData;
import com.strategyquant.tradinglib.engine.stockpicker.constants.PickerTriggerTypes;
import com.strategyquant.tradinglib.engine.stockpicker.signals.Signals;
import com.strategyquant.tradinglib.engine.stockpicker.signals.entry.PickerEntrySignal;
import com.strategyquant.tradinglib.formulas.Formulas;
import com.strategyquant.tradinglib.options.parameters.StockpickerOptions;

import SQ.Internal.ITradingOptionsEvaluator;
import SQ.Internal.Rule;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class StockpickerPS  extends Rule {
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	public IFormula Value;
	private BacktestData data;
	
	private byte direction = -1;
	
	@Override
	public void evaluateRule(int updateEventType, ITradingOptionsEvaluator evaluator, String event) throws Exception {
		if(Strategy.Stockpicker.isSingleAssetStrategy()) {
			return;
		}

		if(Strategy.Stockpicker.entryType() != Strategy.Stockpicker.strategyTriggeredAt()) {
			return;
		}
		
		if(Value == null) {
			return;
		}

		data = Strategy.Stockpicker.data;
		
		Signals signals;
		
		boolean setTradeDelays = Strategy.Stockpicker.entryType == PickerTriggerTypes.BeforeBarOpen;
		if(setTradeDelays) {
			if(!Strategy.Stockpicker.data.exists(-1)) {
				return;
			}			
			
			signals = data.Signals(data.getTime(-1), false);
		} else {

			signals = data.Signals(data.getTime(0), false);
		}
				
		if(signals != null) {
			int symbolHash = Strategy.getSymbolHash();
			ObjectArrayList<PickerEntrySignal> entrySignals;
			
			if(direction == Directions.Long) {
				entrySignals = signals.getLongEntrySignals(symbolHash);
			} else {
				entrySignals = signals.getShortEntrySignals(symbolHash);
			}

			if(entrySignals !=null && !entrySignals.isEmpty()) {
				String symbol = Strategy.getSymbol();
				double posScore = SQUtils.round(Value.evaluateFormula(Strategy, symbol, 0, 0), 5);

				for(int i=0; i<entrySignals.size(); i++) {
					PickerEntrySignal entrySignal = entrySignals.get(i);
					entrySignal.posScore = posScore;
				}
			}
		}
	}

	//------------------------------------------------------------------------

	@Override
	protected void parseXml(Element elRule) throws BlockDefinitionException {
		//super.parseXml(elRule);
		
		if(Strategy.Stockpicker.isSingleAssetStrategy()) {
			return;
		}
		
		String ruleName = elRule.getAttributeValue("name");
		if(ruleName.equalsIgnoreCase("Position Score Long")) {
			direction = Directions.Long;
		} else if(ruleName.equalsIgnoreCase("Position Score Short")) {
			direction = Directions.Short;
		} else {
			throw new BlockDefinitionException(String.format("Invalid name of rule '%s'.", ruleName));
		}

		Element elValue = elRule.getChild("Value");
		
		for(Element elItem : elValue.getChildren("Item")) {
			if(elItem==null || !elItem.getAttributeValue("key").equals("AssignVariable")) {
				throw new BlockDefinitionException("Invalid StockpickerPS rule definition.");
			}
			
			List<Element> params = elItem.getChildren("Param");
			
			Element elParamType = params.get(0);
			Element elParamValue = params.get(1);
			
			String name = elParamType.getAttributeValue("name");
					
			if(name.equals("PositionScore")) {				
				String isFormula = elParamValue.getAttributeValue("isFormula");
				if(isFormula != null && isFormula.equals("true")) {
					Element elFormula = elParamValue.getChild("Formula");
					
					String formulaKey = elFormula.getAttributeValue("key");
					IFormula formula = Formulas.get(formulaKey);
	
					if(elFormula.getChild("Block").getChild("Item") == null) {
						break; //position score was not set
					}
					
					Value = formula.newFormulaInstance(Strategy, elFormula);
				}
			} else if(name.equals("MaxOpenPositions")) {
				String value = elParamValue.getText().trim();
				
				String isVariable = elParamValue.getAttributeValue("variable");
				if(isVariable != null && isVariable.equals("true")) {
					Variable existingVariable = Strategy.variables().getById(value);
					if(existingVariable!=null) {
						value = existingVariable.getValue();
					}
				}
				
				int maxPos = Integer.valueOf(value);
				if(direction == Directions.Long) {
					Strategy.Stockpicker.MaxOpenPositionsLong = maxPos;
				} else {
					Strategy.Stockpicker.MaxOpenPositionsShort = maxPos;
				}
				
				StockpickerOptions options = this.Strategy.getStockpickerOptions();
				if(options != null && !Strategy.isAlgoWizard) {
					if(direction == Directions.Long) {
						Strategy.Stockpicker.MaxOpenPositionsLong = options.getMaxOpenPositionsLong(Strategy.Stockpicker.MaxOpenPositionsLong);
					} else {
						Strategy.Stockpicker.MaxOpenPositionsShort = options.getMaxOpenPositionsShort(Strategy.Stockpicker.MaxOpenPositionsShort);
					}
				}
			}
		}
		
		if(Value==null) { //position score was not set, resets max positions to 0
			if(direction == Directions.Long) {
				Strategy.Stockpicker.MaxOpenPositionsLong = 0;
			} else {
				Strategy.Stockpicker.MaxOpenPositionsShort = 0;
			}
		}
	}

}