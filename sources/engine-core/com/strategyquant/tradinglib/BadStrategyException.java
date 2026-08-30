package com.strategyquant.tradinglib;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.crosscheck.CrossCheckMethod;
import com.strategyquant.tradinglib.exception.StrategyProblem;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.ArrayList;

public class BadStrategyException extends TradingException {
   public static final int ReasonNoTrades = 1;
   public static final int ReasonTooLittleTrades = 2;
   public static final int ReasonZeroPLTrades = 4;
   public static final int ReasonTooShortTrades = 8;
   public static final int ReasonZeroDurationTrades = 16;
   public static final int ReasonTooManyOpenTrades = 32;
   public static final int ReasonTooLongTrade = 64;
   public static final int ReasonNoFilledTrades = 256;
   public static final int ReasonOutlierTrade = 512;
   public static final int ReasonAmbiguousTrades = 1024;
   public static final int ReasonStockpickerShiftChanged = 2048;
   public static final int ReasonThreshold = 10000;
   public static final int ReasonBacktestException = 10001;
   public static final int ReasonStopped = 10002;
   public static final int ReasonExceptionEvaluatingConditions = 10003;
   public static final int ReasonCrossCheckException = 10004;
   public static final int ReasonInitialPopulation = 10005;
   public static final int ReasonStrategyTooSimilar = 10006;
   public static final int ReasonReplacedWithBetterStrategy = 10007;
   public static final int ReasonOptProfileProfitableOptimizations = 10008;
   public static final int ReasonOptProfileAvgProfit = 10009;
   public static final int ReasonOptProfileUniformDistribution = 10010;
   public static final int ReasonOptProfileBestOptimizationProfit = 10011;
   public static final int ReasonAdditionalMarketsFilter = 10012;
   public static final int ReasonWrongParamsException = 10013;
   public static final int ReasonCorrelationWithExistingPortfolio = 10014;
   public static final int ReasonPortfolioMasterCorrelationLimitation = 10015;
   public static final int ReasonPortfolioMasterSectorLimitation = 10016;
   public static final int ReasonStrategyRequireXreservedBars = 10017;
   public static final int ReasonCCExceptionMCManipulation = 10030;
   public static final int ReasonCCExceptionMCRetest = 10031;
   public static final int ReasonCCExceptionOptProfileSPP = 10032;
   public static final int ReasonCCExceptionAddMarkets = 10033;
   public static final int ReasonCCExceptionWFMatrix = 10034;
   public static final int ReasonCCExceptionWFOptim = 10035;
   public static final int ReasonCCExceptionHigherBacktestPrecis = 10036;
   public static final int ReasonCCExceptionWhatIf = 10037;
   public static final int ReasonCustomAnalysisFilter = 10038;
   public static final int ReasonCCExceptionSequentialOptimization = 10039;
   public static final String TxtShortReasonNoTrades = L.t("no trades", new Object[0]);
   public static final String TxtShortReasonTooLittleTrades = L.t("too little trades", new Object[0]);
   public static final String TxtShortReasonZeroPLTrades = L.t("zero PL trades", new Object[0]);
   public static final String TxtShortReasonTooShortTrades = L.t("too many trades closing at the same bar", new Object[0]);
   public static final String TxtShortReasonZeroDurationTrades = L.t("zero duration trades", new Object[0]);
   public static final String TxtShortReasonTooManyOpenTrades = L.t("too many open trades", new Object[0]);
   public static final String TxtShortReasonTooLongTrade = L.t("unfinished trades", new Object[0]);
   public static final String TxtShortReasonNoFilledTrades = L.t("no filled trades", new Object[0]);
   private static final String TxtShortReasonThreshold = L.t("Fitness threshold not reached", new Object[0]);
   private static final String TxtShortReasonBacktestException = L.t("backtest exception", new Object[0]);
   private static final String TxtReasonCrossCheckException = L.t("cross check exception", new Object[0]);
   private static final String TxtReasonCustomAnalysisFilter = L.t("custom analysis filter failed", new Object[0]);
   private static final String TxtShortReasonStopped = L.t("test stopped", new Object[0]);
   private static final String TxtShortReasonExceptionEvaluatingConditions = L.t("exception evaluating conditions", new Object[0]);
   public static final String TxtShortReasonInitialPopulation = L.t("initial population", new Object[0]);
   public static final String TxtShortReasonStrategyTooSimilar = L.t("too similar strategy in databank", new Object[0]);
   public static final String TxtShortReasonOutlierTrade = L.t("outlier trade - one exceptionally big trade", new Object[0]);
   public static final String TxtShortReasonAmbiguousTrades = L.t("too many ambiguous trades", new Object[0]);
   public static final String TxtShortReplacedWithBetterStrategy = L.t("replaced with better strategy", new Object[0]);
   public static final String TxtShortOptProfileProfitableOptimizations = L.t(
      "Cross Check filter in 'Opt. Profile / Sys. Param. Permutation': % of Profitable Optimizations", new Object[0]
   );
   public static final String TxtShortOptProfileAvgProfit = L.t("Cross Check filter in 'Opt. Profile / Sys. Param. Permutation': average profit", new Object[0]);
   public static final String TxtShortOptProfileUniformDistribution = L.t(
      "Cross Check filter in 'Opt. Profile / Sys. Param. Permutation': uniform distribution", new Object[0]
   );
   public static final String TxtShortOptProfileBestOptimizationProfit = L.t(
      "Cross Check filter in 'Opt. Profile / Sys. Param. Permutation': best Optimization profit", new Object[0]
   );
   public static final String TxtShorAdditionalMarketsFilter = L.t(
      "Cross Check filter in 'Retest on additional markets': additional markets filter failed (multiple conditions)", new Object[0]
   );
   private static final String TxtShortReasonWrongParamsException = L.t("wrong params exception", new Object[0]);
   public static final String TxtLongReasonNoTrades = L.t("Strategy produces no trades", new Object[0]);
   public static final String TxtLongReasonTooLittleTrades = L.t(
      "Strategy produces too little trades, results are not statistically significant", new Object[0]
   );
   public static final String TxtLongReasonZeroPLTrades = L.t("Strategy produces trades with zero P/L", new Object[0]);
   public static final String TxtLongReasonTooShortTrades = L.t("Strategy produces too many trades closing at the same bar", new Object[0]);
   public static final String TxtLongReasonZeroDurationTrades = L.t(
      "Strategy produces trades with zero duration - trades that are closed immediately after they are opened", new Object[0]
   );
   public static final String TxtLongReasonTooManyOpenTrades = L.t("Strategy produces too many open trades", new Object[0]);
   public static final String TxtLongReasonTooLongTrade = L.t(
      "Strategy produces unfinished trade(s) - trade that is not closed until the very end of trading", new Object[0]
   );
   public static final String TxtLongReasonNoFilledTrades = L.t("Strategy produces no filled trades", new Object[0]);
   public static final String TxtLongReasonInitialPopulation = L.t("initial population", new Object[0]);
   public static final String TxtLongReasonStrategyTooSimilar = L.t("Strategy is too similar or the same as some other strategy in databank", new Object[0]);
   public static final String TxtLongReasonOutlierTrade = L.t("Strategy produces outlier trade - one exceptionally big trade", new Object[0]);
   public static final String TxtLongReasonAmbiguousTrades = L.t(
      "Strategy produces too many ambiguous trades - where it is not possible to determine if SL or PT is reached first", new Object[0]
   );
   public static final String TxtLongReplacedWithBetterStrategy = L.t("Replaced with better strategy", new Object[0]);
   public static final String TxtLongOptProfileProfitableOptimizations = L.t(
      "Cross Check filter in 'Opt. Profile / Sys. Param. Permutation': % of Profitable Optimizations", new Object[0]
   );
   public static final String TxtLongOptProfileAvgProfit = L.t("Cross Check filter in 'Opt. Profile / Sys. Param. Permutation': average profit", new Object[0]);
   public static final String TxtLongOptProfileUniformDistribution = L.t(
      "Cross Check filter in 'Opt. Profile / Sys. Param. Permutation': uniform distribution", new Object[0]
   );
   public static final String TxtLongOptProfileBestOptimizationProfit = L.t(
      "Cross Check filter in 'Opt. Profile / Sys. Param. Permutation': best Optimization profit", new Object[0]
   );
   public static final String TxtLongAdditionalMarketsFilter = L.t(
      "Cross Check filter in 'Retest on additional markets': additional markets filter failed (multiple conditions)", new Object[0]
   );
   public static final String TxtReasonCCExceptionMCManipulation = L.t(
      "Cross Check filter in '%s': Automatic filter", new Object[]{CrossCheckMethod.NameMCManipulation}
   );
   public static final String TxtReasonCCExceptionMCRetest = L.t("Cross Check filter in '%s': Automatic filter", new Object[]{CrossCheckMethod.NameMCRetest});
   public static final String TxtReasonCCExceptionOptProfileSPP = L.t(
      "Cross Check filter in '%s': Automatic filter", new Object[]{CrossCheckMethod.NameOptProfileSPP}
   );
   public static final String TxtReasonCCExceptionAddMarkets = L.t(
      "Cross Check filter in '%s': Automatic filter", new Object[]{CrossCheckMethod.NameAddMarkets}
   );
   public static final String TxtReasonCCExceptionWFMatrix = L.t("Cross Check filter in '%s': Automatic filter", new Object[]{CrossCheckMethod.NameWFMatrix});
   public static final String TxtReasonCCExceptionWFOptim = L.t(
      "Cross Check filter in '%s': Automatic filter", new Object[]{CrossCheckMethod.NameWFOptimization}
   );
   public static final String TxtReasonCCExceptionSequentialOptimization = L.t(
      "Cross Check filter in '%s': Automatic filter", new Object[]{CrossCheckMethod.NameSequentialOptimization}
   );
   public static final String TxtReasonCCExceptionHigherBacktestPrecis = L.t(
      "Cross Check filter in '%s': Automatic filter", new Object[]{CrossCheckMethod.NameHigherBacktestPrecis}
   );
   public static final String TxtCorrelationWithExistingPortfolio = L.t("Correlation with existing portfolio", new Object[0]);
   public static final String TxtPortfolioMasterCorrelationLimitation = L.t("Correlation limitation", new Object[0]);
   public static final String TxtPortfolioMasterSectorLimitation = L.t("Sector limitation", new Object[0]);
   public static final String TxtStrategyRequireXreservedBars = L.t("Strategy require X reserved bars", new Object[0]);
   public static final int PrefixInitialConditions = 100000;
   public static final int PrefixNormalConditions = 200000;
   public static final int PrefixCrossCheckConditions = 300000;
   public static final String ExplanationUnknown = L.t("Unknown", new Object[0]);
   public static final String ReplacedWorseStrategy = "ReplacedWorseStrategy";
   public static final String TxtLongReasonStockpickerShiftChanged = L.t(
         "Your strategy contains condition that uses Close, High, Low, Typical, Median, Weighted with Shift=0 in combination with trigger On Bar Open.",
         new Object[0]
      )
      + "<br>"
      + L.t("This is not allowed, it would lead to incorrect backtests (looking into future). Shift for this condition was changed to 1.", new Object[0]);
   private int reason;

