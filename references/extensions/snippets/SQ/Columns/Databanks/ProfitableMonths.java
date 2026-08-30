package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;
import com.strategyquant.tradinglib.ValueTypes;

public class ProfitableMonths extends DatabankColumn {
    
	public ProfitableMonths() {
		super(L.tsq("Profitable Months"), DatabankColumn.Integer, ValueTypes.Maximize, 0, 0, 100);

		setTooltip(L.tsq("Profitable Months"));
		setDependencies("TotalTradingDays");
	}
	
	//------------------------------------------------------------------------

	@Override
	public double compute(SQStats stats, StatsTypeCombination combination, OrdersList ordersList, SettingsMap settings, SQStats statsLong, SQStats statsShort) throws Exception {		
		return stats.getInt("ProfitableMonths");
	}
}