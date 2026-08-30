package SQ.CustomAnalysis;

import com.strategyquant.lib.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class OutlierTrade extends CustomAnalysisMethod {

	public static final Logger Log = LoggerFactory.getLogger(OutlierTrade.class);
	public OutlierTrade() {
		super("OutlierTrade", TYPE_FILTER_STRATEGY);
	}
	
	//------------------------------------------------------------------------
	
	@Override
	public boolean filterStrategy(String project, String task, String databankName, ResultsGroup rg) throws Exception {

		// We have two arguments for this custom analysis:
		// The first one is the OutlierCoefficient.
		// The second one is an option: 0 or 1, meaning:
		// 0: Without filtering trades with the same profit/loss
		// 1: Filtering trades with the same profit/loss
		// Therefore, you can set, for example:
		// 1.1, 1
		// 1.1 is the OutlierCoefficient
		// 1 is the option: filtering trades with the same profit/loss

		String inputArgs = this.getInputArgs();
        inputArgs.trim();

		try 
        {
			String[] numberStrings = inputArgs.split(",");

			double OutlierCoeficient = Double.parseDouble(numberStrings[0].trim()); 
			int Option = Integer.parseInt(numberStrings[1].trim()); 

			String strName = rg.getName();
			Result mainResult = rg.mainResult();
			String MainResultKey = rg.getMainResultKey();
			OrdersList Order = rg.orders().filterWithClone(MainResultKey, Directions.Both, SampleTypes.InSample);
       		
			if (Order.size() == 0) { return false;} 

			double firstPL = 0;
			double secondPL = 0;
			double thirdPL = 0;
			int count = 0;
			for(int i2 = 0; i2 < Order.size(); i2++){
                Order order = Order.get(i2);
				if(order.isBalanceOrder()) {continue; }
				
				double PL = order.PL;
				if (Option == 0){
					if(PL > firstPL){
						thirdPL = secondPL;
						secondPL = firstPL;
						firstPL = PL;
					} else if(PL > secondPL) {  
						thirdPL = secondPL;
						secondPL = PL;
					} 
					else if(PL > thirdPL) { 
						thirdPL = PL;
					}
				}
				else if (Option == 1){
					if(PL > firstPL){
						thirdPL = secondPL;
						secondPL = firstPL;
						firstPL = PL;
					} else if(PL > secondPL && PL != firstPL) {   
						thirdPL = secondPL;
						secondPL = PL;
					} 
					else if(PL > thirdPL && PL != firstPL && PL != secondPL) {     
						thirdPL = PL;
					}
				} 

				count++;
            }

			if(count > 2) {
				if(firstPL > OutlierCoeficient * (secondPL + thirdPL)) {
					Log.debug("Biggest Order PL: {} > {} + {}", firstPL, secondPL, thirdPL);
					return false;
			}}

			return true;
	  	} catch(Exception e)
        {
			Log.error("Error OutlierTrade", e);
            return false;																																							       // "Error: " + e.getMessage();
        }
	}
}