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

public class ActionOnly extends Rule {

	private ArrayList<ActionBlock> actions = null;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	public void evaluateRule(int updateEventType, ITradingOptionsEvaluator evaluator, String event) throws Exception {
		if(actions != null) {
			for(int i=0; i<actions.size(); i++) {
				if(actions.get(i).getReturnType() != ReturnTypes.Order || evaluator.continueBarUpdate()){
					actions.get(i).OnAction();
				}
			}
		}
	}

	//------------------------------------------------------------------------

	@Override
	protected void parseXml(Element elRule) throws BlockDefinitionException {
		super.parseXml(elRule);

		ArrayList<IBlock> blocks = null;
		blocks = getBlocks("Then");
		if(blocks == null) {
			blocks = getBlocks("Actions");
		}
		
		if(blocks != null && blocks.size() != 0) {
			actions = new ArrayList<ActionBlock>();
			
			for(IBlock block : blocks) {
				if(!(block instanceof ActionBlock)) {
					throw new BlockDefinitionException(String.format("Block '%' in THEN part is not an ActionBlock!", block.getClass().getSimpleName()));
				}
				
				actions.add((ActionBlock) block);
			}
		}
	}

}
