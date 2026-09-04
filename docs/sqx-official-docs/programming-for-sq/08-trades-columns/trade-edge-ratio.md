# Example – Trade Edge Ratio

- Source: <https://strategyquant.com/doc/programming-for-sq/trade-edge-ratio/>
- Section: Programming for StrategyQuant X › Trades Columns
- Fetched: 2026-09-04

---

The edge ratio tells us how high the potential was for a particular trade. The calculation is the ratio of normalized MFE and normalized MAE. We have already written about the Edge Ratio in this [article](https://strategyquant.com/blog/edge-ratio-in-strategyquant-x/). In the following tutorial, we will show you how to create a new trade column that will allow us to analyze the Edge Ratio of each trade.

## Step 1 – Create new Trade column

Open CodeEditor, click on Create new and choose List of trades column option- Name it **EdgeRatioTrade**.

This will create a new snippet **EdgeRatioTrade****.java** in folder **User/Snippets/SQ/Columns/Trades**

Let’s set Decimal2 in the constructor in order to get the value with two decimal places.

```
public class EdgeRatioTrade extends TradelistColumn {
    
    public EdgeRatioTrade() {
        super("EdgeRatioTrade", Decimal2);
        setWidth(80);
    }
```

## Step 2 – Implement getValue() method

This method has in the parameter [Order](https://strategyquant.com/sqxapi/com/strategyquant/tradinglib/Order.html) order is a class that stores the values of each order of the strategy. At the same time, this class contains methods that allow us to sort or identify the order. In the background, StrategyQuant X calls the getValue(Order order) method and produces a value for each order in the strategy.

```
    @Override
    public Object getValue(Order order) {
    
    double mae = order.PipsMAE; //get order MAE in pips
    double mfe = order.PipsMFE; // get order MAE in pips
    double atrOnOpen = order.ATROnOpen; // get order ATR in pips

    double normMAE = mae/atrOnOpen; // get normalized MAE
    double normMFE = mfe/atrOnOpen; // get normalized MFE

    double eRTrade = SQUtils.safeDivide(normMFE,normMAE); // SQUtils.safeDivide preceds null zero divide exception

    return eRTrade;

}
```

### **Tip**:

If you want to debug your code  this snippet just add function fdebug(“”,””) in to your code. The log where the values are written can be found in the user/Log/StrategyQuant folder.

```
@Override
    public Object getValue(Order order) {
        
        double mae = order.PipsMAE; //get order MAE in pips
        double mfe = order.PipsMFE; // get order MAE in pips
        double atrOnOpen = order.ATROnOpen; // get order ATR in pips

        double normMAE = mae/atrOnOpen; // get normalized MAE
        double normMFE = mfe/atrOnOpen; // get normalized MFE

        double eRTrade = SQUtils.safeDivide(normMFE,normMAE); // SQUtils.safeDivide preceds null zero divide exception

        /// fdebug
        fdebug("EdgeRatioTrade---> ","  order.Ticket: "+order.Ticket+"  order.PipsMAE: "+order.PipsMAE+"  order.ATROnOpen: "+order.ATROnOpen);

        return eRTrade;

    }
```

## Step 3 – Adding snippet to list of trades

After compiling the snippet and restarting StrategyQuantX, you can add the snippet to the trade list.

[![](https://strategyquant.com/wp-content/uploads/2021/11/1-1-750x316.png)](https://strategyquant.com/wp-content/uploads/2021/11/1-1-750x316.png)

[![](https://strategyquant.com/wp-content/uploads/2021/11/2-1.png)](https://strategyquant.com/wp-content/uploads/2021/11/2-1.png)

## Full commented code of the snippet

```
package SQ.Columns.Trades;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

public class EdgeRatioTrade extends TradelistColumn {
    
    public EdgeRatioTrade() {
        super("EdgeRatioTrade", Decimal2);
        setWidth(80);
    }
    
    @Override
    public Object getValue(Order order) {
        
        double mae = order.PipsMAE; //get order MAE in pips
        double mfe = order.PipsMFE; // get order MAE in pips
        double atrOnOpen = order.ATROnOpen; // get order ATR in pips

        double normMAE = mae/atrOnOpen; // get normalized MAE
        double normMFE = mfe/atrOnOpen; // get normalized MFE

        double eRTrade = SQUtils.safeDivide(normMFE,normMAE); // SQUtils.safeDivide preceds null zero divide exception

        return eRTrade;

    }

}
```
