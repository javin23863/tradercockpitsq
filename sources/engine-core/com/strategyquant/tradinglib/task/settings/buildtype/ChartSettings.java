package com.strategyquant.tradinglib.task.settings.buildtype;

import java.io.Serializable;

public class ChartSettings implements Serializable {
   public int minConditions;
   public int maxConditions;
   public int minExitConditions;
   public int maxExitConditions;
   public int minPeriod;
   public int maxPeriod;
   public int minShift;
   public int maxShift;
   public int maxExitTypes;
}
