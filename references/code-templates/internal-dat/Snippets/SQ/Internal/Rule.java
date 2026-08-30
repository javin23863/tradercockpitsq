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

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;

import org.jdom2.Element;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.ParametersHelper;
import com.strategyquant.tradinglib.debug.Debugger;
import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.Blocks;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.strategy.xml.XmlStrategyException;

import SQ.Blocks.Comparisons.AND;

/**
 * The Class Rule.
 */
abstract public class Rule extends Debugger {

	/** The Strategy. */
	public XmlStrategy Strategy = null;
	
	/** The blocks map. */
	private HashMap<String, ArrayList<IBlock>> blocksMap = new HashMap<String, ArrayList<IBlock>>();
	
	/** The bar event type. */
	protected int barEventType;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	/**
	 * Evaluate rule.
	 *
	 * @param updateEventType the update event type
	 * @param evaluator the evaluator
	 * @throws Exception the exception
	 */
	abstract public void evaluateRule(int updateEventType, ITradingOptionsEvaluator evaluator, String event) throws Exception;

	//------------------------------------------------------------------------
	
	/**
	 * Evaluate actions.
	 *
	 * @param rulePart the rule part
	 * @return the double
	 * @throws TradingException the trading exception
	 */
	protected double evaluateActions(String rulePart) throws TradingException {

		double returnVal = 0;
		
		if(blocksMap.containsKey(rulePart)) {
			for(IBlock block : blocksMap.get(rulePart)) {
				if(block instanceof ActionBlock) {
					ActionBlock ab = (ActionBlock) block;
					
					ab.OnAction();
				}
			}
		}

		return returnVal;
	}
	
	//------------------------------------------------------------------------

	/**
	 * Gets the blocks.
	 *
	 * @param rulePart the rule part
	 * @return the blocks
	 */
	protected ArrayList<IBlock> getBlocks(String rulePart) {
		if(blocksMap.containsKey(rulePart)) {
			return blocksMap.get(rulePart);
		}
		
		return null;
	}
	
	//------------------------------------------------------------------------

	/**
	 * New instance.
	 *
	 * @param strategy the strategy
	 * @param elRule the el rule
	 * @return the rule
	 * @throws XmlStrategyException the xml strategy exception
	 */
	public Rule newInstance(XmlStrategy strategy, Element elRule) throws XmlStrategyException {
		try {
			Constructor<? extends Rule> constructor = null;

			constructor = this.getClass().getConstructor();

			Rule rule =  SQUtils.invokeUnchecked(constructor);

			rule.initialize(strategy, elRule);
			
			return rule;

		} catch (NoSuchMethodException | SecurityException e) {
			throw new XmlStrategyException("Exception cloning block! "+e.getMessage(), e);
		}
	}
	
/*
	public Rule newInstanceOld(XmlStrategy strategy, Element elRule) throws XmlStrategyException {
		Rule rule = SQUtils.deepClone(this);
		
		rule.initialize(strategy, elRule);
		
		return rule;
	}
*/
	//------------------------------------------------------------------------

	/**
	 * Initialize.
	 *
	 * @param strategy the strategy
	 * @param elRule the el rule
	 * @throws XmlStrategyException the xml strategy exception
	 */
	private void initialize(XmlStrategy strategy, Element elRule) throws XmlStrategyException {
		this.Strategy = (XmlStrategy) strategy;
		
		this.barEventType = Strategy.getBarEventType();

		String ruleName = "";
		
		try {
			ruleName = XMLUtil.getStringAttr(elRule, "name", "???");
			
			parseXml(elRule);
		} catch (BlockDefinitionException e) {
			throw new XmlStrategyException(String.format("Cannot create strategy from XML! Error while parsing rule '%s' - %s ", ruleName, e.getMessage()), e);
		}
	}

	//------------------------------------------------------------------------

	/**
	 * Deinitialize.
	 */
	public void deinitialize() {
		if(blocksMap != null && !blocksMap.isEmpty()) {
			for(String key : blocksMap.keySet()) {
				ArrayList<IBlock> blocks = blocksMap.get(key);
				
				for(int i=0; i<blocks.size(); i++) {
					IBlock block = blocks.get(i);
					if(block instanceof StandardBlock) {
						((StandardBlock) block).deinitialize();
					}
				}
			}
		}
	}

	//------------------------------------------------------------------------

	/**
	 * Parses the xml.
	 *
	 * @param elRule the el rule
	 * @throws BlockDefinitionException the block definition exception
	 */
	protected void parseXml(Element elRule) throws BlockDefinitionException {
		for(Element elRulePart : elRule.getChildren()) {
			if(elRulePart.getName().contains("Description")) {
				continue;
			}
			
			parseBlocksInRulePart(elRulePart);
		}
	}

	//------------------------------------------------------------------------

	/**
	 * Parses the blocks in rule part.
	 *
	 * @param elRulePart the el rule part
	 * @throws BlockDefinitionException the block definition exception
	 */
	private void parseBlocksInRulePart(Element elRulePart) throws BlockDefinitionException {
		ArrayList<IBlock> blocks = new ArrayList<IBlock>();
		
		for(Element elBlock : elRulePart.getChildren()) {
			if(!elBlock.getName().equals("Item")) {
				throw new BlockDefinitionException("Block has an unallowed name '"+elBlock.getName()+"'");
			}
			
			IBlock block = Blocks.getBlockObject(elBlock.getAttributeValue("key"), Strategy, elBlock);
			
			BuildingBlock bb = block.getClass().getAnnotation(BuildingBlock.class);
			if(bb != null) {
				block.setReturnType(bb.returnType());
			}
			
			blocks.add(block);
		}
		
		blocksMap.put(elRulePart.getName(), blocks);
	}	

	//------------------------------------------------------------------------

	/**
	 * Checks for is bar open condition.
	 *
	 * @param block the block
	 * @return true, if successful
	 */
	protected boolean hasIsBarOpenCondition(IBlock block) {
		String blockName = block.getClass().getSimpleName();
		
		if(blockName.equals("IsBarOpen")) {
			return true;
		}
		
		if(blockName.equals("AND")) {
			AND andBlock = (AND) block;
			
			for(IBlock subBlock : andBlock.Children) {
				if(hasIsBarOpenCondition(subBlock)) {
					return true;
				}
			}
			
			return false;
		} else {
			return false;
		}
	}	
}
