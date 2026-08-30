package SQ.Columns.Databanks;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;
import static java.lang.Math.sqrt;
import static java.lang.Math.pow;

public class UlcerIndex extends DatabankColumn {
    
	public UlcerIndex() {
		super(L.tsq("Ulcer Index %"), 
          DatabankColumn.Decimal2Pct /*Decimal2Pct*/, // value display format
          ValueTypes.Minimize, // whether value should be maximized / minimized / approximated to a value   
          0, // target value if approximation was chosen  
          0, // average minimum of this value
          100); // average maximum of this value
	
   
		setDependencies("NumberOfTrades");
	}
  
  //------------------------------------------------------------------------

	@Override
	public double compute(SQStats stats, StatsTypeCombination combination, OrdersList ordersList, SettingsMap settings, SQStats statsLong, SQStats statsShort) throws Exception {
    
    	double sum  = 0;

		for(int i = 0; i<ordersList.size(); i++) {
			Order order = ordersList.get(i);
			
			if(order.isBalanceOrder()) {
				// don't count balance orders (deposits, withdrawals) in
				continue;
			}
      
      		// Ulcer Index can be found here: https://blog.thinknewfound.com/2014/04/looking-into-the-ulcer-index/ computed as percentage value. So we focus on perc DD.
			double dd = order.PctDD;
			sum += Math.pow(dd,2);
		}
    
    	int numberOfTrades = stats.getInt("NumberOfTrades");

  		return round2(safeDivide(sqrt(sum), numberOfTrades));
	}	
}