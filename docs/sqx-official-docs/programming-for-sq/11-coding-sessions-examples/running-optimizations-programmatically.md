# Running optimizations programmatically – update

- Source: <https://strategyquant.com/doc/programming-for-sq/running-optimizations-programmatically/>
- Section: Programming for StrategyQuant X › Coding sessions examples
- Fetched: 2026-09-04

---

This example will show how to run optimizations programmatically by calling SQ optimization engine. The example is made in the form of Custom Analysis snippet.

You can download the complete snippet as an attachment in this article, its full source code will be also published at the end of article.

The source code of the snippet is extensively commented, we’ll describe the most important parts of the it in the article. The optimization is implemented in Custom Analysis method ***processDatabank()***, where it performs custom optimization of the very first strategy in the databank.

For version 140 and newer, please use the \_sqx140 version; for older SQX versions, use the \_136 version.

## Implementing processDatabank() CA snippet method

This method simply gets first strategy from the databank and calls the ***performOptimization()*** method on it – this is where the optimization is performed.

It ill also get target databank – this is where the optimization result is stored.  
It is up to you what you’d do with the optimization result, you don’t need to store it anywhere, you can evaluate the optimization metrics and accept/refuse the original strategy.

```
public ArrayList<ResultsGroup> processDatabank(String project, String task, String databankName, ArrayList<ResultsGroup> databankRG) throws Exception {
    if(databankRG.size() == 0) {
        return databankRG;
    }

    // get target databank to save the final result
    Databank targetDatabank = ProjectEngine.get(project).getDatabanks().get("Results");

    // get first strategy in databank to perform optimization on it
    ResultsGroup strategy = databankRG.get(0);

    // perform optimization on this strategy
    performOptimization(strategy, targetDatabank);

    return databankRG;
}
```

## The core – using OptimizationEngine in performOptimization() method

There are quite a few settings that have to be prepared in order to run optimization on the strategy. They are broken down to their own methods explained further.  
The whole setting is prepared and returned by the ***prepareSettings()*** method.

```
SettingsMap settings = prepareSettings(strategyToOptimize, dontSaveOriginalStr);
```

When we have the setting the next step is to use the SQ OptimizationEngine object to perform the actual optimization.

```
OptimizationEngine engine = new OptimizationEngine("CustomCAOptimization", progressEngine, null, "OptimTask", null);

// it is initialized with settings prepared above
engine.initialize(settings);

// sets listener that will be called when the optimization result(s) is ready
engine.setNewResultListener(new INewResultListener() {
    public void newResult(ResultsGroup resultsGroup) throws Exception {
        processResult(resultsGroup, strategyToOptimize.getName(), targetDatabank, dontSaveOriginalStr);
    }
});

// set other listeners that can be ignored for now.
// You could use these listeners to track the optimization progress and messages
engine.setLogListener(new IEngineLogListener() {
    public void processMessage(String message) {
        Log.debug(message);
    }
});
engine.setStepsListener(new IStepsListener() {
    @Override
    public void step(int stepNumber) {
    }
});

// start the optimization - method will finish after the optimization fully finished
engine.optimize();
```

It is relatively simple in principle:

- create ***OptimizationEngine*** object
- initialize it with settings
- register listeners as necessary
- call ***optimize()***

The listeners are used by the engine to send progress information and logs, as well as final optimization results to you. The method ***processMessage()*** used here will receive the result and store it to the target databank, it is further described later below.

## Preparing settings for OptimizationEngine

Check the commented code below on how all the settings for optimization are prepared. There are few levels of settings:

***SettingsMap*** is what is returned by this method, it contains all its settings – including things like fitness function, trading options, chart (symbol, timeframe, date ranges) and so on.

***ParametersSettings*** are settings of parameters ranges and steps.

***OptimizationSettings*** are settings of the optimization itself – you choose the type of optimization (simple, WF, WFM) and its parameters.

Full ***prepareSettings()*** code:

```
private SettingsMap prepareSettings(ResultsGroup strategyToOptimize, boolean dontSaveOriginalStr) throws Exception {
    try {

        // create parameter settings - configures ranges for optimized parameters
        ParametersSettings paramSettings = createParametersSettings(strategyToOptimize);

        // get strategy XML and create strategy object from XML with variables for parameters
        Element elStrategyXml = strategyToOptimize.getStrategyXml();
        StrategyBase xmlS = StrategyBase.createXmlStrategy(elStrategyXml);
        xmlS.transformToVariables(paramSettings.symmetry, paramSettings.paramTypes);

        // create fitness function and trading options used in optimization
        IFitnessFunction fitnessFunction = createFitnessFunction();
        TradingOptions tradingOptions = createTradingOptions();

        // applies parameter settings to strategy variables
        applyParamSettingsToStrategy(paramSettings, xmlS, tradingOptions);

        // creates optimization setting - simple, WF? steps etc. - look at the method for the correct parameters
        //OptimizationSettings optimizationSettings = createOptimizationSettingsSimple(paramSettings, dontSaveOriginalStr);
        OptimizationSettings optimizationSettings = createOptimizationSettingsWF(paramSettings, dontSaveOriginalStr);
        //OptimizationSettings optimizationSettings = createOptimizationSettingsWFM(paramSettings, dontSaveOriginalStr);

        // creates final SettingsMap to return
        SettingsMap settings = new SettingsMap();

        // standard backtesting settings
        settings.set(SettingsKeys.TestPrecision, Precisions.SelectedTF);
        settings.set(SettingsKeys.StrategyXml, elStrategyXml);
        settings.set(SettingsKeys.StrategyObject, xmlS);
        settings.set(SettingsKeys.StrategyName, strategyToOptimize.getName());
        settings.set(SettingsKeys.FitnessFunction, fitnessFunction);
        settings.set(SettingsKeys.TradingOptions, tradingOptions.getClone());
        settings.set(SettingsKeys.ATM, null);
        settings.set(SettingsKeys.InitialCapital, 100000d);
        settings.set(SettingsKeys.Slippage, 0d);
        settings.set(SettingsKeys.MinimumDistance, 0d);

        // prepare chart setup for backtest
        ChartSetup chartSetup = new ChartSetup(
                "History",
                "GBPUSD_M1",
                TimeframeManager.TF_H1,
                SQTimeOld.toLong(2008, 1, 1),
                SQTimeOld.toLong(2018, 12, 31),
                3,
                Session.Forex_247);

        chartSetup.setTestPrecision(settings.getInt(SettingsKeys.TestPrecision));
        chartSetup.setBacktestEngine(Engines.MetaTrader4);

        ChartSetups chartSetups = new ChartSetups();
        chartSetups.add((ChartSetup) chartSetup);

        settings.set(SettingsKeys.BacktestChart, chartSetup);
        settings.set(SettingsKeys.ChartSetups, chartSetups);

        //OptimizationSettings jobOptimizationSettings = optimizationSettings.clone();

        settings.set(SettingsKeys.OptimizationSettings, optimizationSettings);
        settings.set(SettingsKeys.DateGenerated, strategyToOptimize.specialValues().getLong(SpecialValues.DateGenerated, -1));

        return settings;

    } catch(Exception e){
        throw new Exception("Error while preparing settings - " + e.getMessage(), e);
    }
}
```

