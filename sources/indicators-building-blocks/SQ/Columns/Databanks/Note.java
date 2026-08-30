package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.results.SpecialValues;

public class Note extends DatabankColumn {
   public Note() {
      super(L.tsq("Note"), "Text", (byte)2, 0.0, 0.0, 0.0);
      this.setWidth(150);
      this.setEditable(true);
   }

   public String getValue(ResultsGroup var1, String var2, byte var3, byte var4, byte var5) throws Exception {
      String var6 = var1.specialValues().getString(SpecialValues.Note, "");
      return "{{inputWidget value='" + var6 + "'}}";
   }

   public boolean setValue(Object var1, Object var2) {
      ResultsGroup var3 = (ResultsGroup)var1;
      var3.setNote(var2.toString());
      var3.trySaveToFile();
      return true;
   }

   public String exportValue(ResultsGroup var1, String var2, byte var3, byte var4, byte var5) throws Exception {
      return var1.getNote("");
   }
}
