# Changing task config programmatically

- Source: <https://strategyquant.com/doc/programming-for-sq/changing-task-config-programmatically/>
- Section: Programming for StrategyQuant X › Coding sessions examples
- Fetched: 2026-09-04

---

**Important note – this example will work only in Build 136 or later**

In this article we’ll show how you can modify the configuration of tasks in your custom project programmatically, using Custom Analysis snippet.

The idea is that you create some loop in custom project workflow, and you’ll have a CA snippet as one of the tasks, which changes settings of another build/retest/optimization tasks(s) in the project with every loop.

This way you can for example retest or reoptimize the strategies on moving range of data.

We’ll again implement this as CustomAnalysis snippet, the implementation will be in ***processDatabank()*** method.

## How it looks in the UI

See the screenshot of a simple project below – it consists of just two tasks:

1. CustomAnalysis task – it runs our new CA snippet that changes date range of the next ‘Build strategies’ task
2. Build strategies task – this is the task whose settings are changed by our CA snippet

[![](https://strategyquant.com/wp-content/uploads/2022/08/ca_change_task-750x145.jpg)](https://strategyquant.com/wp-content/uploads/2022/08/ca_change_task-750x145.jpg)

This is only a primitive example – a demonstration of what can be done.

This sample custom project is attached to this article.

Now we’ll have a look at our Ca snippet that does it.

## Getting project and task(s)

This is how you get your current project and the task(s) you want:

```
public ArrayList<ResultsGroup> processDatabank(String project, String task, String databankName, ArrayList<ResultsGroup> databankRG) throws Exception {

    // get current project
    SQProject sqProject = ProjectEngine.get(project);

    // get task by name
    ISQTask buildTask = sqProject.getTaskByName("Build strategies");
    if(buildTask == null) {
        throw new Exception("No such task exists!");
    }
    // alternatively you can use sqProject.getTasks() to get a list of all tasks
    // and go through them to get the one you want
```

## Getting config from task

Getting current configuration of this task is again simple – yo just have to call ***getConfig()*** method of the task:

```
// this is how you get task settings XML in the new way (SQ 136 up)
// It returns JDOM XML Element with full task configuration
Element elConfig = buildTask.getConfig();

// create a new config by cloning it - it is important to make a clone,
// otherwise your new config will be not applied
Element elNewConfig = elConfig.clone();
```

Note that ***getConfig()*** will return JDOM Element that contains XML of the task configuration. We’ll have to clone it to a new element in order to modify it.

The task XML structure is beyond the scope of this article, but you can easily see the XML configuration of every task when you save it to **.cfx** format, or you can have a look at ***/user/projects/Your\_Project/project.cfx***

.cfx file is a ZIP archive, and can be normally opened using **WinZip** or **WinRar**.

When you open it you’ll see it contains separate XML file for every task in the project.

[![](https://strategyquant.com/wp-content/uploads/2022/08/cfx_zip-750x248.jpg)](https://strategyquant.com/wp-content/uploads/2022/08/cfx_zip-750x248.jpg)

You can **View** it to see the exact structure of the task XML.

## Changing something in the task config

It depends on what you want, you can modify anything in the XML – as long as it is still valid XML structure for a given task.

In our example we’ll get the first <Setup> element – which is where the symbol settings for main backtest are stored – and modify its from – to dates.

```
// now change anything in the new config
Element data = XMLUtil.getChildElem(elNewConfig, "Data");
Element setups = data.getChild("Setups");
List setupList = setups.getChildren("Setup");

if (setupList.size() > 0) {
    Element firstSetup = (Element) setupList.get(0);

    DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy.MM.dd");

    firstSetup.setAttribute("dateFrom", SQTime.formatDate(SQTime.toLong(2007, 1, 1), formatter));
    firstSetup.setAttribute("dateTo",SQTime.formatDate(SQTime.toLong(2017, 1, 1), formatter));
}
```

## Alternative of changing config by loading it from prepared file

There is another way of applying a different config to a task – you can first prepare it as a file, and then just load it to Element in our snippet:

```
// alternatively you can load the new task config from prepared file
Element elNewConfig = XMLUtil.fileToXmlElement(new File("c:/BuildTask2009_2019.xml"));
```

## Applying the config to a task

he last step is applying the config to a task, it is done by simply calling ***setConfig()*** method

```
// Apply the modified config to the task
buildTask.setConfig(elNewConfig, true);
```

This will apply the modified config to a given task, which will then use it.

It will all work, the only issue is that this change is done only on background, you will not see your change in the UI.

If you want to see it also in the UI, you have to do one remaining step to request project config refresh:

```
// this notifies UI to load che changed config and apply it to the UI.
// Without this it would still work but you'll not see the changes in the UI
JSONObject jsonData = new JSONObject().put("projectConfig",  sqProject.toJSON());
DataToSend dataToSend = new DataToSend(WebSocketConst.UpdateProject, jsonData);
SQWebSocketManager.addToDataQueue(dataToSend, SQConst.CODE_TASKMANAGER);
```

Full code of the CAChangeTaskConfig snippet:

```
package SQ.CustomAnalysis;

import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.constants.SQConst;
import com.strategyquant.tradinglib.CustomAnalysisMethod;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.project.ProjectEngine;
import com.strategyquant.tradinglib.project.SQProject;
import com.strategyquant.tradinglib.project.websocket.DataToSend;
import com.strategyquant.tradinglib.project.websocket.SQWebSocketManager;
import com.strategyquant.tradinglib.project.websocket.WebSocketConst;
import com.strategyquant.tradinglib.taskImpl.ISQTask;
import org.jdom2.Element;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CAChangeTaskConfig extends CustomAnalysisMethod {

    //------------------------------------------------------------------------
    //------------------------------------------------------------------------
    //------------------------------------------------------------------------

    public CAChangeTaskConfig() {
        super("CAChangeTaskConfig", TYPE_PROCESS_DATABANK);
    }

    //------------------------------------------------------------------------

    @Override
    public ArrayList<ResultsGroup> processDatabank(String project, String task, String databankName, ArrayList<ResultsGroup> databankRG) throws Exception {

        // get current project
        SQProject sqProject = ProjectEngine.get(project);

        // get task by name
        ISQTask buildTask = sqProject.getTaskByName("Build strategies");
        if(buildTask == null) {
            throw new Exception("No such task exists!");
        }
        // alternatively you can use sqProject.getTasks() to get a list of all tasks
        // and go through them to get the one you want

        // this is how you get task settings XML in the new way (SQ 136 up)
        // It returns JDOM XML Element with full task configuration
        Element elConfig = buildTask.getConfig();
        // create a new config by cloning it - it is important to make a clone,
        // otherwise your new config will be not applied
        Element elNewConfig = elConfig.clone();

        // now change anything in the new config
        Element data = XMLUtil.getChildElem(elNewConfig, "Data");
        Element setups = data.getChild("Setups");
        List setupList = setups.getChildren("Setup");

        if (setupList.size() > 0) {
            Element firstSetup = (Element) setupList.get(0);

            DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy.MM.dd");

            firstSetup.setAttribute("dateFrom", SQTime.formatDate(SQTime.toLong(2007, 1, 1), formatter));
            firstSetup.setAttribute("dateTo",SQTime.formatDate(SQTime.toLong(2017, 1, 1), formatter));
        }

        // alternatively you can load the new task config from prepared file
        //Element elNewConfig = XMLUtil.fileToXmlElement(new File("c:/BuildTask2009_2019.xml"));

        // Apply the modified config to the task
        buildTask.setConfig(elNewConfig, true);

        // this notifies UI to load che changed config and apply it to the UI.
        // Without this it would still work but you'll not see the changes in the UI
        JSONObject jsonData = new JSONObject().put("projectConfig",  sqProject.toJSON());
        DataToSend dataToSend = new DataToSend(WebSocketConst.UpdateProject, jsonData);
        SQWebSocketManager.addToDataQueue(dataToSend, SQConst.CODE_TASKMANAGER);

        return databankRG;
    }
}
```
