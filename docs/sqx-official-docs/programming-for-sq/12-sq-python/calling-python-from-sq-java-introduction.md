# Calling Python from SQ (Java) – Introduction

- Source: <https://strategyquant.com/doc/programming-for-sq/calling-python-from-sq-java-introduction/>
- Section: Programming for StrategyQuant X › SQ Python
- Fetched: 2026-09-04

---

Python is a very popular language, particularly in the scientific and quant community thanks to its extensive numerical and statistical libraries.  
In this article we’ll show a few ways how Python can be called from StrategyQuant Java Snippet.

## Python script

We’ll use a very simple Python script that will be defined in the file sqpython.py

```
print("Hello from Python!")
```

If you’d call it by Python interpreter it will return an output “Hello from Python!”

```
$ python sqpython.py
Hello from Python!
```

In this example we’ll call this Python script from CustomAnalysis snippet – it will call the script and stores its output to a special value.

To see the Python script output in SQ we’ll create a special databank column that will show this value. This example is a variation of this Custom Analysis example: [Example – per strategy custom analysis](../05-custom-analysis/example-per-strategy-custom-analysis.md).

Following the [Step 6](../05-custom-analysis/example-per-strategy-custom-analysis.md) in the previous example we’ll create a custom databank column that will read and display the string value stored in the strategy result variable (this will be done by the Custom Analysis snippet after calling Python script).

The whole new databank column snippet code is:

```
package SQ.Columns.Databanks;

import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.ValueTypes;

public class TestPythonOutput extends DatabankColumn {

    public TestPythonOutput() {
        super("TestPythonOutput", DatabankColumn.Text, ValueTypes.Maximize, 0, 0, 100);
    }
  
  //------------------------------------------------------------------------
    
    @Override
    public String getValue(ResultsGroup rg, String resultKey, byte direction, byte plType, byte sampleType) throws Exception {		
        String value = rg.specialValues().getString("PythonOutput", NOT_AVAILABLE);
        
        return value;
    }
}
```

If you’ll compile this snippet, restart SQ and add this new column to your databank you should see “N/A” string in this column – it is because we haven’t called any Python script to fill it yet:

