package com.strategyquant.plugin.Task.impl.AutomaticPortfolioBuilder.bruteForce;

import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.backtestrunner.BacktestResult;
import java.util.ArrayList;

public interface PMComputationListener {
   PortfolioCombination checkPortfolio(int[] var1) throws Exception;

   ArrayList<ResultsGroup> getResults(int[] var1) throws Exception;

   void portfolioComputed(BacktestResult var1) throws Exception;

   void printLog(String var1);

   void resetStats();

   boolean isStopped();
}
