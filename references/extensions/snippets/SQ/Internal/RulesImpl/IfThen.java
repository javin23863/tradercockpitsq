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

import org.jdom2.Element;

import SQ.Blocks.Comparisons.AND;
import SQ.Internal.ActionBlock;
import SQ.Internal.Rule;
import SQ.Internal.ITradingOptionsEvaluator;

import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.datalib.UpdateEventTypes;
import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.ReturnTypes;

public class IfThen extends Rule {

	private IBlock condition = null;
	private ArrayList<ActionBlock> actions = null;
	boolean everyTick = true;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	public void evaluateRule(int updateEventType, ITradingOptionsEvaluator evaluator, String event) throws Exception {
		if(everyTick == false  && updateEventType != barEventType && !event.equals("OnInit") && !event.equals("OnDeinit")) {
			// to speed up evaluation skip the rule if it should be evaluated only once in a bar
			return;
		}
/*		
		String s = SQTime.toDateMinuteString(Strategy.Time(0));
		if(s.contains("2011.04.18 15:00")) {
			int a = 1;
			int b = a;
		}
*/		
		double ifResult = 0;
		
		if(condition != null) {
			
			ifResult = condition.evaluateBlock();
		}
			
		if(ifResult > 0) {
/*			
			DataSeries o = Strategy.Open;
			DataSeries c = Strategy.Close;
			String sTime = SQTime.toDateMinuteString(Strategy.Time(1));
			System.out.println("t[1] : " + sTime);
			System.out.println("o[1] < c[1] : " + o.get(1) + " < " +c.get(1));
			System.out.println("o[2] < c[2] : " + o.get(2) + " < " +c.get(2));
			System.out.println("o[3] > c[3] : " + o.get(3) + " > " +c.get(3));
			System.out.println("o[4] > c[4] : " + o.get(4) + " > " +c.get(4));
			System.out.println("o[5] < c[5] : " + o.get(5) + " < " +c.get(5));
*/			
			//}

			if(actions != null) {
				for(int i=0; i<actions.size(); i++) {
					ActionBlock action = actions.get(i);
					
					if(action.getReturnType() != ReturnTypes.Order || evaluator.continueBarUpdate()){
						action.OnAction();
					}
				}
			}
		}
		
		if(Strategy.getApplyExitsAtTheEndOfRule()) {
			applyExits();
		}
	}

	//------------------------------------------------------------------------

	private void applyExits() throws TradingException {
		if(actions != null) {
			for(int i=0; i<actions.size(); i++) {
				if(actions.get(i).getReturnType() == ReturnTypes.Order){
					actions.get(i).OnApplyExits();
				}
			}
		}
	}

	//------------------------------------------------------------------------

	@Override
	protected void parseXml(Element elRule) throws BlockDefinitionException {
		super.parseXml(elRule);

		// get parsed IF part
		ArrayList<IBlock> blocks = getBlocks("If");
		
		if(blocks.size() > 1) {
			throw new BlockDefinitionException("IF part cannot have more than one sub-block!");
		}
		
		if(blocks != null && blocks.size() != 0) {
			condition = blocks.get(0);
		}
		
		// get parsed THEN part
		blocks = getBlocks("Then");
		if(blocks != null && blocks.size() != 0) {
			actions = new ArrayList<ActionBlock>();
			
			for(int i=0; i<blocks.size(); i++) {
				IBlock block = blocks.get(i);

				if(!(block instanceof ActionBlock)) {
					throw new BlockDefinitionException(String.format("Block '%s' in THEN part is not an ActionBlock!", block.getClass().getSimpleName()));
				}
				
				actions.add((ActionBlock) block);
			}
		}
		
		
		String attrEveryTick = elRule.getAttributeValue("everyTick");
		if(attrEveryTick == null || !attrEveryTick.equals("false")) {
			everyTick = true;
			
			if(condition != null) {
				everyTick = !hasIsBarOpenCondition(condition);
			}
			
		} else {
			everyTick = false;
		}
	}

}