   public BadStrategyException(int var1) {
      this.reason = var1;
   }

   public int getReason() {
      return this.reason;
   }

   public static String getReasonAsString(int var0) {
      switch (var0) {
         case 1:
            return L.t("Automatic filter", new Object[0]) + ": " + TxtShortReasonNoTrades;
         case 2:
            return L.t("Automatic filter", new Object[0]) + ": " + TxtShortReasonTooLittleTrades;
         case 4:
            return L.t("Automatic filter", new Object[0]) + ": " + TxtShortReasonZeroPLTrades;
         case 8:
            return L.t("Automatic filter", new Object[0]) + ": " + TxtShortReasonTooShortTrades;
         case 16:
            return L.t("Automatic filter", new Object[0]) + ": " + TxtShortReasonZeroDurationTrades;
         case 32:
            return L.t("Automatic filter", new Object[0]) + ": " + TxtShortReasonTooManyOpenTrades;
         case 64:
            return L.t("Automatic filter", new Object[0]) + ": " + TxtShortReasonTooLongTrade;
         case 256:
            return L.t("Automatic filter", new Object[0]) + ": " + TxtShortReasonNoFilledTrades;
         case 512:
            return L.t("Automatic filter", new Object[0]) + ": " + TxtShortReasonOutlierTrade;
         case 1024:
            return L.t("Automatic filter", new Object[0]) + ": " + TxtShortReasonAmbiguousTrades;
         case 2048:
            return TxtLongReasonStockpickerShiftChanged;
         case 10000:
            return TxtShortReasonThreshold;
         case 10001:
            return TxtShortReasonBacktestException;
         case 10002:
            return TxtShortReasonStopped;
         case 10003:
            return TxtShortReasonExceptionEvaluatingConditions;
         case 10004:
            return TxtReasonCrossCheckException;
         case 10005:
            return TxtShortReasonInitialPopulation;
         case 10006:
            return TxtShortReasonStrategyTooSimilar;
         case 10007:
            return TxtShortReplacedWithBetterStrategy;
         case 10008:
            return TxtShortOptProfileProfitableOptimizations;
         case 10009:
            return TxtShortOptProfileAvgProfit;
         case 10010:
            return TxtShortOptProfileUniformDistribution;
         case 10011:
            return TxtShortOptProfileBestOptimizationProfit;
         case 10012:
            return TxtShorAdditionalMarketsFilter;
         case 10013:
            return TxtShortReasonWrongParamsException;
         case 10014:
            return TxtCorrelationWithExistingPortfolio;
         case 10015:
            return TxtPortfolioMasterCorrelationLimitation;
         case 10016:
            return TxtPortfolioMasterSectorLimitation;
         case 10017:
            return TxtStrategyRequireXreservedBars;
         case 10030:
            return TxtReasonCCExceptionMCManipulation;
         case 10031:
            return TxtReasonCCExceptionMCRetest;
         case 10032:
            return TxtReasonCCExceptionOptProfileSPP;
         case 10033:
            return TxtReasonCCExceptionAddMarkets;
         case 10034:
            return TxtReasonCCExceptionWFMatrix;
         case 10035:
            return TxtReasonCCExceptionWFOptim;
         case 10036:
            return TxtReasonCCExceptionHigherBacktestPrecis;
         case 10038:
            return TxtReasonCustomAnalysisFilter;
         case 10039:
            return TxtReasonCCExceptionSequentialOptimization;
         default:
            if (var0 >= 100000 && var0 < 200000) {
               return L.t("initial condition: %d", new Object[]{var0 - 100000});
            } else if (var0 >= 200000 && var0 < 300000) {
               return L.t("condition: %d", new Object[]{var0 - 200000});
            } else {
               return var0 >= 300000 ? L.t("cross check condition: %d", new Object[]{var0 - 300000}) : String.format("%s: %d", ExplanationUnknown, var0);
            }
      }
   }

