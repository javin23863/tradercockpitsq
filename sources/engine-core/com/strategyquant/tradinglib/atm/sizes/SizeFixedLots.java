package com.strategyquant.tradinglib.atm.sizes;

import com.strategyquant.lib.XMLUtil;
import org.jdom2.Element;

public class SizeFixedLots extends AbstractATMPositionSize {
   private double Lots;

   public SizeFixedLots(Element var1, int var2, double var3) {
      super(var1, var2, var3);
      this.Lots = XMLUtil.getItemDoubleParam(var1, "Lots", 0.01);
   }

   @Override
   public Element getXML() {
      Element var1 = new Element("PositionSize");
      var1.setAttribute("key", "SizeFixedLots");
      XMLUtil.setItemDoubleParam(var1, "Lots", this.Lots);
      return var1;
   }

   @Override
   public double computeSize(double var1, double var3, boolean var5) {
      if (var5) {
         return var1 - var3;
      }

      double var6 = this.Lots;
      if (var6 < this.minimalSize) {
         var6 = this.minimalSize;
      }

      return var6;
   }

   @Override
   public String toString() {
      return String.format("Close %s lots", this.Lots);
   }
}
