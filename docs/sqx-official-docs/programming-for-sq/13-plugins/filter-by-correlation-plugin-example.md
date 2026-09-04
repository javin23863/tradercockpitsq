# Filter by correlation – plugin example

- Source: <https://strategyquant.com/doc/programming-for-sq/filter-by-correlation-plugin-example/>
- Section: Programming for StrategyQuant X › Plugins
- Fetched: 2026-09-04

---

**Note**

- **This example and functionality it requires is added only to SQ Build 136 Dev 2 or later**
- **The functionality described is for “advanced” and dedicated users only. It is not completely documented, this is the first example showing this possibility.**

**Note 2– there is an updated version for SQX Build 140 and above.** **If you have some issues with appearing plugin in the user interface, please delete the cache in this folder SQ X install folder\internal\electron\resources\userData and restart SQ X.**

**Installation instructions:**

1. **Unzip the file to the SQ X install folder/user/extend/plugins**
2. **Restart SQ X**

This is an example on how to extend SQX functionality with your own plugin.

StrategyQuant X is made as a plugin-based system, every part of the program is a plugin. However, extending SQX with your own plugin was not possible until now (Build 136 Dev 2), because you needed access to full source code, and you had no way to compile your plugin.

SQX UI is basically a web application written in AngularJS, where backend is in Java servlets.

In this example we’ll show step by step how to create a simple plugin including a simple UI.

The full plugin is available also as a ZIP file attached to this article.

This plugin will implement a functionality of filtering strategies by correlation. It works like this:

- plugin it will add new button **Filter by correlation** to the databank
- upon click on the button a popup dialog is shown, where user configures maximum allowed correlation
- when confirmed, plugin will call its backend method and it will go through all the strategies in actual databank filtering out (removing) the ones that have higher than Max correlation with other strategies in this databank

