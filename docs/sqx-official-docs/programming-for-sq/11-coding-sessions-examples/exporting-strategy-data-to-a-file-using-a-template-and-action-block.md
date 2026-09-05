# Exporting Strategy Data to a File Using a Template and Action Block

- Source: <https://strategyquant.com/doc/programming-for-sq/exporting-strategy-data-to-a-file-using-a-template-and-action-block/>
- Section: Programming for StrategyQuant X › Coding sessions examples
- Fetched: 2026-09-04

---

Using the Code Editor, we can create and develop snippets. In this example, we’ll explore how to export data from a strategy using an action block.

This **Export Data** block helps us better understand the behavior of a strategy and is especially useful when debugging complex templates that involve multiple rules.

In **AlgoWizard**, we can build a template that runs in two modes:

- **Default mode**: Used during strategy building. No data is exported.
- **Retester mode**: Enables exporting data to a file.

## I. Creating a Template

### 1. Installing the ExportData Snippet

- Open the **Code Editor**
- Import the file **`SQExtensionExportData2.sxp`**
- Restart **SQX** to apply the changes

### 2. Creating a New Template

- Create a new template
- Add some basic/random conditions to the logic

[![](https://strategyquant.com/wp-content/uploads/2025/04/image1-1.png)](https://strategyquant.com/wp-content/uploads/2025/04/image1-1.png)

### 3. Adding an “Export Data” Rule to Export Indicator Values

Create a rule named **"Export Data"** that handles the data export.

[![](https://strategyquant.com/wp-content/uploads/2025/04/image2-2.png)](https://strategyquant.com/wp-content/uploads/2025/04/image2-2.png)

In this example, we’ll export indicator values at each new entry using the "Export Data" rule.

This rule collects values specified in the **Export Data** block and writes them to a file.

To do this:

- Set the file path where the data will be exported
- Define the variables to export (if a value is set to `999999, it will be ignored during export)`

[![](https://strategyquant.com/wp-content/uploads/2025/04/image3.png)](https://strategyquant.com/wp-content/uploads/2025/04/image3.png)

Within a template, you can export values on trade entry, during a trade, or at exit. This allows for detailed observation of strategy behavior.

You may also pre-process values (e.g., calculate averages) before exporting them.

You can use multiple **ExportData** blocks across different rules to export to multiple files as needed.

### 4. Adding One Export Variable

Create a boolean variable named **ExportData**. This variable will be used to activate the export rule when running in the Retester.

## II. Building from the Template and Exporting Data

### 1. Generate Strategies Using the Builder

- Once the attached template is ready, run it in the **Builder** to generate strategies.
- Select the generated strategies and send them to the **Retester**.

[![](https://strategyquant.com/wp-content/uploads/2025/04/capture1-750x364.jpg)](https://strategyquant.com/wp-content/uploads/2025/04/capture1-750x364.jpg)

[![](https://strategyquant.com/wp-content/uploads/2025/04/image4.png)](https://strategyquant.com/wp-content/uploads/2025/04/image4.png)

### 2. Enabling Data Export in the Retester

- In the Retester, select a strategy
- Edit the **Export Data** parameter and set it to **True**
- Apply the configuration and run the Retester

With export mode enabled, the strategy will write data to the defined file.

[![](https://strategyquant.com/wp-content/uploads/2025/04/image5.png)](https://strategyquant.com/wp-content/uploads/2025/04/image5.png)

[![](https://strategyquant.com/wp-content/uploads/2025/04/image6.png)](https://strategyquant.com/wp-content/uploads/2025/04/image6.png)

## III. Full Code of the Snippet

```
package SQ.Blocks.OtherActions;

import SQ.Internal.ActionBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@BuildingBlock(name="(EXP) ExportData", display="ExportData", returnType = ReturnTypes.Action) 
@Help("ExportData values to a file")
@SortOrder(100)
@CategoryOrder(400)
@IgnoreInBuilder
public class ExportData extends ActionBlock 
{

    @Parameter
    public ChartData Chart;
    
    @Parameter
    public int Shift;

    @Parameter(defaultValue="C:\\Temp\\output.txt", category="Path" )
    @Help("Path of the file")
    public String Path1;

    @Parameter(category="Value", name="Value1", minValue=-999999999, maxValue=999999999, defaultValue="999999", step=0.1)
    public double  Value1;

    @Parameter(category="Value", name="Value2", minValue=-999999999, maxValue=999999999, defaultValue="999999", step=0.1)
    public double  Value2;

    @Parameter(category="Value", name="Value3", minValue=-999999999, maxValue=999999999, defaultValue="999999", step=0.1)
    public double  Value3;

    @Parameter(category="Value", name="Value4", minValue=-999999999, maxValue=999999999, defaultValue="999999", step=0.1)
    public double  Value4;

    @Parameter(category="Value", name="Value5", minValue=-999999999, maxValue=999999999, defaultValue="999999", step=0.1)
    public double  Value5;
    
    //------------------------------------------------------------------------
    //------------------------------------------------------------------------
    //------------------------------------------------------------------------

    @Override
    public void OnAction() throws TradingException 
    {
        long time;
        time = Chart.Time(Shift);
        Integer date1 = (SQTime.getYear(time)*10000 + SQTime.getMonthOriginal(time)*100 + SQTime.getDay(time));
        Integer time1 =  (SQTime.getHour(time)*100 + SQTime.getMinute(time));
        double open1 = Chart.Open(Shift);
        double high1 = Chart.High(Shift);
        double low1 = Chart.Low(Shift);
        double close1 = Chart.Close(Shift);
        double Volume1 = Chart.Volume(Shift);
        String newline = System.getProperty("line.separator");

        String filePath = Path1;        

        if (Value1 == 999999)
        {
            writeToFile(filePath, Integer.toString(date1) + "," + Integer.toString(time1) + "," + Double.toString(open1) + "," + Double.toString(high1) + "," + Double.toString(low1) + "," + Double.toString(close1) + "," + Double.toString(Volume1));    // + newline
        }
        else if (Value1 != 999999 && Value2 == 999999)
        {
            writeToFile(filePath, Integer.toString(date1) + "," + Integer.toString(time1) + "," + Double.toString(open1) + "," + Double.toString(high1) + "," + Double.toString(low1) + "," + Double.toString(close1) + "," + Double.toString(Volume1) + "," + Double.toString(Value1));
        }
        else if (Value2 != 999999 && Value3 == 999999)
        {
            writeToFile(filePath, Integer.toString(date1) + "," + Integer.toString(time1) + "," + Double.toString(open1) + "," + Double.toString(high1) + "," + Double.toString(low1) + "," + Double.toString(close1) + "," + Double.toString(Volume1) + "," + Double.toString(Value1) + "," + Double.toString(Value2));
        }
        else if (Value3 != 999999 && Value4 == 999999)
        {
            writeToFile(filePath, Integer.toString(date1) + "," + Integer.toString(time1) + "," + Double.toString(open1) + "," + Double.toString(high1) + "," + Double.toString(low1) + "," + Double.toString(close1) + "," + Double.toString(Volume1) + "," + Double.toString(Value1) + "," + Double.toString(Value2) + "," + Double.toString(Value3));
        }	
        else if (Value4 != 999999 && Value5 == 999999)
        {
            writeToFile(filePath, Integer.toString(date1) + "," + Integer.toString(time1) + "," + Double.toString(open1) + "," + Double.toString(high1) + "," + Double.toString(low1) + "," + Double.toString(close1) + "," + Double.toString(Volume1) + "," + Double.toString(Value1) + "," + Double.toString(Value2) + "," + Double.toString(Value3)+ "," + Double.toString(Value4));
        }
        else if (Value4 != 999999 && Value5 != 999999)
        {
            writeToFile(filePath, Integer.toString(date1) + "," + Integer.toString(time1) + "," + Double.toString(open1) + "," + Double.toString(high1) + "," + Double.toString(low1) + "," + Double.toString(close1) + "," + Double.toString(Volume1)  + "," + Double.toString(Value1) + "," + Double.toString(Value2) + "," + Double.toString(Value3) + "," + Double.toString(Value4)+ "," + Double.toString(Value5));
        }
    }
    public static void writeToFile(String path, String content) 
    {
        try (FileWriter writer = new FileWriter(path, true)) 
        {
            writer.write(content + "\n");
           // System.out.println("Données écrites avec succès dans le fichier " + path);
        } catch (IOException e) {
           // System.out.println("Une erreur s'est produite lors de l'écriture du fichier.");
            //e.printStackTrace();
        }
    }
}
```

### See the video step by step:
