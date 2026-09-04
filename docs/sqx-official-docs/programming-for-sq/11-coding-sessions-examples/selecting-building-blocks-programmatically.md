# Selecting building blocks programmatically

- Source: <https://strategyquant.com/doc/programming-for-sq/selecting-building-blocks-programmatically/>
- Section: Programming for StrategyQuant X › Coding sessions examples
- Fetched: 2026-09-04

---

This is an extension of this article: <https://strategyquant.com/doc/programming-for-sq/changing-task-config-programmatically/>  
which demonstrated how you can change task configuration programmatically.

The general idea of the initial example is that you can change task configuration by manipulating its XML configuration and reapplying it to the task.  
In this example we’ll demonstrate how you can enable/disable building blocks this way.

## How building blocks are stored in the task XML

All building blocks are stored as children and sub-children in element **<Blocks> -> <BuildingBlocks>**.

Each building block is configured in its own <Block> element, uniquely identified by its **key** – which is its snippet name.

There are a few standards in key values:

- if it is **signal block** or **comparison**, the key contains only the name of the snippet, for example **key=”ADXChangesDown”** or **key=”IsGreater”**
- if it is **indicator** (not signal), its key is prefixed by string “Indicator.”, for example **key=”Indicators.ADX”**
- if it is **price**, its key is prefixed by string “Prices.”, for example **key=”Prices.High”**
- if it is **stop/limit level** (for Stop or Limit orders), its key is prefixed by string “Stop/Limit Price Levels.”, for example **key=”Stop/Limit Price Levels.TEMA”**
- if it is **stop/limit range** (for Stop or Limit orders), its key is prefixed by string “Stop/Limit Price Ranges.”, for example **key=”Stop/Limit Price Ranges.BBWidthRatio”**

Another important attribute of <Block> element is **use**. If you want block to be used during build you should set it to “true”, otherwise to “false”.

So if you want to enable or disable particular block you only need to:

1. find its <Block> element
2. set use attribute to true or false like this: <Block … use=”true”>…

There is an example of Builder task XML attached in this article – file Build-Task1.xml

## Example

As an example of this functionality we’ll be programmatically switching between Keltner Channel (KC) and Bollinger Bands (BB) signals blocks, all the other blocks will be disabled.

We’ll have a custom project with two tasks – first one will be Custom Analysis task that will apply our Custom Analysis snippet. The second is Builder task – we’ll be modifying the configuraton of this Builder task in our CA snipet.