   public static boolean isBadStrategyException(int var0) {
      switch (var0) {
         case 1:
         case 2:
         case 4:
         case 8:
         case 16:
         case 32:
         case 64:
         case 256:
         case 512:
         case 1024:
            return true;
         default:
            return false;
      }
   }

   public String getReasonAsString() {
      return getReasonAsString(this.reason);
   }

   public static boolean check(int var0, int var1) {
      return (var0 & var1) != 0;
   }

   public static int setOrThrow(int var0, boolean var1, int var2) throws BadStrategyException {
      if ((var0 & var2) != 0) {
         throw new BadStrategyException(var2);
      } else {
         return var2;
      }
   }

   public static String getExplanation(int var0) {
      switch (var0) {
         case 1:
            return TxtLongReasonNoTrades;
         case 2:
            return TxtLongReasonTooLittleTrades;
         case 4:
            return TxtLongReasonZeroPLTrades;
         case 8:
            return TxtLongReasonTooShortTrades;
         case 16:
            return TxtLongReasonZeroDurationTrades;
         case 32:
            return TxtLongReasonTooManyOpenTrades;
         case 64:
            return TxtLongReasonTooLongTrade;
         case 256:
            return TxtLongReasonNoFilledTrades;
         case 512:
            return TxtLongReasonOutlierTrade;
         case 1024:
            return TxtLongReasonAmbiguousTrades;
         case 2048:
            return TxtLongReasonStockpickerShiftChanged;
         case 10005:
            return TxtLongReasonInitialPopulation;
         case 10006:
            return TxtLongReasonStrategyTooSimilar;
         case 10007:
            return TxtLongReplacedWithBetterStrategy;
         case 10008:
            return TxtLongOptProfileProfitableOptimizations;
         case 10009:
            return TxtLongOptProfileAvgProfit;
         case 10010:
            return TxtLongOptProfileUniformDistribution;
         case 10011:
            return TxtLongOptProfileBestOptimizationProfit;
         case 10012:
            return TxtLongAdditionalMarketsFilter;
         case 10014:
            return TxtCorrelationWithExistingPortfolio;
         case 10015:
            return TxtPortfolioMasterCorrelationLimitation;
         case 10016:
            return TxtPortfolioMasterSectorLimitation;
         case 10030:
            return TxtReasonCCExceptionMCManipulation;
         case 10031:
            return TxtReasonCCExceptionMCRetest;
         case 10032:
            return TxtReasonCCExceptionOptProfileSPP;
         case 10033:
            return TxtReasonCCExceptionAddMarkets;
         case 10034:
            return TxtReasonCCExceptionWFMatrix;
         case 10035:
            return TxtReasonCCExceptionWFOptim;
         case 10036:
            return TxtReasonCCExceptionHigherBacktestPrecis;
         case 10038:
            return TxtReasonCustomAnalysisFilter;
         case 10039:
            return TxtReasonCCExceptionSequentialOptimization;
         default:
            return ExplanationUnknown;
      }
   }

