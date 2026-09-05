# Merging multiple results into portfolio

- Source: <https://strategyquant.com/doc/programming-for-sq/merging-multiple-results-into-portfolio/>
- Section: Programming for StrategyQuant X › Coding sessions examples
- Fetched: 2026-09-04

---

In this article we’ll show how StrategyQuant X handles backtest results, and how we can merge them into portfolio.

This can already be done by in SQX by selecting multiple strategies and choosing **Portfolio** -> **Merge strategies**, but we’ll show how it is done on the background.

This is intended as a first part of multiple series, we’d like to use this functionality to explore if trading could be improved by picking particular subset of strategies to trade – this will be examined in the next articles.

*Note that by merging to portfolio we mean merging the final backtest results into one portfolio result, not merging individual trading strategies.*

This feature is again made in form of Custom Analysis snippet, and the complete snippet is attached to this article.

## Working with backtest results – ResultsGroup object

Just a quick reminder that in SQX every backtest results are stored in Result object, and they are grouped into ResultsGroup object.

RegultGroup is what you see in databank – it can contain one or more results – backtests of strategy on main market, additional markets, etc.

There is an article with introduction: [Working with ResultsGroup](../01-introduction/working-with-resultsgroup.md)

When we want to merge multiple strategy results into one portfolio we have to

- create new ResultsGroup object that will hold the portfolio
- go through ResultsGroup object of every strategy in databank
- find the main result – the one containing backtest on main data – this is the one we’ll be merging
- create a new Result object for this strategy and add it to portfolio
- clone and copy orders from original ResultsGroup to portfolio
- copy symbols from original ResultsGroup to portfolio – this is necessary for computation of PL etc.
- in the end create a new Portfolio result and compute all stats

## Full source code of the snippet

The code is extensively commented, you can follow its logic.

It will create two new Portfolio results – one created by us manually, and another created by SQX method for comparison.

