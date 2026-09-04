# Backtesting strategy programmatically including robustness tests

- Source: <https://strategyquant.com/doc/programming-for-sq/backtesting-strategy-programmatically-including-robustness-tests/>
- Section: Programming for StrategyQuant X › Coding sessions examples
- Fetched: 2026-09-04

---

This article is a continuation of the previous article [Running strategy backtests programmatically](running-strategy-backtests-programmatically.md).

It will show how to run backtest programmatically, this time including selected cross checks (robustness tests). The principle of programmatical backtest is the same, we will not explain it again here.

We’ll explain only the new things.

## Using processDatabank() to run backtest of a strategy

Just like in previous example we’ll use CustomAnalysis snippet and its method ***processDatabank()***. Here we’ll pick first strategy from databank and perform backtest + robustness tests on it.

The code of method processDatabank() is:

```
//------------------------------------------------------------------------

@Override
public ArrayList<ResultsGroup> processDatabank(String project, String task, String databankName, ArrayList<ResultsGroup> databankRG) throws Exception {
    if(databankRG.size() == 0) {
        return databankRG;
    }

    // get first strategy in databank to perform retest on it
    ResultsGroup strategyToRetest = databankRG.get(0);

    //------------------------------------
    // MAIN SETUP BACKTEST
    ResultsGroup backtestResultRG = runMainBacktest(strategyToRetest);

    //------------------------------------
    // CROSS CHECKS
    runAndSaveCrossChecks(backtestResultRG, strategyToRetest.getName());

    // get backtest result details
    int trades = backtestResultRG.portfolio().stats(Directions.Both, PlTypes.Money, SampleTypes.FullSample).getInt(StatsKey.NUMBER_OF_TRADES);
    double profit = backtestResultRG.portfolio().stats(Directions.Both, PlTypes.Percent, SampleTypes.FullSample).getDouble(StatsKey.NET_PROFIT);

    // do something with new backtest results
    // ResultsGroup now contains main backtest + all the configured cross checks.
    // look at: https://strategyquant.com/doc/programming-for-sq/working-with-resultsgroup/
    // on how to get the individual results

    // here we'll just save the newly backtested strategy to the target databank
    Databank targetDatabank = ProjectEngine.get(project).getDatabanks().get("Results");

    targetDatabank.add(backtestResultRG, true);

    return databankRG;
}
```

You can see that we have two sub methods there:

- ***runMainBacktest()*** will run the main backtest of the strategy and produces ***ResultsGroup*** with backtest result. This method is as same as in original example and it will be not explained here
- ***runAndSaveCrosschecks()*** will run the specified crosschecks and put them all into the same ***ResultsGroup*** object as separate results

Then it will save this new result to a target databank.

## Configuring crosschecks

Crosschecks in SQX are implemented as plugins, and each crosscheck has a method ***runTest()*** that will run given cross check on the strategy.

So when we have the crosschecks configured we can just call this method on every cross check we want to perform.

The creation / configuration of the crosscheck is the challenge here, it is done in a sub method ***configureCrosschecks()*** – this method returns an array of crosschecks, which are then executed one by one.

There are more ways to create crosscheck configurations, but the easiest one both for programming and for normal use is to simply let them be created from the XML format that SQX is storing it. This is the method we are using here.

The code of **configureCrosschecks()** method and some sub methods:

```
   //------------------------------------------------------------------------

   private ArrayList<ICrossCheck> configureCrosschecks() throws Exception {
       ArrayList<ICrossCheck> crossChecks = new ArrayList<ICrossCheck>();

       crossChecks.add( createFromXml("c:\\RetestWithHigherPrecision.xml") );
       crossChecks.add( createFromXml("c:\\MonteCarloManipulation.xml") );
       crossChecks.add( createFromXml("c:\\MonteCarloRetest.xml") );

       return crossChecks;
   }

   //------------------------------------------------------------------------

   private ICrossCheck createFromXml(String xmlFile) throws Exception {
       Element elXml = XMLUtil.fileToXmlElement(new File(xmlFile));

       String ccName = elXml.getName();

       ICrossCheck crossCheck = getCrossCheckPlugin(ccName);
       if(crossCheck != null) {
           crossCheck.readSettings(elXml, null);
       }
       else {
           throw new Exception ("Cannot find cross check plugin '" + ccName + "'");
       }

       return crossCheck;
   }

   //------------------------------------------------------------------------

   private ICrossCheck getCrossCheckPlugin(String name) {
       ArrayList<ICrossCheck> plugins = SQPluginManager.getPlugins(ICrossCheck.class);

       for(int i=0; i<plugins.size(); i++) {
           if(plugins.get(i).getSettingName().equals(name)) {
               return plugins.get(i).clone(null);
           }
       }

       return null;
   }
```

