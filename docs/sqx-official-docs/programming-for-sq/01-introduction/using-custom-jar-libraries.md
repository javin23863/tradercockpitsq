# Using custom JAR libraries

- Source: <https://strategyquant.com/doc/programming-for-sq/using-custom-jar-libraries/>
- Section: Programming for StrategyQuant X › Introduction
- Fetched: 2026-09-04

---

Note – this is a new and experimental functionality available from SQ X Build 130.

It is now possible to use your own Custom Java JAR libraries in snippets.

All you need to do is to copy your JARs into **/user/libs** folder in your StrategyQuant installation and after restart of SQ you should be able to call them in snippets.

An example could be a sample RSI indicator called from TA4J library (<https://github.com/ta4j/ta4j>). You can download the core JAR for this library [here](https://strategyquant.com/downloads/ta4j-core-0.13.jar).

Then place it to **/user/libs** folder – if **/libs** subfolder doesn’t exist yet in **/user** folder then create it.

Then you are able to use it in a snippet. Open CodeEditor and create a new sample snippet for RSI indicator computed by TA4J library:

```
package SQ.Blocks.Indicators.RSIta4j;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Colors;
import com.strategyquant.tradinglib.Indicator;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.ReturnTypes;

import SQ.Internal.IndicatorBlock;

@BuildingBlock(name="(RSI ta4j) Relative Strength Index", display="RSIta4j(#Period#)[#Shift#]", returnType = ReturnTypes.Number)
@Indicator(oscillator=true, middleValue=50, min=0, max=100, step=0.5)
public class RSIta4j extends IndicatorBlock {
    @Parameter(category="Default", name="Input", defaultValue="0")
    public ChartData Input;
    
    @Parameter(defaultValue="14", minValue=2, maxValue=10000, step=1)
    public int Period;

    @Output(name = "RSIta4j", color = Colors.Red)
    public DataSeries Value;
    
    private RSIIndicator rsi;
    private ZonedDateTime zdt;
    
    private BarSeries series = new BaseBarSeries("series");
    private long lastTime = 0;
    
    //------------------------------------------------------------------------
    //------------------------------------------------------------------------
    //------------------------------------------------------------------------

    @Override
    protected void OnInit() throws TradingException {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        rsi = new RSIIndicator(closePrice, Period);
    }
    
    //------------------------------------------------------------------------
    
    @Override
    protected void OnBarUpdate() throws TradingException {
        zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(Input.Time()), ZoneOffset.UTC);
                
        if(Input.Time()==lastTime) {
            return;
        }
        
        series.addBar(zdt, Input.Open.get(0), Input.High.get(0), Input.Low.get(0), Input.Close.get(0), Input.Volume.get(0));
        
        Value.set(0, rsi.getValue(series.getEndIndex()).doubleValue());
        
        lastTime = Input.Time();
    }
}
```

This is pretty standard SQ indicator snippet, but it doesn’t contain a code to compute the indicator. Instead it is calling **TA4J RSIIndicator** class to compute it.

Please note – it is simple to extend StrategyQuant with indicators from already existing libraries, but it is only half of the work required.

You can create a new indicator in SQ this way, but this indicator will be still computed only in StrategyQuant. In order to use your strategy with this custom indicator in MetaTrader, Tradestation, MultiCharts or other platform you need to:

- add definition how this new snippet will be translated to the source code of the platform
- add implementation of this indicator also to your target platform

Please check more about this in our article [Adding indicators and signals](https://strategyquant.com/doc/programming-sq/adding-indicators-and-signals/).
