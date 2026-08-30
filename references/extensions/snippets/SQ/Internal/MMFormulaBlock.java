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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.StrategyBase;

/**
 * The Class MMFormulaBlock.
 */
abstract public class MMFormulaBlock extends FormulaBlock {
	
	/** The Constant Log. */
	public static final Logger Log = LoggerFactory.getLogger("MMFormulaBlock");
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	/**
	 * Compute size.
	 *
	 * @param strategy the strategy
	 * @param symbol the symbol
	 * @param orderType the order type
	 * @param price the price
	 * @param sl the sl
	 * @return the double
	 * @throws TradingException the trading exception
	 */
	abstract public double computeSize(StrategyBase strategy, String symbol, byte orderType, double price, double sl) throws TradingException;

	//------------------------------------------------------------------------

	/**
	 * Evaluate formula.
	 *
	 * @param strategy the strategy
	 * @param symbol the symbol
	 * @param price the price
	 * @param direction the direction
	 * @return the double
	 * @throws TradingException the trading exception
	 */
	@Override
	public double evaluateFormula(StrategyBase strategy, String symbol, double price, int direction) throws TradingException {
		throw new TradingException("This method shouldn't be called!");
	}
	

	
}
