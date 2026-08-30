/*
 * Copyright (c) 2017-2024, StrategyQuant - All rights reserved.
 * Derived work based on examples provided.
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
package SQ.MonteCarlo.Manipulation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

import java.util.List;
import java.util.ArrayList;


@ClassConfig(name="Simulate Parameter Jitter Effects", display="Simulate Param Jitter: Skip Prob #SkipProbability#%, Adjust Prob #AdjustProbability#%, Max Price Adj #MaxPriceAdjustPercent#%")
@Help("Simulates effects of minor strategy parameter instability by randomly skipping some trades and/or randomly adjusting the Close Price (better or worse) of others based on trade range.")
public class SimulateParameterJitter extends MonteCarloManipulation {
    public static final Logger Log = LoggerFactory.getLogger(SimulateParameterJitter.class);

    @Parameter(name="Skip Probability", defaultValue="5", minValue=0, maxValue=100, step=1)
    public int SkipProbability; // Chance (%) a trade is skipped entirely

    @Parameter(name="Adjust Probability", defaultValue="20", minValue=0, maxValue=100, step=1)
    public int AdjustProbability; // Chance (%) a non-skipped trade's close price is adjusted

    @Parameter(name="Max Price Adjust Percent", defaultValue="15", minValue=0, maxValue=100, step=1)
    public int MaxPriceAdjustPercent; // Max adjustment (+/-) as % of Open-Close range

    //------------------------------------------------------------------------
    //------------------------------------------------------------------------
    //------------------------------------------------------------------------

    @Override
    public void modifyTrades(IRandomGenerator rng, OrdersList originalOrders) throws Exception {

        if (originalOrders == null || originalOrders.isEmpty()) {
            return; // Nothing to do
        }

        double skipProb = (double) SkipProbability / 100.0d;
        double adjustProb = (double) AdjustProbability / 100.0d;
        double maxAdjustFactor = (double) MaxPriceAdjustPercent / 100.0d;

        List<Integer> indicesToRemove = new ArrayList<>();

        // First pass: Identify trades to skip
        for (int i = 0; i < originalOrders.size(); i++) {
            if (rng.nextDouble() < skipProb) {
                indicesToRemove.add(i);
                if(Log.isTraceEnabled()) {
                    Log.trace("Trade index {} marked for removal (Skip Probability).", i);
                }
            }
        }

        // Remove skipped trades (iterating backwards through indicesToRemove)
        for (int i = indicesToRemove.size() - 1; i >= 0; i--) {
            int index = indicesToRemove.get(i);
            originalOrders.remove(index);
             if(Log.isDebugEnabled()) {
                // Note: Original ticket might be lost if Order object reference changes, logging index is safer
                Log.debug("Removed trade originally at index {} due to simulated skip.", index);
             }
        }

        // Second pass: Adjust remaining trades
        for (int i = 0; i < originalOrders.size(); i++) {
             Order o = originalOrders.get(i);
             if (o == null) continue;

             // Check if this trade should be adjusted
             if (rng.nextDouble() < adjustProb) {
                 double originalPriceDifference = o.ClosePrice - o.OpenPrice;

                 // Skip adjustment if trade was breakeven (no range to base adjustment on)
                 if (Math.abs(originalPriceDifference) < 1e-9) {
                    if(Log.isTraceEnabled()){
                        Log.trace("Skipping adjustment for trade {} - Open/Close price nearly identical.", o.Ticket);
                    }
                    continue;
                 }

                 // Calculate random adjustment factor between -maxAdjustFactor and +maxAdjustFactor
                 // rng.nextDouble() gives [0.0, 1.0), so (rng.nextDouble() * 2.0 - 1.0) gives [-1.0, 1.0)
                 double randomFactor = (rng.nextDouble() * 2.0 - 1.0) * maxAdjustFactor;

                 // Calculate the price adjustment amount
                 double priceAdjustment = Math.abs(originalPriceDifference) * randomFactor; // Note: priceAdjustment can now be +/-

                 double originalClosePrice = o.ClosePrice;
                 double newClosePrice;

                 // Apply adjustment: Remember a positive randomFactor should WORSEN shorts and IMPROVE longs
                 if (o.isLong()) {
                     // Positive adjustment = higher close (better)
                     // Negative adjustment = lower close (worse)
                     newClosePrice = o.ClosePrice + priceAdjustment;
                 } else if (o.isShort()) {
                     // Positive adjustment = higher close (worse)
                     // Negative adjustment = lower close (better)
                     newClosePrice = o.ClosePrice + priceAdjustment; // Still adding, but effect reversed due to short nature
                     // Let's re-think the adjustment logic sign for clarity
                     // Positive price adjustment ALWAYS means price goes UP.
                     // For LONG: Price Up = Good.
                     // For SHORT: Price Up = Bad.
                     // So the formula is correct, but interpretation matters. Let's keep it simple: apply the raw price adjustment.
                     // An alternative: adjust relative to OPEN price? Maybe too complex. Let's stick to ClosePrice adjustment.

                     // Let's try making adjustment sign direction-dependent for clarity
                     double directionalAdjustment;
                     if(o.isLong()) {
                         // priceAdjustment positive -> better P/L ; negative -> worse P/L
                          directionalAdjustment = priceAdjustment;
                     } else { // Short
                         // priceAdjustment positive -> worse P/L ; negative -> better P/L
                          directionalAdjustment = -priceAdjustment; // Invert for short
                     }
                     newClosePrice = o.ClosePrice + directionalAdjustment;


                 } else {
                      if(Log.isWarnEnabled()) {
                         Log.warn("Trade {} has unknown direction, skipping adjustment.", o.Ticket);
                      }
                      continue;
                 }

                 o.ClosePrice = (float)newClosePrice;

                 if (Log.isDebugEnabled()) {
                    Log.debug("Adjusted trade {} ({}): Factor {:.2f}%, Price Adj {:.5f}. ClosePrice {} -> {}",
                                o.Ticket, (o.isLong()? "Long" : "Short"), randomFactor*100, priceAdjustment,
                                originalClosePrice, o.ClosePrice);
                 }
             }
        }
    }
}