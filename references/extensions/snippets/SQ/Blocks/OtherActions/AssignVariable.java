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

@BuildingBlock(name="(VAR) Assign variable", display="Assign variable", returnType = ReturnTypes.Action) 
@Help("Assigns a value to the variable")
@SortOrder(100)
@CategoryOrder(400)
@IgnoreInBuilder
public class AssignVariable extends ActionBlock {
	
	@Parameter()
	@Help("Choose variable to which you want to assign some value")
	@Editor(type=Editors.SelectionVariables)
	public Variable Variable;
	
	@Parameter
	@Editor(type=Editors.Formula, formulaName="Price")
	public IFormula Value;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	public void OnAction() throws TradingException {
		if(Value.isBooleanValue()) {
			double val = Value.evaluateFormula(Strategy, Strategy.Symbol, 0, 0);
			Variable.setValue((val == 0 || val == Order.NOT_DEFINED ? false : true));	
		} 
		else {
			Variable.setValue(Value.evaluateFormula(Strategy, Strategy.Symbol, 0, 0));
		}
	}
}
