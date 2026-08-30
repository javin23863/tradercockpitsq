/**
 * Authors: see task: https://roadmap.strategyquant.com/tasks/sq4_3361
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES
 * OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package SQ.Columns.Databanks;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

public class KellyFormula extends DatabankColumn {

	public KellyFormula() {
		super("Kelly formula", 
				DatabankColumn.Decimal2Pct, // value display format
				ValueTypes.Maximize, // whether value should be maximized / minimized / approximated to a value   
				0, // target value if approximation was chosen  
				-100, // average minimum of this value
				100); // average maximum of this value

		setWidth(80); // default column width in pixels

		setTooltip("Kelly formula");  

		setDependencies("WinningPct", "Efficiency");
	}

	//------------------------------------------------------------------------

	@Override
	public double compute(SQStats stats, StatsTypeCombination combination, OrdersList ordersList, SettingsMap settings, SQStats statsLong, SQStats statsShort) throws Exception {

		double winningPct = stats.getDouble("WinningPct") / 100;
		double efficiency = stats.getDouble("Efficiency") / 100;

		return SQUtils.round2( winningPct * efficiency * 100);
	}	
}
