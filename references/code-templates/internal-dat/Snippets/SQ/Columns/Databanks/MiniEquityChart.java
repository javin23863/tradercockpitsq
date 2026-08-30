package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.ValueTypes;
import com.strategyquant.tradinglib.miniequitychart.MiniEquityChartComputer;

public class MiniEquityChart extends DatabankColumn {
    
	public MiniEquityChart() {
		super(L.tsq("Mini equity chart"), DatabankColumn.Text, ValueTypes.Minimize, 0, 0, 0);
		setWidth(150);
		setEditable(true);
	}

	//------------------------------------------------------------------------
	
	@Override
	public String getValue(ResultsGroup results, String resultKey, byte direction, byte plType, byte sampleType) throws Exception {		
		return MiniEquityChartComputer.getValue(results, resultKey, sampleType);
	}
	
	//------------------------------------------------------------------------

	@Override
	public String exportValue(ResultsGroup results, String resultKey, byte direction, byte plType, byte sampleType) throws Exception {					
		return "";
	}
}