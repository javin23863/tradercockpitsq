package SQ.Checker;

import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.Checker;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * check for indicator that has fast and slow component - they shouldn't be equal
 *
 */
public class IndicatorFastSlowChecker extends Checker {
	public static final Logger Log = LoggerFactory.getLogger("IndicatorFastSlowChecker");
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	public boolean check(Element elBlock) {

		if(!checkFastSlow(elBlock)) {
			return false;
		}

		ArrayList<Element> listItems = new ArrayList<Element>();
		XMLUtil.findAll(elBlock, "Item", listItems);

		if(listItems != null && listItems.size() > 0) {
			for(int i=0; i<listItems.size(); i++) {
				Element elItem = listItems.get(i);

				if(!checkFastSlow(elItem)) {
					return false;
				}
			}
		}

		return true;
	}

	//------------------------------------------------------------------------

	private boolean checkFastSlow(Element elItem) {
		List<Element> children = elItem.getChildren("Param");

		// check Fast, Slow
		Element elFast = findInParams(children, "#Fast");
		if(elFast != null) {
			Element elSlow = findInParams(children, "#Slow");

			if(elSlow != null) {
				return checkFastSlow(elFast, elSlow);
			}
		}

		// check Min, Max
		Element elMin = findInParams(children, "#MinPeriod");
		if(elMin != null) {
			Element elMax = findInParams(children, "#MaxPeriod");

			if(elMax != null) {
				return checkMinMax(elMin, elMax);
			}
		}

		return true;
	}

	//------------------------------------------------------------------------
	private boolean checkFastSlow(Element elFast, Element elSlow) {
		String fast = elFast.getText();
		String slow = elSlow.getText();

		try {
			int fastPeriod = Integer.parseInt(fast);
			int slowPeriod = Integer.parseInt(slow);

			if(fastPeriod >= slowPeriod) {
				return false;
			}

		} catch(NumberFormatException e) {
			return true;
		}

		return true;
	}

	//------------------------------------------------------------------------
	private boolean checkMinMax(Element elMin, Element elMax) {
		String min = elMin.getText();
		String max = elMax.getText();

		try {
			int minPeriod = Integer.parseInt(min);
			int maxPeriod = Integer.parseInt(max);

			if(minPeriod >= maxPeriod) {
				return false;
			}

		} catch(NumberFormatException e) {
			return true;
		}

		return true;
	}

	//------------------------------------------------------------------------

	private Element findInParams(List<Element> children, String parameterName) {
		for(Element elParam: children) {
			String key = elParam.getAttributeValue("key");
			if(key != null && key.startsWith(parameterName)) {

				String type = elParam.getAttributeValue("type");
				if(type != null && type.equals("int")) {
					return elParam;
				}
			}
		}

		return null;
	}

}
