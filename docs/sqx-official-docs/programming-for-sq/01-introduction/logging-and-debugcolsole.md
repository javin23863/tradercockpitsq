# Logging and DebugConsole

- Source: <https://strategyquant.com/doc/programming-for-sq/logging-and-debugcolsole/>
- Section: Programming for StrategyQuant X › Introduction
- Fetched: 2026-09-04

---

DebugConsole in StrategyQuant X is intended to allow easier debugging/logging when you develop your own snippets in SQ CodeEditor.

It is simple to use and allows you to see what’s going on “behind the scene” when your snippet is executed.

## How to use it in code

There are two methods available:

**debug**(category, message)  
logs the message to a debug console (more about this later)

and

**fdebug**(category, message)  
logs the message to a standard log file located in /user/log/StrategyQuant

Both methods have the same two parameters:

- **category** (string)– name of logging category – when you have multiple debug messages you can categorize them to be able to filetr messages by category
- **message** (string) – an actual text message to be logged

Note – functions debug() and fdebug() should be available in all the snippet classes in builds from 128 up. In Build 128 these methods are missing in indicator classes, so you can use alternative calls:  
debug() -> DebugConsole.log()  
log() -> Log.info()

### Example of use

We will add a logging to a CCI indicator, to its **OnBarUpdate()** method, where the indicator values are computed:

```
@Override
protected void OnBarUpdate() throws TradingException {
    averageCalculator.onBarUpdate(Input.get(0), CurrentBar);
    
    if (CurrentBar == 0) {
        Value.set(0, 0);
    } 
    else {
        double mean = 0;
        double sma = averageCalculator.getValue();
        
        for (int idx = Math.min(CurrentBar, Period - 1); idx >= 0; idx--) {
            mean += Math.abs(Input.get(idx) - sma);
        }
        
        if(mean < 0.0000000001) {
            Value.set(0, 0);
        } else {
            double cci = (Input.get(0) - sma) / (mean == 0 ? 1 : (0.015 * (mean / Math.min(Period, CurrentBar + 1))));
            Value.set(0, cci);

            debug("CCI", "CCI value is: "+cci);
        }
    }
}
```

Note that we added only a line:

```
debug("CCI", "CCI value is: "+cci);
```

We used **debug()** method, so the logged messages were added to a special debug console which is available on the top right under debug icon:

[![DebugConsole icon](https://strategyquant.com/wp-content/uploads/2020/06/debug_console_icon.png)](https://strategyquant.com/wp-content/uploads/2020/06/debug_console_icon.png)

When you open it after running a test of some strategy containing CCI indicator you’ll see the messages were logged:

[![DebugConsole displayed](https://strategyquant.com/wp-content/uploads/2020/06/debug_console_displayed-1024x784.png)](https://strategyquant.com/wp-content/uploads/2020/06/debug_console_displayed-1024x784.png)
