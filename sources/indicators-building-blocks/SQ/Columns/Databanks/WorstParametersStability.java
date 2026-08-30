package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.results.SpecialValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorstParametersStability extends DatabankColumn {
   public static final Logger Log = LoggerFactory.getLogger("WorstParametersStability");

   public WorstParametersStability() {
      super(L.tsq("Worst Parameters Stability"), "Decimal2", (byte)1, 0.0, 0.0, 1.0);
      this.setTooltip(L.tsq("Worst Parameters Stability - what is the worst parameters stability in all Walk Forward results"));
      this.setPLTypeRestrictions(new byte[]{10});
      this.setDirectionRestrictions(new byte[]{0});
   }

   public String getValue(ResultsGroup var1, String var2, byte var3, byte var4, byte var5) throws Exception {
      SettingsMap var6 = var1.specialValues();
      return var6.containsKey(SpecialValues.WorstParametersStability) ? var6.getDouble(SpecialValues.WorstParametersStability) + "" : "N/A";
   }
}
