package SQ.Blocks.Indicators.HullMovingAverage;

import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;
import SQ.Calculators.*;
import SQ.Internal.IndicatorBlock;
import java.lang.*;

/**
 * Indicator name as it will be displayed in UI, and its return type.
 * Possible return types:
 * ReturnTypes.Price - indicator is drawn on the price chart, like SMA, Bollinger Bands etc.
 * ReturnTypes.Price - indicator is drawn on separate chart, like CCI, RSI, MACD
 * ReturnTypes.PriceRange - indicator is price range, like ATR.
 */
@BuildingBlock(name="(HMA) Hull Moving Average ", display="HMA(@Chart@#Period#)[#Shift#]", returnType = ReturnTypes.Price)
@Help("HMA is an adaptive MA")
@ParameterSet(set="Period=12")
@ParameterSet(set="Period=24")
@ParameterSet(set="Period=48")
@ParameterSet(set="Period=120")
@ParameterSet(set="Period=240")
@ParameterSet(set="Period=12,ComputedFrom=0")
@ParameterSet(set="Period=24,ComputedFrom=0")
@ParameterSet(set="Period=48,ComputedFrom=0")
@ParameterSet(set="Period=120,ComputedFrom=0")
@ParameterSet(set="Period=240,ComputedFrom=0")
public class HullMovingAverage extends IndicatorBlock {

    @Parameter
    public DataSeries Input;

    @Parameter(defaultValue="10", isPeriod=true, minValue=2, maxValue=1000, step=1)
    public int Period;

    @Output(name = "HMA", color = Colors.Blue)
    public DataSeries Value;

    private AverageCalculator fastwma;
    private AverageCalculator slowwma;
    private AverageCalculator hma;
    
	private int fastMaPeriod;
	private int squaredPeriod;

    //------------------------------------------------------------------------
    //------------------------------------------------------------------------
    //------------------------------------------------------------------------


    @Override
    protected void OnInit() throws TradingException {
    
	fastMaPeriod = (int)Math.floor(Period/2);
	squaredPeriod =  (int)Math.floor(Math.sqrt(Period));
   
    fastwma =  new AverageCalculator(AverageCalculator.LWMA, fastMaPeriod);
    slowwma = new AverageCalculator(AverageCalculator.LWMA, Period);
    hma = new AverageCalculator(AverageCalculator.LWMA, squaredPeriod);
    }

    @Override
    protected void OnBarUpdate() throws TradingException {

        if(CurrentBar>squaredPeriod){
        double hmainner;
        
        fastwma.onBarUpdate(Input.get(0), getCurrentBar());
        slowwma.onBarUpdate(Input.get(0), getCurrentBar());
        
        double fma = fastwma.getValue();
        double sma = slowwma.getValue();

        hmainner =  ((2* fma) - sma);
        hma.onBarUpdate(hmainner, getCurrentBar());
     
        Value.set(0, hma.getValue());
        }
        else return;



        
    }
}

