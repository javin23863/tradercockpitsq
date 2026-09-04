# Running strategy backtests programmatically

- Source: <https://strategyquant.com/doc/programming-for-sq/running-strategy-backtests-programmatically/>
- Section: Programming for StrategyQuant X › Coding sessions examples
- Fetched: 2026-09-04

---

This example will show how to run strategy backtests programmatically by calling our backtesting engine. The example is made in form of Custom Analysis snippet.

The backtesting code can be theoretically included in any other snippet, you just need to realize that backtests are slow, so it is definitely not recommended to run backtests in databank columns and similar other snippets that have to be fast.

You can download the complete snippet as an attachment in this article, its full source code will be also published at the end of article.

We will describe the most important parts of the backtest step by step. The backtest itself is implemented in Custom Analysis method ***processDatabank()***, where it performs custom backtest of very first strategy in the databank.

## Using BacktestEngine to run backtest

Running backtest is relatively simple, you need only to initialize **BacktestEngine** and then call its method ***runBacktest***() and then ***getResults***() to get the backtest result.

```
// create trading simulator (later used for backtest engine)
ITradingSimulator simulator = new MetaTrader4Simulator();

// set testing precision for the simulator
simulator.setTestPrecision(Precisions.getPrecision(Precisions.PRECISION_BASE_TF));

// create backtest engine using simulator
BacktestEngine backtestEngine = new BacktestEngine(simulator);
backtestEngine.setSingleThreaded(true);

// add backtest settings to the engine
backtestEngine.addSetup(settings);

// ------------------------
// run backtest - this will run the actual backtest using the settings configured above
// Depending on the settings it could take a while.
// When finished, it will return new ResultsGroup object with backtest result.
// In case of error an Exception is thrown with the description of the error
ResultsGroup backtestResultRG = backtestEngine.runBacktest().getResults();
```

We first create trading simulator of given type (MT4, MT5, Tradestation, etc.) and set its backtest precision.

Then we’ll create **BacktestEngine** using this simulator. We have to add settings that describe other backtest parameters to the engine using ***addSetup()*** – the settings themselves will be explained later.

The last thing is calling ***runBacktest().getResults()*** to run the actual backtest and obtain its results.

## Creating settings for backtest

BacktestEngine must be configured with settings – a SettingsMap object. It is a map of key -> value pairs, where you can set various parameters of the backtest.

```
SettingsMap settings = new SettingsMap();

// prepare chart setup for backtest - you must specify the data name, range, etc
ChartSetup chartSetup = new ChartSetup(
        "History", // this is constant
        "EURUSD_M1", // symbol name, must match the name in Data Manager
        TimeframeManager.TF_H1, // timeframe
        SQTimeOld.toLong(2008, 4, 20), // date from
        SQTimeOld.toLong(2009, 6, 29), // date to
        3.5, // spread
        Session.Forex_247 // session
);
settings.set(SettingsKeys.BacktestChart, chartSetup);

// prepare other backtest settings - this will put other optional/required parts to the settings map
prepareBacktestSettings(settings, strategyToRetest);
```

You can see that we first create the **SettingsMap** object. We then define **ChartSetup** – it is a configuration of data used for backtest, and it is added to the settings by calling ***settings.set(SettingsKeys.BacktestChart, chartSetup)***.

The last call is method ***prepareBacktestSettings(…)*** that will set the rest of the settings to keep the code simpler and readable.

## Method prepareBacktestSettings()

It is the method that set other optional and required configuration values for the backtest settings – like strategy, money management, initial capital, trading options and so on.

```
private void prepareBacktestSettings(SettingsMap settings,ResultsGroup strategyToRetest) throws Exception {
    // creates strategy object from strategy XML that is stored in the source ResultsGroup
    String strategyName = strategyToRetest.getName();
    Element elStrategy = strategyToRetest.getStrategyXml();
    if(elStrategy == null) {
        // result doesn't have any strategy, cannot be tested
        throw new Exception("Result doesn't have any strategy, cannot be tested!");
    }
    StrategyBase strategy = StrategyBase.createXmlStrategy(elStrategy.clone(), strategyName);
    settings.set(SettingsKeys.StrategyObject, strategy);

    settings.set(SettingsKeys.MinimumDistance, 0);

    // set initial capital and Money management
    settings.set(SettingsKeys.InitialCapital, 100000d);
    settings.set(SettingsKeys.MoneyManagement, MoneyManagementMethodsList.create("FixedSize", 0.1));
    // Note - you can create a different Money Managemtn method by specifying its (snippet) name
    // and parameters in their exact order, for example:
    // MoneyManagementMethodsList.create("RiskFixedPctOfAccount", 5, 100, 0.1, 0.5))

    // create and set required trading options
    TradingOptions options = createTradingOptions();
    settings.set(SettingsKeys.TradingOptions, options);
}
```

