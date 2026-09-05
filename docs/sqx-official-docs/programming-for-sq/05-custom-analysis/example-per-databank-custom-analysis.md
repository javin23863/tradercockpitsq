# Example – per databank custom analysis

- Source: <https://strategyquant.com/doc/programming-for-sq/example-per-databank-custom-analysis/>
- Section: Programming for StrategyQuant X › Custom analysis
- Fetched: 2026-09-04

---

This example is about per databank custom analysis. Unlike per strategy custom analysis which processes one strategy, this CA snippet runs over the whole databank, and it can access every strategy result in a databank.

Per databank custom analysis can run only from a custom task in the custom project.

In this example we’ll make a fairly complex example of custom analysis snippet that will build a portfolio from Walk-Forward results of the strategies in databank.  
This was a request from our first coding session.

## Step 1 – Create new Custom analysis snippet

Open CodeEditor, click on Create new and choose Custom analysis (CA) option at the very bottom. Name it **WFPortfolio**.

This will create a new snippet **WFPortfolio.java** in folder **User/Snippets/SQ/CustomAnalysis**

This time, we’ll set it as **TYPE\_PROCESS\_DATABANK** in the class constructor:

```
public WFPortfolio() {
    super("WFPortfolio", TYPE_PROCESS_DATABANK);
}
```

## Step 2 – Implement processDatabank() method

This CA calls method **processDatabank()** that has 4 parameters:

- **String project** – name of project where the CA task is placed
- **String task** – name of CA task
- **String databankName** – name of databank the CA snippet works on
- **ArrayList<ResultsGroup> databankRG** – list of all strategies ([ResultsGroups](../01-introduction/working-with-resultsgroup.md)) from this databank.

It also returns a list ArrayList<ResultsGroup>, that should contain only the strategies from input list that should be preserved in the databank.

This way it can be used as a filter – you can go through the strategies in databankRG list and create a new list where you’ll put only these that match some of your criterion. Strategies that are not in the output list will be deleted from the databank.

In our case we don’t want to delete something from databank but add a new strategy (our newly created portfolio) instead. We’ll see how it is done.

To sum it up the code does this:

- Creates new ResultsGroup for portfolio – it will be empty at the start
- Go through every strategy in the databank, checks if it has WF result. F yes, it will copy this result to a new portfolio including all the WF orders.
- Compute Portfolio result from subresults – by default it is not computed from WF results, so we must do it by ourselves in the code
- Add the finished portfolio ResultsGroup to the databank

The code is long and complex, we will not explain it all here. It is shown at the end of this article fully commented.

## Step 3 – running our custom CA snippet

To run a “per databank” CA snippet we need to create a custom project and a custom task of type **CustomAnalysis**.

Select the **WFPortfolio** in the custom task.

