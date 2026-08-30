package com.strategyquant.tradinglib.generator;

import org.jdom2.Element;

public interface IStrategyTemplate {
   Element generate() throws Exception;

   void setUseSameIdForLongShort(boolean var1);
}
