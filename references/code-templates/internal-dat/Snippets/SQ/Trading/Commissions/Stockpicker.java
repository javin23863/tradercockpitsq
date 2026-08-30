package SQ.Trading.Commissions;

import com.strategyquant.tradinglib.ClassConfig;
import com.strategyquant.tradinglib.CommissionsMethod;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.Editors;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.ILiveOrder;
import com.strategyquant.tradinglib.Parameter;

@ClassConfig(name="Stockpicker", display="#Commission# / #CommissionType#")
@Help("<b>Stockpicker commissions</b>")
public class Stockpicker extends CommissionsMethod {

	@Parameter(defaultValue="0.0035", minValue=0d, name="Commission", maxValue=100000d, step=0.001d, decimals=5, category="Default")
	@Help("Commission")
	public double Commission;

	@Parameter(defaultValue="share", name="Commission type")
	@Editor(type=Editors.Selection, values="$ per share=share,$ per order=order,% of trade value=equity")
	@Help("Commission type")
	public String CommissionType;
	
	@Parameter(defaultValue="0.35", minValue=0d, name="Min per order", maxValue=100000d, step=0.1d, category="Default")
	@Help("Minimum commission per order. Zero means no minimum")
	public double MinPerOrder;
	
	@Parameter(defaultValue="money", name="Min per order type")
	@Editor(type=Editors.Selection, values="$=money,% of trade value=equity")
	@Help("Minimum commission per order type")
	public String MinPerOrderType;
	
	@Parameter(defaultValue="1", minValue=0d, name="Max per order", maxValue=100000d, step=1d, category="Default")
	@Help("Maximum commission per order. Zero means no maximum")
	public double MaxPerOrder;
	
	@Parameter(defaultValue="equity", name="Max per order type")
	@Editor(type=Editors.Selection, values="$=money,% of trade value=equity")
	@Help("Maximum commission per order type")
	public String MaxPerOrderType;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public double computeCommissionsOnOpen(ILiveOrder order, double tickSize, double pointValue) throws Exception {	
		double commission, minCommission, maxCommision;
		double pctCommission;
		
		double purchasedEquity = order.getSize() * order.getOpenPrice();
		
		//calculate commission
		switch(CommissionType) {
			case "share": 
				commission = Commission * order.getSize(); 
				break;   
			
			case "order": 
				commission = Commission;
				break; 

			case "equity": 
				pctCommission = Commission / 100d;
				commission = pctCommission * purchasedEquity;
				break; 
				
			default: throw new Exception("Invalid commision type: " + CommissionType);
		}
		
		//calculate min commision
		switch(MinPerOrderType) {
			case "money": 
				minCommission = MinPerOrder;
				break;   

			case "equity": 
				pctCommission = MinPerOrder / 100d;
				minCommission = pctCommission * purchasedEquity;
				break; 
				
			default: throw new Exception("Invalid min per order type: " + MinPerOrderType);
		}

		//calculate max commision
		switch(MaxPerOrderType) {
			case "money": 
				maxCommision = MaxPerOrder;
				break;   
	
			case "equity": 
				pctCommission = MaxPerOrder / 100d;
				maxCommision = pctCommission * purchasedEquity;
				break; 
				
			default: throw new Exception("Invalid max per order type: " + MaxPerOrderType);
		}
		
		//check min / max commision
		if(minCommission > 0 && commission < minCommission) commission = minCommission;
		if(maxCommision > 0 && commission > maxCommision) commission = maxCommision;
		
		return commission;
	}
	//------------------------------------------------------------------------

	@Override
	public double computeCommissionsOnClose(ILiveOrder order, double tickSize, double pointValue) throws Exception {
		return computeCommissionsOnOpen(order, tickSize, pointValue);
	}
	
}
