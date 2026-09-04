# Example plugin – a complete custom project task

- Source: <https://strategyquant.com/doc/programming-for-sq/example-plugin-a-complete-custom-project-task/>
- Section: Programming for StrategyQuant X › Plugins
- Fetched: 2026-09-04

---

This article is a description of developing a complex custom task plugin that can be used inside a custom project in StrategyQuant.

Note 1 – we had to make some changes in class structure to enable creating external plugins of this type, and because of this this plugin will work only from SQX Build 138 and later.

Note 2 – we have only limited documentation and examples about extending SQX with plugins, this is one of them. This example is intended for users who have the knowledge and are able to understand existing code.

Note 3 – there is an updated version for SQX Build 140 and above.

## The goal

To develop a new custom task in StrategyQuant including full settings.

We’ll implement a new custom task plugin named **Filter by correlation**.

Please don’t confuse this with already existing example plugin with the same name in the article here: <https://strategyquant.com/doc/programming-for-sq/filter-by-correlation-plugin-example/>

This older example is a simpler plugin implementing a new databank action.

Our new example would be to extend this idea and implement a whole task plugin.

## The result

As you can see below – a new task named Filter by correlation in the Custom projects tasks to choose:

[![](https://strategyquant.com/wp-content/uploads/2023/11/cp.jpg)](https://strategyquant.com/wp-content/uploads/2023/11/cp.jpg)

And settings of this new task:

[![](https://strategyquant.com/wp-content/uploads/2023/11/ct-750x181.jpg)](https://strategyquant.com/wp-content/uploads/2023/11/ct-750x181.jpg)

This task does a simple job – it compares strategies from **Source** databank with existing strategies in **Existing** databank and copies those that have lower than allowed correlation into **Target** databank.

This (and every) custom task is actually composed of two interdependent plugins:

**TaskFilterByCorrelation**

Plugin implementing the task itself and the main functionality, plus UI for the simple settings – it is the one you see in the Progress tab for this task.

**SettingsFilterByCorrelation**

Plugin implementing the full settings for this plugin, including the UI – it is the one you see in the Full settings tab.

All settings of this task are loaded using this Settings plugin.

Please note that this type of plugin combines Java – for backend and functionality implementation – and JavaScript (AngularJS) for frontend implementation.

**General steps needed to implement new task plugin**

1. Create Task and Settings plugins – both java and JS parts
2. Create default task.xml with the settings of databanks that are automatically created + default settings in Task plugin

Any new task you create must adhere to the package names:  
*package com.strategyquant.plugin.Task.impl.XXXXXX**package com.strategyquant.plugin.Settings.impl.XXXXX*

Where XXXXX is your name, SQX then creates folders so that package names match:  
*TaskXXXXXXXXXX**SettingsXXXXXXXXX*

In our case the folders are:  
*TaskFilterByCorrelation**SettingsFilterByCorrelation*

The best approach when creating your own task plugin would be to:

1. Make a copy of our two example plugins, change the class names and strings in the code from “FilterByCorrelation” to your own name
2. Make sure you delete/don’t copy the respective .jar files when copying the examples – .jar files will be created when you compile your own plugin.
3. Implement different settings handling and main functionality in Task and Settings plugins (Java)
4. Implement different UI in Settings and Task plugins – in both .HTML and .JS files

## Importing example plugins to your SQX

Example plugins are attached to this article (on the top). Both plugins come in two different archives – .sxp and standard ZIP.

ZIP version is not for importing, it is just for convenience – if somebody wants to have a look at the files without importing them into SQX.

Importing plugins using .sxp is simple – open your CodeEditor, go to Import choose, both .sxp files – for both plugins and click on OK.

[![](https://strategyquant.com/wp-content/uploads/2023/11/import.jpg)](https://strategyquant.com/wp-content/uploads/2023/11/import.jpg)

Pluggins will be imported, and you should see the new plugin folders in the file explorer:

[![](https://strategyquant.com/wp-content/uploads/2023/11/plugin_folders.jpg)](https://strategyquant.com/wp-content/uploads/2023/11/plugin_folders.jpg)

After this you only need to restart your SQX to see the new task available in Custom projects.

## SettingsFilterByCorrelation plugin

We’ll first describe the settings plugin. This is a plugin that implements the “full” settings, then ones that appear in the **Full settings** tab. It can have a different and more complicated UI than the simple settings displayed on the **Progress** tab.

[![](https://strategyquant.com/wp-content/uploads/2023/11/settings_ui-750x283.jpg)](https://strategyquant.com/wp-content/uploads/2023/11/settings_ui-750x283.jpg)

Again we’ll go through all the files:

[![](https://strategyquant.com/wp-content/uploads/2023/11/settingsfiles.jpg)](https://strategyquant.com/wp-content/uploads/2023/11/settingsfiles.jpg)

### FilterByCorrelationCtrl.js

Angular controller for this plugin. The main function of the controller is to add variables (their values) into scope and thus make them available in the HTML file.

In the settings plugins we set the whole config from FilterByCorrelationService:

```
$scope.config = FilterByCorrelationService.config;
```

Plus we can set some additional variables, for example list of correlation types/periods used in list boxes.

```
    $scope.correlationTypes = [];
    $scope.correlationPeriods = [];

    loadToArray($scope.correlationTypes, SQConstants.getConstants().correlationTypes);
    loadToArray($scope.correlationPeriods, SQConstants.getConstants().correlationPeriods);
```

In addition to setting the variables this controller also handles load/save of settings – when they should be saved and when read.

There is a controller action ***settingsChanged()*** called from HTML in every change of input, and this method then calls ***FilterByCorrelationService.saveSettings()*** to save the settings.

Load of settings is triggered by an event ***SETTINGS\_TAB\_RELOAD*** that is automatically called on tab init or when you switch to this tab from another one.

Settings controller has to add a listener to this event and on switching to this settings tab it would trigger this event which will call the ***init()*** method which in turn loads current settings by calling ***FilterByCorrelationService.loadSettings();***

### FilterByCorrelationService.js

Service for this filter, handling things like loading and saving data from/to the backend, setting default values, etc.

All services are singleton instances, they are created only once when the app starts.

Each settings service contains a config object that holds the settings for the currently selected task and the loadSettings/saveSettings methods.

The current xml settings for the selected task is obtained by call:

```
var filteringObj = AppService.getCurrentTaskTabSettings("FilterByCorrelation");
```

Then we can read the value from xml:

```
instance.config.correlation.max = getNodeFloatValue(filteringObj, 'CorrMax', 0.3);
```

or write values into it:

```
addNode('CorrMax', instance.config.correlation.max, filteringObj, xmlDoc);
```

So in service we directly change the XML.

Please note – service instance is only one, it is shared between Settings plugin (full settings UI) and Task plugin (simple settings UI).

### module.js

Registers the plugin in the UI interface. To display the full settings panel, a plugin of the SettingsTab type should be registered.

Mandatory plugin attributes are:

- task – name of the task for which it should be displayed, in this case “FilterByCorrelation”. If we would like to display this setting tab also for the Build and Retest task, there will be ‘FilterByCorrelation,Build,Retest’
- configElemName – setting name in xml
- templateUrl – which html to display
- controller – which controller is bound to the html from the templateUrl definition
- dataItem – ID setting tab, must have a unique name among SettingsTab tabs

other settings such as title, help, helpUrl, are understandable from the attribute name.

### settings.html, styles.css

HTML file containing UI definition of the settings and CSS definition of styles used.

### SettingsFilterByCorrelation.jar

This is an already compiled plugin. You will not see it in your own plugin until you compile it – see instructions on how to do this later.

### SettingsFilterByCorrelation.java

Java class that handles backend for this plugin. It implements ***ISettingTabPlugin***

Interface and the most important method is ***readSettings()*** – that reads settings from the XML provided by the UI (parameter elSettings), parses it and saves it into data object under the keys unique to this plugin:

```
public void readSettings(String projectName, ISQTask task, Element elSettings, TaskSettingsData data) {
        Element elFiltering = null;

        try {
            elFiltering = XMLUtil.getChildElem(elSettings, getSettingName());
        } catch(Exception e){
            data.addError(getSettingName(), null, e.getMessage());
            return;
        }
       
        try {          
            Databank dbSource = ProjectConfigHelper.getDatabankByType(projectName, "Source", elSettings);
            Databank dbTarget = ProjectConfigHelper.getDatabankByType(projectName, "Target", elSettings);
            Databank dbExisting = ProjectConfigHelper.getDatabankByType(projectName, "Existing", elSettings);

            data.addParam(SettingsKeys.DatabankSource, dbSource);
            data.addParam(SettingsKeys.DatabankTarget, dbTarget);
            data.addParam("DatabankExisting", dbExisting);      

            CorrelationSettings corr = new CorrelationSettings();
            corr.loadFromXml(elFiltering);

            data.addParam(SettingsKeys.Correlation, corr);

        } catch(Exception e){
            data.addError(getSettingName(), null, L.t("Cannot load FilterByCorrelation settings. ") + e.getMessage());
        }
    }
```

## TaskFilterByCorrelation plugin

As said above, this is the “main” plugin that implements the functionality. This plugin is also responsible for displaying the “simple” settings visible in the Progress tab.

[![](https://strategyquant.com/wp-content/uploads/2023/11/ct-1-750x181.jpg)](https://strategyquant.com/wp-content/uploads/2023/11/ct-1-750x181.jpg)

You can see the plugin consists of several files, including subfolder structure. We’ll go through them one by one.

[![](https://strategyquant.com/wp-content/uploads/2023/11/taskfiles.jpg)](https://strategyquant.com/wp-content/uploads/2023/11/taskfiles.jpg)

### module.js

Registers the plugin in the UI interface. In order to display the simple settings it has to register a plugin of type “SimpleTaskSettings”.

The mandatory arguments of the plugin are:

- taskType – name of the task it is referring to
- templateUrl – URL to the HTML file containing the UI – it is one of the other files described below
- controller – which controller will be connected to the HTML template – again this controller is defined in one of the next files

Its JavaScript code:

```
angular.module('app.settings').config(function(sqPluginProvider, SQEventsProvider) {
    sqPluginProvider.plugin("SimpleTaskSettings", 1, {
        taskType: 'FilterByCorrelation',
        templateUrl: '../../../plugins/TaskFilterByCorrelation/simpleSettings/simpleSettings.html',
        controller: 'SimpleFilterByCorrelationCtrl',
        getInfoPanels: function(xmlConfig, injector){
            var L = injector.get("L");
            var settingsPlugins = sqPluginProvider.getPlugins('SettingsTab');
            var groups = [
                {
                    title: L.tsq('Filtering options'),
                    settings: [
                        { name : L.tsq("Filtering"), value : getTaskInfoPanelSettings(xmlConfig, getItem(settingsPlugins, 'configElemName', "FilterByCorrelation")) }
                    ]
                }
            ];
           
            return groups;
        }
    });
});
```

### simpleSettings folder

Contains UI for the simple settings of this plugin – it is the mini-settings that is displayed in the Progress tab.

### SimpleFilterByCorrelationCtrl.js and simpleSettings.html

HTML code for the settings and Angular controller that handles it. Simple settings controller works as same as the full settings controller described above, with one difference – it does not need to implement when to load settings because this is already done in full settings controller.

### styles.css

File that can contain styles used in the UI

### task.xml

Default settings of this task. You can see that in this plugin we basically save:

- 3 databanks – Source, Target, Existing
- Correlation filtering settings

As described in ***FilterByCorrelationService*** description, the service manipulates the XML directly – it loads XML from the backend and sends XML to the backend to save the changes.

The default structure of XML settings for our plugin is defined here and in the service class.

### TaskFilterByCorrelation.jar

This is an already compiled plugin. You will not see it in your own plugin until you compile it – see instructions on how to do this later.

### TaskFilterByCorrelation.java

Java class that handles backend for this plugin. Because it is a task plugin, it is extended from ***AbstractTask***:

```
public class TaskFilterByCorrelation extends AbstractTask {
```

The most important method is ***start()*** – this is the method that is called when you click on Start button in the custom project, and this is where the main functionality is implemented:

```
    public void start() throws Exception {
        loadSettings();
```

in the beginning it loads settings – this loads settings that were prepared in our second plugin **SettingsFilterByCorrelation**

Then it updates the progress engine:

```
        progressEngine.setLogPrefix(taskLogPrefix);
        progressEngine.printToLog(L.t("Starting strategies filtering..."));
        progressEngine.start();
```

And the rest is the main functionality – going through strategies in source databank one by one, computing their correlation and copying them to the target databank:

```
        ResultsGroup strategyCloned;
        ArrayList<ResultsGroup> results = databankSource.getRecords();

        this.totalCopied = 0;
        this.totalResults = results.size();

        for(int i=0; i<results.size(); i++) {
            ResultsGroup strategy = results.get(i);
            String strategyName = strategy.getName();
           
            try {
                String dismissalMessage = checkCorrelation(strategy);
                if(dismissalMessage != null) {
                    progressEngine.printToLogDebug(L.t("%s skipped, %s", strategyName, dismissalMessage));
                    increaseGlobalStats(false);
                   
                    continue;
                }
                //copy from source to target
                strategyCloned = strategy.clone();
                databankTarget.add(strategyCloned, true);
               
                progressEngine.printToLogDebug(L.t("%s copied, %s", strategyName, String.format("correlation < %.2f", correlation.maxCorrelation)));
               
                this.totalCopied++;
               
                increaseGlobalStats(true);
               
                progressEngine.checkPaused();
               
                if(progressEngine.isStopped()) {
                    break;
                }
            }
            catch (OutOfMemoryError e) {
                project.onMemoryError(e);
                break;
            }
        }
      
        databankTarget.updateBestResults();
       
        progressEngine.finish();
```

## Compiling the plugins

Both plugins have to be compiled in order for SQX to load them. This can be done simply in **CodeEditor**, just click with right mouse on the plugin folder and choose **Compile plugin** from the menu:

If there are no errors the plugin will be compiled and a new file with plugin name and **.jar** extension will appear in the folder.

Please note that you have to compile both the plugins. Then you have to restart SQX and you should see your new plugin in the Custom project tasks.