Credits – this plugin is based on the code posted by **AgentPot** on Discord as a part of our [live coding sessions](https://strategyquant.com/codebase/live-coding-sessions/), the code is used with his agreement.

## Location of user plugins

User-developed plugins should be located in the folder **{SQ installation}/user/extend/Plugins**. You are welcome to download the ZIP file of this plugin and extract it to this folder.

When you open CodeEditor, you should see the folder of this plugin there:

[![](https://strategyquant.com/wp-content/uploads/2022/06/ce_plugin_navigator.jpg)](https://strategyquant.com/wp-content/uploads/2022/06/ce_plugin_navigator.jpg)

This plugin consists of three files:

- ***FilterByCorrelationServlet.java*** – java file that implements the functionality of filtering strategies by correlation.
- ***module.js*** – JavaScript file containing definition of the module in Angular
- ***popup.html*** – HTML file containing the UI of this new plugin

Let’s go through them one by one, starting with the simplest.

## Defining UI for the plugin – popup.html

Plugin UI is defined in file ***popup.html***. It is a standard HTML file that is displayed in modal window.

There are DIVs to decorate the window, the actual “form” is defined inside DIV modal-body:

```
<!-- Modal window-->
<div class="modal centered" id="filterByCorrelationButtonModal" tabindex="-1" role="dialog" aria-labelledby="myModalLabel">
    <div class="modal-dialog" role="document">
        <div class="modal-content">
            <div class="modal-header">
                <h4 class="modal-title" id="myModalLabel" tsq>Filter by correlation</h4>
            </div>
            <div class="modal-body" style="height: auto;">
                Functionality - all strategies that have correlation bigger than Max will be removed from the current databank.
                <label tsq>Filtering settings</label>
                <div class="row">
                    <div class="col-sm-12">
                        <div class="col-sm-6">
                            <label class="sqn-label" tsq>Correlation period:</label>
                        </div>
                        <div class="col-sm-6">
                            <select id="fbc-correlation-period" class="setting-control">    
                                <option value="5" tsq>Hour</option>
                                <option value="10" selected tsq>Day</option>
                                <option value="20" tsq>Week</option>
                                <option value="30" tsq>Month</option>
                            </select>
                        </div>
                    </div>
                </div>
                <div class="row">
                    <div class="col-sm-12">
                        <div class="col-sm-6">
                            <label class="sqn-label" tsq>Max correlation:</label>
                        </div>
                        <div class="col-sm-6">
                            <div class="sqn-spinner setting-control">
                                <input type="text" class="sqn-input" id="fbc-max-correlation" min="0" max="1" step="0.01" value="0.5" />
                                <div class="sqn-spinner-btns">
                                    <div class="sqn-spinner-minus" onClick="sqnSpinnerMinus(this)"></div>
                                    <div class="sqn-spinner-plus" onClick="sqnSpinnerPlus(this)"></div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            <div class="modal-footer">
                <a data-dismiss="modal" tsq>Close</a>
                <button type="button" class="btn btn-primary" onclick="onFilter()" tsq>Filter</button>
            </div>
        </div>
    </div>
</div>

<style>
    #filterByCorrelationButtonModal .setting-control {
        width: 100px;
    }
</style>

<script>
    function onFilter(){
        let period = document.getElementById("fbc-correlation-period").value;
        let maxCorrelation = document.getElementById("fbc-max-correlation").value;

        const urlParams = new URLSearchParams({
            projectName: window.parent.filterByCorrelationData.projectName,
            databankName: window.parent.filterByCorrelationData.databankName,
            period,
            maxCorrelation
        });

        CustomPluginController.sendRequest("filterByCorrelation/filter?" + urlParams.toString(), "GET", null, function(response){
            let responseJSON = JSON.parse(response);
            console.error(responseJSON);
            if(responseJSON.error){
                alert(responseJSON.error);
            }
            else {
                alert(responseJSON.success);
                window.parent.hidePopup("#filterByCorrelationButtonModal");
            }
        });
    }
</script>
```

The important thing here is the id **filterByCorrelationButtonModal** that is used in the first line. It is important because this popup is then referenced by this id in the JavaScript function in module.js.

When you’ll be developing your own plugin you must create your own unique ID of the popup, and use this id in ***module.js*** – see later.

Another specialty is that we use ***onClick()*** JavaScript event to capture form submit, and we’ll then use simple JS to retrieve the configured values.

They are then sent to the backend using a special method **CustomPluginController.sendRequest()** which makes a request to ***/filterByCorrelation/filter*** where it passes the parameters from the form and its return value is displayed in alert.

## Definition of the plugin in module.js

Plugin needs a special ***module.js*** file so that it will be properly recognized and incorporated into the UI.

It is JavaScript Angular code, but you don’t need to have detailed knowledge of Angular, you can use this example plugin and just copy & paste + modify it if you ae implementing your own plugin.

The whole code of module.js is:

```
angular.module('app.resultsdatabankactions.filterByCorrelation', ['sqplugin']).config(function(sqPluginProvider, $controllerProvider) {
    
    function callback(projectName, taskName, databankName, selectedStrategies){
        window.parent.filterByCorrelationData = {
            projectName,
            taskName,
            databankName
        };
        window.parent.showPopup("#filterByCorrelationButtonModal");
    }

    let buttonController = new CustomPluginController('app.resultsdatabankactions.filterByCorrelation', $controllerProvider, callback).init();

    sqPluginProvider.plugin("ResultsDatabankAction", 100, {
        title: Ltsq("Tools:Filter by correlation"),
        class: 'btn btn-normal btn-default',
        controller: buttonController,
        id: "databank-action-filterbycorrelation"
    });
    
    sqPluginProvider.addPopupWindow("plugins/FilterByCorrelation/popup.html", null, 'SQUANT');

});
```

Let’s go through the code part by part.

First part is registering our new plugin as Angular module:

```
angular.module('app.resultsdatabankactions.filterByCorrelation', ['sqplugin']).config(function(sqPluginProvider, $controllerProvider) {
```

You generally only need to give it an unique name, like **app.resultsdatabankactions.filterByCorrelation**.  
We internally use convention app.resultsdatabankaction.YourUniqueName

The second part is creating a button controller and callback that will show the popup dialog.

```
function callback(projectName, taskName, databankName, selectedStrategies){
    window.parent.filterByCorrelationData = {
        projectName,
        taskName,
        databankName
    };
    window.parent.showPopup("#filterByCorrelationButtonModal");
}

let buttonController = new CustomPluginController('app.resultsdatabankactions.filterByCorrelation', $controllerProvider, callback).init();
```

The important thing here is calling method ***window.parent.showPopup()*** to which you give an unique ID of the popup that was defined previously in popup.html. As the name suggests, calling this will show the popup and our form,

and button controller invokes it when the button is clicked.

The third part is registering this plugin and defining where it will be displayed:

```
sqPluginProvider.plugin("ResultsDatabankAction", 100, {
    title: Ltsq("Filter by correlation"),
    class: 'btn btn-normal btn-default',
    controller: buttonController,
    id: "databank-action-filterbycorrelation"
});
```

Here we say it is connected to **ResultsDatabankAction** extension point, which means it will be displayed as a button in databank.

The interesting thing here is title – if you don’t want it to be displayed as separate button, but displayed as another item in existing **Tools** button, you can simply use title:

```
title: Ltsq("Tools:Filter by correlation"),
```

You can also see that we defined button controller and set it to our ***buttonController*** defined previously.

The last part is registering our UI file in SQX pugin system – this wil ensure that the pooup.html file is loaded and becomes part of the UI.

```
sqPluginProvider.addPopupWindow("plugins/FilterByCorrelation/popup.html", null, 'SQUANT');
```

## Implementing backend – FilterByCorrelationServlet.java

Now that we have the UI, we need to implement the desired functionality on the backend.

The backend is made as HTTP servlet that processes HTTP requests from the frontend (UI) and performs the required functionality. If you remember, we defined this request in popup.html file, it uses a special method **CustomPluginController.sendRequest()** which makes a request to ***/filterByCorrelation/filter**.*

So we have to implement a servlet that handles this request and does what we want.

The servlet must implement [IServletPlugin](https://strategyquant.com/sqxapi/com/strategyquant/tradinglib/servlet/IServletPlugin.html) interface. The important method here is [getHandler()](https://strategyquant.com/sqxapi/com/strategyquant/tradinglib/servlet/IServletPlugin.html#getHandler()), which is called when backend receives request for ***/filterByCorrelation/***.

The request is then handled by our ***FilterByCorrelation*** inner class that implements the functinality.

So the part of the snippet that just handles the request and assigns a handler to it is:

```
@PluginImplementation
public class FilterByCorrelationServlet implements IServletPlugin {

    private static final Logger Log = LoggerFactory.getLogger(FilterByCorrelationServlet.class);
    private ServletContextHandler connectionContext;

    //------------------------------------------------------------------------
    //------------------------------------------------------------------------
    //------------------------------------------------------------------------

    @Override
    public String getProduct() {
        return SQConst.CODE_SQ;
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

    @Override
    public Handler getHandler() {
        if(connectionContext == null){
            connectionContext = new ServletContextHandler(ServletContextHandler.SESSIONS);
            connectionContext.setContextPath("/filterByCorrelation/");
            connectionContext.addServlet(new ServletHolder(new FilterByCorrelation()),"/*");
        }
        return connectionContext;
    }
```

### FilterByCorrelation inner class

This is the class that handles the request ***/filterByCorrelation/filter** from the UI.*

The first part of this class checks which command exactly was called – in our case there is only **filter** command.

```
class FilterByCorrelation extends HttpJSONServlet {
    @Override
    protected String execute(String command, Map<String, String[]> args, String method){
        switch(command) {
            case "filter": return onFilter(args);
            default:
                return apiErrorJSON("Execution failed. Unknown command '"+command+"'.", null);
        }
    }

    //------------------------------------------------------------------------

    private String onFilter(Map<String, String[]> args) {
        JSONObject response = new JSONObject();
        try {
            String projectName = tryGetParam(args, "projectName")[0];
            String databankName = tryGetParam(args, "databankName")[0];
            int period = Integer.parseInt(tryGetParam(args, "period")[0]);
            double maxCorrelation = Double.parseDouble(tryGetParam(args, "maxCorrelation")[0]);

            int removedCount = filterByCorrelation(projectName, databankName, period, maxCorrelation);

            response.put("success", "Strategies filtered, "+removedCount+" strategies removed.");
            return response.toString();
        }
        catch(Exception e){
            Log.error("Error while filtering strategies by correlation", e);
            response.put("error", "Filtering failed - " + e.getMessage());
            return response.toString();
        }
    }
```

Upon receiving this command it will execute **onFilter()** method which:

- gets parameters like project name, databank name, period and maxCorrelation from request parameters
- performs the actual filtering by correlation by calling method **filterByCorrelation(projectName, databankName, period, maxCorrelation)**
- it will then send back message with actual number of removed strategies or error message in case of error.

### filterByCorrelation() method

this method performs the actual filtering – going through all the strategies in the actual databank, computing correlations between the strategies and removing the strategies where correlation > maxCorrelation.

Computing correlation deserves an article and example by itself, you can follow how it is implemented in the code:

```
    private int filterByCorrelation(String projectName, String databankName, int period, double maxCorrelation) throws Exception {
        Log.info("Filtering by correlation (Project: " + projectName + ", databank: " + databankName + ")...");
        SQProject project = ProjectEngine.get(projectName);
        Databank databank = project.getDatabanks().get(databankName);
        int removedCount = 0;

        // Check for correlated strategies using fitness score as a ranking/priority list
        ArrayList<ResultsGroup> rgSortedCandidates = new ArrayList<>(databank.getRecords());

        rgSortedCandidates.sort((o1, o2) -> Double.valueOf(o1.getFitness()).compareTo(Double.valueOf(o2.getFitness())));

        ArrayList<ResultsGroup> rgSortedCandidatesRanked = new ArrayList();

        for(ResultsGroup rgSC : rgSortedCandidates) {
            rgSortedCandidatesRanked.add(0, rgSC);
        }

        ArrayList<ResultsGroup> rgUncorrelated = new ArrayList();
        // todo: multiple strategies could have the same fitness. this should be addressed using additional sorting (i.e. fitness > pf > ret/dd .. etc)

        while (rgSortedCandidatesRanked.size() > 0) {
            ResultsGroup focusStrat = rgSortedCandidatesRanked.get(0).clone();
            String focusStratName = focusStrat.getName();

            Log.info("Focus strategy to compare: {}", focusStratName);

            rgUncorrelated.add(focusStrat.clone()); // add the best strategy in the pool to the uncorrelated list as a starting point
            rgSortedCandidatesRanked.remove(0);

            Iterator<ResultsGroup> it = rgSortedCandidatesRanked.iterator();

            while(it.hasNext()) {
                ResultsGroup subStrat = it.next();

                String subStratName = subStrat.getName();

                if (subStratName != focusStratName) {
                    CorrelationPeriods corrPeriods = new CorrelationPeriods();

                    CorrelationType corrType = CorrelationTypes.getInstance().findClassByName("ProfitLoss");
                    CorrelationComputer correlationComputer = new CorrelationComputer();

                    // compute correlation periods
                    CorrelationPeriods correlationPeriods = precomputePeriodsAP(focusStrat, subStrat, period, corrType);
                    double corrValue = SQUtils.round2(correlationComputer.computeCorrelation(false, focusStratName, subStratName, focusStrat.orders(), subStrat.orders(), correlationPeriods, corrType));

                    Log.info(" - Correlation of '{}'' to focus strategy: {}", subStratName, corrValue);

                    if (corrValue > maxCorrelation) {
                        it.remove();
                        removedCount++;
                        Log.info("   - Removing strategy " + subStratName + " (correlation " + corrValue + " > " + maxCorrelation + ")...");
                        databank.remove(subStratName, true, true, true, true, true, null);
                    }
                } else {
                    it.remove();
                }
            }
        }

        return removedCount;
    }

    //------------------------------------------------------------------------
    
    private CorrelationPeriods precomputePeriodsAP(ResultsGroup paramResultsGroup1, ResultsGroup paramResultsGroup2, int paramInt, CorrelationType paramCorrelationType) throws Exception {
        CorrelationPeriods correlationPeriods = new CorrelationPeriods();
        TimePeriod timePeriod = CorrelationLib.getPeriod(paramResultsGroup1.orders());
        long l1 = timePeriod.from;
        long l2 = timePeriod.to;
        timePeriod = CorrelationLib.getPeriod(paramResultsGroup2.orders());
        if (timePeriod.from < l1) {
            l1 = timePeriod.from;
        }
        if (timePeriod.to > l2) {
            l2 = timePeriod.to;
        }

        TimePeriods timePeriods1 = CorrelationLib.generatePeriods(paramInt, l1, l2);
        TimePeriods timePeriods2 = timePeriods1.clone();

        paramCorrelationType.computePeriods(paramResultsGroup1.orders(), paramInt, timePeriods1);
        correlationPeriods.put(paramResultsGroup1.getName(), timePeriods1);

        paramCorrelationType.computePeriods(paramResultsGroup2.orders(), paramInt, timePeriods2);
        correlationPeriods.put(paramResultsGroup2.getName(), timePeriods2);

        return correlationPeriods;
    }        
}
```

## Working with plugins in Code Editor

We added a new functionality to work with user-defined plugins in SQX Build 136 Dev 2.

If you unzipped the attached plugin to the correct folder (**{SQ installation}/user/extend/Plugins)** and open the Code Editor you’ll see a new Plugins section with our new plugin there:

[![](https://strategyquant.com/wp-content/uploads/2022/06/ce_plugin_navigator-1.jpg)](https://strategyquant.com/wp-content/uploads/2022/06/ce_plugin_navigator-1.jpg)

You can normally create and edit files and folders there as usual. The new feature is the possibility to compile the plugin.

Right-click on the plugin folder and select Compile plugin:

[![](https://strategyquant.com/wp-content/uploads/2022/06/plugin_compile.jpg)](https://strategyquant.com/wp-content/uploads/2022/06/plugin_compile.jpg)

The plugin will be compiled and a new file FilterByCorrelation.jar will appear in the folder.

Now restart StrategyQuant and you should see a new databank button:

[![](https://strategyquant.com/wp-content/uploads/2022/06/button_plugin.jpg)](https://strategyquant.com/wp-content/uploads/2022/06/button_plugin.jpg)

When you click on it it should open a dialog shown below:

[![](https://strategyquant.com/wp-content/uploads/2022/06/plugin_dialog.jpg)](https://strategyquant.com/wp-content/uploads/2022/06/plugin_dialog.jpg)

Configure the filtering settings and click on Filter button. If you have some strategies in the databank you might notice that some of them will be removed and alert is shown:

[![](https://strategyquant.com/wp-content/uploads/2022/06/plugin_alert5.jpg)](https://strategyquant.com/wp-content/uploads/2022/06/plugin_alert5.jpg)

So our plugin worked – it computed and compared correlations between all strategies in current databank and removed 5 strategies that had correlation bigger than the configured max correlation.
