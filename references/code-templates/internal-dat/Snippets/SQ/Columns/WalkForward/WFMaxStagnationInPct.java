package SQ.Columns.WalkForward;

import java.util.ArrayList;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

public class WFMaxStagnationInPct extends WalkForwardColumn {

	public WFMaxStagnationInPct() {
		super("WFMaxStagnationInPct", L.tsq("Max Stagnation in %"), DatabankColumn.Decimal2Pct, ValueTypes.Minimize);
	}

	@Override
	public double compute(WalkForwardResult wfResult) {		
		ArrayList<WalkForwardPeriod> periods = wfResult.wfPeriods;

		if(periods.isEmpty() || periods.get(0).runStatData == null) {
			return 0;
		}

		double maxStagPct = periods.get(0).runStatData.getDouble(StatsKey.STAGNATION_PERIOD_PCT);
		
		for(int i=0; i<periods.size(); i++) {
			WalkForwardPeriod period = periods.get(i);
			if(period.runStatData==null) {
				continue;
			}
			
			if(period.runStatData.getDouble(StatsKey.STAGNATION_PERIOD_PCT) > maxStagPct) {
				maxStagPct = period.runStatData.getDouble(StatsKey.STAGNATION_PERIOD_PCT);
			}
		}

		return maxStagPct;
	}
}