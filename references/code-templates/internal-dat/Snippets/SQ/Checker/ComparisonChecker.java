package SQ.Checker;

import java.util.List;

import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.Checker;

/**
 * checks that left side of comparison isn't same as right side
 *
 */
public class ComparisonChecker extends Checker {
	public static final Logger Log = LoggerFactory.getLogger("ComparisonChecker");
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	public boolean check(Element elBlock) {
		String returnType = elBlock.getAttributeValue("returnType");
		
		if(returnType != null && !returnType.equals("boolean")) {
			// it is not a comparison, skip it
			return true;
		}
		
		Element elLeft = getParameter(elBlock, "#Left#");
		Element elRight = getParameter(elBlock, "#Right#");

		if(elLeft == null || elRight == null) {
			// it doesn't have Left/Right parameters, skip it
			return checkIfItIsntNotBlock(elBlock);
		}
		
		if(checkIncorrectNumberIndicatorsComparison(elLeft, elRight, elBlock)) {
			// cannot compare two different number indicators
			return false;
		}

		if(checkLeftRightAreSame(elLeft, elRight)) {
			return false;
		}
		
		//Log.info("All ok");
		return true;
	}

	//------------------------------------------------------------------------

	private boolean checkIfItIsntNotBlock(Element elBlock) {
		if(!elBlock.getAttributeValue("key").equals("Not")) {
			// it is not a comparison, skip it
			return true;
		}
		
		Element elValue = getParameter(elBlock, "#Value#");
		if(elValue == null) {
			return true;
		}
		
		String retType = elValue.getAttributeValue("returnType");
		if(retType != null && !retType.equals("boolean")) {
			return false;
		}
		
		return true;
	}

	//------------------------------------------------------------------------

	/**
	 * returns true if comparison is incorrect
	 * @param elLeft
	 * @param elRight
	 * @param elComparison 
	 * @return
	 */
	private boolean checkIncorrectNumberIndicatorsComparison(Element elLeft, Element elRight, Element elComparison) {
		
		String retTypeL = elLeft.getAttributeValue("returnType");
		String retTypeR = elRight.getAttributeValue("returnType");
		
		if(retTypeL != null && retTypeR != null) {
			if(!retTypeL.equals(retTypeR)) {
				// cannot compare values of different types
				return true;
			}
			
			if(retTypeL.equals("number") && retTypeR.equals("number")) {
				
				String catTypeL = elLeft.getAttributeValue("categoryType");
				String catTypeR = elRight.getAttributeValue("categoryType");
				
				if(catTypeL != null && catTypeR != null) {

					String keyL = elLeft.getAttributeValue("key");
					String keyR = elRight.getAttributeValue("key");

					if(catTypeL.equals("indicator") && catTypeR.equals("indicator")) {
						// both sides are indicators and they both return number.
						// It doesn't make sense to compare two different number indicators (like CCI and RSI), 
						// unless they are the same indicator with different parameters
						if(!keyL.equals(keyR)) {
							// both sides are different indicators that return number
							return true;
						}
						
						
					} else if(catTypeL.equals("indicator") && catTypeR.equals("other")) {

						if(!keyR.equals("Number")) {
							// number indicator can be compared only with number
							return true;
						}
					
						
					} else if(catTypeL.equals("other") && catTypeR.equals("indicator")) {

						if(!keyL.equals("Number")) {
							// number indicator can be compared only with number
							return true;
						}

						
					} else if(catTypeL.equals("other") && catTypeR.equals("other")) {
						
						if(keyL.equals("Number")) {
							if(isBarTimeBlock(keyR)) {
								// time block can be only on the left
								return true;
							}
							
						} else if(keyR.equals("Number")) {
							if(isBarTimeBlock(keyL)) {
								// check if the number value is within range of hour or minute
								if(checkBadTimeRange(elLeft, elRight)) {
									return true;
								}
								
								if(elComparison.getAttributeValue("key").contains("Crosses")) {
									// it doesn't make sense using cross comparisons with time blocks
									return true;
								}

							} else {
								// only other allowed comparison is between bar time blocks and numbers
								return true;
							}
							
						} else if(isBarTimeBlock(keyL) && isBarTimeBlock(keyR)) {
							return true;
						}
					}
				}

			} else {
				String keyL = elLeft.getAttributeValue("key");

				if(keyL.equals("HighestInRange") || keyL.equals("LowestInRange")) {
					if(!checkCorrectHHMMTime(elLeft)) {
						return true;
					}
				}

				String keyR = elRight.getAttributeValue("key");

				if(keyR.equals("HighestInRange") || keyR.equals("LowestInRange")) {
					if(!checkCorrectHHMMTime(elRight)) {
						return true;
					}
				}
				
			}
		}
		
		return false;
	}

	//------------------------------------------------------------------------

	private boolean checkCorrectHHMMTime(Element elHLBlock) {
		List<Element> children = elHLBlock.getChildren();
		
		for(int i=0; i<children.size(); i++) {
			Element elParam = children.get(i);
			
			String key = elParam.getAttributeValue("key");
			
			if(key != null && (key.equals("#TimeFrom#") || key.equals("#TimeTo#"))) {
				String sVal = elParam.getText();
				
				if(sVal != null) {
					try {
						int val = Integer.parseInt(sVal);
						
						int hh = val / 100;
						if(hh < 0 || hh > 23) {
							Log.info("Bad hour: {}", val);
							return false;
						}
						
						int mm = val % 100;
						if(mm < 0 || mm > 59) {
							Log.info("Bad minute: {}", val);
							return false;
						}
								
					} catch(NumberFormatException e) {
						return false;
					}
				}
			}
		}
		
		return true;
	}

	//------------------------------------------------------------------------

	private boolean checkBadTimeRange(Element elBarTimeBlock, Element elNumber) {
		int numberValue = 0;
		try {
		
			Element elParam = elNumber.getChild("Param");
			if(elParam == null) {
				return false;
			}
			
			String strNumberValue = elParam.getTextTrim();
			
			numberValue = (int) Double.parseDouble(strNumberValue);
			
		} catch(Exception e) {
			// cannot convert it, probably a variable string
			return false;
		}
		
		String key = elBarTimeBlock.getAttributeValue("key");
		if(key.contains("Hour") && (numberValue < 0 || numberValue > 23)) {
			//Log.info("Hour in incorrect range: "+numberValue);
			return true;
		}
		
		if(key.contains("Minute") && (numberValue < 0 || numberValue > 59)) {
			//Log.info("Minute in incorrect range: "+numberValue);
			return true;
		}

		if(key.contains("DayOfWeek") && (numberValue < 0 || numberValue > 6)) {
			//Log.info("Minute in incorrect range: "+numberValue);
			return true;
		}

		return false;
	}

	//------------------------------------------------------------------------

	private boolean isBarTimeBlock(String key) {
		return key.contains("Hour") || key.contains("Minute") || key.contains("DayOfWeek");
	}

	//------------------------------------------------------------------------

	private boolean checkLeftRightAreSame(Element elLeft, Element elRight) {
		String xml1 = XMLUtil.xmlToStringRaw(elLeft);
		String xml2 = XMLUtil.xmlToStringRaw(elRight);
		

		return xml1.equals(xml2);
	}

	//------------------------------------------------------------------------

	private Element getParameter(Element elBlock, String parameterName) {
		
		for(Element elParam: elBlock.getChildren()) {
			String key = elParam.getAttributeValue("key");
			if(key != null && key.equals(parameterName)) {
				return elParam.getChild("Item");
				//return elParam;
			}
		}
		
		return null;
	}

		
}