   public static String reasonToShortText(int var0) {
      switch (var0) {
         case 1:
            return TxtShortReasonNoTrades;
         case 2:
            return TxtShortReasonTooLittleTrades;
         case 4:
            return TxtShortReasonZeroPLTrades;
         case 8:
            return TxtShortReasonTooShortTrades;
         case 16:
            return TxtShortReasonZeroDurationTrades;
         case 32:
            return TxtShortReasonTooManyOpenTrades;
         case 64:
            return TxtShortReasonTooLongTrade;
         case 256:
            return TxtShortReasonNoFilledTrades;
         case 512:
            return TxtShortReasonOutlierTrade;
         case 1024:
            return TxtShortReasonAmbiguousTrades;
         default:
            return ExplanationUnknown;
      }
   }

   public static IntArrayList getSubReasons(int var0) {
      IntArrayList var1 = new IntArrayList();
      if (var0 < 10000) {
         if ((var0 & 1) > 0) {
            var1.add(1);
         }

         if ((var0 & 2) > 0) {
            var1.add(2);
         }

         if ((var0 & 4) > 0) {
            var1.add(4);
         }

         if ((var0 & 8) > 0) {
            var1.add(8);
         }

         if ((var0 & 16) > 0) {
            var1.add(16);
         }

         if ((var0 & 32) > 0) {
            var1.add(32);
         }

         if ((var0 & 64) > 0) {
            var1.add(64);
         }

         if ((var0 & 256) > 0) {
            var1.add(256);
         }

         if ((var0 & 512) > 0) {
            var1.add(512);
         }

         if ((var0 & 1024) > 0) {
            var1.add(1024);
         }
      } else {
         var1.add(var0);
      }

      return var1;
   }

