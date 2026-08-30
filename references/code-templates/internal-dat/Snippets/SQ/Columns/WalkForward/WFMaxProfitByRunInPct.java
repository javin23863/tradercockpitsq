package SQ.Columns.WalkForward;

import java.util.ArrayList;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

public class WFMaxProfitByRunInPct extends WalkForwardColumn {

	public WFMaxProfitByRunInPct() {
		super("WFMaxProfitByRunInPct", L.tsq("Max profit in one run as % of total"), DatabankColumn.Decimal2Pct, ValueTypes.Maximize);
	}

	@Override
	public double compute(WalkForwardResult wfResult) {		
		ArrayList<WalkForwardPeriod> periods = wfResult.wfPeriods;

		if(periods.isEmpty() || periods.get(0).runStatData == null) {
			return 0;
		}

		double maxProfitInRun = periods.get(0).runStatData.getDouble(StatsKey.NET_PROFIT);
		double totalProfit = 0;
		
		for(int i=0; i<periods.size(); i++) {
			WalkForwardPeriod period = periods.get(i);
			if(period.runStatData==null) {
				continue;
			}
			
			double runProfit = period.runStatData.getDouble(StatsKey.NET_PROFIT);
			
			totalProfit += runProfit;
			if(runProfit > maxProfitInRun) {
				maxProfitInRun = period.runStatData.getDouble(StatsKey.NET_PROFIT);
			}
		}

		double valueDistribution = 0;
		
		if(maxProfitInRun > 0 &&  totalProfit< 0) {
			valueDistribution = 100;
		} else {
			valueDistribution = maxProfitInRun / (totalProfit / 100);
		}
		
		return valueDistribution;
	}
}