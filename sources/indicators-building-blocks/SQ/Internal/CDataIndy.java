package SQ.Internal;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.datalib.customData.CustomDataInfo;
import com.strategyquant.datalib.customData.CustomDataManager;
import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.data.CustomDataIndyCache;
import com.strategyquant.tradinglib.data.CustomDataIndyMap;
import org.jdom2.Element;

public class CDataIndy extends ValueBlock {
   @Parameter
   public int Value = 0;
   @Parameter
   public int Shift;
   private String Name;
   private CustomDataIndyMap map;
   private String DataType;
   private Element elBlock;

   @Override
   public double OnBlockEvaluate(int var1) throws TradingException {
      if (this.map == null) {
         Log.error("External Indicator {} doesn't have data!", this.Name);
         return 0.0;
      } else {
         long var2 = this.Strategy.MarketData.Chart(0).Time(var1 + this.Shift);
         return this.map.getTime(var2);
      }
   }

   @Override
   public IBlock newInstance(StrategyBase var1, Element var2) throws BlockDefinitionException {
      CDataIndy var3 = (CDataIndy)this.clone(true, var1);
      var3.initialize(var1, var2);
      var3.elBlock = var2.clone();
      var3.map = CustomDataIndyCache.getMapForIndy(var2);
      if (var3.map != null) {
         var3.Name = var3.map.getName();
         var3.Value = var3.map.getValueIndex();
         var3.Value = var3.map.getValueIndex();
      }

      return var3;
   }

   public CDataIndy cloneCDataIndy() {
      CDataIndy var1 = new CDataIndy();
      var1.Name = this.Name;
      var1.Shift = this.Shift;
      var1.Value = this.Value;
      var1.map = this.map;
      var1.elBlock = this.elBlock != null ? this.elBlock.clone() : null;
      return var1;
   }

   @Override
   public IBlock clone(boolean var1, StrategyBase var2) throws BlockDefinitionException {
      return this.cloneCDataIndy();
   }

   @Override
   public Element getCustomBlockXml(int var1) throws BlockDefinitionException {
      if (this.elBlock != null) {
         return this.elBlock;
      }

      this.elBlock = new Element("Item");
      this.elBlock.setAttribute("key", "CDataIndy_" + this.Name);
      Element var2 = new Element("Param");
      var2.setAttribute("key", "#Shift#");
      var2.setAttribute("controlType", "jspinnerVar");
      var2.setAttribute("type", "int");
      var2.setText(Integer.toString(this.Shift));
      this.elBlock.addContent(var2);
      var2 = new Element("Param");
      var2.setAttribute("key", "#Value#");
      var2.setAttribute("controlType", "combo");
      var2.setAttribute("type", "int");
      var2.setText(Integer.toString(this.Value));
      this.elBlock.addContent(var2);
      return this.elBlock;
   }

   public void negateValueIndex() {
      if (this.Name != null) {
         CustomDataInfo var1 = CustomDataManager.getDataInfo(this.Name);
         if (var1 != null) {
            if (var1.values == 2) {
               this.Value = this.Value == 1 ? 0 : 1;
            }
         }
      }
   }
}
