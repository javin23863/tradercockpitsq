package com.strategyquant.tradinglib.atm.sizes;

import org.jdom2.Element;

public class SizeAllRemaining extends AbstractATMPositionSize {
   public SizeAllRemaining(Element var1, int var2, double var3) {
      super(var1, var2, var3);
   }

   @Override
   public Element getXML() {
      Element var1 = new Element("PositionSize");
      var1.setAttribute("key", "SizeAllRemaining");
      return var1;
   }

   @Override
   public double computeSize(double var1, double var3, boolean var5) {
      return var1 - var3;
   }

   @Override
   public String toString() {
      return "Close remaining position";
   }
}
