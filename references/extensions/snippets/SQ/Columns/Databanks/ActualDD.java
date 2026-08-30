package SQ.Columns.Databanks;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

public class ActualDD extends DatabankColumn {
    
	public ActualDD() {
		super("Actual Drawdown", 
				DatabankColumn.Decimal2PL, // value display format
				ValueTypes.Minimize, // whether value should be maximized / minimized / approximated to a value   
				0, // target value if approximation was chosen  
				0, // average minimum of this value
				100); // average maximum of this value

		setWidth(80); // defaultcolumn width in pixels

		setTooltip("Actual Drawdown - drawdown during very last trade");  

		/* If this new column is dependent on some other columns that have to vbe computed first, put them here.
       Make sure you don't creat circular dependency, such as A depends on B and B depends on A.
       Columns (=stats values) are identified by the name of class)
		 */
		setDependencies("Drawdown");
	}

	//------------------------------------------------------------------------

	/**
	 * This method should return computed value of this new column. You should typically compute it from the list of orders 
	 * or from some already computed statistical values (other databank columns). 
	 */
	@Override
	public double compute(SQStats stats, StatsTypeCombination combination, OrdersList ordersList, SettingsMap settings, SQStats statsLong, SQStats statsShort) throws Exception {

		if(ordersList.size() > 0) {
			Order order = ordersList.get(ordersList.size()-1);

			double actualDD = (double) order.DD;

			return round2(Math.abs(actualDD));

		} else  {
			return 0;
		}
	}
	
}