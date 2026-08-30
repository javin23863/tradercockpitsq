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
package SQ.Trading.Commissions;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@ClassConfig(name="Per trade ", display="$ #Commission# per trade")
@Help("<b>Per trade commission</b><br/>Applies commission to every trade entry and exit.")
public class PerTrade extends CommissionsMethod {

	@Parameter(defaultValue="0", minValue=-100000d, name="Commission", maxValue=100000d, step=1d, category="Default")
	@Help("Commission in $ per trade side")
	public double Commission;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public double computeCommissionsOnOpen(ILiveOrder order, double tickSize, double pointValue) throws Exception {
		// commission is in $
		return Commission;
	}
	//------------------------------------------------------------------------

	@Override
	public double computeCommissionsOnClose(ILiveOrder order, double tickSize, double pointValue) throws Exception {
		if(order.isPendingOrder()) {
			return 0;
		}

		// commission is in $
		return Commission;
	}
	
}
