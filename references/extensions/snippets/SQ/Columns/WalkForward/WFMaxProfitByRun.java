package SQ.Columns.WalkForward;

import java.util.ArrayList;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

public class WFMaxProfitByRun extends WalkForwardColumn {

	public WFMaxProfitByRun() {
		super("WFMaxProfitByRun", L.tsq("Max profit in one run"), DatabankColumn.Decimal2PL, ValueTypes.Maximize);
	}

	@Override
	public double compute(WalkForwardResult wfResult) {		
		ArrayList<WalkForwardPeriod> periods = wfResult.wfPeriods;

		if(periods.isEmpty() || periods.get(0).runStatData == null) {
			return 0;
		}

		double maxProfit = periods.get(0).runStatData.getDouble(StatsKey.NET_PROFIT);
		
		for(int i=0; i<periods.size(); i++) {
			WalkForwardPeriod period = periods.get(i);
			if(period.runStatData==null) {
				continue;
			}
			
			if(period.runStatData.getDouble(StatsKey.NET_PROFIT) > maxProfit) {
				maxProfit = period.runStatData.getDouble(StatsKey.NET_PROFIT);
			}
		}

		return maxProfit;
	}
}