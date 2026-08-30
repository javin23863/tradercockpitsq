package com.strategyquant.tradinglib;

import com.strategyquant.datalib.TradingException;
import org.jdom2.Element;

public interface IBlock {
   double True = 1.0;
   double False = 0.0;

   double evaluateBlock() throws TradingException;

   double evaluateBlock(int var1) throws TradingException;

   IBlock newInstance(StrategyBase var1, Element var2) throws BlockDefinitionException;

   StrategyBase getStrategy();

   IBlock clone(boolean var1, StrategyBase var2) throws BlockDefinitionException;

   void setCustomData(String var1, int var2);

   int getCustomData(String var1);

   void copyCustomData(IBlock var1);

   int getReturnType();

   void setReturnType(int var1);

   Element getCustomBlockXml(int var1) throws BlockDefinitionException;
}