The code is commented and you should be able to understand what it does. There is again a separate sub-method ***createTradingOptions()*** used to keep the code more organized. This method will be not explained here, it is commented in the code and it returns a list of trading options that should apply to the backtest.

## Working with backtest result

When your backtest finished successfully you’ll get a new **ResultsGroup** object with backtest result. You can then generally do two things with it:

1. Read its metrics (number of trades, Net profit, Sharpe, etc.) to determine if you want to filter out the strategy
2. Save the result to some databank or to file

Both things are quite simple:

```
// 1. Get the metrics values and compare / filter them in some way, example:
int trades = backtestResultRG.portfolio().stats(Directions.Both, PlTypes.Money, SampleTypes.FullSample).getInt(StatsKey.NUMBER_OF_TRADES);
double profit = backtestResultRG.portfolio().stats(Directions.Both, PlTypes.Money, SampleTypes.FullSample).getDouble(StatsKey.NET_PROFIT);
	// now do something with these values
Log.info("Trades: {}, profit: {}", trades, profit);

// 2. You can save the new backtest to a databank or file
SQProject project = ProjectEngine.get(projectName);
if(project == null) {
    throw new Exception("Project '"+projectName+"' cannot be loaded!");
}

Databank targetDB = project.getDatabanks().get("Results");
if(targetDB == null) {
    throw new Exception("Target databank 'Results' was not found!");
}

// add the new strategy+backtest to this databank and refresh the databank grid
targetDB.add(backtestResultRG, true);
```

## Full source code of CAStrategyTestByProgramming snippet:

```
package SQ.CustomAnalysis;

import SQ.TradingOptions.*;
import com.strategyquant.datalib.TimeframeManager;
import com.strategyquant.datalib.consts.Precisions;
import com.strategyquant.datalib.session.Session;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.time.SQTimeOld;
import com.strategyquant.tradinglib.*;
import com.strategyquant.tradinglib.engine.BacktestEngine;
import com.strategyquant.tradinglib.moneymanagement.MoneyManagementMethodsList;
import com.strategyquant.tradinglib.options.TradingOptions;
import com.strategyquant.tradinglib.simulator.Engines;
import com.strategyquant.tradinglib.simulator.ITradingSimulator;
import com.strategyquant.tradinglib.simulator.impl.*;
import com.strategyquant.tradinglib.project.ProjectEngine;
import com.strategyquant.tradinglib.project.SQProject;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class CAStrategyTestByProgramming extends CustomAnalysisMethod {
    public static final Logger Log = LoggerFactory.getLogger("CAStrategyTestByProgramming");

    //------------------------------------------------------------------------
    //------------------------------------------------------------------------
    //------------------------------------------------------------------------

    public CAStrategyTestByProgramming() {
        super("CAStrategyTestByProgramming", TYPE_PROCESS_DATABANK);
    }
    
    //------------------------------------------------------------------------
    
    @Override
    public boolean filterStrategy(String projectName, String task, String databankName, ResultsGroup rg) throws Exception {
        return false;
    }
    
    
    //------------------------------------------------------------------------
    
    @Override
    public ArrayList<ResultsGroup> processDatabank(String projectName, String task, String databankName, ArrayList<ResultsGroup> databankRG) throws Exception {
        if(databankRG.size() == 0) {
            return databankRG;
        }

        // get first strategy in databank to perform retest on it
        ResultsGroup strategyToRetest = databankRG.get(0);

        // we'll store all settings for new backtest in this SettingsMap
        SettingsMap settings = new SettingsMap();

        // prepare chart setup for backtest - you must specify the data name, range, etc
        ChartSetup chartSetup = new ChartSetup(
                "History", // this is constant
                "EURUSD_M1", // symbol name, must match the name in Data Manager
                TimeframeManager.TF_H1, // timeframe
                SQTimeOld.toLong(2008, 4, 20), // date from
                SQTimeOld.toLong(2009, 6, 29), // date to
                3.5, // spread
                Session.Forex_247 // session
        );
        settings.set(SettingsKeys.BacktestChart, chartSetup);

        // prepare other backtest settings - this will put other optional/required parts to the settings map
        prepareBacktestSettings(settings, strategyToRetest);

        // create trading simulator (later used for backtest engine)
        ITradingSimulator simulator = new MetaTrader4Simulator();
        // simulators available:
        //MetaTrader4Simulator()
        //MetaTrader5SimulatorHedging(OrderExecutionTypes.EXCHANGE)
        //MetaTrader5SimulatorNetting(OrderExecutionTypes.EXCHANGE)
        //TradestationSimulator()
        //MultiChartsSimulator()
        //JForexSimulator()

        // set testing precision for the simulator
        simulator.setTestPrecision(Precisions.getPrecision(Precisions.PRECISION_BASE_TF));
        // Precisions available (depending also on data - you cannot use tick precision if you don't have tick data):
        //PRECISION_SELECTED_TF = "Selected timeframe only (fastest)";
        //PRECISION_BASE_TF = "1 minute data tick simulation (slow)";
        //PRECISION_TICK_CUSTOM_SPREADS = "Real Tick - custom spread (slowest)";
        //PRECISION_TICK_REAL_SPREADS = "Real Tick - real spread (slowest)";
        //PRECISION_OPEN_PRICES = "Trade On Bar Open";

        // create backtest engine using simulator
        BacktestEngine backtestEngine = new BacktestEngine(simulator);
        backtestEngine.setSingleThreaded(true);

        // add backtest settings to the engine
        backtestEngine.addSetup(settings);

        // ------------------------
        // run backtest - this will run the actual backtest using the settings configured above
        // Depending on the settings it could take a while.
        // When finished, it will return new ResultsGroup object with backtest result.
        // In case of error an Exception is thrown with the description of the error
        ResultsGroup backtestResultRG = backtestEngine.runBacktest().getResults();

        // get backtest result details - here you can access all the results in ResultsGroup
        // and do something with them.

        // You can generally do two things:
        // 1. Get the metrics values and compare / filter them in some way, example:
        int trades = backtestResultRG.portfolio().stats(Directions.Both, PlTypes.Money, SampleTypes.FullSample).getInt(StatsKey.NUMBER_OF_TRADES);
        double profit = backtestResultRG.portfolio().stats(Directions.Both, PlTypes.Money, SampleTypes.FullSample).getDouble(StatsKey.NET_PROFIT);
        // now do something with these values
        // For example - you can use the result of this custom backtest t filter out this particular strategy
        // from databank - by removing it from databankRG array
        Log.info("Trades: {}, profit: {}", trades, profit);

        // 2. You can save the new backtest to a databank or file
        SQProject project = ProjectEngine.get(projectName);
        if(project == null) {
            throw new Exception("Project '"+projectName+"' cannot be loaded!");
        }

        Databank targetDB = project.getDatabanks().get("Results");
        if(targetDB == null) {
            throw new Exception("Target databank 'Results' was not found!");
        }

        // add the new strategy+backtest to this databank and refresh the databank grid
        targetDB.add(backtestResultRG, true);

        // method must return a list of original strategies in databank that passed this filter
        return databankRG;
    }

    //------------------------------------------------------------------------

    private void prepareBacktestSettings(SettingsMap settings,ResultsGroup strategyToRetest) throws Exception {
        // creates strategy object from strategy XML that is stored in the source ResultsGroup
        String strategyName = strategyToRetest.getName();
        Element elStrategy = strategyToRetest.getStrategyXml();
        if(elStrategy == null) {
            // result doesn't have any strategy, cannot be tested
            throw new Exception("Result doesn't have any strategy, cannot be tested!");
        }
        StrategyBase strategy = StrategyBase.createXmlStrategy(elStrategy.clone(), strategyName);
        settings.set(SettingsKeys.StrategyObject, strategy);

        settings.set(SettingsKeys.MinimumDistance, 0);

        // set initial capital and Money management
        settings.set(SettingsKeys.InitialCapital, 100000d);
        settings.set(SettingsKeys.MoneyManagement, MoneyManagementMethodsList.create("FixedSize", 0.1));
        // Note - you can create a different Money Managemtn method by specifying its (snippet) name
        // and parameters in their exact order, for example:
        // MoneyManagementMethodsList.create("RiskFixedPctOfAccount", 5, 100, 0.1, 0.5))

        // create and set required trading options
        TradingOptions options = createTradingOptions();
        settings.set(SettingsKeys.TradingOptions, options);
    }

    //------------------------------------------------------------------------

    private TradingOptions createTradingOptions() {
        TradingOptions options = new TradingOptions();

        // all trading options are defined as snippets
        // in SQ.TradingOptions.* (visible in CodeEditor)
        // below is an example of few of them applied
        ExitAtEndOfDay option = new ExitAtEndOfDay();
        option.ExitAtEndOfDay = true;
        option.EODExitTime = 0;
        options.add(option);

        ExitOnFriday option2 = new ExitOnFriday();
        option2.ExitOnFriday = true;
        option2.FridayExitTime = 0;
        options.add(option2);

        LimitTimeRange option3 = new LimitTimeRange();
        option3.LimitTimeRange = true;
        option3.SignalTimeRangeFrom = 700;
        option3.SignalTimeRangeTo = 1900;
        option3.ExitAtEndOfRange = true;
        options.add(option3);

        MinMaxSLPT optionMmSLPT = new MinMaxSLPT();
        optionMmSLPT.MinimumSL = 50;
        optionMmSLPT.MaximumSL = 100;
        optionMmSLPT.MinimumPT = 50;
        optionMmSLPT.MaximumPT = 100;
        options.add(optionMmSLPT);

        return options;
    }
}
```
