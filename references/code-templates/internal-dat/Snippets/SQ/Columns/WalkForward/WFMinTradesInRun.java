package SQ.Columns.WalkForward;

import java.util.ArrayList;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

public class WFMinTradesInRun extends WalkForwardColumn {

	public WFMinTradesInRun() {
		super("WFMinTradesInRun", L.tsq("Min trades in one run"), DatabankColumn.Integer, ValueTypes.Maximize);
	}

	@Override
	public double compute(WalkForwardResult wfResult) {		
		ArrayList<WalkForwardPeriod> periods = wfResult.wfPeriods;

		if(periods.isEmpty() || periods.get(0).runStatData == null) {
			return 0;
		}

		double minTrades = periods.get(0).runStatData.getDouble(StatsKey.NUMBER_OF_TRADES);
		
		for(int i=0; i<periods.size(); i++) {
			WalkForwardPeriod period = periods.get(i);
			if(period.runStatData==null) {
				continue;
			}
			
			if(period.runStatData.getDouble(StatsKey.NUMBER_OF_TRADES) < minTrades) {
				minTrades = period.runStatData.getDouble(StatsKey.NUMBER_OF_TRADES);
			}
		}

		return minTrades;
	}
}