[![Databank column Python output NA](https://strategyquant.com/wp-content/uploads/2021/11/databank_column_python_na-750x142.png)](https://strategyquant.com/wp-content/uploads/2021/11/databank_column_python_na-750x142.png)

Our goal with Python is to fill this column with value returned by Python.

## Calling Python scripts from Java using Jython

Probably the easiest way to call Python from SQ is by using **[Jython](https://www.jython.org/)**. Jython is a Java implementation of Python which runs on the JVM, which means that it is a pure Java library without any external dependencies.

You can download it (the standalone version JAR) from <https://www.jython.org/download>

and put it to your ***{SQ installation}\user\libs*** folder.

This is a folder where custom JAR libraries can be placed and are located and loaded by SQ during start.

Once the jython-standalone-2.7.2.jar is there and you’ll restart SQ you’ll be able to use the Jython functionality in SQ.

We’ll now create a new Custom Analysis snippet named ***CAJythonScript*** with the following code:

```
package SQ.CustomAnalysis;

import java.io.FileReader;
import java.io.StringWriter;
import java.util.List;

import javax.script.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strategyquant.lib.app.MainApp;
import com.strategyquant.tradinglib.CustomAnalysisMethod;
import com.strategyquant.tradinglib.Result;
import com.strategyquant.tradinglib.ResultsGroup;

public class CAJythonScript extends CustomAnalysisMethod {
    public static final Logger Log = LoggerFactory.getLogger(CAJythonScript.class);
    
    //------------------------------------------------------------------------
    //------------------------------------------------------------------------
    //------------------------------------------------------------------------
    
    public CAJythonScript() {
        super("CAJythonScript", TYPE_FILTER_STRATEGY);
    }
    
    //------------------------------------------------------------------------
    
    @Override
    public boolean filterStrategy(String project, String task, String databankName, ResultsGroup rg) throws Exception {
        Log.info("Calling Jython Script");

        StringWriter writer = new StringWriter();
        ScriptContext context = new SimpleScriptContext();
        context.setWriter(writer);

        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("jython");
        String pythonScript = MainApp.getDataPath()+"sqpython.py";
        Log.info("Jython script path: "+pythonScript);
        engine.eval(new FileReader(pythonScript), context);

        String result = writer.toString().trim();

        Log.info("Jython script output: "+result);

        rg.specialValues().set("PythonOutput", result);

        return true;
    }
}
```

The code is fairly simple, it uses Java build-in script engine functionality, and we’ll choose “jython” engine by name.

Then we’ll just call ***engine.eval()*** and pass it the path to our Python script file as a parameter.

This snippet executes the Python script, reads its output and saves this output under the “PythonOutput” key in the custom values map in our strategy (ResultGroup).

When you’ll compile, restart SQ and run our new Custom Analysis snippet.

You can run a Custom analysis snippet as a part of Builder/Retester process or as a custom task in a custom project. In our example we’ll simply execute it in Retester:

[![Custom analysis python snippet](https://strategyquant.com/wp-content/uploads/2021/11/customanalysis_python_snippet.png)](https://strategyquant.com/wp-content/uploads/2021/11/customanalysis_python_snippet.png)

Note that the Custom Analysis script is called for every strategy after its retest, so make sure to hit Start to execute the Retest.

You should see that our control databank column TestPythonOutput now contains the text “Hello from Python!” – which is the output of our Python script:

[![Databank column python output](https://strategyquant.com/wp-content/uploads/2021/11/databank_column_python_output-750x143.png)](https://strategyquant.com/wp-content/uploads/2021/11/databank_column_python_output-750x143.png)

This means that the Python script was properly called and it returned the value that is now displayed in our custom databank column.

**Jython advantages:**

- Relatively fast call – no external process has to be called, everything is handled internally in JVM
- Simple to use – just add one .JAR library to SQ

**Disadvantages:**

- Jython is the Python implementation for Java, it may not contain all the same sub-packages as native Python
- It is no longer actively developed, so it is available only for an older version of Python

Jython is suitable for simple scripts, but if you need a full power of Python, with all its supported libraries you must use a different way.

## Calling Python from Java by invoking an external Python installation

As title suggests this method will invoke a native operating system process to launch python and execute our simple script.

This means you must have Python installed on your computer. Python installation is beyond scope of this article, you can follow the documentation on the <https://www.python.org/> website.

You should install Python onto your computer so that you will be able to normally execute scripts by calling

```
$ python myscript.py
```

from the command line.

We’ll create a new Custom Analysis snippet named CAPythonScript with the following code:

```
package SQ.CustomAnalysis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strategyquant.lib.app.MainApp;
import com.strategyquant.tradinglib.CustomAnalysisMethod;
import com.strategyquant.tradinglib.Result;
import com.strategyquant.tradinglib.ResultsGroup;

public class CAPythonScript extends CustomAnalysisMethod {
    public static final Logger Log = LoggerFactory.getLogger(CAPythonScript.class);
    
    //------------------------------------------------------------------------
    //------------------------------------------------------------------------
    //------------------------------------------------------------------------
    
    public CAPythonScript() {
        super("CAPythonScript", TYPE_FILTER_STRATEGY);
    }
    
    //------------------------------------------------------------------------
    
    @Override
    public boolean filterStrategy(String project, String task, String databankName, ResultsGroup rg) throws Exception {
    
        String pythonScript = MainApp.getDataPath()+"sqpython.py";
        
        Log.info("Calling Python Script: "+pythonScript);
        
        ProcessBuilder processBuilder = new ProcessBuilder("py", pythonScript);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        String result = readProcessOutput(process.getInputStream());

        int exitCode = process.waitFor();
        
        Log.info("Python script output: "+result);

        rg.specialValues().set("PythonOutput", result);
        
        return true;
    }

    //------------------------------------------------------------------------

    private String readProcessOutput(InputStream stream) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

        StringBuilder stringBuilder = new StringBuilder();

        try {
        String line;
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
        }

        stream.close();

        } catch (IOException ex) {
        Log.error("Error: ", ex);
        }
        
        return stringBuilder.toString();
    }
}
```

In this snippet the call of external script is made using ProcessBuilder, but again it is simple to follow.

You have to create a ProcessBuilder with give parameters, then execute it by caling the **.start()** method and collect its output.

This will call the externally installed Python, so it can contain any libraries and modules you are able to use in your standard Python.

## Calling Python by HTTP

The previous two examples were about executing Python scripts directly, as you could do it from the command line.

A different way of calling Python from SQ would be using a HTTP protocol as the abstraction layer between the two different languages.

Python by default ships with a simple built-in HTTP server which we can use for sharing content or files over HTTP:

```
python -m http.server 9000
```

If you’ll go to <http://localhost:9000>, you’ll see the contents listed for the directory where we launched the previous command.

You can use popular web frameworks like [Flask](https://palletsprojects.com/p/flask/) or [Django](https://www.djangoproject.com/) to create web endpoints in Python.  
Once you have the web endpoints in Python you can call them from SQ using one of the Java HTTP libraries.

We’ll create a new Custom analysis snippet named CAPythonHttp that calls Python via HTTP:

```
package SQ.CustomAnalysis;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.tradinglib.CustomAnalysisMethod;
import com.strategyquant.tradinglib.ResultsGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleScriptContext;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class CAPythonHttp extends CustomAnalysisMethod {
    public static final Logger Log = LoggerFactory.getLogger(CAPythonHttp.class);

    //------------------------------------------------------------------------
    //------------------------------------------------------------------------
    //------------------------------------------------------------------------

    public CAPythonHttp() {
        super("CAJythonScript", TYPE_FILTER_STRATEGY);
    }
    
    //------------------------------------------------------------------------
    
    @Override
    public boolean filterStrategy(String project, String task, String databankName, ResultsGroup rg) throws Exception {
        HttpURLConnection connection = null;

        try {
            Log.info("Calling Python via HTTP");

            URL url = new URL("http://localhost:9000");
            connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "text/html");
            connection.setRequestProperty("Content-Language", "en-US");

            connection.setUseCaches(false);
            connection.setDoOutput(true);

            // get response
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            StringBuilder response = new StringBuilder(); // or StringBuffer if not Java 5+
            String line;
            while((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            rd.close();

            String result = response.toString();

            // remove HTML tags from response
            result = result.replaceAll("\\<.*?\\>", "");

            Log.info("PythonHTTP script output: "+result);

            // we'll store only first 10 characters of the result
            rg.specialValues().set("PythonOutput", result.substring(0, 10));

        } catch (Exception e) {
            throw new Exception("Request error: GET, " + e.getMessage());
        } finally {
            if(connection != null) {
                connection.disconnect();
            }
        }

        return true;
    }
}
```

Again, it should be easy to follow. Script will create HTTP request to a given URL and reads the response.

In this example the response would be a long HTML document containing directory listing, so we’ll remove HTML tags and shorten it to 10 characters.

You should see the output as:

[![Databank column Python http](https://strategyquant.com/wp-content/uploads/2021/11/databank_column_python_http.png)](https://strategyquant.com/wp-content/uploads/2021/11/databank_column_python_http.png)

## Passing parameters

This is where things get interesting and more complicated. Calling Python scripts from SQ without passing any parameters to them has only very limited use.

You normally want to pass some (or a lot) of parameters from SQ to Python – strategy name, list of trades, test results, etc. so that you can work with them in Python and possibly return something back.

The way of passing these parameters and getting results back from Python depends on the Python invocation method you use.

**In case of HTTP call the situation is simple.**

You can pass even a very big amount of data between SQ and Python using standard HTTP request/response.

You can send data to your Python web endpoint as standard GET parameters or POST data- You can return data to SQ using HTTP response

**In case of external Python script call (using Jython or Python invocation)**

If the passed data is small (for example only strategy name) you can pass it as arguments of the script.

When sending bigger data one possibility is to save the data to a temporary file in any format you want in SQ, and then load the data from this temporary file in Python. You can use the same way to send bigger data from Python to SQ.
