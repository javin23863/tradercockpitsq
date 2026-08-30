package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.ValueTypes;
import com.strategyquant.tradinglib.results.SpecialValues;

public class FiltersResult extends DatabankColumn {
    
	public FiltersResult() {
		super(L.tsq("Filters result"), DatabankColumn.Text, ValueTypes.Maximize, 0, 0, 100);

		setTooltip(L.tsq("Filters result"));
		setWidth(100);
	}

	//------------------------------------------------------------------------

	@Override
	public String getValue(ResultsGroup results, String resultKey, byte direction, byte plType, byte sampleType) throws Exception {
		SettingsMap map = results.specialValues();
		
		if(map.containsKey(SpecialValues.FiltersResultFailedReason)) {	
			if(map.getString(SpecialValues.FiltersResultFailedReason).equals(SpecialValues.FiltersResultPassed)) {
				return "{{tooltipWidget text='" + L.t("PASSED") + "' select='PASSED' class='passed'}}";
			}
			
			return "{{tooltipWidget text='" + L.t("FAILED") + "' tooltip='" + map.getString(SpecialValues.FiltersResultFailedReason) + "' select='FAILED' class='failed'}}";
		}
		
		return "";
	}
	
	//------------------------------------------------------------------------

	@Override
	public String exportValue(ResultsGroup results, String resultKey, byte direction, byte plType, byte sampleType) throws Exception {			
		SettingsMap map = results.specialValues();
		
		if(map.containsKey(SpecialValues.FiltersResultFailedReason)) {	
			if(map.getString(SpecialValues.FiltersResultFailedReason).equals(SpecialValues.FiltersResultPassed)) {
				return L.t("PASSED");
			}
			
			return L.t("FAILED");
		}
		
		return "";
	}
	
}
