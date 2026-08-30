package com.strategyquant.tradinglib.options;

import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.tradinglib.TradingOption;
import com.strategyquant.tradinglib.options.parameters.ReservedBars;
import com.strategyquant.tradinglib.options.parameters.StoreChartData;
import java.util.ArrayList;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TradingOptions extends ArrayList<TradingOption> {
   public static final Logger Log = LoggerFactory.getLogger("TradingOptions");

   public TradingOptions getClone() throws CloneNotSupportedException {
      TradingOptions var1 = new TradingOptions();

      for (int var2 = 0; var2 < this.size(); var2++) {
         TradingOption var3 = this.get(var2);
         var1.add(var3.getClone());
      }

      return var1;
   }

   public static String list() {
      Element var0 = new Element("Options");
      boolean var1 = MainApp.checkProduct("BACKTESTNODE");

      for (TradingOption var4 : TradingOptionsList.getInstance().getAvailableClasses()) {
         if (var4 != null && (!var1 || !(var4 instanceof StoreChartData) && !(var4 instanceof ReservedBars))) {
            var0.addContent(var4.getXML());
         }
      }

      return XMLUtil.elementToString(var0);
   }
}
