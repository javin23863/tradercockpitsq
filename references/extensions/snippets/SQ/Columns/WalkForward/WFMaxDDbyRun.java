package SQ.Columns.WalkForward;

import java.util.ArrayList;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

public class WFMaxDDbyRun extends WalkForwardColumn {

	public WFMaxDDbyRun() {
		super("WFMaxDDbyRun", L.tsq("Max Drawdown in one run"), DatabankColumn.Decimal2PL, ValueTypes.Minimize);
	}

	@Override
	public double compute(WalkForwardResult wfResult) {		
		ArrayList<WalkForwardPeriod> periods = wfResult.wfPeriods;

		if(periods.isEmpty() || periods.get(0).runStatData == null) {
			return 0;
		}

		double maxDD = periods.get(0).runStatData.getDouble(StatsKey.DRAWDOWN);
		
		for(int i=0; i<periods.size(); i++) {
			WalkForwardPeriod period = periods.get(i);
			if(period.runStatData==null) {
				continue;
			}
			
			if(period.runStatData.getDouble(StatsKey.DRAWDOWN) > maxDD) {
				maxDD = period.runStatData.getDouble(StatsKey.DRAWDOWN);
			}
		}

		return maxDD;
	}
}