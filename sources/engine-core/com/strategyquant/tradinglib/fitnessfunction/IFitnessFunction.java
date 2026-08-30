package com.strategyquant.tradinglib.fitnessfunction;

import com.strategyquant.pluginlib.ISQPlugin;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.optimization.MetricForFitness;
import java.io.Serializable;
import java.util.ArrayList;
import org.jdom2.Element;

public interface IFitnessFunction extends ISQPlugin, Serializable {
   String ComputeFromStrategyResult = "ComputeFromStrategyResult";

   double computeFitness(ResultsGroup var1, byte var2, byte var3) throws Exception;

   double computeFitness(ResultsGroup var1, byte var2, byte var3, boolean var4) throws Exception;

   String getFitnessKey();

   String getFitnessName();

   void initFitnessFromXml(Element var1);

   byte getFitnessType() throws Exception;

   String getFitnessDatabankColumnName();

   ArrayList<DatabankColumn> getUsedStatValues() throws Exception;

   String printWeightedGoals() throws Exception;

   ArrayList<MetricForFitness> getMetricsForFitness();

   double getMetricValue(ResultsGroup var1, byte var2, byte var3, String var4) throws Exception;

   IFitnessFunction clone();
}
