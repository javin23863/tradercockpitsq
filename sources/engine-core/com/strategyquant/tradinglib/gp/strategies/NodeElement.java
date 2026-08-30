package com.strategyquant.tradinglib.gp.strategies;

import org.jdom2.Element;

public class NodeElement {
   public Element element;
   public Element elementParent;

   public NodeElement(Element var1, Element var2) {
      this.element = var1;
      this.elementParent = var2;
   }

   public NodeElement(Element var1) {
      this.element = var1;
      this.elementParent = var1.getParentElement();
   }
}
