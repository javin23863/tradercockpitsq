package com.strategyquant.tradinglib.results;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.concurrent.locks.StampedLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatsKeyCache {
   public static final Logger Log = LoggerFactory.getLogger("StatsKeyCache");
   public static final byte TypeInt = 1;
   public static final byte TypeLong = 2;
   public static final byte TypeDouble = 3;
   public static final byte TypeObject = 4;
   public static final Int2IntOpenHashMap defaultShortKeysMap = new Int2IntOpenHashMap(
      new int[]{
         "NmbLss".hashCode(),
         "MxCnsLss".hashCode(),
         "TtlTrdMons".hashCode(),
         "StgntnPrd".hashCode(),
         "MxCnsWns".hashCode(),
         "NmbPrfts".hashCode(),
         "TtlTrdYrs".hashCode(),
         "NmbCncld".hashCode(),
         "TtlTrdDs".hashCode(),
         "DgrsFrdm".hashCode(),
         "NmbTrds".hashCode(),
         "MxLss".hashCode(),
         "ShrpRt".hashCode(),
         "Cmmsn".hashCode(),
         "AvgCnsLss".hashCode(),
         "ZPrbb".hashCode(),
         "ZScr".hashCode(),
         "RXpScr".hashCode(),
         "Ahpr".hashCode(),
         "ClmrRt".hashCode(),
         "DD".hashCode(),
         "NtPrft".hashCode(),
         "Stblt".hashCode(),
         "AvgTrdsMnt".hashCode(),
         "PipDD".hashCode(),
         "AvgPrftYr".hashCode(),
         "GrsLss".hashCode(),
         "AvgPrftMnt".hashCode(),
         "AvgTrd".hashCode(),
         "AvgAbsTrd".hashCode(),
         "AvgPrftD".hashCode(),
         "AvgCnsWn".hashCode(),
         "PctDD".hashCode(),
         "WnLssRt".hashCode(),
         "AarDDRt".hashCode(),
         "AnnPctRet".hashCode(),
         "RetDDRt".hashCode(),
         "PayRt".hashCode(),
         "PrfFctr".hashCode(),
         "AvgBrWn".hashCode(),
         "Sng".hashCode(),
         "AvgBrTrd".hashCode(),
         "AvgWn".hashCode(),
         "AvfLss".hashCode(),
         "CAGR".hashCode(),
         "RXpctnc".hashCode(),
         "RXpctncScr".hashCode(),
         "Smmtr".hashCode(),
         "SqnScr".hashCode(),
         "AvgTrdsD".hashCode(),
         "AvgTrdsY".hashCode(),
         "Expc".hashCode(),
         "GrssPrft".hashCode(),
         "AvrBrLss".hashCode(),
         "WinPct".hashCode(),
         "StagPct".hashCode(),
         "MxPrft".hashCode(),
         "Expsr".hashCode(),
         "IntDep".hashCode(),
         "Fitness".hashCode(),
         "StdDev".hashCode(),
         "StagFrm".hashCode(),
         "StagTo".hashCode(),
         "SHPR".hashCode(),
         "AmbTrd".hashCode(),
         "AmbTrdPct".hashCode(),
         "BckDur".hashCode(),
         "FiltRes".hashCode(),
         "MinEC".hashCode(),
         "RSQR".hashCode(),
         "StabSQ3".hashCode(),
         "TrdSym".hashCode(),
         "WYP".hashCode(),
         "ActDD".hashCode(),
         "ActDDPct".hashCode(),
         "NSym".hashCode(),
         "StgTo".hashCode(),
         "StgFrom".hashCode(),
         "TF".hashCode(),
         "Sym".hashCode(),
         "ResNam".hashCode(),
         "Note".hashCode(),
         "WFMaxDDbyRun".hashCode(),
         "WFMaxPctDDbyRun".hashCode(),
         "WFMaxProfitByRun".hashCode(),
         "WFMaxProfitByRunInPct".hashCode(),
         "WFMaxStagnationInPct".hashCode(),
         "WFMinTradesInRun".hashCode(),
         "WFPctOfProfitableRuns".hashCode(),
         "NetPrPct".hashCode(),
         "NetPrPips".hashCode(),
         "AvgPctPrYr".hashCode(),
         "CommSwapM".hashCode(),
         "SlipM".hashCode(),
         "LongestTrade".hashCode(),
         "ProfitableMonths".hashCode(),
         "ProfitableMonthsPct".hashCode(),
         "TtlDDs".hashCode(),
         "TtlDMs".hashCode(),
         "TtlDYs".hashCode(),
         "ASTDR".hashCode(),
         "APS".hashCode(),
         "WPS".hashCode(),
         "Effic".hashCode(),
         "KllFrm".hashCode(),
         "MID".hashCode(),
         "TSI".hashCode(),
         "ODDPct".hashCode(),
         "DG".hashCode(),
         "DLM".hashCode()
      },
      new int[]{
         "NumberOfLosses".hashCode(),
         "MaxConsecLosses".hashCode(),
         "TotalTradingMonths".hashCode(),
         "Stagnation".hashCode(),
         "MaxConsecWins".hashCode(),
         "NumberOfProfits".hashCode(),
         "TotalTradingYears".hashCode(),
         "NumberOfCanceled".hashCode(),
         "TotalTradingDays".hashCode(),
         "DegreesOfFreedom".hashCode(),
         "NumberOfTrades".hashCode(),
         "MaxLoss".hashCode(),
         "SharpeRatio".hashCode(),
         "Commission".hashCode(),
         "AvgConsecLosses".hashCode(),
         "ZProbability".hashCode(),
         "ZScore".hashCode(),
         "RExpectancyscore".hashCode(),
         "AHPR".hashCode(),
         "CalmarRatio".hashCode(),
         "Drawdown".hashCode(),
         "NetProfit".hashCode(),
         "Stability".hashCode(),
         "AvgTradesPerMonth".hashCode(),
         "DrawdownPips".hashCode(),
         "AvgProfitPerYear".hashCode(),
         "GrossLoss".hashCode(),
         "AvgProfitPerMonth".hashCode(),
         "AvgTrade".hashCode(),
         "AvgAbsTrade".hashCode(),
         "AvgProfitPerDay".hashCode(),
         "AvgConsecWins".hashCode(),
         "DrawdownPct".hashCode(),
         "WinLossRatio".hashCode(),
         "AnnualPctReturnDDRatio".hashCode(),
         "AnnualPctReturn".hashCode(),
         "ReturnDDRatio".hashCode(),
         "PayoutRatio".hashCode(),
         "ProfitFactor".hashCode(),
         "AvgBarsWin".hashCode(),
         "SQN".hashCode(),
         "AvgBarsInTrade".hashCode(),
         "AvgWin".hashCode(),
         "AvgLoss".hashCode(),
         "CAGR".hashCode(),
         "RExpectancy".hashCode(),
         "RExpectancyScore".hashCode(),
         "Symmetry".hashCode(),
         "SQNScore".hashCode(),
         "AvgTradesPerDay".hashCode(),
         "AvgTradesPerYear".hashCode(),
         "Expectancy".hashCode(),
         "GrossProfit".hashCode(),
         "AvgBarsLoss".hashCode(),
         "WinningPct".hashCode(),
         "StagnationPct".hashCode(),
         "MaxProfit".hashCode(),
         "Exposure".hashCode(),
         "InitialDeposit".hashCode(),
         "Fitness".hashCode(),
         "StandardDev".hashCode(),
         "StagnationTo".hashCode(),
         "StagnationFrom".hashCode(),
         "SHPR".hashCode(),
         "AmbiguousTrades".hashCode(),
         "AmbiguousTradesPct".hashCode(),
         "BacktestDuration".hashCode(),
         "FiltersResult".hashCode(),
         "MiniEquityChart".hashCode(),
         "RSquared".hashCode(),
         "StabilitySQ3".hashCode(),
         "TradesSymmetry".hashCode(),
         "WorstYearProfit".hashCode(),
         "ActualDD".hashCode(),
         "ActualDrawdownPct".hashCode(),
         "NSymmetry".hashCode(),
         "StagnationTo".hashCode(),
         "StagnationFrom".hashCode(),
         "TimeFrame".hashCode(),
         "Symbol".hashCode(),
         "ResultsName".hashCode(),
         "Note".hashCode(),
         "WFMaxDDbyRun".hashCode(),
         "WFMaxPctDDbyRun".hashCode(),
         "WFMaxProfitByRun".hashCode(),
         "WFMaxProfitByRunInPct".hashCode(),
         "WFMaxStagnationInPct".hashCode(),
         "WFMinTradesInRun".hashCode(),
         "WFPctOfProfitableRuns".hashCode(),
         "NetProfitInPct".hashCode(),
         "NetProfitInPips".hashCode(),
         "AvgPctProfitPerYear".hashCode(),
         "CommSwapInMoney".hashCode(),
         "SlippageInMoney".hashCode(),
         "LongestTrade".hashCode(),
         "ProfitableMonths".hashCode(),
         "ProfitableMonthsPct".hashCode(),
         "TotalDataDays".hashCode(),
         "TotalDataMonths".hashCode(),
         "TotalDataYears".hashCode(),
         "AvgTrStddevRatio".hashCode(),
         "AvgParametersStability".hashCode(),
         "WorstParametersStability".hashCode(),
         "Efficiency".hashCode(),
         "KellyFormula".hashCode(),
         "MaxIntradayDrawdown".hashCode(),
         "TSIndex".hashCode(),
         "OpenDrawdownPct".hashCode(),
         "DateGenerated".hashCode(),
         "DateLastModified".hashCode()
      }
   );
   public static final Int2ObjectOpenHashMap<StatsKeyCache.StatInfo> defaultKeysMap = new Int2ObjectOpenHashMap(
      new int[]{
         "NumberOfLosses".hashCode(),
         "MaxConsecLosses".hashCode(),
         "TotalTradingMonths".hashCode(),
         "Stagnation".hashCode(),
         "MaxConsecWins".hashCode(),
         "NumberOfProfits".hashCode(),
         "TotalTradingYears".hashCode(),
         "NumberOfCanceled".hashCode(),
         "TotalTradingDays".hashCode(),
         "DegreesOfFreedom".hashCode(),
         "NumberOfTrades".hashCode(),
         "Complexity".hashCode(),
         "LongestTrade".hashCode(),
         "ProfitableMonths".hashCode(),
         "TotalDataDays".hashCode(),
         "TotalDataMonths".hashCode(),
         "TotalDataYears".hashCode(),
         "MaxLoss".hashCode(),
         "SharpeRatio".hashCode(),
         "Commission".hashCode(),
         "AvgConsecLosses".hashCode(),
         "ZProbability".hashCode(),
         "ZScore".hashCode(),
         "RExpectancyscore".hashCode(),
         "AHPR".hashCode(),
         "CalmarRatio".hashCode(),
         "Drawdown".hashCode(),
         "NetProfit".hashCode(),
         "Stability".hashCode(),
         "AvgTradesPerMonth".hashCode(),
         "DrawdownPips".hashCode(),
         "AvgProfitPerYear".hashCode(),
         "GrossLoss".hashCode(),
         "AvgProfitPerMonth".hashCode(),
         "AvgTrade".hashCode(),
         "AvgAbsTrade".hashCode(),
         "AvgProfitPerDay".hashCode(),
         "AvgConsecWins".hashCode(),
         "DrawdownPct".hashCode(),
         "WinLossRatio".hashCode(),
         "AnnualPctReturnDDRatio".hashCode(),
         "AnnualPctReturn".hashCode(),
         "ReturnDDRatio".hashCode(),
         "PayoutRatio".hashCode(),
         "ProfitFactor".hashCode(),
         "AvgBarsWin".hashCode(),
         "SQN".hashCode(),
         "AvgBarsInTrade".hashCode(),
         "AvgWin".hashCode(),
         "AvgLoss".hashCode(),
         "CAGR".hashCode(),
         "RExpectancy".hashCode(),
         "RExpectancyScore".hashCode(),
         "Symmetry".hashCode(),
         "SQNScore".hashCode(),
         "AvgTradesPerDay".hashCode(),
         "AvgTradesPerYear".hashCode(),
         "Expectancy".hashCode(),
         "GrossProfit".hashCode(),
         "AvgBarsLoss".hashCode(),
         "WinningPct".hashCode(),
         "StagnationPct".hashCode(),
         "MaxProfit".hashCode(),
         "Exposure".hashCode(),
         "InitialDeposit".hashCode(),
         "Fitness".hashCode(),
         "StandardDev".hashCode(),
         "SHPR".hashCode(),
         "AmbiguousTrades".hashCode(),
         "AmbiguousTradesPct".hashCode(),
         "BacktestDuration".hashCode(),
         "FiltersResult".hashCode(),
         "MiniEquityChart".hashCode(),
         "RSquared".hashCode(),
         "StabilitySQ3".hashCode(),
         "TradesSymmetry".hashCode(),
         "WorstYearProfit".hashCode(),
         "ActualDD".hashCode(),
         "ActualDrawdownPct".hashCode(),
         "NSymmetry".hashCode(),
         "TimeFrame".hashCode(),
         "Symbol".hashCode(),
         "ResultsName".hashCode(),
         "Note".hashCode(),
         "WFMaxDDbyRun".hashCode(),
         "WFMaxPctDDbyRun".hashCode(),
         "WFMaxProfitByRun".hashCode(),
         "WFMaxProfitByRunInPct".hashCode(),
         "WFMaxStagnationInPct".hashCode(),
         "WFMinTradesInRun".hashCode(),
         "WFPctOfProfitableRuns".hashCode(),
         "ExitIndicators".hashCode(),
         "EntryIndicators".hashCode(),
         "MagicNumber".hashCode(),
         "WFScore".hashCode(),
         "PriceIndicators".hashCode(),
         "BiggestMAE".hashCode(),
         "NetProfitInPct".hashCode(),
         "NetProfitInPips".hashCode(),
         "AvgPctProfitPerYear".hashCode(),
         "CommSwapInMoney".hashCode(),
         "SlippageInMoney".hashCode(),
         "OpenDrawdown".hashCode(),
         "ProfitableMonthsPct".hashCode(),
         "AvgTrStddevRatio".hashCode(),
         "AvgParametersStability".hashCode(),
         "WorstParametersStability".hashCode(),
         "Efficiency".hashCode(),
         "KellyFormula".hashCode(),
         "MaxIntradayDrawdown".hashCode(),
         "TSIndex".hashCode(),
         "OpenDrawdownPct".hashCode(),
         "StagnationTo".hashCode(),
         "StagnationFrom".hashCode(),
         "DateGenerated".hashCode(),
         "DateLastModified".hashCode()
      },
      new StatsKeyCache.StatInfo[]{
         new StatsKeyCache.StatInfo(0, "NumberOfLosses", "NmbLss", 1),
         new StatsKeyCache.StatInfo(1, "MaxConsecLosses", "MxCnsLss", 1),
         new StatsKeyCache.StatInfo(2, "TotalTradingMonths", "TtlTrdMons", 1),
         new StatsKeyCache.StatInfo(3, "Stagnation", "StgntnPrd", 1),
         new StatsKeyCache.StatInfo(4, "MaxConsecWins", "MxCnsWns", 1),
         new StatsKeyCache.StatInfo(5, "NumberOfProfits", "NmbPrfts", 1),
         new StatsKeyCache.StatInfo(6, "TotalTradingYears", "TtlTrdYrs", 1),
         new StatsKeyCache.StatInfo(7, "NumberOfCanceled", "NmbCncld", 1),
         new StatsKeyCache.StatInfo(8, "TotalTradingDays", "TtlTrdDs", 1),
         new StatsKeyCache.StatInfo(9, "DegreesOfFreedom", "DgrsFrdm", 1),
         new StatsKeyCache.StatInfo(10, "NumberOfTrades", "NmbTrds", 1),
         new StatsKeyCache.StatInfo(11, "Complexity", "Cmplxt", 1),
         new StatsKeyCache.StatInfo(12, "LongestTrade", "LongestTrade", 1),
         new StatsKeyCache.StatInfo(13, "ProfitableMonths", "ProfitableMonths", 1),
         new StatsKeyCache.StatInfo(14, "TotalDataDays", "TtlDDs", 1),
         new StatsKeyCache.StatInfo(15, "TotalDataMonths", "TtlDMs", 1),
         new StatsKeyCache.StatInfo(16, "TotalDataYears", "TtlDYs", 1),
         new StatsKeyCache.StatInfo(0, "MaxLoss", "MxLss", 3),
         new StatsKeyCache.StatInfo(1, "SharpeRatio", "ShrpRt", 3),
         new StatsKeyCache.StatInfo(2, "Commission", "Cmmsn", 3),
         new StatsKeyCache.StatInfo(3, "AvgConsecLosses", "AvgCnsLss", 3),
         new StatsKeyCache.StatInfo(4, "ZProbability", "ZPrbb", 3),
         new StatsKeyCache.StatInfo(5, "ZScore", "ZScr", 3),
         new StatsKeyCache.StatInfo(6, "RExpectancyscore", "RXpScr", 3),
         new StatsKeyCache.StatInfo(7, "AHPR", "Ahpr", 3),
         new StatsKeyCache.StatInfo(8, "CalmarRatio", "ClmrRt", 3),
         new StatsKeyCache.StatInfo(9, "Drawdown", "DD", 3),
         new StatsKeyCache.StatInfo(10, "NetProfit", "NtPrft", 3),
         new StatsKeyCache.StatInfo(11, "Stability", "Stblt", 3),
         new StatsKeyCache.StatInfo(12, "AvgTradesPerMonth", "AvgTrdsMnt", 3),
         new StatsKeyCache.StatInfo(13, "DrawdownPips", "PipDD", 3),
         new StatsKeyCache.StatInfo(14, "AvgProfitPerYear", "AvgPrftYr", 3),
         new StatsKeyCache.StatInfo(15, "GrossLoss", "GrsLss", 3),
         new StatsKeyCache.StatInfo(16, "AvgProfitPerMonth", "AvgPrftMnt", 3),
         new StatsKeyCache.StatInfo(17, "AvgTrade", "AvgTrd", 3),
         new StatsKeyCache.StatInfo(18, "AvgAbsTrade", "AvgAbsTrd", 3),
         new StatsKeyCache.StatInfo(19, "AvgProfitPerDay", "AvgPrftD", 3),
         new StatsKeyCache.StatInfo(20, "AvgConsecWins", "AvgCnsWn", 3),
         new StatsKeyCache.StatInfo(21, "DrawdownPct", "PctDD", 3),
         new StatsKeyCache.StatInfo(22, "WinLossRatio", "WnLssRt", 3),
         new StatsKeyCache.StatInfo(23, "AnnualPctReturnDDRatio", "AarDDRt", 3),
         new StatsKeyCache.StatInfo(24, "AnnualPctReturn", "AnnPctRet", 3),
         new StatsKeyCache.StatInfo(25, "ReturnDDRatio", "RetDDRt", 3),
         new StatsKeyCache.StatInfo(26, "PayoutRatio", "PayRt", 3),
         new StatsKeyCache.StatInfo(27, "ProfitFactor", "PrfFctr", 3),
         new StatsKeyCache.StatInfo(28, "AvgBarsWin", "AvgBrWn", 3),
         new StatsKeyCache.StatInfo(29, "SQN", "Sng", 3),
         new StatsKeyCache.StatInfo(30, "AvgBarsInTrade", "AvgBrTrd", 3),
         new StatsKeyCache.StatInfo(31, "AvgWin", "AvgWn", 3),
         new StatsKeyCache.StatInfo(32, "AvgLoss", "AvfLss", 3),
         new StatsKeyCache.StatInfo(33, "CAGR", "CAGR", 3),
         new StatsKeyCache.StatInfo(34, "RExpectancy", "RXpctnc", 3),
         new StatsKeyCache.StatInfo(35, "RExpectancyScore", "RXpctncScr", 3),
         new StatsKeyCache.StatInfo(36, "Symmetry", "Smmtr", 3),
         new StatsKeyCache.StatInfo(37, "SQNScore", "SqnScr", 3),
         new StatsKeyCache.StatInfo(38, "AvgTradesPerDay", "AvgTrdsD", 3),
         new StatsKeyCache.StatInfo(39, "AvgTradesPerYear", "AvgTrdsY", 3),
         new StatsKeyCache.StatInfo(40, "Expectancy", "Expc", 3),
         new StatsKeyCache.StatInfo(41, "GrossProfit", "GrssPrft", 3),
         new StatsKeyCache.StatInfo(42, "AvgBarsLoss", "AvrBrLss", 3),
         new StatsKeyCache.StatInfo(43, "WinningPct", "WinPct", 3),
         new StatsKeyCache.StatInfo(44, "StagnationPct", "StagPct", 3),
         new StatsKeyCache.StatInfo(45, "MaxProfit", "MxPrft", 3),
         new StatsKeyCache.StatInfo(46, "Exposure", "Expsr", 3),
         new StatsKeyCache.StatInfo(47, "InitialDeposit", "IntDep", 3),
         new StatsKeyCache.StatInfo(48, "Fitness", "Fitness", 3),
         new StatsKeyCache.StatInfo(49, "StandardDev", "StdDev", 3),
         new StatsKeyCache.StatInfo(50, "SHPR", "SHPR", 3),
         new StatsKeyCache.StatInfo(51, "AmbiguousTrades", "AmbTrd", 3),
         new StatsKeyCache.StatInfo(52, "AmbiguousTradesPct", "AmbTrdPct", 3),
         new StatsKeyCache.StatInfo(53, "BacktestDuration", "BckDur", 3),
         new StatsKeyCache.StatInfo(54, "FiltersResult", "FiltRes", 3),
         new StatsKeyCache.StatInfo(55, "MiniEquityChart", "MinEC", 3),
         new StatsKeyCache.StatInfo(56, "RSquared", "RSQR", 3),
         new StatsKeyCache.StatInfo(57, "StabilitySQ3", "StabSQ3", 3),
         new StatsKeyCache.StatInfo(58, "TradesSymmetry", "TrdSym", 3),
         new StatsKeyCache.StatInfo(59, "WorstYearProfit", "WYP", 3),
         new StatsKeyCache.StatInfo(60, "ActualDD", "ActDD", 3),
         new StatsKeyCache.StatInfo(61, "ActualDrawdownPct", "ActDDPct", 3),
         new StatsKeyCache.StatInfo(62, "NSymmetry", "NSym", 3),
         new StatsKeyCache.StatInfo(63, "TimeFrame", "TF", 3),
         new StatsKeyCache.StatInfo(64, "Symbol", "Sym", 3),
         new StatsKeyCache.StatInfo(65, "ResultsName", "ResNam", 3),
         new StatsKeyCache.StatInfo(66, "Note", "Note", 3),
         new StatsKeyCache.StatInfo(67, "WFMaxDDbyRun", "WFMaxDDbyRun", 3),
         new StatsKeyCache.StatInfo(68, "WFMaxPctDDbyRun", "WFMaxPctDDbyRun", 3),
         new StatsKeyCache.StatInfo(69, "WFMaxProfitByRun", "WFMaxProfitByRun", 3),
         new StatsKeyCache.StatInfo(70, "WFMaxProfitByRunInPct", "WFMaxProfitByRunInPct", 3),
         new StatsKeyCache.StatInfo(71, "WFMaxStagnationInPct", "WFMaxStagnationInPct", 3),
         new StatsKeyCache.StatInfo(72, "WFMinTradesInRun", "WFMinTradesInRun", 3),
         new StatsKeyCache.StatInfo(73, "WFPctOfProfitableRuns", "WFPctOfProfitableRuns", 3),
         new StatsKeyCache.StatInfo(74, "ExitIndicators", "ExitI", 3),
         new StatsKeyCache.StatInfo(75, "EntryIndicators", "EntryI", 3),
         new StatsKeyCache.StatInfo(76, "MagicNumber", "MagNum", 3),
         new StatsKeyCache.StatInfo(77, "WFScore", "WFScore", 3),
         new StatsKeyCache.StatInfo(78, "PriceIndicators", "PriceI", 3),
         new StatsKeyCache.StatInfo(79, "BiggestMAE", "BOL", 3),
         new StatsKeyCache.StatInfo(80, "NetProfitInPct", "NetPrPct", 3),
         new StatsKeyCache.StatInfo(81, "NetProfitInPips", "NetPrPips", 3),
         new StatsKeyCache.StatInfo(82, "AvgPctProfitPerYear", "AvgPctPrYr", 3),
         new StatsKeyCache.StatInfo(83, "CommSwapInMoney", "CommSwapM", 3),
         new StatsKeyCache.StatInfo(84, "SlippageInMoney", "SlipM", 3),
         new StatsKeyCache.StatInfo(85, "OpenDrawdown", "ODD", 3),
         new StatsKeyCache.StatInfo(86, "ProfitableMonthsPct", "ProfitableMonthsPct", 3),
         new StatsKeyCache.StatInfo(87, "AvgTrStddevRatio", "ASTDR", 3),
         new StatsKeyCache.StatInfo(88, "AvgParametersStability", "APS", 3),
         new StatsKeyCache.StatInfo(89, "WorstParametersStability", "WPS", 3),
         new StatsKeyCache.StatInfo(90, "Efficiency", "Effic", 3),
         new StatsKeyCache.StatInfo(91, "KellyFormula", "KllFrm", 3),
         new StatsKeyCache.StatInfo(92, "MaxIntradayDrawdown", "MID", 3),
         new StatsKeyCache.StatInfo(93, "TSIndex", "TSI", 3),
         new StatsKeyCache.StatInfo(94, "OpenDrawdownPct", "ODDPct", 3),
         new StatsKeyCache.StatInfo(0, "StagnationTo", "StagFrm", 2),
         new StatsKeyCache.StatInfo(1, "StagnationFrom", "StagTo", 2),
         new StatsKeyCache.StatInfo(2, "DateGenerated", "DG", 2),
         new StatsKeyCache.StatInfo(3, "DateLastModified", "DLM", 2)
      }
   );
   private static final Int2ObjectOpenHashMap<Object> unknownKeysMap = new Int2ObjectOpenHashMap();
   private static final StampedLock stampedLock = new StampedLock();
   private static int intsCount = 17;
   private static int doublesCount = 95;
   private static int longsCount = 4;

   public static StatsKeyCache.StatInfo getKey(String var0) {
      int var1 = var0.hashCode();
      return getKey(var1);
   }

   public static StatsKeyCache.StatInfo getKey(int var0) {
      return defaultKeysMap.containsKey(var0) ? (StatsKeyCache.StatInfo)defaultKeysMap.get(var0) : null;
   }

   public static StatsKeyCache.StatInfo getShortKey(int var0) {
      int var1 = -1111111111;
      if (defaultShortKeysMap.containsKey(var0)) {
         var1 = defaultShortKeysMap.get(var0);
      }

      return var1 == -1111111111 ? null : getKey(var1);
   }

   public static int registerKey(String var0, byte var1) {
      int var2 = var0.hashCode();
      if (defaultKeysMap.containsKey(var2)) {
         throw new IllegalArgumentException("Key " + var0 + "is known, use getKey() instead!");
      }

      long var3 = stampedLock.tryOptimisticRead();
      if (unknownKeysMap.containsKey(var2) && stampedLock.validate(var3)) {
         return var2;
      }

      var3 = stampedLock.writeLock();

      try {
         Log.debug("Unknown {} key added {}", typeToString(var1), var0);
         if (unknownKeysMap.containsKey(var2)) {
            return var2;
         }

         unknownKeysMap.put(var2, var0);
         Log.debug("Unknown {} key added {}", typeToString(var1), var0);
         return var2;
      } finally {
         stampedLock.unlockWrite(var3);
      }
   }

   public static String typeToString(byte var0) {
      switch (var0) {
         case 1:
            return "int";
         case 2:
            return "long";
         case 3:
            return "double";
         default:
            return "object";
      }
   }

   public static String getUnknownKeyName(int var0) {
      if (defaultKeysMap.containsKey(var0)) {
         throw new IllegalArgumentException("Key with hash " + var0 + "is known, use getKey() instead!");
      }

      long var1 = stampedLock.tryOptimisticRead();
      String var3 = (String)unknownKeysMap.get(var0);
      if (!stampedLock.validate(var1)) {
         var1 = stampedLock.readLock();

         try {
            var3 = (String)unknownKeysMap.get(var0);
         } finally {
            stampedLock.unlockRead(var1);
         }
      }

      return var3;
   }

   public static boolean isDefaultValue(int var0) {
      return defaultKeysMap.containsKey(var0);
   }

   public static int getIntsCount() {
      return intsCount;
   }

   public static int getDoublesCount() {
      return doublesCount;
   }

   public static int getLongsCount() {
      return longsCount;
   }

   public static class StatInfo {
      public int index;
      public int type;
      public String name;
      public String shortName;

      public StatInfo(int var1, String var2, String var3, int var4) {
         this.index = var1;
         this.name = var2;
         this.shortName = var3;
         this.type = var4;
      }
   }
}
