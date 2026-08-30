package SQ.Checker;

import org.jdom2.Element;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

/**
 * checks if really indicator is used in IsRising or IsFalling property
 *
 */
public class PropertyChecker extends Checker {

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	public boolean check(Element elBlock) {
		String returnType = elBlock.getAttributeValue("returnType");
		
		if(returnType != null && !returnType.equals("boolean")) {
			// it is not a property, skip it
			return true;
		}
		
		String key = elBlock.getAttributeValue("key");
		
		if(!key.equalsIgnoreCase("IsRising") && !key.equalsIgnoreCase("IsFalling")) {
			// it is not IsRising/Falling property, skip
			return true;
		}
		
		Element elIndicator = getParameter(elBlock, "Indicator");
		
		if(elIndicator == null) {
			// it doesn't have Indicator parameters, skip it
			return true;
		}
		
		String xml1 = XMLUtil.xmlToStringRaw(elIndicator);
		
		if(!xml1.contains("#Shift#")) {
			// WRONG: we shouldn't use block that doesn't have Shift parameter in IsRising/Falling 
			return false;
		}
		
		if(xml1.contains("Hour") || xml1.contains("Minute") || xml1.contains("DayOfWeek")) {
			// WRONG: we shouldn't use hour and minute in IsRising,IsFalling
			return false;
		}
		
		return true;
	}

	//------------------------------------------------------------------------

	private Element getParameter(Element elBlock, String parameterName) {

		parameterName = fixKey(parameterName);
		
		for(Element elParam: elBlock.getChildren()) {
			String key = elParam.getAttributeValue("key");
			if(key != null && key.equals(parameterName)) {
				return elParam;
			}
		}
		
		return null;
	}

	private String fixKey(String parameterName) {
		StringBuilder sb = new StringBuilder();
		
		sb.append("#");
		sb.append(parameterName);
		sb.append("#");
		
		return sb.toString();
	}

		
}
