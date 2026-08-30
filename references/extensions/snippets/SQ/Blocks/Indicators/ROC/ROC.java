package SQ.Blocks.Indicators.ROC;

import SQ.Internal.IndicatorBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.*;

/**
 * Indicator name as it will be displayed in UI, and its return type.
 * Possible return types:
 * ReturnTypes.Price - indicator is drawn on the price chart, like SMA, Bollinger Bands etc.
 * ReturnTypes.Price - indicator is drawn on separate chart, like CCI, RSI, MACD
 * ReturnTypes.PriceRange - indicator is price range, like ATR.
 */
@BuildingBlock(name="(ROC) ROC", display="ROC(@Chart@#Period#)[#Shift#]", returnType = ReturnTypes.Number)
@Help("Price rate of change")
public class ROC extends IndicatorBlock {

    @Parameter
    public ChartData Input;

    @Parameter(defaultValue="5", isPeriod=true, minValue=2, maxValue=1000, step=1)
    public int Period;

    @Output
    public DataSeries Value;

    //------------------------------------------------------------------------
    //------------------------------------------------------------------------
    //------------------------------------------------------------------------

    /**
     * This method is called on every bar update and here the indicator value is computed.
     *
     * Unlike in MT4 you don't compute indicator values for multiple bars in a loop, 
     * you need to compute value only for the latest (current) bar.
     * Trading engine will take care of calling this method for every bar in the chart.
     *
     * Actual bar for which the indicator value is computed is stored in CurrentBar variable. 
     * If 0, it means it is the very first bar of the chart.
     */
    @Override
	protected void OnBarUpdate() throws TradingException {
        if (CurrentBar < Period) {
            Value.set(0, 0);
        }
        else {
            double prevClose = Input.Close(Period);
            if(prevClose == 0){
                Value.set(0, 0);
            }
            else {
                double roc = (Input.Close(0) - prevClose) / prevClose * 100;
                Value.set(0, roc);
            }
        }
	}
}