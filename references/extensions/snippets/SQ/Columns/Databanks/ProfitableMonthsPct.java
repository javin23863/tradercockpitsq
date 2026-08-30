package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;
import com.strategyquant.tradinglib.ValueTypes;

public class ProfitableMonthsPct extends DatabankColumn {
    
	public ProfitableMonthsPct() {
		super(L.tsq("% Profitable Months"), DatabankColumn.Decimal2Pct, ValueTypes.Maximize, 0, 0, 100);

		setDependencies("ProfitableMonths", "TotalTradingMonths");
	}
	
	//------------------------------------------------------------------------

	@Override
	public double compute(SQStats stats, StatsTypeCombination combination, OrdersList ordersList, SettingsMap settings, SQStats statsLong, SQStats statsShort) throws Exception {
		int profitMonths = stats.getInt("ProfitableMonths");
		int totalMonths = stats.getInt("TotalTradingMonths");

		return SQUtils.round2(SQUtils.safeDivide(profitMonths, totalMonths) * 100);
	}		
}