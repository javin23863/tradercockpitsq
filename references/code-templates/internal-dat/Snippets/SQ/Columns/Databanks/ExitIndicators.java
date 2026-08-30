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
package SQ.Columns.Databanks;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.jdom2.Element;

import com.strategyquant.lib.L;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.ValueTypes;
import com.strategyquant.tradinglib.blocks.CustomBlocks;
import com.strategyquant.tradinglib.blocks.random.BlockDefinition;
import com.strategyquant.tradinglib.results.SpecialValues;

public class ExitIndicators extends DatabankColumn {
    
	public ExitIndicators() {
		super(L.tsq("Exit indicators"), DatabankColumn.Text, ValueTypes.Maximize, 0, 0, 100);

		setTooltip(L.tsq("Which indicators are used in exit conditions"));
		setWidth(100);
		printsSpecialValue(true);
	}
	
	//------------------------------------------------------------------------
	
	@Override
	public String getValue(ResultsGroup results, String resultKey, byte direction, byte plType, byte sampleType) throws Exception {		
		if(results.subResult(resultKey).containsKey(SpecialValues.ExitIndicators)) {
			return results.subResult(resultKey).getString(SpecialValues.ExitIndicators);
		}
		
		String exitIndys = recognizeExitIndicators(results);
		
		results.specialValues().setString(SpecialValues.ExitIndicators, exitIndys);	
		
		return exitIndys;	
	}
	
	//------------------------------------------------------------------------

	private String recognizeExitIndicators(ResultsGroup results) {
		try {
			Element elStrategy = results.getStrategyXml();
			if(elStrategy == null) {
				return NOT_AVAILABLE;
			}
			
			Element elOnBarUpdateEvent = getOnBarUpdateEvent(elStrategy);
			if(elOnBarUpdateEvent == null) {
				return NOT_AVAILABLE;
			}
			
			List<Element> rules = elOnBarUpdateEvent.getChildren("Rule");
			if(rules == null || rules.size() == 0) {
				return NOT_AVAILABLE;
			}
			
			ArrayList<String> indicators = new ArrayList<String>();
			
			String firstRuleType = rules.get(0).getAttributeValue("type");
			if(firstRuleType != null && firstRuleType.equals("Signal")) {
				// it is signal based strategy
				Element elSignals = rules.get(0).getChild("signals");
				if(elSignals == null) {
					return NOT_AVAILABLE;
				}
				
				List<Element> signals = elSignals.getChildren("signal");
				
				if(signals.size() >= 3) findIndysRecursive(signals.get(2), indicators); // signal for long exit
				if(signals.size() >= 4) findIndysRecursive(signals.get(3), indicators); // signal for short exit
				
			} else {
				// it is if-then strategy
				if(rules.size() >= 3) findIndysRecursive(rules.get(2), indicators); // it should be long exit rule
				if(rules.size() >= 4) findIndysRecursive(rules.get(3), indicators); // it should be short exit rule
			}
			
			findExitAfterBars(rules, indicators);
			
			return convertToString(indicators);
		
		} catch(Exception e) {
			return NOT_AVAILABLE;
		}
	}

	//------------------------------------------------------------------------

	private String convertToString(ArrayList<String> indicators) {
		TreeSet<String> indysSet = new TreeSet<String>();
		
		for(int i=0; i<indicators.size(); i++) {
			indysSet.add(indicators.get(i));
		}
		
		return String.join(",", indysSet);
	}

	//------------------------------------------------------------------------

	private void findIndysRecursive(Element element, ArrayList<String> indicators) {
		ArrayList<Element> blocks = XMLUtil.getNestedElements(element, "Item");
		if(blocks == null || blocks.size() == 0) {
			return;
		}

		for(int i=0; i<blocks.size(); i++) {
			Element elBlock = blocks.get(i);
			
			String key = elBlock.getAttributeValue("key");
			
			//custom blocks handling
			
			if(key != null && key.startsWith("CBlock_")) {
				BlockDefinition customBlock = CustomBlocks.getBlock(key);
				if(customBlock != null) {
					indicators.add(customBlock.name);
				}
				else {
					indicators.add("Unknown custom block");
				}
				continue;
			}
			
			String categoryType = elBlock.getAttributeValue("categoryType");
			if(categoryType == null) continue;

			String rootIndyName = elBlock.getAttributeValue("mI");
			
			if(categoryType.equals("simpleRules")) {
				if(rootIndyName != null && !rootIndyName.equals("") && !rootIndyName.equals("StrategyControl")) {
					indicators.add(rootIndyName);
				}
				
			} else if(categoryType.equals("indicator")) {
				indicators.add(key);

			} else if(categoryType.equals("priceValue")) {
				indicators.add(key);

			} else if(categoryType.equals("priceRange")) {
				indicators.add(key);
			}
			else if(categoryType.equals("other") && rootIndyName != null && rootIndyName.equals("BarAndTime")) {
				indicators.add(key);
			}
		}
	}

	//------------------------------------------------------------------------

	private void findExitAfterBars(List<Element> rules, ArrayList<String> indicators) {
		for(Element rule : rules) {
			findExitAfterBarsRecursive(rule, indicators);
		}
	}
	
	//------------------------------------------------------------------------

	private void findExitAfterBarsRecursive(Element element, ArrayList<String> indicators) {
		ArrayList<Element> blocks = XMLUtil.getNestedElements(element, "Item");
		if(blocks == null || blocks.size() == 0) {
			return;
		}

		for(int i=0; i<blocks.size(); i++) {
			Element elBlock = blocks.get(i);
			
			String key = elBlock.getAttributeValue("key");
			
			//custom blocks handling
			
			if(key != null && (key.startsWith("EnterAt") || key.startsWith("EnterReverseAt"))) {

				Element elExitAfterXBars = XMLUtil.getItemParameterNoException(elBlock, "#ExitAfterBars.ExitAfterBars#");
				if(elExitAfterXBars != null) {
					String value = elExitAfterXBars.getText();
					if(value != null && !value.equals("0")) {
						// this order uses Exit after X bars
						indicators.add("ExitAfterXBars");
					}
				}
			}
		}
	}

	//------------------------------------------------------------------------

	private Element getOnBarUpdateEvent(Element elStrategyXml) {
		Element elStrategy = elStrategyXml.getChild("Strategy");
		if(elStrategy == null) return null;
		
		Element elRules = elStrategy.getChild("Rules");
		if(elRules == null) return null;
		
		Element elEvents = elRules.getChild("Events");
		if(elEvents == null) return null;

		List<Element> events = elEvents.getChildren("Event");
		if(events == null || events.size() == 0)  return null;
		
		for(int i=0; i<events.size(); i++) {
			Element elEvent = events.get(i);
			
			String key = elEvent.getAttributeValue("key");
			if(key.equals("OnBarUpdate")) {
				return elEvent;
			}
		}
		
		return null;
	}
	
}