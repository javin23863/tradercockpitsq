package com.strategyquant.tradinglib.propertygrid;

import com.strategyquant.lib.settings.IXMLAble;

public interface IPGParameter extends IXMLAble {
   String getName();

   String getKey();

   String getCategory();

   String getEngine();

   String getActivator();

   void setActivator(String var1);

   void setCategory(String var1);

   void setEngine(String var1);

   int getType();

   String print();

   void setValue(String var1);
}