## Preparing ParameterSettings

This method creates and returns settings for parameter optimization ranges – which parameters to optimize, what are their ranges, etd.

Because we’ll make it general to be used by any strategy we’ll use recommended parameters with automatic parameter settings.

It should be equivalent to setting this in the UI:

[![](https://strategyquant.com/wp-content/uploads/2022/05/optimization_parameters-750x231.jpg)](https://strategyquant.com/wp-content/uploads/2022/05/optimization_parameters-750x231.jpg)

Full code of the ***createParametersSettings()*** method:

```
private ParametersSettings createParametersSettings(ResultsGroup strategyToOptimize) throws Exception {
    ParametersSettings settings = new ParametersSettings();

    try {
        settings.paramTypes.set(ParametrizationTypes.ParamTypeRecommended, true);
        settings.symmetry = true;

        settings.params.clear();

        settings.distributionUp = 20;
        settings.distributionDown = 20;
        settings.maxSteps = 20;

        Element elStrategyXml = strategyToOptimize.getStrategyXml();

        StrategyBase strategy = StrategyBase.createXmlStrategy(elStrategyXml);
        strategy.transformToVariables(settings.symmetry, settings.paramTypes);

        ArrayList<Element> signals = XMLUtil.getNestedElements(elStrategyXml, "signal");

        for(Variable variable : strategy.variables()){
            if(!variable.getName().equals("MagicNumber") && !isSignalVariable(variable.getId(), signals)){
                settings.params.addParam(createParameter(variable, settings.distributionUp, settings.distributionDown, settings.maxSteps));
            }
        }

    } catch(Exception e){
        throw new Exception("Cannot load Parameters settings. " + e.getMessage(), e);
    }

    return settings;
}
```

## Preparing OptimizationSettings

This method creates and returns optimization setings – what kind of optimization to run, and its parameters.

There are three examples of this method in the full code for Simple, WF and WFM optimization. You can just uncomment out the one that you want to use in ***prepareSettings()*** method.

The optimizatin settings is fairly simple and understandable,

Full code of the ***createOptimizationSettingsWF()*** method:

```
private OptimizationSettings createOptimizationSettingsWF(ParametersSettings paramSettings, boolean dontSaveOriginalStr) {
    OptimizationSettings optimizationSettings = new OptimizationSettings("CustomCAOptimization");
    optimizationSettings.type = OptimizationTypes.WalkForward;
    optimizationSettings.optimizationMethod = OptimizationMethods.GeneticOptimization; //OptimizationMethods.BruteForce;
    optimizationSettings.optimizationType = OptimizationConst.WF_TYPE_SIMIS_EXACTOOS;
    optimizationSettings.maxOptimizationBacktests = 1000; // max optimizations limit as in UI

    optimizationSettings.parameters = paramSettings.params;
    optimizationSettings.dontSaveOriginalStr = dontSaveOriginalStr;

    optimizationSettings.periodInPercent = true;

    optimizationSettings.param1Start = 20;
    optimizationSettings.param1Stop = 40;
    optimizationSettings.param1Step = 10;

    // param2XXX settings are not used, but must be set properly
    optimizationSettings.param2Start = 5;
    optimizationSettings.param2Stop = 20;
    optimizationSettings.param2Step = 5;

    return optimizationSettings;
}
```

## Processing the optimization results with ***processResult()*** method

The final important step is to implement what you want to do with the results of the optimization. As said earlier, ***OptimizationEngine*** uses the New result listener to send us the optimization result, we created a ***processResult()*** method that is called by this listener.

Note that this method can be called multiple times – engine first retests the original strategy and returns it in this callback, then proceeds with the optimization add returns multiple results (by making multiple calls) when Simple optimization is used, or just one final WF/WFM result for Walk Forward or Walk Forward Matrix optimization.

It is up to us what we’ll do with these results, in this example we’ll simply save them to the target databank.

There is one special handling here to (optionally) not save the original strategy retest, it will save only new optimization results.

Full code of the ***processResult()*** method:

```
protected void processResult(ResultsGroup newRG, String originalStrategyName, Databank targetDatabank, boolean dontSaveOriginalStr) {
    try {
        if(SQProject.isMemoryProtectionUsed()) {
            MemoryUsageChecker.checkAvailableMemory();
        }

        if(newRG.getOptimizationProfile()!=null) {
            //newRG.getOptimizationProfile().reset();
        }

        if(newRG.getName().equals(originalStrategyName)){
            // it is retest of original strategy
            if(!dontSaveOriginalStr) {
                targetDatabank.add(newRG, true);
            }

            return;
        }

        newRG.removeUnsavableSettings();
        //newRG.setLastSettings(lastSettingsXml);
        //newRG.computeEquityChartData();

        //Log.info("Saving RG: {}", newRG.getName());
        targetDatabank.add(newRG, true);

    } catch(Exception e) {
        String message = "Error while processing strategy '" + newRG.getName() + "'";
        if(newRG != null) {
            newRG.clear();
        }
        Log.error(message, e);
    } catch (OutOfMemoryError e) {
        if(newRG != null) {
            newRG.clear();
        }
    }
}
```

## Custom fitness function class

Fitness function settings must be created in the ***prepareSettings()*** method. There is one specialty about fitness method – it wasn’t ready to be used like this, so we had to create a special class FitnessFromStrategyResultForCA

that we put to the ***SQ.Utils*** package. It was create donly because we are not able to use the built-in fitness functions, and it is a copy of the default one-metric fitness function from SQ.

Code of the ***FitnessFromStrategyResultForCA*** class:

```
package SQ.Utils;

import com.strategyquant.lib.L;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.snippets.NonexistingCustomClassException;
import com.strategyquant.tradinglib.*;
import com.strategyquant.tradinglib.databank.DatabankColumns;
import com.strategyquant.tradinglib.fitnessfunction.IFitnessFunction;
import com.strategyquant.tradinglib.results.stats.StatsComputer;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class FitnessFromStrategyResultForCA implements IFitnessFunction {
    public static final Logger Log = LoggerFactory.getLogger(FitnessFromStrategyResultForCA.class);

    private String databankColumnName = null;
    private ArrayList<Goal> weightedGoals = new ArrayList<Goal>();

    //------------------------------------------------------------------------
    //------------------------------------------------------------------------
    //------------------------------------------------------------------------

    public FitnessFromStrategyResultForCA() {
    }

    //------------------------------------------------------------------------

    public FitnessFromStrategyResultForCA(String databankColumnName) {
        this.databankColumnName = databankColumnName;
    }

    //------------------------------------------------------------------------

    @Override
    public double computeFitness(ResultsGroup results, byte direction, byte sampleType) throws Exception {
        return computeFitness(results, direction, sampleType, true);
    }

    //------------------------------------------------------------------------

    public double computeFitness(ResultsGroup results, byte direction, byte sampleType, boolean useRatioByTrades) throws Exception {
        if (databankColumnName.equals("Weighted")) {
            double fitness = computeWeightedFitness(results, direction, sampleType, useRatioByTrades);
            if (fitness == 0) {
                return getFitnessValue(results, direction, sampleType, "NetProfit", 0, (byte) 0, useRatioByTrades);
            } else return fitness;
        }

        return getFitnessValue(results, direction, sampleType, databankColumnName, 0, (byte) 0, useRatioByTrades);
    }

    //------------------------------------------------------------------------

    private double getFitnessValue(ResultsGroup results, byte direction, byte sampleType, String databankColumnName, double target, byte valueType, boolean useRatioByTrades) throws Exception {
        DatabankColumn c = DatabankColumns.get().findClassByName(databankColumnName);

        double val = c.getNumericValue(results, results.getMainResultKey(), direction, PlTypes.Money, sampleType);

        double fitness = c.transformToFitnessRange(val, target, valueType);

        if (fitness < 0) {
            fitness = 0;
        }

        if (fitness > 1) {
            fitness = 1;
        }

        if (!useRatioByTrades) {
            return fitness;
        }

        // special handling - if number of trades is too small, decrease also fitness
        SQStats stats = results.mainResult().statsOrNull(Directions.Both, PlTypes.Money, sampleType);
        int trades = 0;
        if (stats != null) {
            trades = stats.getInt(StatsKey.NUMBER_OF_TRADES);
        }

        if (trades < 20) {
            fitness *= 0.3;
        } else if (trades < 30) {
            fitness *= 0.4;
        } else if (trades < 50) {
            fitness *= 0.6;
        } else if (trades < 70) {
            fitness *= 0.8;
        } else if (trades < 100) {
            fitness *= 0.85;
        } else if (trades < 150) {
            fitness *= 0.9;
        }

        return fitness;
    }

    //------------------------------------------------------------------------

    private double computeWeightedFitness(ResultsGroup results, byte direction, byte sampleType, boolean useRatioByTrades) throws Exception {
        // first compute total weight
        float totalWeight = 0;
        for (int i = 0; i < weightedGoals.size(); i++) {
            if (weightedGoals.get(i).use) {
                totalWeight += weightedGoals.get(i).weight;
            }
        }

        // now compute weighted fitness
        float weightedFitness = 0;
        for (int i = 0; i < weightedGoals.size(); i++) {
            Goal goal = weightedGoals.get(i);

            if (goal.use) {
                double rankingFitness = getFitnessValue(results, direction, sampleType, goal.statsValueName, goal.target, goal.valueType, useRatioByTrades);

                weightedFitness += (goal.weight / totalWeight) * rankingFitness;
            }
        }

        return weightedFitness;
    }

    //------------------------------------------------------------------------

    @Override
    public void initFitnessFromXml(Element elSettings) {
        weightedGoals.clear();

        Element elRanking = elSettings.getChild("Ranking");
        this.databankColumnName = elRanking.getAttributeValue("type");

        for (Element elGoal : elRanking.getChildren("Goal")) {
            Goal goal = new Goal();
            String use = elGoal.getAttributeValue("use");
            goal.use = (use.equals("true") || use.equalsIgnoreCase("1") ? true : false);
            goal.statsValueName = elGoal.getAttributeValue("type");
            goal.weight = XMLUtil.getDoubleAttr(elGoal, "weight", 1);
            goal.valueType = XMLUtil.getByteAttr(elGoal, "valueType", (byte) 0);
            goal.target = XMLUtil.getDoubleAttr(elGoal, "target", 0);

            try {
                if (goal.use) {
                    DatabankColumn c = DatabankColumns.get().findClassByName(goal.statsValueName);

                    weightedGoals.add(goal);
                }
            } catch (Exception e) {
                Log.error("Ranking Criterium SKIPPED. Reason: ", e);
            }
        }
    }

    //------------------------------------------------------------------------

    @Override
    public String getProduct() {
        return "SQUANT";
    }

    //------------------------------------------------------------------------

    @Override
    public int getPreferredPosition() {
        return 0;
    }

    //------------------------------------------------------------------------

    @Override
    public void initPlugin() throws Exception {
    }

    //------------------------------------------------------------------------

    private class Goal {
        public byte valueType;
        public boolean use;
        public String statsValueName;
        public double weight;
        public double target;
    }

    //------------------------------------------------------------------------

    @Override
    public byte getFitnessType() throws Exception {
        if (this.databankColumnName.equals("Weighted")) return ValueTypes.Maximize;
        else {
            return DatabankColumns.get().findClassByName(databankColumnName).valueType;
        }
    }

    //------------------------------------------------------------------------

    @Override
    public String getFitnessKey() {
        return ComputeFromStrategyResult;
    }

    //------------------------------------------------------------------------

    @Override
    public String getFitnessName() {
        return L.tsq("Main data backtest");
    }

    //------------------------------------------------------------------------

    @Override
    public String getFitnessDatabankColumnName() {
        return databankColumnName;
    }

    //------------------------------------------------------------------------

    @Override
    public ArrayList<DatabankColumn> getUsedStatValues() throws NonexistingCustomClassException {
        ArrayList<DatabankColumn> columnsUsed = new ArrayList<DatabankColumn>();

        // get list of all stat values (columns) used for computing fitness
        if (!databankColumnName.equals("Weighted")) {
            columnsUsed.add(DatabankColumns.get().findClassByName(databankColumnName));

        } else {
            for (int i = 0; i < weightedGoals.size(); i++) {
                Goal goal = weightedGoals.get(i);

                if (goal.use) {
                    columnsUsed.add(DatabankColumns.get().findClassByName(goal.statsValueName));
                }
            }
        }

        return StatsComputer.getAllDependentStatValues(columnsUsed);
    }

    //------------------------------------------------------------------------

    @Override
    public String printWeightedGoals() {
        String goals = "";

        for (int i = 0; i < weightedGoals.size(); i++) {
            Goal goal = weightedGoals.get(i);

            if (goal.use) {
                goals += weightedGoals.get(i).statsValueName;

                if (i < weightedGoals.size() - 1) goals += ", ";
            }
        }

        return goals;
    }
}
```

## Running this Custom analysis snippet in SQ

Below you can see the result of our snippet. For the purpose of this example I created a new custom project with one **CustomAnalysis** task. When I run it it will get the first (and the only one) strategy in the databank and performs a WF optimization on it.

The optimization could take some time, depending on your setting, and the result is saved into the same databank.

[![Optimization by programming in SQ](https://strategyquant.com/wp-content/uploads/2022/05/ca_optimization_programming-750x432.jpg)](https://strategyquant.com/wp-content/uploads/2022/05/ca_optimization_programming-750x432.jpg)

## Full code of the CAOptimizationByProgramming snippet

```
package SQ.CustomAnalysis;

import SQ.TradingOptions.ExitAtEndOfDay;
import SQ.TradingOptions.ExitOnFriday;
import SQ.TradingOptions.LimitTimeRange;
import SQ.TradingOptions.MinMaxSLPT;
import SQ.Utils.FitnessFromStrategyResultForCA;
import com.strategyquant.datalib.TimeframeManager;
import com.strategyquant.datalib.consts.Precisions;
import com.strategyquant.datalib.session.Session;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.memory.MemoryUsageChecker;
import com.strategyquant.lib.time.SQTimeOld;
import com.strategyquant.tradinglib.*;
import com.strategyquant.tradinglib.engine.ChartSetups;
import com.strategyquant.tradinglib.fitnessfunction.IFitnessFunction;
import com.strategyquant.tradinglib.optimization.*;
import com.strategyquant.tradinglib.optimization.Parameter;
import com.strategyquant.tradinglib.options.TradingOptions;
import com.strategyquant.tradinglib.project.ProgressEngine;
import com.strategyquant.tradinglib.project.ProjectEngine;
import com.strategyquant.tradinglib.project.SQProject;
import com.strategyquant.tradinglib.results.SpecialValues;
import com.strategyquant.tradinglib.simulator.Engines;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

public class CAOptimizationByProgramming extends CustomAnalysisMethod {
    public static final Logger Log = LoggerFactory.getLogger("CAOptimizationByProgramming");

    //------------------------------------------------------------------------
    //------------------------------------------------------------------------
    //------------------------------------------------------------------------

    public CAOptimizationByProgramming() {
        super("CAOptimizationByProgramming", TYPE_PROCESS_DATABANK);
    }

    //------------------------------------------------------------------------

    @Override
    public boolean filterStrategy(String project, String task, String databankName, ResultsGroup rg) throws Exception {
        return false;
    }

    //------------------------------------------------------------------------

    @Override
    public ArrayList<ResultsGroup> processDatabank(String project, String task, String databankName, ArrayList<ResultsGroup> databankRG) throws Exception {
        if(databankRG.size() == 0) {
            return databankRG;
        }

        // get target databank to save the final result
        Databank targetDatabank = ProjectEngine.get(project).getDatabanks().get("Results");

        // get first strategy in databank to perform optimization on it
        ResultsGroup strategy = databankRG.get(0);

        // perform optimization on this strategy
        performOptimization(strategy, targetDatabank);

        return databankRG;
    }

    //------------------------------------------------------------------------

    private void performOptimization(ResultsGroup strategyToOptimize, Databank targetDatabank) throws Exception {

        // set to false if you want to save also test of strategy with original parameters
        boolean dontSaveOriginalStr = true;

        // prepare all optimization settings
        // here is where you define type of optimization (simple, WF, WFM), parameters range and such
        SettingsMap settings = prepareSettings(strategyToOptimize, dontSaveOriginalStr);

        // create required progress engine - although we'll not really use it in Custom analysis snippet
        ProgressEngine progressEngine = new ProgressEngine("CAOptimizationByProgramming");

        // ----------- The optimization engine is used to perform the optimization

        //  first OptimizationEngine object is created
        OptimizationEngine engine = new OptimizationEngine("CustomCAOptimization", progressEngine, null, "OptimTask", null);

        // it is initialized with settings prepared above
        engine.initialize(settings);

        // sets listener that will be called when the optimization result(s) is ready
        engine.setNewResultListener(new INewResultListener() {
            public void newResult(ResultsGroup resultsGroup) throws Exception {
                processResult(resultsGroup, strategyToOptimize.getName(), targetDatabank, dontSaveOriginalStr);
            }
        });

        // set other listeners that can be ignored for now.
        // You could use these listeners to track the optimization progress and messages
        engine.setLogListener(new IEngineLogListener() {
            public void processMessage(String message) {
                Log.debug(message);
            }
        });
        engine.setStepsListener(new IStepsListener() {
            @Override
            public void step(int stepNumber) {
            }
        });

        // start the optimization - method will finish after the optimization fully finished
        engine.optimize();
    }

    //------------------------------------------------------------------------

    private SettingsMap prepareSettings(ResultsGroup strategyToOptimize, boolean dontSaveOriginalStr) throws Exception {
        try {

            // create parameter settings - configures ranges for optimized parameters
            ParametersSettings paramSettings = createParametersSettings(strategyToOptimize);

            // get strategy XML and create strategy object from XML with variables for parameters
            Element elStrategyXml = strategyToOptimize.getStrategyXml();
            StrategyBase xmlS = StrategyBase.createXmlStrategy(elStrategyXml);
            xmlS.transformToVariables(paramSettings.symmetry, paramSettings.paramTypes);

            // create fitness function and trading options used in optimization
            IFitnessFunction fitnessFunction = createFitnessFunction();
            TradingOptions tradingOptions = createTradingOptions();

            // applies parameter settings to strategy variables
            applyParamSettingsToStrategy(paramSettings, xmlS, tradingOptions);

            // creates optimization setting - simple, WF? steps etc. - look at the method for the correct parameters
            //OptimizationSettings optimizationSettings = createOptimizationSettingsSimple(paramSettings, dontSaveOriginalStr);
            OptimizationSettings optimizationSettings = createOptimizationSettingsWF(paramSettings, dontSaveOriginalStr);
            //OptimizationSettings optimizationSettings = createOptimizationSettingsWFM(paramSettings, dontSaveOriginalStr);

            // creates final SettingsMap to return
            SettingsMap settings = new SettingsMap();

            // standard backtesting settings
            settings.set(SettingsKeys.TestPrecision, Precisions.SelectedTF);
            settings.set(SettingsKeys.StrategyXml, elStrategyXml);
            settings.set(SettingsKeys.StrategyObject, xmlS);
            settings.set(SettingsKeys.StrategyName, strategyToOptimize.getName());
            settings.set(SettingsKeys.FitnessFunction, fitnessFunction);
            settings.set(SettingsKeys.TradingOptions, tradingOptions.getClone());
            settings.set(SettingsKeys.ATM, null);
            settings.set(SettingsKeys.InitialCapital, 100000d);
            settings.set(SettingsKeys.Slippage, 0d);
            settings.set(SettingsKeys.MinimumDistance, 0d);

            // prepare chart setup for backtest
            ChartSetup chartSetup = new ChartSetup(
                    "History",
                    "GBPUSD_M1",
                    TimeframeManager.TF_H1,
                    SQTimeOld.toLong(2008, 1, 1),
                    SQTimeOld.toLong(2018, 12, 31),
                    3,
                    Session.Forex_247);

            chartSetup.setTestPrecision(settings.getInt(SettingsKeys.TestPrecision));
            chartSetup.setBacktestEngine(Engines.MetaTrader4);

            ChartSetups chartSetups = new ChartSetups();
            chartSetups.add((ChartSetup) chartSetup);

            settings.set(SettingsKeys.BacktestChart, chartSetup);
            settings.set(SettingsKeys.ChartSetups, chartSetups);

            //OptimizationSettings jobOptimizationSettings = optimizationSettings.clone();

            settings.set(SettingsKeys.OptimizationSettings, optimizationSettings);
            settings.set(SettingsKeys.DateGenerated, strategyToOptimize.specialValues().getLong(SpecialValues.DateGenerated, -1));

            return settings;

        } catch(Exception e){
            throw new Exception("Error while preparing settings - " + e.getMessage(), e);
        }
    }

    //------------------------------------------------------------------------

    /**
     * Optimization settings for Simple optimization
     */
    private OptimizationSettings createOptimizationSettingsSimple(ParametersSettings paramSettings, boolean dontSaveOriginalStr) {
        OptimizationSettings optimizationSettings = new OptimizationSettings("CustomCAOptimization");
        optimizationSettings.type = OptimizationTypes.Simple;
        optimizationSettings.optimizationMethod = OptimizationMethods.BruteForce; // or OptimizationMethods.GeneticOptimization
        optimizationSettings.maxOptimizationBacktests = 1000; // max optimizations limit as in UI

        optimizationSettings.parameters = paramSettings.params;
        optimizationSettings.dontSaveOriginalStr = dontSaveOriginalStr;

        return optimizationSettings;
    }

    //------------------------------------------------------------------------

    /**
     * Optimization settings for Walk-Forward optimization
     */
    private OptimizationSettings createOptimizationSettingsWF(ParametersSettings paramSettings, boolean dontSaveOriginalStr) {
        OptimizationSettings optimizationSettings = new OptimizationSettings("CustomCAOptimization");
        optimizationSettings.type = OptimizationTypes.WalkForward;
        optimizationSettings.optimizationMethod = OptimizationMethods.GeneticOptimization; //OptimizationMethods.BruteForce;
        optimizationSettings.optimizationType = OptimizationConst.WF_TYPE_SIMIS_EXACTOOS;
        optimizationSettings.maxOptimizationBacktests = 1000; // max optimizations limit as in UI

        optimizationSettings.parameters = paramSettings.params;
        optimizationSettings.dontSaveOriginalStr = dontSaveOriginalStr;

        optimizationSettings.periodInPercent = true;

        optimizationSettings.param1Start = 20;
        optimizationSettings.param1Stop = 40;
        optimizationSettings.param1Step = 10;

        // param2XXX settings are not used, but must be set properly
        optimizationSettings.param2Start = 5;
        optimizationSettings.param2Stop = 20;
        optimizationSettings.param2Step = 5;

        return optimizationSettings;
    }

    //------------------------------------------------------------------------

    /**
     * Optimization settings for Walk-Forward Matrix optimization
     */
    private OptimizationSettings createOptimizationSettingsWFM(ParametersSettings paramSettings, boolean dontSaveOriginalStr) {
        OptimizationSettings optimizationSettings = new OptimizationSettings("CustomCAOptimization");
        optimizationSettings.type = OptimizationTypes.WalkForwardMatrix;
        optimizationSettings.optimizationMethod = OptimizationMethods.BruteForce;
        optimizationSettings.optimizationType = OptimizationConst.WF_TYPE_SIMIS_EXACTOOS;
        optimizationSettings.maxOptimizationBacktests = 1000; // max optimizations limit as in UI

        optimizationSettings.parameters = paramSettings.params;
        optimizationSettings.dontSaveOriginalStr = dontSaveOriginalStr;

        optimizationSettings.periodInPercent = true;
        optimizationSettings.param1Start = 20;
        optimizationSettings.param1Stop = 40;
        optimizationSettings.param1Step = 10;

        optimizationSettings.param2Start = 5;
        optimizationSettings.param2Stop = 20;
        optimizationSettings.param2Step = 5;

        return optimizationSettings;
    }

    //------------------------------------------------------------------------

    /**
     * creates settings for parameters ranges optimization.
     * Because we'll make it general we'll use automatic
     * @param strategyToOptimize
     * @return
     * @throws Exception
     */
    private ParametersSettings createParametersSettings(ResultsGroup strategyToOptimize) throws Exception {
        ParametersSettings settings = new ParametersSettings();

        try {
            settings.paramTypes.set(ParametrizationTypes.ParamTypeRecommended, true);
            settings.symmetry = true;

            settings.params.clear();

            settings.distributionUp = 20;
            settings.distributionDown = 20;
            settings.maxSteps = 20;

            Element elStrategyXml = strategyToOptimize.getStrategyXml();

            StrategyBase strategy = StrategyBase.createXmlStrategy(elStrategyXml);
            strategy.transformToVariables(settings.symmetry, settings.paramTypes);

            ArrayList<Element> signals = XMLUtil.getNestedElements(elStrategyXml, "signal");

            for(Variable variable : strategy.variables()){
                if(!variable.getName().equals("MagicNumber") && !isSignalVariable(variable.getId(), signals)){
                    settings.params.addParam(createParameter(variable, settings.distributionUp, settings.distributionDown, settings.maxSteps));
                }
            }

        } catch(Exception e){
            throw new Exception("Cannot load Parameters settings. " + e.getMessage(), e);
        }

        return settings;
    }

    //------------------------------------------------------------------------

    /**
     * this method is called from Optimization engine (possibly multiple times) when the new optimization result is ready.
     * It is up to you what to do with the result. We will save it to the provided target databank.
     * @param newRG
     * @param originalStrategyName
     * @param targetDatabank
     * @param dontSaveOriginalStr
     */
    protected void processResult(ResultsGroup newRG, String originalStrategyName, Databank targetDatabank, boolean dontSaveOriginalStr) {
        try {
            if(SQProject.isMemoryProtectionUsed()) {
                MemoryUsageChecker.checkAvailableMemory();
            }

            if(newRG.getOptimizationProfile()!=null) {
                //newRG.getOptimizationProfile().reset();
            }

            if(newRG.getName().equals(originalStrategyName)){
                // it is retest of original strategy
                if(!dontSaveOriginalStr) {
                    targetDatabank.add(newRG, true);
                }

                return;
            }

            newRG.removeUnsavableSettings();
            //newRG.setLastSettings(lastSettingsXml);
            //newRG.computeEquityChartData();

            //Log.info("Saving RG: {}", newRG.getName());
            targetDatabank.add(newRG, true);

        } catch(Exception e) {
            String message = "Error while processing strategy '" + newRG.getName() + "'";
            if(newRG != null) {
                newRG.clear();
            }
            Log.error(message, e);
        } catch (OutOfMemoryError e) {
            if(newRG != null) {
                newRG.clear();
            }
        }
    }

    //------------------------------------------------------------------------

    private void applyParamSettingsToStrategy(ParametersSettings paramSettings, StrategyBase xmlS, TradingOptions tradingOptions) throws Exception {
        //apply default parameter values to strategy if manual mode
        if(paramSettings.manualMode) {
            OptimizationParams params = paramSettings.params;

            for(int a=0; a<params.size(); a++) {
                Parameter param = params.get(a);
                Variable var = xmlS.variables().get(param.getName());

                if(var != null) {
                    if(ParametrizationTypes.ParamTypeTradingOptions.equals(var.getParamType())) {
                        String[] toNames = var.getId().split("\\.");
                        Object value = null;

                        if(param.getType() == Variable.TypeBoolean) {
                            value = param.getOriginalValue() == 1;
                        }
                        else if(param.getType() == Variable.TypeTime || param.getType() == Variable.TypeInt) {
                            value = (int) param.getOriginalValue();
                        }
                        else {
                            value = param.getOriginalValue();
                        }

                        for(int i=0; i<tradingOptions.size(); i++) {
                            TradingOption option = tradingOptions.get(i);
                            if(option.getClass().getSimpleName().equals(toNames[0])) {
                                option.setParameterValue(toNames[1], value.toString());
                            }
                        }
                    }
                    else {
                        if(param.getType() == Variable.TypeBoolean) {
                            var.setValue(param.getOriginalValue() == 1);
                        }
                        else if(param.getType() == Variable.TypeTime) {
                            var.setValue((int) param.getOriginalValue());
                        }
                        else {
                            var.setValue(param.getOriginalValue());
                        }
                    }
                }
                else {
                    Log.error("Unable to set original value of parameter " + param.getName() + " - Variable not found");
                }
            }
        }
    }

    //------------------------------------------------------------------------

    private boolean isSignalVariable(String varId, ArrayList<Element> signals) {
        if(signals == null || signals.size() == 0) {
            return false;
        }

        for(int i=0; i<signals.size(); i++) {
            Element signal = signals.get(i);

            String variable = signal.getAttributeValue("variable");
            if(variable != null && variable.equals(varId)) {
                return true;
            }
        }

        return false;
    }

    //------------------------------------------------------------------------

    public Parameter createParameter(Variable variable, int distributionUp, int distributionDown, int maxSteps) throws InvocationTargetException, InstantiationException, IllegalAccessException {
        String name = variable.getName();
        byte type = variable.getInternalType();

        if(variable.getParamType() == null) variable.setParamType(ParametrizationTypes.ParamTypeOtherParam);

        double startValue = 0;
        double stopValue = 0;
        double stepValue = 0;
        double originalValue = 0;
        String varId = variable.getId();

        if(varId.contains("MinimumSL") || varId.contains("MaximumSL") || varId.contains("MinimumPT") || varId.contains("MaximumPT")) {
            // special handling for Min/MaxSL/PT
            originalValue = variable.getValueAsDouble();
            startValue = (int) 0;
            stopValue = (int) 500;

            stepValue = findStep((int) startValue, (int) stopValue, maxSteps);

        } else {
            if(type == Variable.TypeDouble){
                double value = variable.getValueAsDouble();
                double deltaUp = value / 100 * distributionUp;
                double deltaDown = value / 100 * distributionDown;

                int decimals = 2;

                originalValue = variable.getValueAsDouble();
                startValue = SQUtils.round(value - deltaDown, decimals);
                stopValue = SQUtils.round(value + deltaUp, decimals);
                stepValue = findStep(startValue, stopValue, decimals, maxSteps);
            }
            else if(type == Variable.TypeInt) {
                int varMin = (int) variable.getMin();
                int varMax = (int) variable.getMax();

                if(varMin != Integer.MIN_VALUE && varMax != Integer.MIN_VALUE) {
                    startValue = varMin;
                    stopValue = varMax;
                }
                else {
                    double value = variable.getValueAsDouble();
                    double deltaUp = value / 100 * distributionUp;
                    double deltaDown = value / 100 * distributionDown;

                    originalValue = variable.getValueAsDouble();
                    startValue = (int) (value - deltaDown);
                    stopValue = (int) (value + deltaUp);

                    if(value <= 3 & stopValue <= 4) {
                        stopValue = 6;
                    }
                }

                if(variable.getParamType().equals(ParametrizationTypes.ParamTypePeriod)){
                    startValue = startValue > 2 ? startValue : 2;
                }

                stepValue = findStep((int) startValue, (int) stopValue, maxSteps);
            }
            else if(type == Variable.TypeTime){
                originalValue = variable.getValueAsInt();
                double totalMinutes = SQTime.HHMMToMinutes((int) originalValue);
                int deltaUp = (int)(totalMinutes / 100 * distributionUp);
                int deltaDown = (int)(totalMinutes / 100 * distributionDown);

                startValue = totalMinutes - deltaDown;
                startValue = startValue < 0 ? 0 : startValue;

                stopValue = totalMinutes + deltaUp;
                stopValue = stopValue > 1439 ? 1439 : stopValue;

                stepValue = findStep((int) startValue, (int) stopValue, maxSteps);

                startValue = SQTime.minutesToHHMM((int) startValue);
                stopValue = SQTime.minutesToHHMM((int) stopValue);
                stepValue = SQTime.minutesToHHMM((int) stepValue);
            }
            else if(type == Variable.TypeBoolean){
                boolean value = variable.getValueAsBoolean();
                originalValue = value ? 1 : 0;
                startValue = originalValue;
            }
        }

        return createParameterObject(name, type, true, startValue, stopValue, stepValue, originalValue);
    }

    //------------------------------------------------------------------------

    private Parameter createParameterObject(String name, byte type, boolean b, double startValue, double stopValue, double stepValue, double originalValue) throws InvocationTargetException, InstantiationException, IllegalAccessException {
        // this is a Java hack necessary because Parameter constructor is not public in Builds until 136.
        // In Build 136 and above the whole code below can be replaced with:
        // return new Parameter(name, type, true, startValue, stopValue, stepValue, originalValue);

        Constructor<?>[] constructors = Parameter.class.getDeclaredConstructors();
        for (Constructor<?> constructor : constructors) {
            constructor.setAccessible(true);
            return (Parameter) constructor.newInstance(name, type, true, startValue, stopValue, stepValue, originalValue);
        }

        throw new InstantiationException("Cannot create Parameter object!");
    }

    //------------------------------------------------------------------------

    private int findStep(int start, int stop, int maxSteps){
        double step = ((double) stop - start) / (maxSteps - 1);

        if(Math.abs(step - (int) step) < 0.001){
            return (int) step;
        }
        else {
            if(stop - start > 0){
                step = step < 1 ? 1 : (int) step;
            }
            else {
                step = step > -1 ? -1 : (int) step;
            }
        }

        int steps = getStepsCount(start, stop, step);

        while(steps > maxSteps - 1){
            step += step > 0 ? 1 : -1;
            steps = getStepsCount(start, stop, step);
        }

        return (int) step;
    }

    //------------------------------------------------------------------------

    private double findStep(double start, double stop, int decimals, int maxSteps){
        double step = (stop - start) / (maxSteps - 1);
        double roundedStep = SQUtils.round(step, decimals);

        double minDiff = 1.0d / Math.pow(10, decimals);

        if(roundedStep == step){
            return roundedStep;
        }

        if(roundedStep > step){
            step = step > 0 ? roundedStep - minDiff : roundedStep;
        }
        else {
            step = step > 0 ? roundedStep : roundedStep + minDiff;
        }

        int steps = getStepsCount(start, stop, step);

        while(steps > maxSteps - 1){
            step += step >= 0 ? minDiff : -minDiff;
            steps = getStepsCount(start, stop, step);
        }

        return SQUtils.round(step, decimals);
    }

    //------------------------------------------------------------------------

    private int getStepsCount(double start, double stop, double step){
        return (int)((stop - start) / step);
    }

    //------------------------------------------------------------------------

    private IFitnessFunction createFitnessFunction() {
        return new FitnessFromStrategyResultForCA("NetProfit");
    }

    //------------------------------------------------------------------------

    private TradingOptions createTradingOptions() {
        TradingOptions options = new TradingOptions();

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

## Note – 29/9/2022 update of FitnessFromStrategyResultForCA snippet

In SQX Build 136 Dev version we extended the **IFitnessFunction** with new abstract members, and because of this a helper snippet **FitnessFromStrategyResultForCA** used in this article will not work – depending on which version of SQX you are using.

You need to add a few missing methods to the end of this snippets AFTER the last method printWeightedGoals() in order to make it compile:

```
//------------------------------------------------------------------------

@Override
public ArrayList<MetricForFitness> getMetricsForFitness() {
    ArrayList<MetricForFitness> metrics = new ArrayList<MetricForFitness>();

    for (int i = 0; i < weightedGoals.size(); i++) {
        Goal goal = weightedGoals.get(i);

        if (goal.use) {
            MetricForFitness m = new MetricForFitness();

            m.metric = weightedGoals.get(i).statsValueName;
            m.weight = weightedGoals.get(i).weight;
            m.valueType = weightedGoals.get(i).valueType;
        }
    }

    return metrics;
}

@Override
public double getMetricValue(ResultsGroup results, byte direction, byte sampleType, String databankColumnName) throws Exception {
    return 0;
}

@Override
public IFitnessFunction clone() {
    FitnessFromStrategyResultForCA obj = new FitnessFromStrategyResultForCA();

    obj.databankColumnName = this.databankColumnName;
    obj.weightedGoals = cloneGoals();

    return obj;
}

private ArrayList<Goal> cloneGoals() {
    ArrayList<Goal> newWeightedGoals = new ArrayList<Goal>();

    for(int i=0; i<weightedGoals.size(); i++) {
        Goal g = weightedGoals.get(i);
        Goal gNew = new Goal();
        gNew.valueType = g.valueType;
        gNew.use = g.use;
        gNew.statsValueName = g.statsValueName;
        gNew.weight = g.weight;
        gNew.target = g.target;

        newWeightedGoals.add(gNew);
    }
    return newWeightedGoals;
}
```
