# ATR Volatility Simple Sizing

- Source: <https://strategyquant.com/doc/programming-for-sq/atr-volatility-simple-sizing/>
- Section: Programming for StrategyQuant X › Position Sizing
- Fetched: 2026-09-04

---

In situations where short-term volatility is rising or falling, it may make sense to change the position size. In the following tutorial we will show you a simple snippet in StrategyQuant X x that allows you to increase/decrease the size of a trade.  Snippet you can download [here.](https://strategyquant.com/codebase/atr-volatility-money-mannagement-sizing/)

## Step 1 – Create new Money Management snippet

Open **CodeEditor**, click on **Create new** and choose **Money Management**  option at the very bottom. Name it ***ATRVolatilitySizing***.

This will create a new snippet ***ATRVolatilitySizing******.java*** in folder ***User/Snippets/SQ/MoneyManagement***

[![](https://strategyquant.com/wp-content/uploads/2021/12/1-2-750x402.jpg)](https://strategyquant.com/wp-content/uploads/2021/12/1-2-750x402.jpg)

## Step 2 – Define Parameters

At this point, we define the parameters that we will use later when we implement the snippet. In this case, we will define the following

- Size – the default value for trade size.
- Multiplier – Multiplier that we will use to change the trade size.
- FastATR- Fast ATR period
- SlowATR- Slow ATR period

```
@ClassConfig(name="ATR Volatility Sizing", display="ATR Volatility Sizing: #Size# lots with multiplication #Multiplier#")
@Help("<b>ATR Volatility Sizing</b>")
@Description("ATR Volatility Sizing, #Size# and #Multiplier#")
@SortOrder(100)
public class ATRVolatilitySizing extends MoneyManagementMethod {

    @Parameter(defaultValue="0.1", minValue=0.01d, name="Order size", maxValue=1000000d, step=0.1d, category="Default")
    @Help("Order size (number of lots for forex)")
    public double Size;

    @Parameter(defaultValue="0.1", minValue=0.01d, name="Multiplier", maxValue=1000, step=0.1d, category="Default")
    @Help("Multiplier")
    public double Multiplier;

    @Parameter(defaultValue="5", minValue=2, name="Fast ATR Period", maxValue=480, step=1, category="Default")
    @Help("Fast ATR Period")
    public int FastATR;

    @Parameter(defaultValue="20", minValue=2, name="Slow ATR Period", maxValue=480, step=1, category="Default")
    @Help("Slow ATR Period")
    public int SlowATR;
```

## Step 3 – Implementation of the double computeTradeSize method

In this method, you specify the lot size. The parameters of the method are worth mentioning.

- StrategyBase strategy is the base class for the strategy, from it there are derived classes that implement additional functions  
  String symbol – the symbol with which the strategy was ordered
- byte orderType
- double price – the price at the opening of the trade
- double sl – stop loss
- double tickSize tick size
- double pointValue – value of the point.

With this method we can perform simpler, but also more complex calculations. In our case, we set the previous closing price and two different ATR values. If the short-term ATR is higher than the long-term ATR, we change the position size.

```
@Override
public double computeTradeSize(StrategyBase strategy, String symbol, byte orderType, double price, double sl, double tickSize, double pointValue) throws Exception {
    
    double tradeSize;
    // get previous close
    double prevClose = strategy.MarketData.Chart(symbol).Close(1); 
    // get rounded ATR with slower period
    double slowValueATR = SQUtils.round(strategy.getATRValue(strategy.MarketData.Chart(symbol),SlowATR, 1), 5);
    // get rounded ATR with faster period
    double fastValueATR = SQUtils.round(strategy.getATRValue(strategy.MarketData.Chart(symbol),FastATR, 1), 5);
    // if short volatility spikes UP then modify size of trade
    if(fastValueATR>slowValueATR){
        tradeSize = Size*Multiplier;

    }
    // else do nothing
    else tradeSize = Size;
    // return trade size
    return tradeSize;
    
}
```

## Step 4 – Using ATR Volatility Sizing snippet

After restarting SQX, you will find the new snippet in the Money Management tab.

[![](https://strategyquant.com/wp-content/uploads/2021/12/2-1-750x404.jpg)](https://strategyquant.com/wp-content/uploads/2021/12/2-1-750x404.jpg)

In the chart below you can see the changing sizes of the trades.

[![](https://strategyquant.com/wp-content/uploads/2021/12/3-750x391.jpg)](https://strategyquant.com/wp-content/uploads/2021/12/3-750x391.jpg)

## Commented Code

```
/*
 * Copyright (c) 2017-2018, StrategyQuant - All rights reserved.
 *
 * Code in this file was made in a good faith that it is correct and does what it should.
 * If you found a bug in this code OR you have an improvement suggestion OR you want to include
 * your own code snippet into our standard library please contact us at:
 * https://roadmap.strategyquant.com
 *
 * This code can be used only within StrategyQuant products.
 * Every owner of valid (free, trial or commercial) license of any StrategyQuant product
 * is allowed to freely use, copy, modify or make derivative work of this code without limitations,
 * to be used in all StrategyQuant products and share his/her modifications or derivative work
 * with the StrategyQuant community.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES
 * OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package SQ.MoneyManagement;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@ClassConfig(name="ATR Volatility Sizing", display="ATR Volatility Sizing: #Size# lots with multiplication #Multiplier#")
@Help("<b>ATR Volatility Sizing</b>")
@Description("ATR Volatility Sizing, #Size# and #Multiplier#")
@SortOrder(100)
public class ATRVolatilitySizing extends MoneyManagementMethod {

    @Parameter(defaultValue="0.1", minValue=0.01d, name="Order size", maxValue=1000000d, step=0.1d, category="Default")
    @Help("Order size (number of lots for forex)")
    public double Size;

    @Parameter(defaultValue="0.1", minValue=0.01d, name="Multiplier", maxValue=1000, step=0.1d, category="Default")
    @Help("Multiplier")
    public double Multiplier;

    @Parameter(defaultValue="5", minValue=2, name="Fast ATR Period", maxValue=480, step=1, category="Default")
    @Help("Fast ATR Period")
    public int FastATRPeriod;

    @Parameter(defaultValue="20", minValue=2, name="Slow ATR Period", maxValue=480, step=1, category="Default")
    @Help("Slow ATR Period")
    public int SlowATRPeriod;

    //------------------------------------------------------------------------
    //------------------------------------------------------------------------
    //------------------------------------------------------------------------
    
    @Override
    public double computeTradeSize(StrategyBase strategy, String symbol, byte orderType, double price, double sl, double tickSize, double pointValue) throws Exception {
        
        double tradeSize;
        // get previous close
        double prevClose = strategy.MarketData.Chart(symbol).Close(1); 
        // get rounded ATR with slower period
        double slowValueATR = SQUtils.round(strategy.getATRValue(strategy.MarketData.Chart(symbol),SlowATRPeriod, 1), 5);
        // get rounded ATR with faster period
        double fastValueATR = SQUtils.round(strategy.getATRValue(strategy.MarketData.Chart(symbol),FastATRPeriod, 1), 5);
        // if short volatility spikes UP then modify size of trade
        if(fastValueATR>slowValueATR){
            tradeSize = Size*Multiplier;

        }
        // else size equal default Size
        else tradeSize = Size;
        // return trade size
        return tradeSize;
```
