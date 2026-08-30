package com.strategyquant.tradinglib.project;

import org.json.JSONObject;

public class ProjectProgressInfo {
   public static final String ACTION_LOAD = "load";
   public static final String ACTION_DELETE = "delete";
   public static final String ACTION_TYPE_CHANGE = "type-change";
   public static final String ACTION_MAX_STRATEGIES_CHANGE = "max-strategies-change";
   public static final String ACTION_RETEST = "retest";
   public static final String ACTION_SAVE_EA = "saveEA";
   public static final String ACTION_CREATE_PORTFOLIO = "Create Portfolio";
   public static final String ACTION_MERGE_STRATEGIES = "Merge strategies";
   public static final String ACTION_SPLIT_STRATEGIES = "Split strategies";
   public static final String ACTION_MOVE_TO_PM = "Move to Portfolio Master";
   public static final String ACTION_MOVE_TO_PC = "Move to Portfolio Composer";
   public String action;
   public double percent;
   public String error;
   public boolean hasExtra;
   public String info;
   public String extraName;
   public JSONObject extraValue;
}
