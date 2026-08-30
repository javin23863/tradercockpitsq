package com.strategyquant.tradinglib.atm.sizes;

import org.jdom2.Element;

public abstract class AbstractATMPositionSize {
   protected int sizeDecimals;
   protected double minimalSize;

   public AbstractATMPositionSize(Element var1, int var2, double var3) {
      this.sizeDecimals = var2;
      this.minimalSize = var3;
   }

   public abstract double computeSize(double var1, double var3, boolean var5);

   public abstract Element getXML();
}
