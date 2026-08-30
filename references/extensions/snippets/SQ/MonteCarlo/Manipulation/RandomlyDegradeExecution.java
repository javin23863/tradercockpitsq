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
import com.strategyquant.datalib.*; // Assuming Instrument/other details might be needed indirectly
import com.strategyquant.tradinglib.*; // Contains Order, OrdersList, MonteCarloManipulation etc.

@ClassConfig(name="Randomly degrade execution price", display="Degrade close price for #Probability#% of trades, by max #MaxDegradationPercent#% of price range")
@Help("Simulates execution issues like slippage or wider spreads by randomly worsening the Close Price for a subset of trades, based on a percentage of the trade's price range (Open to original Close).")
public class RandomlyDegradeExecution extends MonteCarloManipulation {
    public static final Logger Log = LoggerFactory.getLogger(RandomlyDegradeExecution.class);

    @Parameter(name="Probability", defaultValue="15", minValue=1, maxValue=100, step=1)
    public int Probability;

    @Parameter(name="Max Degradation Percent", defaultValue="25", minValue=0, maxValue=100, step=1)
    public int MaxDegradationPercent;

    //------------------------------------------------------------------------
    //------------------------------------------------------------------------
    //------------------------------------------------------------------------

    @Override
    public void modifyTrades(IRandomGenerator rng, OrdersList originalOrders) throws Exception {

        double dblProbability = ((double) Probability / 100.0d);
        double dblMaxDegradation = ((double) MaxDegradationPercent / 100.0d);

        if (dblMaxDegradation <= 0) {
            // No degradation requested, nothing to do.
            return;
        }

        for (int i = 0; i < originalOrders.size(); i++) {
            // Check if this trade should be affected based on probability
            if (rng.nextDouble() < dblProbability) {
                Order o = originalOrders.get(i);

                // Calculate the original price difference for this trade
                double originalPriceDifference = o.ClosePrice - o.OpenPrice;

                // If Open and Close prices are the same, there's no range to degrade. Skip.
                if (Math.abs(originalPriceDifference) < 1e-9) { // Use tolerance for floating point comparison
                     if (Log.isTraceEnabled()) {
                        Log.trace("Skipping degradation for trade {} as OpenPrice equals ClosePrice.", o.Ticket);
                     }
                    continue;
                }

                // Calculate a random degradation factor for this specific trade (0 to MaxDegradationPercent)
                double degradationFactor = rng.nextDouble() * dblMaxDegradation;

                // Calculate the amount of price adjustment based on the original range and the factor
                // We use Math.abs because the degradation amount is always positive,
                // its direction depends on the trade type (Long/Short).
                double priceAdjustment = Math.abs(originalPriceDifference) * degradationFactor;

                double originalClosePrice = o.ClosePrice; // Store for logging
                double newClosePrice;

                // Apply the adjustment to worsen the ClosePrice
                if (o.isLong()) {
                    // For Long trades, a worse close price is lower
                    newClosePrice = o.ClosePrice - priceAdjustment;
                    // Ensure close price doesn't go below open price due to degradation alone
                    // (i.e., a winning trade shouldn't become a loser just from this slippage,
                    // though its profit will decrease). A losing trade will become worse.
                    // Note: A more complex rule could prevent crossing the open price,
                    // but simple slippage just adjusts the exit price achieved. Let's stick to simple adjustment.
                    // Consider if price can go negative - might need safety checks depending on asset type.

                } else if (o.isShort()) {
                    // For Short trades, a worse close price is higher
                    newClosePrice = o.ClosePrice + priceAdjustment;
                     // Similar logic applies - short profit decreases, or loss increases.

                } else {
                     // Should not happen for typical Buy/Sell orders, but handle defensively
                     if (Log.isWarnEnabled()) {
                        Log.warn("Trade {} has unknown direction, cannot apply degradation.", o.Ticket);
                     }
                     continue; // Skip this trade
                }

                // Update the order's close price
                o.ClosePrice = (float)newClosePrice;

                if (Log.isDebugEnabled()) {
                    Log.debug("Degraded trade {} ({}): Price range {:.5f}, Factor {:.2f}%, Adjustment {:.5f}. ClosePrice changed from {:.5f} to {:.5f}",
                              o.Ticket, (o.isLong() ? "Long" : "Short"), originalPriceDifference,
                              degradationFactor * 100, (o.isLong() ? -priceAdjustment : priceAdjustment),
                              originalClosePrice, o.ClosePrice);
                }
            }
        }
        // The framework will recalculate profits based on the modified ClosePrice
        // when it evaluates the results of this Monte Carlo iteration.
        // No need to adjust timestamps here.
    }
}