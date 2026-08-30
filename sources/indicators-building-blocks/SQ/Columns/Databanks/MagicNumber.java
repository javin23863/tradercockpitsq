package SQ.Columns.Databanks;

import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.ResultsGroup;
import java.util.List;
import org.jdom2.Element;

public class MagicNumber extends DatabankColumn {
   public MagicNumber() {
      super("Magic number", "Text", (byte)2, 0.0, 0.0, 0.0);
      this.setWidth(150);
      this.setEditable(true);
   }

   public String getValue(ResultsGroup var1, String var2, byte var3, byte var4, byte var5) throws Exception {
      String var6 = this.getMN(var1);
      return "{{inputWidget value='" + var6 + "'}}";
   }

   public boolean setValue(Object var1, Object var2) throws Exception {
      ResultsGroup var3 = (ResultsGroup)var1;
      Element var4 = var3.getStrategyXml().getChild("Strategy").getChild("Variables");
      List var5 = var4.getChildren();

      for (int var6 = 0; var6 < var5.size(); var6++) {
         Element var7 = (Element)var5.get(var6);
         String var8 = var7.getChild("name").getText();
         if (var8.equals("MagicNumber")) {
            var7.getChild("value").setText(var2.toString());
            break;
         }
      }

      return true;
   }

   public String exportValue(ResultsGroup var1, String var2, byte var3, byte var4, byte var5) throws Exception {
      return this.getMN(var1);
   }

   private String getMN(ResultsGroup var1) throws Exception {
      String var2 = "N/A";
      Element var3 = var1.getStrategyXml().getChild("Strategy").getChild("Variables");
      List var4 = var3.getChildren();

      for (int var5 = 0; var5 < var4.size(); var5++) {
         Element var6 = (Element)var4.get(var5);
         String var7 = var6.getChild("name").getText();
         if (var7.equals("MagicNumber")) {
            var2 = var6.getChild("value").getText();
            break;
         }
      }

      return var2;
   }
}
