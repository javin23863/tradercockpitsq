package com.strategyquant.tradinglib.task.settings.buildtype;

import java.io.Serializable;

public class BuildType implements Serializable {
   public String strategyType;
   public int additionalCharts;
   public String templateFile;
   public String improveType;
   public String strategyFile;
   public String improveDatabank;
   public String direction;
   public boolean entrySymmetry;
   public boolean exitSymmetry;
}
