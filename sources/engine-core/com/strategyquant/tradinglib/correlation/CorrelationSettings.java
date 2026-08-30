package com.strategyquant.tradinglib.correlation;

import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.CorrelationType;
import java.io.Serializable;
import org.jdom2.Element;

public class CorrelationSettings implements Serializable {
   public double maxCorrelation;
   public CorrelationType correlationType;
   public int correlationPeriod;
   public boolean allowNegativeCorrelation;
   public boolean addEmptyPeriods;

   public void loadFromXml(Element var1) throws Exception {
      this.maxCorrelation = XMLUtil.getDouble(var1, "CorrMax", 0.3);
      String var2 = XMLUtil.getNodeValue(var1, "CorrType", "ProfitLoss");
      this.correlationType = (CorrelationType)CorrelationTypes.getInstance().findClassByName(var2);
      this.correlationPeriod = XMLUtil.getInt(var1, "CorrPeriod", 10);
      this.allowNegativeCorrelation = XMLUtil.getBoolean(var1, "CorrAllowNegative", true);
      this.addEmptyPeriods = XMLUtil.getBoolean(var1, "CorrAddEmptyPeriods", false);
   }
}
