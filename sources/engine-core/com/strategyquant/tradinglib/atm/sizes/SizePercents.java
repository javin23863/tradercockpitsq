package com.strategyquant.tradinglib.atm.sizes;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.XMLUtil;
import org.jdom2.Element;

public class SizePercents extends AbstractATMPositionSize {
   private int Percents;

   public SizePercents(Element var1, int var2, double var3) {
      super(var1, var2, var3);
      this.Percents = XMLUtil.getItemIntParam(var1, "Percents", 50);
   }

   @Override
   public Element getXML() {
      Element var1 = new Element("PositionSize");
      var1.setAttribute("key", "SizePercents");
      XMLUtil.setItemIntParam(var1, "Percents", this.Percents);
      return var1;
   }

   @Override
   public double computeSize(double var1, double var3, boolean var5) {
      if (var5) {
         return var1 - var3;
      }

      double var6 = SQUtils.round(var1 / 100.0 * this.Percents, this.sizeDecimals);
      if (var6 < this.minimalSize) {
         var6 = this.minimalSize;
      }

      return var6;
   }

   @Override
   public String toString() {
      return String.format("Close %s%% of position", this.Percents);
   }
}
