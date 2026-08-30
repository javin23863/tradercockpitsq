package com.strategyquant.tradinglib.generator;

import org.jdom2.Element;

class RandomAndOr {
   public static final int Unset = -1;
   public static final int Random = 1;
   public static final int Copied = 2;
   Element elRnd;
   int id;
   int type;
   public String key;

   public RandomAndOr(Element var1) {
      this.elRnd = var1;
      this.id = var1.getAttributeValue("identification").hashCode();
      this.type = -1;
   }
}