[![Custom Analysis task](https://strategyquant.com/wp-content/uploads/2021/11/wf_equity_portfolio_task-750x135.png)](https://strategyquant.com/wp-content/uploads/2021/11/wf_equity_portfolio_task-750x135.png)

Then add some strategies with Walk-Forward results to Results databank and run the task.

It should create a new strategy named WF Equity portfolio that will contain WF equities from all the strategies in Results.

[![wf equity databank](https://strategyquant.com/wp-content/uploads/2021/11/wf_equity_portfolio_databank-750x181.png)](https://strategyquant.com/wp-content/uploads/2021/11/wf_equity_portfolio_databank-750x181.png)

and the portfolio equity from all the WF results looks like this:

[![WF portfolio equity](https://strategyquant.com/wp-content/uploads/2021/11/wf_equity_portfolio2-750x336.png)](https://strategyquant.com/wp-content/uploads/2021/11/wf_equity_portfolio2-750x336.png)

## Full commented code of the snippet

```
package SQ.CustomAnalysis;

import com.strategyquant.lib.*;

import java.util.ArrayList;

import com.strategyquant.tradinglib.*;
import com.strategyquant.tradinglib.project.ProjectEngine;
import com.strategyquant.tradinglib.results.SpecialValues;
import com.strategyquant.tradinglib.results.stats.comparator.OrderComparatorByOpenTime;

public class WFPortfolio extends CustomAnalysisMethod {

    //------------------------------------------------------------------------
    //------------------------------------------------------------------------
    //------------------------------------------------------------------------
    
    public WFPortfolio() {
        super("WFPortfolio", TYPE_PROCESS_DATABANK);
    }
    
    //------------------------------------------------------------------------
    
    @Override
    public boolean filterStrategy(String project, String task, String databankName, ResultsGroup rg) throws Exception {
        return true;
    }
    
    //------------------------------------------------------------------------
    
    @Override
    public ArrayList<ResultsGroup> processDatabank(String project, String task, String databankName, ArrayList<ResultsGroup> databankRG) throws Exception {
        // create new ResultsGroup for our new portfolio
        ResultsGroup portfolioRG = new ResultsGroup("WF Equity portfolio");

        // get OrderList from new portfolio (empty so far),
        // we will insert WF orders there
        OrdersList portfolioOrdersList = portfolioRG.orders();

        // go through all strategy results in databank
        for(ResultsGroup strategyRG : databankRG) {

            // skip strategies whose name starts with "WF Equity portfolio"
            // this will ensure that we'll ignore our portfolios created
            // in previous runs
            String strName = strategyRG.getName();
            if(strName.startsWith("WF Equity portfolio")) {
                continue;
            }

            // get best Walk-Forward result from a strategy
            // If it doesn't exist it will return null
            Result wfResult = getWFResult(strategyRG);
            if(wfResult == null) {
                continue;
            }

            // ---------------------
            // wfResult is the best Walk-Forward result from existing strategy
            // clone it to a new result with all its settings

            String wfResultKey = wfResult.getResultKey();

            // we'll name the new subresult as "StrategyName "+ "existing WF result name"
            String newSubResultKey = String.format("%s-%s", strategyRG.getName(), wfResultKey);

            // clone settings to use them in the new result
            SettingsMap settingsMapCopy = wfResult.getSettings().clone();

            // create new result that will be added to the portfolio
            Result wfResultCopied = new Result(newSubResultKey, portfolioRG, settingsMapCopy);
            portfolioRG.addSubresult(newSubResultKey, settingsMapCopy, wfResultCopied);

            // ---------------------
            // copy WF orders from original strategy to new results group
            OrdersList filteredOL = strategyRG.orders().filterWithClone(wfResultKey, Directions.Both, SampleTypes.FullSample);
                
            for(int i=0; i<filteredOL.size(); i++) {
                Order order = filteredOL.get(i);
                
                // change setup name to match the new result name in the new portfolio
                order.SetupName = newSubResultKey;
                order.IsInPortfolio = 1;

                // add the order to the portfolio OrdersList
                portfolioOrdersList.add(order);
            }

            // copy also symbol definitions from old strategy
            // this is needed for proper cancullation of PL and metrics
            portfolioRG.symbols().add(strategyRG.symbols());
        }

        // ---------------------
        // compute portfolio result
        // normally we'd call portfolioRG.createPortfolioResult() but the default version
        // ignores WF results, so we have to implement it by ourselves
        createPortfolioResult(portfolioRG);

        // portfolio ResultsGroup is complete, now add it to the databank
        Databank databank = ProjectEngine.get(project).getDatabanks().get(databankName);
        databank.add(portfolioRG, true);

        return databankRG;
    }

    //------------------------------------------------------------------------

    /**
     * returns best Walk-Forward result - if it exists
     */
    private Result getWFResult(ResultsGroup rg) throws Exception {
        String wfKey = rg.getBestWFResultKey();
        if(wfKey == null) {
            return null;
        }

        return rg.subResult(wfKey);
    }

    //------------------------------------------------------------------------

    /**
     * creates a Portfolio result from all the existing sub-results in the ResultsGroup
     * This is function from standard SQ library, with only exception that this works
     * also on WF results - standard ResultsGroup.createPortfolioResult()
     * ignores WF results when creating portfolio.
     *
     * @param portfolioRG
     * @throws Exception
     */
    public void createPortfolioResult(ResultsGroup portfolioRG) throws Exception {
        Result firstResult = null;
        String tf = null;

        int resultsInPortfolio = 0;

        // go through all existing sub-results and get their initial capital and from-to dates
        for(String existingResultKey : portfolioRG.getResultKeys()) {
            Result result = portfolioRG.subResult(existingResultKey);
            resultsInPortfolio++;

            if(firstResult == null) {
                firstResult = result;
            }
        }

        if(resultsInPortfolio <= 1) {
            throw new Exception("ResultGroup doesn't contain more Results, cannot create a portfolio!");
        }

        if(firstResult == null) {
            throw new Exception("No Result in ResultGroup to create portfolio!");
        }

        SettingsMap settings = firstResult.getSettings();

        Result portfolioResult = new Result(ResultsGroup.Portfolio, portfolioRG, settings);
        portfolioRG.removeSubresult(ResultsGroup.Portfolio, true);
        portfolioRG.addSubresult(ResultsGroup.Portfolio, settings, portfolioResult);

        portfolioRG.specialValues().setString(SpecialValues.Symbol, ResultsGroup.Portfolio);
        portfolioRG.specialValues().setString(SpecialValues.Timeframe, "N/A");

        // for portfolio sort orders by their open time
        OrdersList ordersList = portfolioRG.orders();
        ordersList.sort(new OrderComparatorByOpenTime());

        // compute all metrics on the portfolio
        portfolioResult.computeAllStats(portfolioRG.specialValues(), portfolioRG.getOOS());
    }
}
```
