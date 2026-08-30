/*
 * Copyright (c) 2017-2024, StrategyQuant - All rights reserved.
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
package SQ.Blocks.Indicators.AnchoredVWAP;

import SQ.Internal.IndicatorBlock;

import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.Buffer;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Colors;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.Editors;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.ReturnTypes;

/**
 * Anchored VWAP (Volume Weighted Average Price) indicator.
 * 
 * VWAP Formula: Sum(Price * Volume) / Sum(Volume)
 * 
 * The anchor resets at the start of each session (day/week/month).
 * Standard deviation bands are calculated using the volume-weighted variance.
 * 
 * Statistical foundation:
 * - VWAP represents the average price weighted by trading activity
 * - Standard deviation bands use: sqrt(Sum(Volume * (Price - VWAP)^2) / Sum(Volume))
 * - This is the proper volume-weighted standard deviation
 */
@BuildingBlock(name="(AVWAP) Anchored VWAP", display="AnchoredVWAP(@Chart@#SessionType#,#StdDevMult#)[#Shift#]", returnType = ReturnTypes.Price)
@Help("Anchored VWAP with standard deviation bands. Resets at session start (Day/Week/Month).")
@ForEngine("MT4,MT5,TS,MC")
@ParameterSet(set="SessionType=1,StdDevMult=1.0")
@ParameterSet(set="SessionType=1,StdDevMult=2.0")
@ParameterSet(set="SessionType=2,StdDevMult=1.0")
@ParameterSet(set="SessionType=2,StdDevMult=2.0")
@ParameterSet(set="SessionType=3,StdDevMult=1.0")
@ParameterSet(set="SessionType=3,StdDevMult=2.0")
public class AnchoredVWAP extends IndicatorBlock {

    @Parameter(defaultChartIndex=0)
    public ChartData Chart;
    
    @Parameter(defaultValue="1", name="SessionType")
    @Help("Anchor point for VWAP calculation")
    @Editor(type=Editors.Selection, values="Daily=1,Weekly=2,Monthly=3")
    public int SessionType;
    
    @Parameter(defaultValue="1.0", name="StdDevMult", minValue=0.5, maxValue=4.0, step=0.1)
    @Help("Standard deviation multiplier for bands")
    public double StdDevMult;

    @Output(name="VWAP", color=Colors.Black)
    public DataSeries VWAP;
    
    //@Output(name="Upper", color=Colors.Green)
   //public DataSeries Upper;
    
    //@Output(name="Lower", color=Colors.Red)
    //public DataSeries Lower;

    // Buffers for cumulative values (needed for proper DataSeries handling)
    @Buffer
    public DataSeries cumPV;      // Cumulative (Price * Volume)
    
    @Buffer
    public DataSeries cumVolume;  // Cumulative Volume
    
    @Buffer
    public DataSeries cumPV2;     // Cumulative (Price^2 * Volume) for variance

    // Session tracking
    private long currentSessionEnd = 0;

    //------------------------------------------------------------------------

    @Override
    protected void OnInit() throws TradingException {
        // Buffers are initialized by framework
    }

    //------------------------------------------------------------------------

    @Override
    protected void OnBarUpdate() throws TradingException {
        SessionType = SQUtils.fixAllowedRange(SessionType, 1, 3, 1);
        
        long curTime = Chart.Time(0);
        
        // Typical price: (H + L + C) / 3 - standard VWAP calculation
        double typicalPrice = (Chart.High(0) + Chart.Low(0) + Chart.Close(0)) / 3.0;
        double volume = Chart.Volume(0);
        
        // Handle zero volume (use 1 to avoid division issues)
        if (volume <= 0) {
            volume = 1;
        }
        
        // Check if we entered a new session
        boolean isNewSession = (currentSessionEnd == 0 || curTime >= currentSessionEnd);
        
        if (isNewSession) {
            // Calculate new session boundaries
            calculateSessionBoundaries(curTime);
            
            // Reset cumulative values for new session
            cumPV.set(0, typicalPrice * volume);
            cumVolume.set(0, volume);
            cumPV2.set(0, typicalPrice * typicalPrice * volume);
        } else {
            // Accumulate values within session
            double prevCumPV = cumPV.get(1);
            double prevCumVolume = cumVolume.get(1);
            double prevCumPV2 = cumPV2.get(1);
            
            cumPV.set(0, prevCumPV + typicalPrice * volume);
            cumVolume.set(0, prevCumVolume + volume);
            cumPV2.set(0, prevCumPV2 + typicalPrice * typicalPrice * volume);
        }
        
        // Calculate VWAP
        double totalPV = cumPV.get(0);
        double totalVolume = cumVolume.get(0);
        double totalPV2 = cumPV2.get(0);
        
        double vwap = totalPV / totalVolume;
        
        // Calculate volume-weighted standard deviation
        // Variance = E[X^2] - E[X]^2 = (Sum(P^2*V)/Sum(V)) - VWAP^2
        double variance = (totalPV2 / totalVolume) - (vwap * vwap);
        
        // Handle numerical precision issues
        if (variance < 0) {
            variance = 0;
        }
        
        double stdDev = Math.sqrt(variance);
        
        // Set outputs
        VWAP.set(0, vwap);
        //Upper.set(0, vwap + StdDevMult * stdDev);
        //Lower.set(0, vwap - StdDevMult * stdDev);
    }

    //------------------------------------------------------------------------

    /**
     * Calculates session end time based on SessionType.
     * Mirrors the logic from VolumeProfile indicator.
     */
    private void calculateSessionBoundaries(long curTime) throws TradingException {
        long dayStart = SQTime.setTime(curTime, 0, 0, 0, 0);
        
        switch (SessionType) {
            case 1: // Daily
                currentSessionEnd = SQTime.addDays(dayStart, 1);
                break;
            case 2: // Weekly
                long weekStart = SQTime.setDayOfWeek(dayStart, SQTime.MONDAY);
                if (weekStart > dayStart) {
                    weekStart = SQTime.addDays(weekStart, -7);
                }
                currentSessionEnd = SQTime.addDays(weekStart, 7);
                break;
            case 3: // Monthly
                long monthStart = SQTime.setDayOfMonth(dayStart, 1);
                currentSessionEnd = SQTime.addMonths(monthStart, 1);
                break;
            default:
                currentSessionEnd = SQTime.addDays(dayStart, 1);
        }
    }
}