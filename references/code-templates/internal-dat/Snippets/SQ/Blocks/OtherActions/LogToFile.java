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
package SQ.Blocks.OtherActions;

import SQ.Internal.ActionBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(returnType = ReturnTypes.Action)
@Help("Log message to a given file. The file will be placed in {MT4}/tester/files (for backtest) or in {MT4}/experts/files (for real trading)")
@SortOrder(100)
@CategoryOrder(500)
@IgnoreInBuilder
@ForEngine("*,-MC,-TS,-SP,-SA")
public class LogToFile extends ActionBlock {
	
	@Parameter(defaultValue="some_logfile.txt")
	@Editor(type=Editors.Editbox)
	public String Filename;

	@Parameter(defaultValue="some message")
	@Editor(type=Editors.Editbox)
	public String Message;
	
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	public void OnAction() throws TradingException {
		// this is only a placeholder for Wizard, it is not used in StrategyQuant
	}
}
