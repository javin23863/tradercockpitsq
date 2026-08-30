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
package SQ.TradeAnalysis;

import java.util.*;
import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

public class TradesByCloseTypeChart extends TradeAnalysisChart {

	public TradesByCloseTypeChart() {
		this.name = L.tsq("Trades by Close Type");
	}
	
	@Override
	public AbstractChart draw(OrdersList orders, byte plType, byte tradePeriod) {
		BarChart chart = new BarChart();
		
		if(orders==null) {
			return chart;
		}
		
		// Compute data grouped by close type
		HashMap<Byte, CloseTypeStats> statsMap = computeData(orders, plType);
		
		// Sort by trade count (descending)
		List<Map.Entry<Byte, CloseTypeStats>> sortedEntries = new ArrayList<>(statsMap.entrySet());
		sortedEntries.sort((a, b) -> Integer.compare(b.getValue().tradeCount, a.getValue().tradeCount));
		
		// Calculate total trades for percentage calculation
		int totalTrades = 0;
		for(CloseTypeStats stats : statsMap.values()) {
			totalTrades += stats.tradeCount;
		}
		
		// Add bars to chart with percentage and stats labels
		for(Map.Entry<Byte, CloseTypeStats> entry : sortedEntries) {
			byte closeType = entry.getKey();
			CloseTypeStats stats = entry.getValue();
			
			double sharePercent = (totalTrades > 0) ? (stats.tradeCount * 100.0 / totalTrades) : 0.0;

			// Format label: "CloseType (XX%)" - Shortened for readability
			String label = String.format("%s (%.1f%%)", getShortName(closeType), sharePercent);
			
			chart.addValue(L.tsq("Trades"), label, sharePercent);
		}
		
		return chart;
	}

	private HashMap<Byte, CloseTypeStats> computeData(OrdersList orders, byte plType) {
		HashMap<Byte, CloseTypeStats> statsMap = new HashMap<>();
		
		for(int i = 0; i < orders.size(); i++) {
			Order order = orders.get(i);
						
			// Get or create stats for this close type
			CloseTypeStats stats = statsMap.get(order.CloseType);
			if(stats == null) {
				stats = new CloseTypeStats();
				statsMap.put(order.CloseType, stats);
			}
			
			// Add to stats
			stats.tradeCount++;
		}
		
		return statsMap;
	}
	
	// Helper class to store statistics for each close type
	private class CloseTypeStats {
		int tradeCount = 0;
	}
	
	public static String getShortName(byte closeType) {
	    switch (closeType) {
	        case OrderCloseTypes.Manual: return "MAN";
	        case OrderCloseTypes.SL: return "SL";
	        case OrderCloseTypes.PT: return "PT";
	        case OrderCloseTypes.EndTest: return "ET";

	        case OrderCloseTypes.EOD: return "EOD";
	        case OrderCloseTypes.EOD_TIME: return "EOD-T";
	        case OrderCloseTypes.EOD_NEXT_OPEN: return "EOD-NO";

	        case OrderCloseTypes.EOF: return "EOF";
	        case OrderCloseTypes.EOF_TIME: return "EOF-T";

	        case OrderCloseTypes.EOR: return "EOR";

	        case OrderCloseTypes.Expired: return "EXP";
	        case OrderCloseTypes.Reversed: return "REV";
	        case OrderCloseTypes.Deleted: return "DEL";
	        case OrderCloseTypes.Replaced: return "REP";

	        case OrderCloseTypes.OCA: return "OCA";
	        case OrderCloseTypes.Commission: return "COM";
	        case OrderCloseTypes.NETTING_CONTROL_ORDER: return "CTRL";

	        case OrderCloseTypes.ExitAfterXBars: return "XBAR";
	        case OrderCloseTypes.MoveSL2BE: return "BE";
	        case OrderCloseTypes.TrailingStop: return "TS";
	        case OrderCloseTypes.ExitSignal: return "SIG";

	        case OrderCloseTypes.Delisted: return "DL";

	        default: return "?";
	    }
	}
}
