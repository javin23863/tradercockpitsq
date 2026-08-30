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
import java.util.List;

import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.Blocks;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.Variable;

import SQ.Blocks.Indicators.ATR.ATR;
import SQ.Internal.ITradingOptionsEvaluator;
import SQ.Internal.Rule;

public class Signal extends Rule {
	public static final Logger Log = LoggerFactory.getLogger("Signal");
	
	private IBlock[] signals = null;
	private Variable[] signalVariables = null;
	boolean everyTick = true;
	private static SQ.Blocks.Other.Boolean falseBool = new SQ.Blocks.Other.Boolean(false);
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	public void evaluateRule(int updateEventType, ITradingOptionsEvaluator evaluator, String event) throws TradingException {
		if(signals == null) {
			return;
		}
		
		if(everyTick == false  && updateEventType != barEventType) { 
			// to speed up evaluation skip the rule if it should be evaluated only once in a bar
			for(int i=0; i<signals.length; i++) {
				signalVariables[i].setValue(false);
			}
			
			return;
		}

/*
		String stime = SQTime.toDateMinuteString(Strategy.Time(1));
		String stimeD = SQTime.toDateMinuteString(Strategy.MarketData.Chart(1).Time(1));
		if(stime.contains("2020.03.04 ")) {
			double close = Strategy.MarketData.Chart(0).Close(1);
			double closeD = Strategy.MarketData.Chart(1).Close(1);
			
			//Log.info("{} - Close[1]: {}", stime, Strategy.MarketData.Chart(0).Close(1),	SQTime.toDateMinuteString(Strategy.MarketData.Chart(1).Time(0)));
			int a = 1;
			int b = a;
		}
*/		
/*	
		MACD indicator = Strategy.Indicators.MACD(Strategy.MarketData.Chart(0).Close, 8, 52, 16);
		double main = indicator.Main.getRounded(0);
		double sig = indicator.Signal.getRounded(0);

		String stime2 = SQTime.toDateMinuteString(Strategy.Time(0));
		Log.info("{}, MACD: {}, Signal: {}", stime, main, sig);
*/		
/*		
		String s = SQTime.toDateMinuteString(Strategy.Time(0));
		if(s.contains("2012.12.25 15:00")) {
			int a = 1;
			int b = a;
		}
*/		
		for(int i=0; i<signals.length; i++) {
			Variable var = signalVariables[i];
			IBlock condition = signals[i];

			if(var == null || condition == null) {
				continue;
			}
			
			boolean value = (condition.evaluateBlock() > 0 ? true : false);
			
			var.setValue(value);
		}
	}

	//------------------------------------------------------------------------

	@Override
	protected void parseXml(Element elRule) throws BlockDefinitionException {
		ArrayList<IBlock> alSignals = new ArrayList<IBlock>();
		ArrayList<Variable> alSignalVariables = new ArrayList<Variable>();

		List<Element> children = elRule.getChildren();
		for(int i=0; i<children.size(); i++) {
			Element elRulePart = children.get(i);
			
			if(elRulePart.getName().contains("Description")) {
				continue;
			}
				
			if(elRulePart.getName().contains("signals")) {
				for(Element elSignal : elRulePart.getChildren()) {
					parseSignal(elSignal, alSignals, alSignalVariables);
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
			signals = new IBlock[alSignals.size()];
			signalVariables = new Variable[alSignals.size()];
			
			for(int i=0; i<alSignals.size(); i++) {
				signals[i] = alSignals.get(i);
				signalVariables[i] = alSignalVariables.get(i);
			}
		}
		
		alSignals.clear();
		alSignalVariables.clear();
	}

	//------------------------------------------------------------------------

	private void parseSignal(Element elSignal, ArrayList<IBlock> signals, ArrayList<Variable> signalVariables) throws BlockDefinitionException {
		String signalVarId = elSignal.getAttributeValue("variable");
		Variable signalVar = null;
		falseBool.Value = false;
		
		if(Strategy == null) {
			throw new BlockDefinitionException("Signal rule cannot get strategy!");
		}
		
		signalVar = Strategy.variables().getById(signalVarId);
		if(signalVar == null) {
			throw new BlockDefinitionException("Signal variable is not set!");
		}

		List<Element> signalBlocks = elSignal.getChildren();
		if(signalBlocks.size() > 1) {
			throw new BlockDefinitionException("Signal cannot have more than one child block!");
		}
		
		if(signalBlocks.size() == 0) {
			signalVariables.add(signalVar);
			signals.add(falseBool);
			return;
		}

		Element elBlock = signalBlocks.get(0);
		if(!elBlock.getName().equals("Item")) {
			throw new BlockDefinitionException("Block has an unallowed name '"+elBlock.getName()+"'");
		}
			
		IBlock block = Blocks.getBlockObject(elBlock.getAttributeValue("key"), Strategy, elBlock);
		
		signalVariables.add(signalVar);
		signals.add(block);
	}

}
