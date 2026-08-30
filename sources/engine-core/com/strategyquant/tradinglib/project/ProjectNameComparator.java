package com.strategyquant.tradinglib.project;

import java.util.Comparator;

public class ProjectNameComparator implements Comparator<SQProject> {
   public int compare(SQProject var1, SQProject var2) {
      return var1.getName().compareToIgnoreCase(var2.getName());
   }
}
