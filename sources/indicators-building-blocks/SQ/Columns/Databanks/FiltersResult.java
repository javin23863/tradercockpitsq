package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.results.SpecialValues;

public class FiltersResult extends DatabankColumn {
   public FiltersResult() {
      super(L.tsq("Filters result"), "Text", (byte)1, 0.0, 0.0, 100.0);
      this.setTooltip(L.tsq("Filters result"));
      this.setWidth(100);
   }

   public String getValue(ResultsGroup var1, String var2, byte var3, byte var4, byte var5) throws Exception {
      SettingsMap var6 = var1.specialValues();
      if (var6.containsKey(SpecialValues.FiltersResultFailedReason)) {
         return var6.getString(SpecialValues.FiltersResultFailedReason).equals(SpecialValues.FiltersResultPassed)
            ? "{{tooltipWidget text='" + L.t("PASSED", new Object[0]) + "' select='PASSED' class='passed'}}"
            : "{{tooltipWidget text='"
               + L.t("FAILED", new Object[0])
               + "' tooltip='"
               + var6.getString(SpecialValues.FiltersResultFailedReason)
               + "' select='FAILED' class='failed'}}";
      } else {
         return "";
      }
   }

   public String exportValue(ResultsGroup var1, String var2, byte var3, byte var4, byte var5) throws Exception {
      SettingsMap var6 = var1.specialValues();
      if (var6.containsKey(SpecialValues.FiltersResultFailedReason)) {
         return var6.getString(SpecialValues.FiltersResultFailedReason).equals(SpecialValues.FiltersResultPassed)
            ? L.t("PASSED", new Object[0])
            : L.t("FAILED", new Object[0]);
      } else {
         return "";
      }
   }
}
