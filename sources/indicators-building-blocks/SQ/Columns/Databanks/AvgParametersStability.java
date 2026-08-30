package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.results.SpecialValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AvgParametersStability extends DatabankColumn {
   public static final Logger Log = LoggerFactory.getLogger("AvgParametersStability");

   public AvgParametersStability() {
      super(L.tsq("Avg. Parameters Stability"), "Decimal2", (byte)1, 0.0, 0.0, 1.0);
      this.setTooltip(L.tsq("Avg. Parameters Stability - how stable are parameters in Walk Forward results (averaged for all WF optimiations in WF Matrix)"));
      this.setPLTypeRestrictions(new byte[]{10});
      this.setDirectionRestrictions(new byte[]{0});
   }

   public String getValue(ResultsGroup var1, String var2, byte var3, byte var4, byte var5) throws Exception {
      SettingsMap var6 = var1.specialValues();
      return var6.containsKey(SpecialValues.AvgParametersStability) ? var6.getDouble(SpecialValues.AvgParametersStability) + "" : "N/A";
   }
}
