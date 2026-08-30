package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.SettingsKeys;
import com.strategyquant.tradinglib.ValueTypes;
import com.strategyquant.tradinglib.WalkForwardResult;
import com.strategyquant.tradinglib.optimization.WalkForwardMatrixResult;

public class BestWF extends DatabankColumn {
    
	public BestWF() {
		super(L.tsq("Best WF"), DatabankColumn.Text, ValueTypes.Maximize, 0, 0, 1);

		setWidth(120);
	}
	
	//------------------------------------------------------------------------

	@Override
	public double getNumericValue(ResultsGroup results, String resultKey, byte direction, byte plType, byte sampleType) throws Exception {
		return 0;
	}

	//------------------------------------------------------------------------
	
	@Override
	public String getValue(ResultsGroup results, String resultKey, byte direction, byte plType, byte sampleType) throws Exception {		
		try {
			WalkForwardMatrixResult mwfResult = (WalkForwardMatrixResult) results.mainResult().get(SettingsKeys.WalkForwardResult);
			WalkForwardResult wfResult = mwfResult.getWFResult(results.getBestWFResultKey(), true);
			return wfResult.toString(mwfResult.periodType);
		} catch(Exception e) {}

		return NOT_AVAILABLE;
	}
}