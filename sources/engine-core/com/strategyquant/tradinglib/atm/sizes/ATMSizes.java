package com.strategyquant.tradinglib.atm.sizes;

import com.strategyquant.lib.L;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.task.settings.buildmode.JSONAble;
import org.jdom2.Element;

public class ATMSizes extends JSONAble {
   public static final String PERCENTS = "SizePercents";
   public static final String FIXED_SIZE = "SizeFixedLots";
   public static final String ALL_REMAINING = "SizeAllRemaining";

   public static AbstractATMPositionSize create(Element var0, int var1, double var2) throws Exception {
      String var4 = XMLUtil.getStringAttr(var0, "key", "SizePercents");
      switch (var4) {
         case "SizePercents":
            return new SizePercents(var0, var1, var2);
         case "SizeFixedLots":
            return new SizeFixedLots(var0, var1, var2);
         case "SizeAllRemaining":
            return new SizeAllRemaining(var0, var1, var2);
         default:
            throw new Exception(L.t("Unknown position size type '%s'", new Object[]{var4}));
      }
   }
}