You can see that the crosscheck configuration is created by method ***createFromXml()*** which gets an XML file as a parameter. This XML file is the definition of the given crosscheck.

The method will first get a clone of the plugin for the respective crosscheck by calling ***getCrossCheckPlugin(pluginName)***, and then it simply calls ***readSettings(xml)***on the object to initialize it with settings from XML.

After this the crosscheck configuration object is ready to run.

In our example we are creating three crosschecks:

- Retest with higher timeframe
- Monte Carlo manipulation with some selected methods
- Monte Carlo retesting with selected methods

We have a separate XML file with configuration for each of them.

### How to prepare the crosscheck XML configuration file

The easiest approach is to use SQX UI to configure the exact settings on the crosscheck you want to use in either Builder or Retester project.

Then save the whole Builder/Retester project to a .cfx file. This file as a .cfx extension, but it is a standard ZIP file that you can open in WinZip or WinRar program.

Inside the file you’ll find config.xml – this is the XML document that contains settings for the whole project.

Here find the section **<CrossChecks>** and from there copy & paste the XML of crosscheck you want into a separate file that you’ll later load in our custom analysis snippet.

[![](https://strategyquant.com/wp-content/uploads/2022/05/crosschecks_xml-750x452.png)](https://strategyquant.com/wp-content/uploads/2022/05/crosschecks_xml-750x452.png)

An example of RetestWithHigherPrecision.xml file:

```
<RetestWithHigherPrecision use="true">
  <Settings>
    <Precision>2</Precision>
    <Spread>3</Spread>
  </Settings>
  <AcceptanceSettings>
    <Conditions CrossCheck="RetestWithHigherPrecision">
      <Condition use="true">
        <Left-Side valueType="column">
          <Column-Value column="NetProfit" columnType="0" format="Decimal2PL" resultType="RetestWithHigherPrecision" direction="0" sampleType="127" plType="10" confidenceLevel="50" market="1" subresult="30" pctRatio="0" class="NetProfit" />
        </Left-Side>
        <Comparator value="&gt;=" />
        <Right-Side valueType="column">
          <Column-Value column="NetProfit" columnType="0" name="Net profit" format="Decimal2PL" resultType="main" direction="0" sampleType="127" plType="10" confidenceLevel="undefined" market="undefined" subresult="undefined" pctRatio="80" class="NetProfit" />
        </Right-Side>
      </Condition>
      <Condition use="true">
        <Left-Side valueType="column">
          <Column-Value column="NumberOfTrades" columnType="0" format="Integer" resultType="RetestWithHigherPrecision" direction="0" sampleType="127" plType="10" confidenceLevel="50" market="1" subresult="30" pctRatio="0" class="NumberOfTrades" />
        </Left-Side>
        <Comparator value="&gt;=" />
        <Right-Side valueType="column">
          <Column-Value column="NumberOfTrades" columnType="0" name="# of trades" format="Integer" resultType="main" direction="0" sampleType="127" plType="10" confidenceLevel="undefined" market="undefined" subresult="undefined" pctRatio="80" class="NumberOfTrades" />
        </Right-Side>
      </Condition>
      <Condition use="true">
        <Left-Side valueType="column">
          <Column-Value column="DrawdownPct" columnType="0" format="Decimal2Pct" resultType="RetestWithHigherPrecision" direction="0" sampleType="127" plType="10" confidenceLevel="50" market="1" subresult="30" pctRatio="0" class="DrawdownPct" />
        </Left-Side>
        <Comparator value="&lt;" />
        <Right-Side valueType="column">
          <Column-Value column="DrawdownPct" columnType="0" name="Max DD %" format="Decimal2Pct" resultType="main" direction="0" sampleType="127" plType="10" confidenceLevel="undefined" market="undefined" subresult="undefined" pctRatio="130" class="DrawdownPct" />
        </Right-Side>
      </Condition>
    </Conditions>
  </AcceptanceSettings>
</RetestWithHigherPrecision>
```

An example of MonteCarloManipulation.xml file:

```
<MonteCarloManipulation use="true">
  <Settings>
    <Methods>
      <Method use="true" type="RandomizeTradesOrder">
        <Params>
          <Param key="Method" type="String">resampling</Param>
        </Params>
      </Method>
      <Method use="true" type="RandomlySkipTrades">
        <Params>
          <Param key="Probability" type="Integer">10</Param>
        </Params>
      </Method>
    </Methods>
    <NumberOfSimulations>10</NumberOfSimulations>
    <MCUseFullSample>false</MCUseFullSample>
  </Settings>
  <AcceptanceSettings>
    <Conditions />
  </AcceptanceSettings>
</MonteCarloManipulation>
```

An example of MonteCarloRetest.xml file:

```
<MonteCarloRetest use="true">
  <Settings>
    <Methods>
      <Method use="false" type="RandomizeHistoryData">
        <Params>
          <Param key="ProbabilityUp" type="Integer">20</Param>
          <Param key="MaxChangeUp" type="Integer">10</Param>
          <Param key="ProbabilityDown" type="Integer">20</Param>
          <Param key="MaxChangeDown" type="Integer">10</Param>
          <Param key="KeepConnected" type="Boolean">true</Param>
        </Params>
      </Method>
      <Method use="false" type="RandomizeHistoryDataFixedRange">
        <Params>
          <Param key="MaxChange" type="Integer">40</Param>
        </Params>
      </Method>
      <Method use="true" type="RandomizeMinDistance">
        <Params>
          <Param key="Min" type="Double">0.0</Param>
          <Param key="Max" type="Double">10.0</Param>
        </Params>
      </Method>
      <Method use="true" type="RandomizeSlippage">
        <Params>
          <Param key="Min" type="Double">0.0</Param>
          <Param key="Max" type="Double">5.0</Param>
        </Params>
      </Method>
      <Method use="true" type="RandomizeSpread">
        <Params>
          <Param key="Min" type="Double">1.0</Param>
          <Param key="Max" type="Double">5.0</Param>
        </Params>
      </Method>
      <Method use="false" type="RandomizeStartingBar">
        <Params>
          <Param key="MaxChange" type="Integer">100</Param>
        </Params>
      </Method>
      <Method use="false" type="RandomizeStrategyParameters">
        <Params>
          <Param key="Probability" type="Integer">10</Param>
          <Param key="MaxChange" type="Integer">20</Param>
          <Param key="Symmetric" type="Boolean">true</Param>
        </Params>
      </Method>
      <Method use="false" type="RandomizeStrategyParametersCustom">
        <Params>
          <Param key="Probability" type="Integer">10</Param>
          <Param key="MaxChange" type="Integer">20</Param>
          <Param key="Periods" type="Boolean">true</Param>
          <Param key="Constants" type="Boolean">true</Param>
          <Param key="Shifts" type="Boolean">false</Param>
          <Param key="OtherParams" type="Boolean">true</Param>
          <Param key="EntryLevels" type="Boolean">true</Param>
          <Param key="EntryLogic" type="Boolean">true</Param>
          <Param key="ExitParamsUsed" type="Boolean">true</Param>
          <Param key="ExitParamsUnused" type="Boolean">true</Param>
          <Param key="BooleanParams" type="Boolean">false</Param>
          <Param key="TradingOptions" type="Boolean">false</Param>
          <Param key="Symmetric" type="Boolean">true</Param>
        </Params>
      </Method>
    </Methods>
    <NumberOfSimulations>10</NumberOfSimulations>
    <MCUseFullSample>false</MCUseFullSample>
    <MCBacktestPrecision>-1</MCBacktestPrecision>
  </Settings>
  <AcceptanceSettings>
    <Conditions />
  </AcceptanceSettings>
</MonteCarloRetest>
```

## Running crosschecks

When we have the crosscheck configuration objects ready we’ll run the crosschecks simply by calling their **runTest()** method.

Code:

```
private void runAndSaveCrossChecks(ResultsGroup mainResult, String strategyName) throws Exception {
    ArrayList<ICrossCheck> crossChecks = configureCrosschecks();

    StopPauseEngine stopPauseEngine = new StopPauseEngine();

    for(int i=0; i<crossChecks.size(); i++) {

        ICrossCheck crossCheck = crossChecks.get(i);
        crossCheck.setStopPauseEngine(stopPauseEngine);

        try {
            Log.info(String.format("Running cross check %s for %s...", crossCheck.getName(), strategyName));

            boolean ccResult = crossCheck.runTest(mainResult, i, globalATR, null, true, null, strategyName);

            if(!ccResult) {
                // uncomment if you want to stop running other cross checks if one of them does not pass
                // otherwise all the cross checks are run on the strategy
                //return;
            }

        } catch(Exception e) {
            Log.error("Cross check exception: "+e.getMessage(), e);
        }
    }
}
```

## Running this Custom Analysis snippet

When you compile and run this snippet the result is as follows. It takes the first strategy from databank, performs man backtest and the 3 crosschecks we configured on it and saves it as a new result into the databank.

You can see that the new result contains also Monte Carlo results

[![](https://strategyquant.com/wp-content/uploads/2022/05/programmatical_cc-750x599.png)](https://strategyquant.com/wp-content/uploads/2022/05/programmatical_cc-750x599.png)
