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
package SQ.Blocks.Functions;

import SQ.Internal.ValueBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(DATE) GetDate", display="GetDate(#Day#, #Month#, #Year#)", returnType = ReturnTypes.Number)
@Help("Returns day as YYYMMDD number, comparable with Bar Time or Current Time values")
@SortOrder(1200)
@IgnoreInBuilder
public class GetDate extends ValueBlock {

	@Parameter(minValue=1, maxValue=31, defaultValue="1", step=1)
	public int Day;
	
	@Parameter(minValue=1, maxValue=12, defaultValue="1", step=1)
	public int Month;

	@Parameter(minValue=1900, maxValue=2100, defaultValue="2016", step=1)
	public int Year;

	//------------------------------------------------------------------------
	
	@Override
	public double OnBlockEvaluate(int relativeShift) throws TradingException {
		return SQTime.getYmd(Year - 1900, Month, Day);
	}

}