```
package SQ.CustomAnalysis;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.utils.IUniqueNameChecker;
import com.strategyquant.tradinglib.*;
import com.strategyquant.tradinglib.project.ProjectEngine;
import com.strategyquant.tradinglib.results.SpecialValues;
import com.strategyquant.tradinglib.results.stats.comparator.OrderComparatorByOpenTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class CAMergeStrategiesToPortfolio extends CustomAnalysisMethod {
    public static final Logger Log = LoggerFactory.getLogger("CAMergeStrategiesToPortfolio");

    //------------------------------------------------------------------------
    //------------------------------------------------------------------------
    //------------------------------------------------------------------------

    public CAMergeStrategiesToPortfolio() {
        super("Merge Strategies To Portfolio", TYPE_PROCESS_DATABANK);
    }

    //------------------------------------------------------------------------

    @Override
    public boolean filterStrategy(String project, String task, String databankName, ResultsGroup rg) throws Exception {
        return false;
    }

    //------------------------------------------------------------------------

    @Override
    public ArrayList<ResultsGroup> processDatabank(String project, String task, String databankName, ArrayList<ResultsGroup> databankRG) throws Exception {
        if(databankRG.size() <= 1) {
            // if there is no or just one strategy there is nothing to merge
            return databankRG;
        }

        // get target databank to save the final result
        Databank targetDatabank = ProjectEngine.get(project).getDatabanks().get("Portfolios");
        if(targetDatabank == null) {
            throw new Exception("Please create a new databank named 'Portfolios' !");
        }

        // ------------------------------
        // Creating portfolio manually

        // merge all strategies in databank to one portfolio
        ResultsGroup caPortfolio = mergeStrategiesToPortfolio(databankRG);

        // create final Portfolio result to sum all sub-results
        createPortfolioResult(caPortfolio);

        // add new portfolio to our target databank
        targetDatabank.add(caPortfolio, false);

        // ------------------------------
        // Creating portfolio in a standard way by calling SQ method - for control
        ResultsGroup standardPortfolio = ResultsGroup.merge(databankRG, new String[] { ResultsGroup.AdditionalMarket }, null);

        standardPortfolio.createPortfolioResult(PortfolioInitialBalanceTypes.SINGLE, null);

        targetDatabank.add(standardPortfolio, false);

        return databankRG;
    }

    //------------------------------------------------------------------------

    private ResultsGroup mergeStrategiesToPortfolio(ArrayList<ResultsGroup> databankRG) throws Exception {
        ResultsGroup caPortfolio = new ResultsGroup("PortfolioMadeByCA");

        // add individual strategies to portfolio
        for(ResultsGroup rg : databankRG) {
            addResult(caPortfolio, rg);
        }

        addSymbolsMap(caPortfolio, databankRG);

        return caPortfolio;
    }

    //------------------------------------------------------------------------

    private void addSymbolsMap(ResultsGroup rgCSPortfolio, ArrayList<ResultsGroup> databankRG) {
        for(ResultsGroup rg : databankRG) {
            // add all symbols from merged RGs to portfolio symbols map
            rgCSPortfolio.symbols().add(rg.symbols());
        }
    }

    //------------------------------------------------------------------------

    private void addResult(ResultsGroup caPortfolio, ResultsGroup rg) throws Exception {

        // first create a new result for RG in our new portfolio
        // newResultKey will be returned as its result key used in our portfolio
        String newResultKey = mergeResult(caPortfolio, rg);

        // next clone & copy orders for this results from RG to portfolio
        copyOrdersToPortfolio(caPortfolio, rg, newResultKey);

        // add all symbol data from merged RG to portfolio symbols map - this is necessary for computation of PL etc.
        caPortfolio.symbols().add(rg.symbols());
    }

    //------------------------------------------------------------------------

    private String mergeResult(ResultsGroup caPortfolio, ResultsGroup rg) throws Exception {
        // get main result from current strategy - this is the one we'll be merging
        Result rgMainResult = rg.mainResult();
        String mainResultKey = rgMainResult.getResultKey();

        // determine new result key - it must be unique so we have to check
        // if there isn't result with this key already in our portfolio
        String newResultKey = rg.getName();

        if(caPortfolio.hasResult(newResultKey)) {
            // same key already exists, we have to find a new key
            newResultKey = SQUtils.generateUniqueName(mainResultKey, new IUniqueNameChecker() {
                public boolean checkNameExist(String name) throws Exception {
                    return caPortfolio.hasResult(name);
                }
            });
        }

        // create merged result
        SettingsMap copiedSettingsMap = rgMainResult.getSettings().clone();

        // create new merged result ad add it to our portfolio
        Result mergedResult = new Result(newResultKey, caPortfolio, copiedSettingsMap);
        caPortfolio.addSubresult(newResultKey, copiedSettingsMap, mergedResult);

        // set symbol and timeframe from merged strategy
        mergedResult.setString(SpecialValues.Symbol, rg.mainResult().getString(SpecialValues.Symbol));
        mergedResult.setString(SpecialValues.Timeframe, rg.mainResult().getString(SpecialValues.Timeframe));

        long rgDateFrom = rg.specialValues().getLong(SpecialValues.HistoryFrom, Long.MAX_VALUE);
        long rgDateTo = rg.specialValues().getLong(SpecialValues.HistoryTo, Long.MIN_VALUE);

        long caDateFrom = caPortfolio.specialValues().getLong(SpecialValues.PortfolioHistoryFrom, Long.MAX_VALUE);
        long caDateTo = caPortfolio.specialValues().getLong(SpecialValues.PortfolioHistoryTo, Long.MIN_VALUE);

        caPortfolio.specialValues().set(SpecialValues.HistoryFrom, Math.min(rgDateFrom, caDateFrom));
        caPortfolio.specialValues().set(SpecialValues.HistoryTo, Math.max(rgDateTo, caDateTo));
        copiedSettingsMap.set(SettingsKeys.PortfolioDataStart, Math.min(rgDateFrom, caDateFrom));
        copiedSettingsMap.set(SettingsKeys.PortfolioDataEnd, Math.max(rgDateTo, caDateTo));

        return newResultKey;
    }

    //------------------------------------------------------------------------

    private void copyOrdersToPortfolio(ResultsGroup caPortfolio, ResultsGroup rg, String newResultKey) throws Exception {
        // get orders list from our current portfolio
        OrdersList mergedOrdersList = caPortfolio.orders();

        // get all orders from merging strategy result - note that they must be cloned!
        String mainResultKey = rg.mainResult().getResultKey();
        OrdersList filteredOL = rg.orders().filterWithClone(mainResultKey, Directions.Both, SampleTypes.FullSample);

        // no go through these orders and add them to portfolio orders list
        for(int i=0; i<filteredOL.size(); i++) {
            Order order = filteredOL.get(i);

            // order.SetupName is used as identifier so that we know to which result the order belongs to
            // so we have to set it to newResultKey
            order.SetupName = newResultKey;

            mergedOrdersList.add(order);
        }
    }

    //------------------------------------------------------------------------

    private void createPortfolioResult(ResultsGroup caPortfolio) throws Exception {

        SettingsMap settings = caPortfolio.mainResult().getSettings();

        Result portfolioResult = new Result(ResultsGroup.Portfolio, caPortfolio, settings);

        caPortfolio.removeSubresult(ResultsGroup.Portfolio, true);

        caPortfolio.addSubresult(ResultsGroup.Portfolio, settings, portfolioResult);

        caPortfolio.specialValues().setString(SpecialValues.Symbol, ResultsGroup.Portfolio);

        // for portfolio sort orders by their open time
        OrdersList ordersList = caPortfolio.orders();
        ordersList.sort(new OrderComparatorByOpenTime());

        portfolioResult.computeAllStats(caPortfolio.specialValues(), caPortfolio.getOOS());

        caPortfolio.updated = true;
    }

}
```
