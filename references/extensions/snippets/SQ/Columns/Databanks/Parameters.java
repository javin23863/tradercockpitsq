package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.SettingsKeys;
import com.strategyquant.tradinglib.ValueTypes;
import com.strategyquant.tradinglib.WalkForwardResult;
import com.strategyquant.tradinglib.optimization.WalkForwardMatrixResult;

public class Parameters extends DatabankColumn {
    
	public Parameters() {
		super(L.tsq("Parameters"), DatabankColumn.Text, ValueTypes.Maximize, 0, 0, 1);

		setWidth(200);
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
			return wfResult.testParams;
		} catch(Exception e) {
		}

		return results.getParams(); 
	}
}