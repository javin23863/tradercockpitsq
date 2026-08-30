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

public class AvgTrStddevRatio extends DatabankColumn {

	public AvgTrStddevRatio() {
		super("Avg. trade / StdDev ratio", 
				DatabankColumn.Decimal2, // value display format
				ValueTypes.Maximize, // whether value should be maximized / minimized / approximated to a value   
				0, // target value if approximation was chosen  
				-100, // average minimum of this value
				100); // average maximum of this value

		setWidth(80); // default column width in pixels

		setTooltip("Avg. trade / StdDev ratio");  

		setDependencies("AvgTrade", "StandardDev");
	}

	//------------------------------------------------------------------------

	@Override
	public double compute(SQStats stats, StatsTypeCombination combination, OrdersList ordersList, SettingsMap settings, SQStats statsLong, SQStats statsShort) throws Exception {

		double avgTr = stats.getDouble("AvgTrade") / 100;
		double stddev = stats.getDouble("StandardDev") / 100;

		return SQUtils.round2(SQUtils.safeDivide(avgTr, stddev));
	}	
}
