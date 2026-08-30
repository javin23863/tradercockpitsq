package com.strategyquant.tradinglib.atm.exits;

import com.strategyquant.lib.L;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.task.settings.buildmode.JSONAble;
import org.jdom2.Element;

public class ATMExitLevels extends JSONAble {
   public static final String SL_BASED = "MultipleOfOriginalSL";
   public static final String PT_BASED = "MultipleOfOriginalPT";
   public static final String FIXED_PROFIT = "FixedProfit";
   public static final String TRAILING_STOP = "TrailingStop";
   public static final String NONE = "None";

   public static AbstractExitLevel create(Element var0, int var1) throws Exception {
      String var2 = XMLUtil.getStringAttr(var0, "key", "MultipleOfOriginalSL");
      switch (var2) {
         case "MultipleOfOriginalSL":
            return new MultipleOfOriginalSL(var0, var1);
         case "MultipleOfOriginalPT":
            return new MultipleOfOriginalPT(var0, var1);
         case "FixedProfit":
            return new FixedProfit(var0, var1);
         case "TrailingStop":
            return new TrailingStop(var0, var1);
         case "None":
            return new None(var0, var1);
         default:
            throw new Exception(L.t("Unknown exit level type '%s'", new Object[]{var2}));
      }
   }
}
