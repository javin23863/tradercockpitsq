# Equity moving average simulation

- Source: <https://strategyquant.com/doc/programming-for-sq/equity-moving-average-simulation/>
- Section: Programming for StrategyQuant X › What If
- Fetched: 2026-09-04

---

In this tutorial we will show you how to create and simulate equity curve control  in What If Crosscheck .

What-If scenarios in StrategyQuantx is the tool that allows you to test various hypothesis of trading your strategy.

Equity control simulation function allows to simulate switching a strategy on and off based on the equity curve.  The assumption is based on the hypothesis  that is better to trade strategy  if its performance is above its average. Good article dedicated to this topic is possible to read [here](https://kjtradingsystems.com/equity-curve-trading.html).

Finished snippet you can download [here.](https://strategyquant.com/codebase/equity-moving-average-trading-simulation/)

## Step 1 – Create new What if  snippet

Open CodeEditor, click on Create new and choose List of trades column option- Name it **EquityMATrading**.

This will create a new snippet **EquityMATrading****.java** in folder **User/Snippets/SQ/Whatif**

## Step 2 – Define snippet parameters

Since we want to compute equity average, we need to include a parameter – MAPeriod  that allows us to specify a period of the moving average.

Creating a parameter means that it will adjustable in  Whatif tab in EquityMATrading  in crosschecks.

```
@ClassConfig(name="() Equity MA Trading", display="Equity MA(#MAPeriod#) Trading ")
@Help("Help text")
public class EquityMATrading extends WhatIf {
    public static final Logger Log = LoggerFactory.getLogger(EquityMATrading.class);
    

    @Parameter(name="MAPeriod", defaultValue="10", minValue=2, maxValue=100000, step=1)
    public int MAPeriod;
```

## Step 3 – Implement filter(OrdersList orders) method

The filter([OrdersList](https://strategyquant.com/sqxapi/com/strategyquant/tradinglib/OrdersList.html) orders) method is used to filter the original list of orders in the strategy. [OrdersList](https://strategyquant.com/sqxapi/com/strategyquant/tradinglib/OrdersList.html)  is a class that stores the list of trades of a strategy and provides methods to manipulate with the list of trades.  You can loop troght entire [OrdersList](https://strategyquant.com/sqxapi/com/strategyquant/tradinglib/OrdersList.html)  and get data for very [Order](https://strategyquant.com/sqxapi/com/strategyquant/tradinglib/Order.html) using method [get](https://strategyquant.com/sqxapi/com/strategyquant/tradinglib/OrdersList.html#get(int))​(int index).

Here you need to pay attention to how you loop trough the list of orders. In our example, we are using the classic for loop, so we can not manipulate the orders list during the loop.

Instead, we create a temporary [OrdersList](https://strategyquant.com/sqxapi/com/strategyquant/tradinglib/OrdersList.html) ol where we store the trades that have met our conditions. We copy this orderslist at the end of the snippet into the main orderslist

## Step 4 – Using Whatif snippet EquityMATrading

[![](https://strategyquant.com/wp-content/uploads/2021/11/1-2-750x400.png)](https://strategyquant.com/wp-content/uploads/2021/11/1-2-750x400.png)

To add a snippet in Crosschecks you can use this snippet to simulate what would happen if you traded your strategy if its equity curve is above its moving average.

## Full commented code of the snippet

```
package SQ.WhatIf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.ClassConfig;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.WhatIf;
import com.strategyquant.tradinglib.results.stats.comparator.OrderComparatorByOpenTime;

import it.unimi.dsi.fastutil.objects.ObjectListIterator;

@ClassConfig(name="() Equity MA Trading", display="Equity MA Trading (#MAPeriod#)")
@Help("Help text")
public class EquityMATrading extends WhatIf {
    public static final Logger Log = LoggerFactory.getLogger(EquityMATrading.class);
    
    
    @Parameter(name="MAPeriod", defaultValue="10", minValue=2, maxValue=100000, step=1)
    public int MAPeriod;

    @Override
    public void filter(OrdersList orders) throws Exception {

        // New instance of OrdersList in which we store trades that are above the moving average 
        OrdersList newOL = new OrdersList("AboveMovingAverage"); 
        
        // We are looping trough whole OrdersList orders in order to compute Moving Average
        for(int i = MAPeriod;i < orders.size();i++){

            // Current last order we are deciding whether or not to go to newOL 
            Order order = orders.get(i); 
    
            // get balance of order	
            double balance = order.AccountBalance;
            
            // filter balanced orders
            if(order.isBalanceOrder()) continue; 

                double sum = 0;		
                // compute Moving Average of Equity 
                for(int k =1;k<=MAPeriod;k++){ /// robim ma , pricom zaratavam prvy  orders.get(i-1) a posledny orders.get(i-MAPeriod) pri vyratavani MA
                    Order o = orders.get(i-k);
                    
                    sum = sum + o.AccountBalance; // o.PL = order Profit/Loss
                }
                // average of equity 
                double avg = sum/MAPeriod; 
                // we are comparing get(i-1) with moving average
                Order lastOrder = orders.get(i-1); 
                
                // if last order equity > average of equity 
                if(lastOrder.AccountBalance>avg) { 
                    // we add order.get(i) to newOL 
                    newOL.add(order);  		
                }				
                avg =0; // reset average variable
                sum =0;	// reset sum variable	
            }
            // sorting newOL by positions open time
            newOL.sort(new OrderComparatorByOpenTime()); //// sorting orders by open time
            // we are replacing OrdersList orders with OrdersList newOL and we recieve only trades above moving average
            orders.replaceWithList​(newOL); 
        }

}
```
