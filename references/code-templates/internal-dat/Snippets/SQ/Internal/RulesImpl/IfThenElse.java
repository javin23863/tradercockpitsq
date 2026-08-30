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

import com.strategyquant.datalib.TradingException;
import com.strategyquant.datalib.UpdateEventTypes;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.ReturnTypes;

public class IfThenElse extends Rule {

	private IBlock condition = null;
	private ArrayList<ActionBlock> actionsIf = null;
	private ArrayList<ActionBlock> actionsElse = null;
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
		
		double ifResult = 0;
		
		if(condition != null) { 
			ifResult = condition.evaluateBlock();
		}
			
		if(ifResult > 0) {
			if(actionsIf != null) {
				for(int i=0; i<actionsIf.size(); i++) {
					if(actionsIf.get(i).getReturnType() != ReturnTypes.Order || evaluator.continueBarUpdate()){
						actionsIf.get(i).OnAction();
					}
				}
			}
			
		} else {
			if(actionsElse != null) {
				for(int i=0; i<actionsElse.size(); i++) {
					if(actionsElse.get(i).getReturnType() != ReturnTypes.Order || evaluator.continueBarUpdate()){
						actionsElse.get(i).OnAction();
					}
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
			actionsIf = new ArrayList<ActionBlock>();
			
			for(IBlock block : blocks) {
				if(!(block instanceof ActionBlock)) {
					throw new BlockDefinitionException(String.format("Block '%' in THEN part is not an ActionBlock!", block.getClass().getSimpleName()));
				}
				
				actionsIf.add((ActionBlock) block);
			}
		}
		
		// get parsed ELSE part
		blocks = getBlocks("Else");
		if(blocks != null && blocks.size() != 0) {
			actionsElse = new ArrayList<ActionBlock>();
			
			for(int i=0; i<blocks.size(); i++) {
				IBlock block = blocks.get(i);

				if(!(block instanceof ActionBlock)) {
					throw new BlockDefinitionException(String.format("Block '%' in ELSE part is not an ActionBlock!", block.getClass().getSimpleName()));
				}
				
				actionsElse.add((ActionBlock) block);
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