   public static ArrayList<StrategyProblem> listProblems() {
      ArrayList var0 = new ArrayList();
      var0.add(new StrategyProblem(1, TxtShortReasonNoTrades, TxtLongReasonNoTrades));
      var0.add(new StrategyProblem(4, TxtShortReasonZeroPLTrades, TxtLongReasonZeroPLTrades));
      var0.add(new StrategyProblem(1024, TxtShortReasonAmbiguousTrades, TxtLongReasonAmbiguousTrades));
      var0.add(new StrategyProblem(16, TxtShortReasonZeroDurationTrades, TxtLongReasonZeroDurationTrades));
      var0.add(new StrategyProblem(32, TxtShortReasonTooManyOpenTrades, TxtLongReasonTooManyOpenTrades));
      var0.add(new StrategyProblem(64, TxtShortReasonTooLongTrade, TxtLongReasonTooLongTrade));
      var0.add(new StrategyProblem(256, TxtShortReasonNoFilledTrades, TxtLongReasonNoFilledTrades));
      var0.add(new StrategyProblem(2, TxtShortReasonTooLittleTrades, TxtLongReasonTooLittleTrades));
      var0.add(new StrategyProblem(512, TxtShortReasonOutlierTrade, TxtLongReasonOutlierTrade));
      var0.add(new StrategyProblem(8, TxtShortReasonTooShortTrades, TxtLongReasonTooShortTrades));
      return var0;
   }

