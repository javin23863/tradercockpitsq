package com.strategyquant.tradinglib.task.settings.partstoimprove;

import java.io.Serializable;

public class PartsToImprove implements Serializable {
   public boolean useEntryLongImprovement;
   public boolean useEntryShortImprovement;
   public String entryLongAction;
   public String entryShortAction;
   public boolean useOrderLongImprovement;
   public boolean useOrderShortImprovement;
   public boolean useExitLongImprovement;
   public boolean useExitShortImprovement;
   public String exitLongAction;
   public String exitShortAction;
   public boolean improveATM;
}
