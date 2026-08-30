package com.strategyquant.tradinglib.conditions;

import com.strategyquant.lib.utils.ISQCloneable;

public interface IConditionValue extends ISQCloneable<IConditionValue> {
   String AdditionalMarket = "AdditionalMarket";
   String SampleType = "SampleType";

   double getValue(Object... var1) throws Exception;

   double getStatsValue(Object... var1) throws Exception;

   String getLabel(Object... var1) throws Exception;

   String getTitle() throws Exception;

   String getConditionParamValue(String var1, String var2);

   String getCrossCheckSettingName();

   boolean statsExists(Object... var1);

   String printFormatedValue(double var1) throws Exception;
}
