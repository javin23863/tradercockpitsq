package SQ.Columns.Databanks;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

public class ActualDrawdownPct extends DatabankColumn {
    
	public ActualDrawdownPct() {
		super("Actual Drawdown / Max DD", 
				DatabankColumn.Decimal2Pct, // value display format
				ValueTypes.Minimize, // whether value should be maximized / minimized / approximated to a value   
				0, // target value if approximation was chosen  
				0, // average minimum of this value
				100); // average maximum of this value

		setWidth(80); // defaultcolumn width in pixels

		setTooltip("Actual Drawdown / Max DD");  

		setDependencies("Drawdown");
	}
  
  //------------------------------------------------------------------------

  /**
   * This method should return computed value of this new column. You should typically compute it from the list of orders 
   * or from some already computed statistical values (other databank columns). 
   */
	@Override
	public double compute(SQStats stats, StatsTypeCombination combination, OrdersList ordersList, SettingsMap settings, SQStats statsLong, SQStats statsShort) throws Exception {
	    double drawdown = stats.getDouble("Drawdown");
	
	    
	    if(ordersList.size() > 0) {
	    	Order order = ordersList.get(ordersList.size()-1);
	
	    	double actualDDPct = safeDivide((double)order.DD, -drawdown) * 100d; 
	
			return round2(actualDDPct);
			
	    } else {
	    	return 0;
	    }
	}	
}