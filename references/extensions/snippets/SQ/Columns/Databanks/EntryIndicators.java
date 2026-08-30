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
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.ValueTypes;
import com.strategyquant.tradinglib.blocks.CustomBlocks;
import com.strategyquant.tradinglib.blocks.random.BlockDefinition;
import com.strategyquant.tradinglib.results.SpecialValues;

public class EntryIndicators extends DatabankColumn {
    
	public EntryIndicators() {
		super(L.tsq("Entry indicators"), DatabankColumn.Text, ValueTypes.Maximize, 0, 0, 100);

		setTooltip(L.tsq("Which indicators are used in entry conditions"));
		setWidth(150);
		printsSpecialValue(true);
	}
	
	//------------------------------------------------------------------------
	
	@Override
	public String getValue(ResultsGroup results, String resultKey, byte direction, byte plType, byte sampleType) throws Exception {		
		if(results.specialValues().containsKey(SpecialValues.EntryIndicators)) {
			return results.specialValues().getString(SpecialValues.EntryIndicators);
		}
		
		String entryIndys = recognizeEntryIndicators(results);
		
		results.specialValues().setString(SpecialValues.EntryIndicators, entryIndys);	
		
		return entryIndys;
	}

	//------------------------------------------------------------------------

	private String recognizeEntryIndicators(ResultsGroup results) {
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
				
				if(signals.size() >= 1) findIndysRecursive(signals.get(0), indicators, false); // signal for long entry
				if(signals.size() >= 2) findIndysRecursive(signals.get(1), indicators, false); // signal for short entry
				
			} else {
				// it is if-then strategy
				findIndysRecursive(rules.get(0), indicators, false); // it should be long entry rule
				if(rules.size() > 1) findIndysRecursive(rules.get(1), indicators, false); // it should be short entry rule
			}
			
			return convertToString(indicators);
		
		} catch(Exception e) {
			return NOT_AVAILABLE;
		}
	}

	//------------------------------------------------------------------------

	private String convertToString(ArrayList<String> indicators) {
		TreeSet<String> indysSet = new TreeSet<String>();
		
		final String alwaysTrueName = "AlwaysTrue";
		boolean wasAlwaysTrue = false;
		
		for(int i=0; i<indicators.size(); i++) {
			String indicator = indicators.get(i);
			
			if(indicator != null) {
				if(indicator.equals(alwaysTrueName)) {
					wasAlwaysTrue = true;
					continue;
				}
				
				indysSet.add(indicator);
			}
		}
		
		if(indysSet.size() == 0 && wasAlwaysTrue) {
			indysSet.add(alwaysTrueName);
		}
		
		return String.join(",", indysSet);
	}

	//------------------------------------------------------------------------

	private void findIndysRecursive(Element element, ArrayList<String> indicators, boolean inPrice) {
		List<Element> blocks = element.getChildren();
		if(blocks == null || blocks.size() == 0) {
			return;
		}

		for(int i=0; i<blocks.size(); i++) {
			Element elBlock = blocks.get(i);
			
			boolean blockInPrice = inPrice;
			
			if(elBlock.getName().equals("Param")) {
				String key = elBlock.getAttributeValue("key");
				if(key != null && key.equals("#Price#")) {
					blockInPrice = true;
				}
			} 
			else if(elBlock.getName().equals("Item")) {
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
				if(categoryType != null) {

					String rootIndyName = elBlock.getAttributeValue("mI");
					String IndyName = elBlock.getAttributeValue("name"); 
					
					if(categoryType.equals("simpleRules")) {
						if(rootIndyName != null && !rootIndyName.equals("") && !rootIndyName.equals("StrategyControl")) {
							if(!inPrice) {
								if(rootIndyName.equals("BarAndTime")) {
									indicators.add(key);
								} else {
									indicators.add(IndyName);
								}
							}
						}

					} else if(categoryType.equals("indicator")) {
						if(!inPrice) {
							indicators.add(key);
						}

					} else if(categoryType.equals("priceValue")) {
						if(!inPrice) {
							indicators.add(key);
						}

					} else if(categoryType.equals("priceRange")) {
						if(!inPrice) {
							indicators.add(key);
						}
					}
					else if(categoryType.equals("other") && rootIndyName != null && rootIndyName.equals("BarAndTime")) {
						if(!inPrice) {
							indicators.add(key);
						}
					}
				}
			}
			
			findIndysRecursive(elBlock, indicators, blockInPrice);
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