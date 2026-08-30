package SQ.Columns.Databanks;

import java.util.List;

import org.jdom2.Element;

import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.ValueTypes;

public class MagicNumber extends DatabankColumn {
    
	public MagicNumber() {
		super("Magic number", DatabankColumn.Text, ValueTypes.Minimize, 0, 0, 0);

		setWidth(150);
		setEditable(true);
	}
	
	@Override
	public String getValue(ResultsGroup results, String resultKey, byte direction, byte plType, byte sampleType) throws Exception {		
		String value = getMN(results);
		
		return "{{inputWidget value='"+value+"'}}";
	}
	
	@Override
	public boolean setValue(Object valueObject, Object value) throws Exception {
		ResultsGroup results = (ResultsGroup) valueObject;
		
		Element elVars = results.getStrategyXml().getChild("Strategy").getChild("Variables");
		
		List<Element> children = elVars.getChildren();
		for(int i=0; i<children.size(); i++) {
			Element elVar = children.get(i);
			
			String name = elVar.getChild("name").getText();			
			if(name.equals("MagicNumber")) {
				elVar.getChild("value").setText(value.toString());
				break;
			}
		}

		return true;
	}
	
	//------------------------------------------------------------------------

	@Override
	public String exportValue(ResultsGroup results, String resultKey, byte direction, byte plType, byte sampleType) throws Exception {					
		return getMN(results);
	}
	
	//------------------------------------------------------------------------
	
	private String getMN(ResultsGroup results) throws Exception {
		String value = NOT_AVAILABLE;

		Element elVars = results.getStrategyXml().getChild("Strategy").getChild("Variables");
		
		List<Element> children = elVars.getChildren();
		for(int i=0; i<children.size(); i++) {
			Element elVar = children.get(i);
			
			String name = elVar.getChild("name").getText();			
			if(name.equals("MagicNumber")) {
				value = elVar.getChild("value").getText();
				break;
			}
		}
		
		return value;
	}
}