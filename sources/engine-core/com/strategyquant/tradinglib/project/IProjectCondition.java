package com.strategyquant.tradinglib.project;

import com.strategyquant.pluginlib.ISQPlugin;
import com.strategyquant.tradinglib.taskImpl.ISQTask;
import org.jdom2.Element;

public interface IProjectCondition extends ISQPlugin {
   String getName();

   String getTitle();

   ProjectConditionField[] getFields();

   void readSettings(Element var1) throws Exception;

   boolean isMet(SQProject var1, ISQTask var2) throws Exception;

   IProjectCondition clone();

   String getDescription();

   String getDescriptionFormat();

   String getLastCheckedValue();
}
