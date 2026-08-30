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

import java.lang.reflect.Field;

import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;


/**
 * The Class ActionBlock.
 */
abstract public class ActionBlock extends StandardBlock {
	
	/** The Constant Log. */
	public static final Logger Log = LoggerFactory.getLogger("ValueBlock");
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	/**
	 * On action.
	 *
	 * @throws TradingException the trading exception
	 */
	abstract public void OnAction() throws TradingException;

	//------------------------------------------------------------------------

	/**
	 * Evaluate block.
	 *
	 * @return the double
	 * @throws TradingException the trading exception
	 */
	public double evaluateBlock() throws TradingException {
		throw new TradingException("This shouldn't be called!");
	}

	//------------------------------------------------------------------------

	/**
	 * Evaluate block.
	 *
	 * @param relativeShift the relative shift
	 * @return the double
	 * @throws TradingException the trading exception
	 */
	public double evaluateBlock(int relativeShift) throws TradingException {
		throw new TradingException("This shouldn't be called!");
	}

	
	//------------------------------------------------------------------------

	/**
	 * Initialize.
	 *
	 * @param strategy the strategy
	 * @param elBlock the el block
	 * @throws BlockDefinitionException the block definition exception
	 */
	protected void initialize(StrategyBase strategy, Element elBlock) throws BlockDefinitionException {
		super.initialize(strategy, elBlock);
		
		// initialize also ExitMethod field - if null, set it to empty array
		Class<?> aClass = this.getClass();
			
		for(Field field : aClass.getFields()) {
			if(field.getType().toString().equals("class [Lcom.strategyquant.tradinglib.ExitMethod;")) {
				
				try {
					ExitMethod[] exitMethods = (ExitMethod[]) field.get(this);
					
					if(exitMethods == null) {
						exitMethods = new ExitMethod[0];
						
						field.set(this, exitMethods);
					}
					
				} catch (IllegalArgumentException | IllegalAccessException e) {
					e.printStackTrace();
					Log.error("Error initializing ActionBlock", e);
				}
			}
			
		}
	}

	//------------------------------------------------------------------------

	/**
	 * On apply exits.
	 *
	 * @throws TradingException the trading exception
	 */
	public void OnApplyExits() throws TradingException {
	}	
}
