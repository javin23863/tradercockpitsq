package SQ.Columns.WalkForward;

import java.util.ArrayList;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

public class WFMaxPctDDbyRun extends WalkForwardColumn {

	public WFMaxPctDDbyRun() {
		super("WFMaxPctDDbyRun", L.tsq("Max % Drawdown in one run"), DatabankColumn.Decimal2Pct, ValueTypes.Minimize);
	}

	@Override
	public double compute(WalkForwardResult wfResult) {		
		ArrayList<WalkForwardPeriod> periods = wfResult.wfPeriods;

		if(periods.isEmpty() || periods.get(0).runStatData == null) {
			return 0;
		}

		double maxPctDD = periods.get(0).runStatData.getDouble(StatsKey.PCT_DRAWDOWN);
		
		for(int i=0; i<periods.size(); i++) {
			WalkForwardPeriod period = periods.get(i);
			if(period.runStatData==null) {
				continue;
			}
			
			if(period.runStatData.getDouble(StatsKey.PCT_DRAWDOWN) > maxPctDD) {
				maxPctDD = period.runStatData.getDouble(StatsKey.PCT_DRAWDOWN);
			}
		}

		return maxPctDD;
	}
}