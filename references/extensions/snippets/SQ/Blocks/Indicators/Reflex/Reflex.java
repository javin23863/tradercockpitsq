package SQ.Blocks.Indicators.Reflex;
import static java.lang.Math.exp;
import static java.lang.Math.cos;
import static java.lang.Math.*;
import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;
import SQ.Calculators.SumCalculator;
import com.strategyquant.tradinglib.simulator.Engines;

import SQ.Internal.IndicatorBlock;

/**
 * Indicator name as it will be displayed in UI, and its return type.
 * Possible return types:
 * ReturnTypes.Price - indicator is drawn on the price chart, like SMA, Bollinger Bands etc.
 * ReturnTypes.Price - indicator is drawn on separate chart, like CCI, RSI, MACD
 * ReturnTypes.PriceRange - indicator is price range, like ATR.
 */
@BuildingBlock(name="(RFX) Reflex", display="Reflex(@Chart@#Period#)[#Shift#]", returnType = ReturnTypes.Number)
@Help("Reflex help text")
@ParameterSet(set="Period=6")
@ParameterSet(set="Period=12")
@ParameterSet(set="Period=24")
@ParameterSet(set="Period=36")
@ParameterSet(set="Period=48")
@ParameterSet(set="Period=120")
@ParameterSet(set="Period=240")
@ParameterSet(set="Period=10")
@ParameterSet(set="Period=20")
@ParameterSet(set="Period=30")
@ParameterSet(set="Period=40")
@ParameterSet(set="Period=100")
@ParameterSet(set="Period=200")
public class Reflex extends IndicatorBlock {

    @Parameter
    public ChartData Input;

    @Parameter(defaultValue="24", isPeriod=true, minValue=2, maxValue=1000, step=1)
    public int Period;

    @Output
    public DataSeries Value;
    
    @Buffer
    public DataSeries filt;
    @Buffer
    public DataSeries MS;

    private SumCalculator sumCalculator;

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
		if(Engines.isTradestationEngine(Indicators.Engine)) {
			onBarUpdateTS();
		}
		else {
			
			onBarUpdateMT();
		}
	}

 
    protected void onBarUpdateMT() throws TradingException {
      double indiValue;

        if (CurrentBar < Period) {

            Value.set(0, 0);
            MS.set(0,0);
            filt.set(0,0);

        } else {

            double a1 = Math.exp(-1.414*Math.PI/(Period*0.5)); 
            double b1 = 2.0* a1*cosine(Math.cos(1.414*180/(0.5*Period))); //// here is the only difference between TS and MT engine
            double c2 = b1;
            double c3 = -a1*a1;
            double c1 = 1-c2-c3;
            double f = c1*(Input.Close.get(0)+Input.Close.get(1))/2+c2*filt.get(1)+c3*filt.get(2);
            filt.set(0,f);
            double slope = (filt.get(Period)-f)/Period;

            
            double sum =0;
            for(int i = 1; i <= Period;i++){

                sum += ((f+i*slope)-filt.get(i));

            }
            sum = sum/Period;
            
            MS.set(0,0.04*sum*sum+0.96*MS.get(1));
            double reflex = sum/Math.sqrt(MS.get(0));
            Value.set(0, reflex);
 
        }

    }

    private double cosine(double a)
    {    
    return Math.cos(a*Math.PI/180);
    }

    protected void onBarUpdateTS() throws TradingException {


        if (CurrentBar < Period) {
            Value.set(0, 0);
            MS.set(0,0);
            filt.set(0,0);

        } else {

            double a1 = Math.exp(-1.414*Math.PI/(0.5*Period));
            double b1 = 2.0* a1*Math.cos(Math.toRadians(1.414*180/(0.5*Period))); //// here is the only difference between TS and MT engine
            double c2 = b1;
            double c3 = -a1*a1;
            double c1 = 1-c2-c3;
            //debug("Reflex TS","a1: "+a1+"; ba: "+b1);
            double f = c1*(Input.Close.get(0)+Input.Close.get(1))/2+c2*filt.get(1)+c3*filt.get(2);
            filt.set(0,f);
            
            double slope = (filt.get(Period)-f)/Period;
            


            double sum =0;
            for(int i = 1; i <= Period;i++){

                sum += ((f+i*slope)-filt.get(i));

            }
            sum = sum/Period;
            //Value.set(0, sum);

            MS.set(0,0.04*sum*sum+0.96*MS.get(1));
            double reflex = sum/Math.sqrt(MS.get(0));
            
            Value.set(0, reflex);
            
            
        }
      
    }




}   


