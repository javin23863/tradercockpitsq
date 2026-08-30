package com.strategyquant.tradinglib.optimization;

import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.indicator.IndicatorsObj;
import java.io.Serializable;
import org.jdom2.Element;

public interface IStrategyModifier extends Serializable {
   StrategyParamData createStrategyVariation(StrategyBase var1, OptimizationSettings var2, short[] var3) throws Exception;

   StrategyBase createStrategyFromXml(Element var1, String var2) throws Exception;

   IndicatorsObj createIndicatorsObject(StrategyBase var1);
}
