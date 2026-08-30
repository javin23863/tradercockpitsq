package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.ValueTypes;
import com.strategyquant.tradinglib.results.SpecialValues;

public class DateLastModified extends DatabankColumn {
    
	public DateLastModified() {
		super("Date last modified", DatabankColumn.Time, ValueTypes.Minimize, 0, 0, 10000);

		setTooltip(L.tsq("Date last modified"));
		setWidth(150);
		printsSpecialValue(true);
	}
	
	//------------------------------------------------------------------------

	@Override
	public double getNumericValue(ResultsGroup results, String resultKey, byte direction, byte plType, byte sampleType) throws Exception {
		return results.specialValues().getLong(SpecialValues.DateLastModified, -1);
	}
	
	//------------------------------------------------------------------------
	@Override
	public String getValue(ResultsGroup results, String resultKey, byte direction, byte plType, byte sampleType) throws Exception {
		return (long) getNumericValue(results, resultKey, direction, plType, sampleType) + "";
	}
	
	//------------------------------------------------------------------------

	@Override
	public String exportValue(ResultsGroup results, String resultKey, byte direction, byte plType, byte sampleType) throws Exception {			
		long ms = (long) getNumericValue(results, resultKey, direction, plType, sampleType);
		
		return ms>0 ? SQTime.toFullDateMinuteString(ms) : NOT_AVAILABLE;
	}
}