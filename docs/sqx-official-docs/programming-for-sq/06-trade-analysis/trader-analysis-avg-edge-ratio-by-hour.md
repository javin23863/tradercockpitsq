# Trade Analysis – Avg Edge Ratio by hour

- Source: <https://strategyquant.com/doc/programming-for-sq/trader-analysis-avg-edge-ratio-by-hour/>
- Section: Programming for StrategyQuant X › Trade analysis
- Fetched: 2026-09-04

---

Trade Analysis Charts is a part of StrategQuant X results that allows us to visually analyze some properties of a strategy. In the following tutorial, we will show you how to create an analytical snippet that visually shows us  the average edge ratio of a strategy as a function of the opening hour of the trade.

You can download finished snippet [here.](https://strategyquant.com/codebase/avg-edge-ratio-by-hour/)

## Step 1 – Open Code Editor

Click on the Code editor icon to switch to Editor.

[![](https://strategyquant.com/wp-content/uploads/2021/11/1-1-750x402.jpg)](https://strategyquant.com/wp-content/uploads/2021/11/1-1-750x402.jpg)

## Step 2 – Create new Trade column

Open CodeEditor, click on Create new and choose Trade analysis chart option- Name it **AvgEdgeRatioByHour**. This will create a new snippet **AvgEdgeRatioByHour.java** in folder **User/Snippets/SQ/TradeAnalysis** In the constructor we define the name to be displayed.

```
public class AvgEdgeRatioByHour extends TradeAnalysisChart {

    public AvgEdgeRatioByHour() {
        this.name = L.tsq("Avg.Edge Ratio By Hour");
    }
```

## Step 3 – Modify draw method

In this method we specify the type of graph we want to create. We can choose from the following options

- BarChart
- ScatterChart
- PriceChart

Then you create an array into which you insert the data from the computeData array.  Use the for loop to add the values calculated in the computeData array  to the chart.

```
@Override
    public AbstractChart draw(OrdersList orders, byte plType, byte tradePeriod) {
        // creating new instance of chart
        BarChart chart = new BarChart();
        
        chart.invertIfNegative(true);
        
        if(orders==null) {
            return chart;
        }
        // creating array here we store data computed in computeData method
        double[] hours = computeData(orders, plType, tradePeriod);
        
        // fill chart with array values
        for(int hour=0; hour<hours.length; hour++) {
            
            chart.addValue(L.tsq("Avg.Edge Ratio By Hour"), hour, hours[hour]/hoursCount[hour]);
        }

        return chart;
    }
```

## Step 4 – Implement computeData array

computeData is an array where we work with [OrdersList.](https://strategyquant.com/sqxapi/com/strategyquant/tradinglib/OrdersList.html) [OrdersList](https://strategyquant.com/sqxapi/com/strategyquant/tradinglib/OrdersList.html)  is a class that stores the list of trades of a strategy and provides methods to manipulate with the list of trades.  You can loop troght entire [OrdersList](https://strategyquant.com/sqxapi/com/strategyquant/tradinglib/OrdersList.html)  and get data for very [Order](https://strategyquant.com/sqxapi/com/strategyquant/tradinglib/Order.html) using method [get](https://strategyquant.com/sqxapi/com/strategyquant/tradinglib/OrdersList.html#get(int))​(int index).

```
private double[] computeData(OrdersList orders, byte plType, byte tradePeriod) {
        double[] hours = new double[24];
        
        for(int hour=0; hour<hours.length; hour++) { 
            hours[hour]=0; 
        }
        
        int hour;
        // here we work with orderslist 
        for(int i = 0; i < orders.size(); i++) {
            Order order = orders.get(i);
            
            hour = SQTime.getHour(order.getTimeByPeriodType(tradePeriod));  //0-23

            double mae = order.PipsMAE; //get order MAE in pips
            double mfe = order.PipsMFE; // get order MAE in pips
            double atrOnOpen = order.ATROnOpen; // get order ATR in pips

            double normMAE = mae/atrOnOpen; // get normalized MAE
            double normMFE = mfe/atrOnOpen; // get normalized MFE

            double eRTrade = SQUtils.safeDivide(normMFE,normMAE); // SQUtils.safeDivide preceds null zero divide exception

            hours[hour]+= eRTrade;
            hoursCount[hour]+=1;
        }
```

## Step 5 – Using **Avg. Edge Ratio By Hour in Strategy Quant X**

After compiling the snippet and restarting Code Editor and StrategyQUant X we can work with the newly created snippet.

[![](https://strategyquant.com/wp-content/uploads/2021/11/2-1-750x384.jpg)](https://strategyquant.com/wp-content/uploads/2021/11/2-1-750x384.jpg)

## Commented Code

```
package SQ.TradeAnalysis;
import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

public class AvgEdgeRatioByHour extends TradeAnalysisChart {
    // Class constructor where we define snippet name
    public AvgEdgeRatioByHour() {
        this.name = L.tsq("Avg.Edge Ratio By Hour");
    }
    
    @Override
    public AbstractChart draw(OrdersList orders, byte plType, byte tradePeriod) {
        // creating new instance of chart
        BarChart chart = new BarChart();
        
        chart.invertIfNegative(true);
        
        if(orders==null) {
            return chart;
        }
        // create array where we store data computed in computeData method
        double[] hours = computeData(orders, plType, tradePeriod);
        
        // fill chart with array values
        for(int hour=0; hour<hours.length; hour++) {
            // hours[hour]/hoursCount[hour] we compute average Edge Ratio per hour
            chart.addValue(L.tsq("Avg.Edge Ratio By Hour"), hour, hours[hour]/hoursCount[hour]);
        }

        return chart;
    }
    // create array for counting trades 
    int [] hoursCount = new int[24];
     
    private double[] computeData(OrdersList orders, byte plType, byte tradePeriod) {
        //create array where we store  computed resultu
        double[] hours = new double[24];
        // set values do default state 0 
        for(int hour=0; hour<hours.length; hour++) { 
            hours[hour]=0; 
        }
        
        int hour;
        // here we work with OrdersList orders 
        for(int i = 0; i < orders.size(); i++) {
            Order order = orders.get(i);
            
            hour = SQTime.getHour(order.getTimeByPeriodType(tradePeriod));  //0-23

            double mae = order.PipsMAE; //get order MAE in pips
            double mfe = order.PipsMFE; // get order MAE in pips
            double atrOnOpen = order.ATROnOpen; // get order ATR in pips

            double normMAE = mae/atrOnOpen; // get normalized MAE
            double normMFE = mfe/atrOnOpen; // get normalized MFE

            double eRTrade = SQUtils.safeDivide(normMFE,normMAE); // SQUtils.safeDivide preceds null zero divide exception

            // store edge ratio for every hour
            hours[hour]+= eRTrade;
            // count number of trades for every hour
            hoursCount[hour]+=1;
        }

        return hours;
    }
}
```
