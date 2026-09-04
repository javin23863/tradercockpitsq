# Export strategy from StrategyQuant and test or trade it in MetaTrader

- Source: <https://strategyquant.com/doc/strategyquant/export-strategy-strategyquant-test-trade-metatrader/>
- Section: StrategyQuant X › How to...
- Fetched: 2026-09-04

---

When you generate some strategies and find the ones you would potentially like to use in real trading, it is time to test them in MetaTrader.

StrategyQuant normally saves strategies in its own proprietary .str file format, which is not readable by MetaTrader.  
In order to test strategies in MT4 you have to export its source code in MQL format

This is simple, go to the databank and find the strategy you want to use. Double-click on it, which opens it in the Result details window above the Databank.

[![](https://strategyquant.com/wp-content/uploads/2018/12/test1.png)](https://strategyquant.com/wp-content/uploads/2018/12/test1.png)

There, go to Source code tab and switch the source code to MetaTrader4 Expert Advisor. This will load the MT4 code of the strategy.

[![](https://strategyquant.com/wp-content/uploads/2018/12/test2.png)](https://strategyquant.com/wp-content/uploads/2018/12/test2.png)

Click on Save to file button and save EA of the strategy.

Now start MetaTrader, go to the Main Menu -> File -> Open data folder and finally select MQL4/Experts folder. Here you can copy exported strategy

So the full path of the file will be for example  
*C:\Users\John\AppData\Roaming\MetaQuotes\Terminal\2E8DC23981084565FA3E19C061F586B2\MQL4\Experts*

Now the strategy is copied to Metatrader. You can open MetaTrader now.

In Metatrader go to menu **Tools -> MetaQuotes Language Editor**, or press **F4**. This will open the language editor.

On the right side of the editor you’ll have a list of strategies that are in the **experts** folder. Double-click on our strategy to open it in the editor window and then click on **Compile** on the top toolbar.

[![](https://strategyquant.com/wp-content/uploads/2018/12/test3.png)](https://strategyquant.com/wp-content/uploads/2018/12/test3.png)

The strategy will be compiled and now it is ready for backtest or running live.

> **Note – Compilation warnings are normal**  
> *Please note that there are some compilation warnings on the bottom. These warnings are normal and they don’t influence the strategy work.*  
>  *There are simply some functions that are not used in the strategy and MetaTrader is informing you about that.*

Now that the strategy is compiled , it is ready to be backtested. You can close the **MetaEditor**, go to the main MetaTrader screen and open **Strategy Tester**.

[![](https://strategyquant.com/wp-content/uploads/2018/12/test4.png)](https://strategyquant.com/wp-content/uploads/2018/12/test4.png)

This will open the **Strategy Tester** dialog on the bottom and you can run the backtest.

[![](https://strategyquant.com/wp-content/uploads/2018/12/test5.png)](https://strategyquant.com/wp-content/uploads/2018/12/test5.png)

Make sure you select the correct Expert Advisor, Symbol, Timeframe and Date From and To and then click on the **Start** button. The test will start and after a while you’ll get the results:

[![](https://strategyquant.com/wp-content/uploads/2018/12/test6.png)](https://strategyquant.com/wp-content/uploads/2018/12/test6.png)

> **Explanation of small differences in backtests**  
> *If you’ll compare test results in StrategyQuant and in MetaTrader, you’ll see that on some cases the backtesting results are not the same.*  
>  *The results can differ slightly or significantly – depending on the type of strategy.*
>
> *Backtesting algorithm used in StrategyQuant is very accurate, but it is not exactly the same algorithm used in MetaTrader, so it produces slightly different result.*  
>  *The important thing here is to understand that both testing algorithms are only approximations, one isn’t superior to the other.*
