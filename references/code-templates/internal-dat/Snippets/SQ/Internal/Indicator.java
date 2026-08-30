/**
 * 
 */
/*
 * Copyright (c) 2017-2018, StrategyQuant - All rights reserved.
 *
 * Code in this file was made in a good faith that it is correct and does what it should.
 * If you found a bug in this code OR you have an improvement suggestion OR you want to include
 * your own code snippet into our standard library please contact us at:
 * http://tasks.strategyquant.com/projects/snippets/
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

import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.indicator.IndicatorBase;
import com.strategyquant.tradinglib.indicator.IndicatorsObj;

/**
 * The Class Indicator.
 */
abstract public class Indicator extends IndicatorBase {

	/** The Indicators. */
	protected Indicators Indicators;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	/**
	 * Initialize.
	 *
	 * @param indicatorsObj the indicators obj
	 * @param hasZeroShift the has zero shift
	 * @throws TradingException the trading exception
	 */
	public void initialize(IndicatorsObj indicatorsObj, boolean hasZeroShift) throws TradingException {
		this.Indicators = (Indicators) indicatorsObj;
		
		recognizeAndInitializeDataSeries(hasZeroShift);
		
		callOnInit();
	}
	
}