   public static int getFirstReason(int var0) {
      if ((var0 & 1) > 0) {
         return 1;
      } else if ((var0 & 2) > 0) {
         return 2;
      } else if ((var0 & 4) > 0) {
         return 4;
      } else if ((var0 & 8) > 0) {
         return 8;
      } else if ((var0 & 16) > 0) {
         return 16;
      } else if ((var0 & 32) > 0) {
         return 32;
      } else if ((var0 & 64) > 0) {
         return 64;
      } else if ((var0 & 256) > 0) {
         return 256;
      } else if ((var0 & 512) > 0) {
         return 512;
      } else {
         return (var0 & 1024) > 0 ? 1024 : 0;
      }
   }

   public static int getFirstReason(int var0, int var1) {
      if ((var0 & 1) > 0 && (var1 & 1) > 0) {
         return 1;
      } else if ((var0 & 2) > 0 && (var1 & 2) > 0) {
         return 2;
      } else if ((var0 & 4) > 0 && (var1 & 4) > 0) {
         return 4;
      } else if ((var0 & 8) > 0 && (var1 & 8) > 0) {
         return 8;
      } else if ((var0 & 16) > 0 && (var1 & 16) > 0) {
         return 16;
      } else if ((var0 & 32) > 0 && (var1 & 32) > 0) {
         return 32;
      } else if ((var0 & 64) > 0 && (var1 & 64) > 0) {
         return 64;
      } else if ((var0 & 256) > 0 && (var1 & 256) > 0) {
         return 256;
      } else if ((var0 & 512) > 0 && (var1 & 512) > 0) {
         return 512;
      } else {
         return (var0 & 1024) > 0 && (var1 & 1024) > 0 ? 1024 : 0;
      }
   }

   public String getMessage() {
      return getReasonAsString(this.reason);
   }
}
