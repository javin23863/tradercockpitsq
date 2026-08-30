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
package SQ.Internal.RulesImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jdom2.Element;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.datalib.UpdateEventTypes;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.time.SQTimeOld;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.Blocks;
import com.strategyquant.tradinglib.Variable;

import SQ.Internal.Rule;
import SQ.Internal.ITradingOptionsEvaluator;

public class SignalFuzzy extends Rule {

	private IBlock[][] signals = null;
	private Variable[] signalVariables = null;
	private int[] minTrue = null;
	boolean everyTick = true;
	
	private static ArrayList<IBlock> falseBoolArr = new ArrayList<IBlock>(Arrays.asList(new SQ.Blocks.Other.Boolean(false)));
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	public void evaluateRule(int updateEventType, ITradingOptionsEvaluator evaluator, String event) throws TradingException {
		if(signals == null) {
			return;
		}
		
		if(everyTick == false  && updateEventType != barEventType && !event.equals("OnInit") && !event.equals("OnDeinit")) {
			// to speed up evaluation skip the rule if it should be evaluated only once in a bar
			for(int i=0; i<signals.length; i++) {
				signalVariables[i].setValue(false);
			}
			
			return;
		}
		/*			
		String s = SQTime.toDateMinuteString(Strategy.Time(0));
		if(s.contains("2008.08.12 02:00")) {
			int a = 1;
			int b = a;
		}
		*/
		for(int i=0; i<signals.length; i++) {
			Variable var = signalVariables[i];
			IBlock[] conditions = signals[i];

			if(conditions == null) {
				continue;
			}

			int minTrueConditions = minTrue[i];
			if(minTrueConditions <= 0) {
				minTrueConditions = 1;
			}
			
			if(var == null || conditions == null) {
				continue;
			}
			
			boolean signalValue = false;
			int trueConditionsCount = 0;
			
			for(int j=0; j<conditions.length; j++) {
				IBlock condition = conditions[j];
				
				boolean value = (condition.evaluateBlock() > 0 ? true : false);
				
				if(value) {
					trueConditionsCount++;
				}
				
				if(trueConditionsCount >= minTrueConditions) {
					signalValue = true;
					break;
				}
			}
			
			var.setValue(signalValue);
		}
		
		int a = 1;
		int b = a;
	}

	//------------------------------------------------------------------------

	@Override
	protected void parseXml(Element elRule) throws BlockDefinitionException {
		ArrayList<ArrayList<IBlock>> alSignals = new ArrayList<ArrayList<IBlock>>();
		ArrayList<Variable> alSignalVariables = new ArrayList<Variable>();
		ArrayList<Integer> alMinTrue = new ArrayList<Integer>();

		List<Element> children = elRule.getChildren();
		for(int i=0; i<children.size(); i++) {
			Element elRulePart = children.get(i);
			if(elRulePart.getName().contains("Description")) {
				continue;
			}
				
			if(elRulePart.getName().contains("signals")) {
				for(Element elSignal : elRulePart.getChildren()) {
					parseSignal(elSignal, alSignals, alSignalVariables, alMinTrue);
				}
			}
		}
		
		String attrEveryTick = elRule.getAttributeValue("everyTick");
		if(attrEveryTick == null || !attrEveryTick.equals("false")) {
			everyTick = true;
			
			//if(condition != null) {
			//	everyTick = !hasIsBarOpenCondition(condition);
			//}
			
		} else {
			everyTick = false;
		}
		
		// convert to primitive arrays
		if(alSignals.size() > 0) {
			signals = new IBlock[alSignals.size()][];
			signalVariables = new Variable[alSignals.size()];
			minTrue = new int[alSignals.size()];

			for(int i=0; i<alSignals.size(); i++) {
				ArrayList<IBlock> conditions = alSignals.get(i);
				if(conditions != null && conditions.size() > 0) {
					signals[i] = new IBlock[conditions.size()];
				
					for(int j=0; j<conditions.size(); j++) {
						signals[i][j] = conditions.get(j);
					}
				}
				
				signalVariables[i] = alSignalVariables.get(i);
				minTrue[i] = alMinTrue.get(i);
			}
		}
		
		alSignals.clear();
		alSignalVariables.clear();		
		alMinTrue.clear();		
	}

	//------------------------------------------------------------------------

	private void parseSignal(Element elSignal, ArrayList<ArrayList<IBlock>> alSignals, ArrayList<Variable> alSignalVariables, ArrayList<Integer> alMinTrue) throws BlockDefinitionException {
		String signalVarId = elSignal.getAttributeValue("variable");

		Variable signalVar = null;
		
		if(Strategy == null) {
			throw new BlockDefinitionException("Signal rule cannot get strategy!");
		}
		
		signalVar = Strategy.variables().getById(signalVarId);
		if(signalVar == null) {
			throw new BlockDefinitionException("Signal variable is not set!");
		}


		List<Element> signalBlocks = elSignal.getChildren();

		alSignalVariables.add(signalVar);

		if(signalBlocks.size() == 0) {
			alSignals.add(falseBoolArr);
			alMinTrue.add(1);
			
		} else {
		
			ArrayList<IBlock> blocksInSignal = new ArrayList<IBlock>();

			for(int i=0; i<signalBlocks.size(); i++) {
				Element elBlock = signalBlocks.get(i);
				if(!elBlock.getName().equals("Item")) {
					throw new BlockDefinitionException("Block has an unallowed name '"+elBlock.getName()+"'");
				}

				IBlock block = Blocks.getBlockObject(elBlock.getAttributeValue("key"), Strategy, elBlock);

				blocksInSignal.add(block);		
			}
			
			// maximum pct of false conditions in a signal  
			int minTrueAttr = Integer.parseInt(elSignal.getAttributeValue("minTrue"));
			
			double pct = 0.01 * ((double) minTrueAttr);
			double tmp = ((double) signalBlocks.size()) * pct;
			
			int minTrueConditions = (int) Math.round(tmp);

			alMinTrue.add(minTrueConditions);
			alSignals.add(blocksInSignal);
		}
		
	}

}
