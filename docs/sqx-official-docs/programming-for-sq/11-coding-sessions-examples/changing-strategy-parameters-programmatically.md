# Changing strategy parameters programmatically

- Source: <https://strategyquant.com/doc/programming-for-sq/changing-strategy-parameters-programmatically/>
- Section: Programming for StrategyQuant X › Coding sessions examples
- Fetched: 2026-09-04

---

The original request:

*Is there is a way to apply best suggested parameters to strategy during a passed Walk-Forward Optimization? (Looking to do this in my workflow to run a last OOS with best parameters applied to last period)*

It is possible, see example below. It is made as CustomAnalysis snippet that can run after the WF optimization is finished.

This also demonstrates how to apply parameters to strategy programmatically in general.

Note that setting parameters to strategy is quite complex, we created a special helper class **StrategyParametersHelper** that is used in this example.

Custom Analysis snippet **CAApplyOptimParams** – the actual example:

```
package SQ.CustomAnalysis;

import com.strategyquant.lib.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strategyquant.tradinglib.*;
import com.strategyquant.tradinglib.optimization.ParametersSettings;
import com.strategyquant.tradinglib.optimization.WalkForwardMatrixResult;

import org.jdom2.Element;
import SQ.Utils.StrategyParametersHelper;

public class CAApplyOptimParams extends CustomAnalysisMethod {
    public static final Logger Log = LoggerFactory.getLogger("CAApplyOptimParams");

    //------------------------------------------------------------------------
    //------------------------------------------------------------------------
    //------------------------------------------------------------------------
    
    public CAApplyOptimParams() {
        super("CAApplyOptimParams", TYPE_FILTER_STRATEGY);
    }
    
    //------------------------------------------------------------------------
    
    @Override
    public boolean filterStrategy(String project, String task, String databankName, ResultsGroup rg) throws Exception {

        // get WF result from strategy (ResultsGroup)
        WalkForwardMatrixResult mwfResult = (WalkForwardMatrixResult) rg.mainResult().get(SettingsKeys.WalkForwardResult);
        if(mwfResult==null) {
            return true;
        }

        // get chosen WF result in case of WF Matrix
        WalkForwardResult bestWFResult = mwfResult.getWFResult(rg.getBestWFResultKey(), false);
        if(bestWFResult == null) {
            return true;
        }

        // verify that WF result has some periods
        if(bestWFResult.wfPeriods == null || bestWFResult.wfPeriods.size() <= 0) {
            return true;
        }

        // choose the very last period - it is the one with parameters to be used for future
        WalkForwardPeriod lastPeriod = bestWFResult.wfPeriods.get(bestWFResult.wfPeriods.size()-1);

        String lastParameters = lastPeriod.testParameters;
        Log.info("Recommended future params: {}", lastParameters);

        // this only verifies if we use symmetric variables in the strategy, or separate for Long/Short
        Element lastSettings = XMLUtil.stringToElement(rg.getLastSettings());
        ParametersSettings parametersSettings = new ParametersSettings();
        parametersSettings.setFromXML(lastSettings, rg.getStrategyXml());
        boolean symmetricVariables = parametersSettings.symmetry;

        // set new parameters using ths helper class
        // parameters are in String lastParameters as keys=values separated by comma
        // for example: "FirstPerod=28,SecondPeriod=34,Coef=3.45"
        StrategyParametersHelper.setParameters(rg, lastParameters, symmetricVariables, true);

        return true;
    }
}
```

**StrategyParametersHelper** class:

```
package SQ.Utils;

import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.*;
import com.strategyquant.tradinglib.options.TradingOptionsList;
import com.strategyquant.tradinglib.propertygrid.IPGParameter;
import com.strategyquant.tradinglib.propertygrid.ParametersTableItemProperties;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class StrategyParametersHelper {
    public static final Logger Log = LoggerFactory.getLogger(StrategyParametersHelper.class);

    //------------------------------------------------------------------------
    //------------------------------------------------------------------------
    //------------------------------------------------------------------------

    /**
     * Helper class to set parameters to the strategy
     * @param rg - strategy (ResultsGroup) to apply parameters
     * @param parameters - parameters in String as keys=values separated by comma, example: "FirstPerod=28,SecondPeriod=34,Coef=3.45"
     * @param symmetricVariables - are symmetrical variables used?
     * @param modifyLastSettings - modify also last setting stored for this strategy?
     * @throws Exception
     */
    public static void setParameters(ResultsGroup rg, String parameters, boolean symmetricVariables, boolean modifyLastSettings) throws Exception {

        //Load XML and update variables
        StrategyBase xmlS = StrategyBase.createXmlStrategy(rg.getStrategyXml());
        xmlS.transformToVariables(symmetricVariables);
        Variables variables = xmlS.variables();

        String[] params = parameters.split(",");
        HashMap<String, String> paramMap = new HashMap<>();

        for(int i=0; i<params.length; i++) {
            String[] values = params[i].split("=");
            paramMap.put(values[0], values[1]);
        }

        for(int a=0; a<variables.size(); a++) {
            Variable variable = variables.get(a);
            if(paramMap.containsKey(variable.getName())) {
                variable.setFromString(paramMap.get(variable.getName()));
            }
        }

        xmlS.transformToNumbers();

        rg.portfolio().addStrategyXml(xmlS.getStrategyXml());
        rg.specialValues().setString(StatsKey.OPTIMIZATION_PARAMETERS, parameters);

        if(modifyLastSettings){
            try {
                Element lastSettings = XMLUtil.stringToElement(rg.getLastSettings());
                Element elParams = lastSettings.getChild("Options").getChild("BuildTradingOptions").getChild("Params");
                List<Element> paramElems = elParams.getChildren("Param");

                List<TradingOption> tradingOptions = TradingOptionsList.getInstance().getAvailableClasses();

                for(String paramName : paramMap.keySet()) {
                    boolean processed = false;

                    //we must find the right trading option
                    for(int i=0; i<tradingOptions.size(); i++) {
                        if(processed) break;

                        TradingOption option = tradingOptions.get(i);
                        String optionClass = option.getClass().getSimpleName();
                        ArrayList<IPGParameter> optionParams = option.getParams();

                        //go through its params and find the one with matching name
                        for(int z=0; z<optionParams.size(); z++) {
                            if(processed) break;

                            IPGParameter optionParam = optionParams.get(z);
                            String paramKey = optionParam.getKey();

                            if(!optionParam.getName().equals(paramName)) continue;

                            //now find the right Param element in settings XML and update its value
                            for(int s=0; s<paramElems.size(); s++) {
                                Element elParam = paramElems.get(s);
                                String elParamClass = elParam.getAttributeValue("className");
                                String elParamKey = elParam.getAttributeValue("key");

                                if(elParamClass != null && elParamKey != null && elParamClass.equals(optionClass) && elParamKey.equals(paramKey)) {
                                    String value = paramMap.get(paramName);

                                    if(optionParam.getType() == ParametersTableItemProperties.TYPE_TIME) {
                                        value = "" + SQTime.HHMMToMinutes(Integer.parseInt(value)) * 60;
                                    }

                                    elParam.setText(value);

                                    processed = true;
                                    break;
                                }
                            }
                        }
                    }
                }

                rg.setLastSettings(XMLUtil.elementToString(lastSettings));
            }
            catch(Exception e){
                Log.error("Cannot apply trading options params to last settings", e);
            }
        }
    }
}
```
