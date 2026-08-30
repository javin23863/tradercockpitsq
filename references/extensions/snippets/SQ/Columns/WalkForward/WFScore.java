package SQ.Columns.WalkForward;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.ValueTypes;
import com.strategyquant.tradinglib.WalkForwardColumn;
import com.strategyquant.tradinglib.WalkForwardResult;

public class WFScore extends WalkForwardColumn {

	public WFScore() {
		super(WFScoreStatsColumn, L.tsq("WF Score"), DatabankColumn.Decimal2Pct, ValueTypes.Maximize);
	}

	@Override
	public double compute(WalkForwardResult wfResult) {	
		return wfResult.scorePerc;
	}
}