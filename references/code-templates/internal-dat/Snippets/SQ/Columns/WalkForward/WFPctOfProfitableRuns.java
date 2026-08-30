package SQ.Columns.WalkForward;

import java.util.ArrayList;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

public class WFPctOfProfitableRuns extends WalkForwardColumn {

	public WFPctOfProfitableRuns() {
		super("WFPctOfProfitableRuns", L.tsq("Percentage of profitable runs"), DatabankColumn.Decimal2Pct, ValueTypes.Maximize);
	}

	@Override
	public double compute(WalkForwardResult wfResult) {	
		ArrayList<WalkForwardPeriod> periods = wfResult.wfPeriods;
		
		int numberOfProfitableRuns = 0;
		int numberOfPeriods = periods.size()-1;
				
		for(int i=0; i<numberOfPeriods; i++) {
			WalkForwardPeriod period = periods.get(i);
			if(period.runStatData==null) {
				continue;
			}
			
			if(period.runStatData.getDouble(StatsKey.NET_PROFIT) > 0) {
				numberOfProfitableRuns++;
			}
		}
		
		float pctProfitable = ((float) numberOfProfitableRuns) / (((float) numberOfPeriods)/100f);
		
		return pctProfitable;
	}
}