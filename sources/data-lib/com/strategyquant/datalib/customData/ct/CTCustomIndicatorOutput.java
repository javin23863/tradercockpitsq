package com.strategyquant.datalib.customData.ct;

import com.strategyquant.lib.XMLUtil;
import org.jdom2.Element;

public class CTCustomIndicatorOutput {
   public String name;
   public String el;
   public String jf;
   public String mt4;
   public String mt5;

   public CTCustomIndicatorOutput(Element var1) throws Exception {
      this.name = XMLUtil.getAttr(var1, "name");
      this.el = var1.getChildText("EL");
      this.jf = var1.getChildText("JF");
      this.mt4 = var1.getChildText("MT4");
      this.mt5 = var1.getChildText("MT5");
   }
}