[![](https://strategyquant.com/wp-content/uploads/2023/01/cp1.png)](https://strategyquant.com/wp-content/uploads/2023/01/cp1.png)

So when you run the custom project for the first time, only Bollinger Bands signals blocks will be enabled. When you run it second time only Keltner Channel blocks are enabled, and so on.

All the Bollinger Bands signal blocks have snippets named wit BB prefix – BB\*\*\*\*\*\*\*\*\*, for example key=”BBBarClosesAboveDown”, and all Keltner Channel signal blocks have snippets named KC\*\*\*\*\*\*\*\*\*, for example key=”KCBarClosesAboveLower”.  
We’ll use this BB or KC prefix to recognize hat it is either Keltner Channel or Bollinger Bands signal block.

The example below is essentially the same as in the article <https://strategyquant.com/doc/programming-for-sq/changing-task-config-programmatically/> – only here we do a different things with the XML.

We use standard **[JDOM](http://www.jdom.org)** library functionality to go through the XML and get or modify its attributes.

### Step 1 – Getting all block elements

```
Element elBlocks = XMLUtil.getChildElem(elNewConfig, "Blocks");
if(elBlocks == null) {
    throw new Exception("No <Blocks> element exists, is it Builder task?");
}
Element buildingBlocks = XMLUtil.getChildElem(elBlocks, "BuildingBlocks");
// alternatively you can load the new task config from prepared file
//Element elNewConfig = XMLUtil.fileToXmlElement(new File("c:/BuildTask2009_2019.xml"));

List<Element> blocks = buildingBlocks.getChildren("Block");
```

### Step 2 – Initialize activeBlock variable

we’ll use this variable to determine if BB or KC blocks should be used – remember, we are switching between BB and KC blocks, so we must determine which of those two should be turned on and off.

```
// we'll use this variable to switch from BB to KC. The values are:
// 0 - not known, will be set by first found block
// 1 - BB blocks should be active
// 2 - KC blocks should be active
int activeBlock = 0;
```

### Step 3 – Going through all blocks in a loop

```
for(int i=0; i<blocks.size(); i++) {
    Element elBlock = blocks.get(i);

    String key = elBlock.getAttributeValue("key");
```

### Step 4 – If key doesn’t start with BB or KC it is different block and should be turned off

```
if(!key.startsWith("BB") && !key.startsWith("KC")) {
    // it is not Bollinger Bands nor Keltner Channel block, turn it off
    elBlock.setAttribute("use", "false");
    continue;
}
```

### Step 5 – Otherwise it is KC or BB block

Now recognize the proper value of activeBlock variable:

```
// it is either BB or KC block
String use = elBlock.getAttributeValue("use");

if(activeBlock == 0) {
    // we'll determine if BB or KC blocks should be active by first block found
    if(key.startsWith("BB")) {
        if (use.equals("true")) {
            // BB was active before, now KC will be active
            activeBlock = 2;
        }
    } else if(key.startsWith("KC")) {
        if(use.equals("true")) {
            // KC was active before, now BB will be active
            activeBlock = 1;
        }
    }

    if(activeBlock == 0) {
        activeBlock = 1;
    }

    Log.info("----- Turning on "+(activeBlock == 1 ? "Bollinger Bands" : "Keltner Channel")+" blocks.");
}
```

### Step 6 – the main thing

turn either BB or KC blocks ON according to the **activeBlock** variable by setting their use attribute to true or false:

```
// now turn the block on or off according to activeBlock value
if(key.startsWith("BB")) {
    elBlock.setAttribute("use", (activeBlock == 1 ? "true" : "false"));
} else if(key.startsWith("KC")) {
    elBlock.setAttribute("use", (activeBlock == 2 ? "true" : "false"));
}
```

### Step 7 – apply the modified config to the task

We are done going through all building blocks, now just apply the modified config to the task:

```
// Apply the modified config to the task
buildTask.setConfig(elNewConfig, true);

// this notifies UI to load che changed config and apply it to the UI.
// Without this it would still work but you'll not see the changes in the UI
JSONObject jsonData = new JSONObject().put("projectConfig",  sqProject.toJSON());
DataToSend dataToSend = new DataToSend(WebSocketConst.UpdateProject, jsonData);
SQWebSocketManager.addToDataQueue(dataToSend, SQConst.CODE_TASKMANAGER);
```

**And this is all.**

Now whenever you start the custom project it will switch the building blocks between Bollinger Bands and Keltner Channel signals.

## Full code of the CAChangeBlocksConfig snippet:

```
package SQ.CustomAnalysis;

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
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class CAChangeBlocksConfig extends CustomAnalysisMethod {

    //------------------------------------------------------------------------
    //------------------------------------------------------------------------
    //------------------------------------------------------------------------

    public CAChangeBlocksConfig() {
        super("CAChangeBlocksConfig", TYPE_PROCESS_DATABANK);
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
        Element elBlocks = XMLUtil.getChildElem(elNewConfig, "Blocks");
        if(elBlocks == null) {
            throw new Exception("No <Blocks> element exists, is it Builder task?");
        }
        Element buildingBlocks = XMLUtil.getChildElem(elBlocks, "BuildingBlocks");
        // alternatively you can load the new task config from prepared file
        //Element elNewConfig = XMLUtil.fileToXmlElement(new File("c:/BuildTask2009_2019.xml"));

        List<Element> blocks = buildingBlocks.getChildren("Block");

        // we'll use this variable to switch from BB to KC. The values are:
        // 0 - not known, will be set by first found block
        // 1 - BB blocks should be active
        // 2 - KC blocks should be active
        int activeBlock = 0;

        for(int i=0; i<blocks.size(); i++) {
            Element elBlock = blocks.get(i);

            String key = elBlock.getAttributeValue("key");

            if(!key.startsWith("BB") && !key.startsWith("KC")) {
                // it is not Bollinger Bands nor Keltner Channel block, turn it off
                elBlock.setAttribute("use", "false");
                continue;
            }

            // it is either BB or KC block
            String use = elBlock.getAttributeValue("use");

            if(activeBlock == 0) {
                // we'll determine if BB or KC blocks should be active by first block found
                if(key.startsWith("BB")) {
                    if (use.equals("true")) {
                        // BB was active before, now KC will be active
                        activeBlock = 2;
                    }
                } else if(key.startsWith("KC")) {
                    if(use.equals("true")) {
                        // KC was active before, now BB will be active
                        activeBlock = 1;
                    }
                }

                if(activeBlock == 0) {
                    activeBlock = 1;
                }

                Log.info("----- Turning on "+(activeBlock == 1 ? "Bollinger Bands" : "Keltner Channel")+" blocks.");
            }

            // now turn the block on or off according to activeBlock value
            if(key.startsWith("BB")) {
                elBlock.setAttribute("use", (activeBlock == 1 ? "true" : "false"));
            } else if(key.startsWith("KC")) {
                elBlock.setAttribute("use", (activeBlock == 2 ? "true" : "false"));
            }
        }

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
