package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;
import com.strategyquant.tradinglib.ValueTypes;

public class RecoveryFactor extends DatabankColumn {
	
	public RecoveryFactor() {
		super(L.tsq("RecoveryFactor"), DatabankColumn.Decimal2, ValueTypes.Maximize, 0, 0, 200);
		
		setDependencies("NetProfitInPct", "DrawdownPct");
	}
	
	//------------------------------------------------------------------------

	@Override
	public double compute(SQStats stats, StatsTypeCombination combination, OrdersList ordersList, SettingsMap settings, SQStats statsLong, SQStats statsShort) throws Exception {
		double netProfitPct = stats.getDouble("NetProfitInPct");
		double drawdownPct = stats.getDouble("DrawdownPct");
		
		return round2(safeDivide(netProfitPct, drawdownPct));
	}

}
