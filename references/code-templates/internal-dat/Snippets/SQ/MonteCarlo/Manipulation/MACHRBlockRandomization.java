/*
 * Copyright (c) 2017-2018, StrategyQuant - All rights reserved.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ClassConfig(name="MACHR Block Randomization", display="Randomize blocks of trades, block size #BlockSize#")
@Help("Randomizes blocks of trades using bootstrap sampling to simulate robustness against market regime changes. Divides trades into blocks and then samples blocks with replacement, preserving the internal structure within each block.")
public class MACHRBlockRandomization extends MonteCarloManipulation {
	public static final Logger Log = LoggerFactory.getLogger(MACHRBlockRandomization.class);
	
	@Parameter(name="BlockSize", defaultValue="5", minValue=2, maxValue=50, step=1)
	@Help("Number of trades per block. Larger blocks preserve more of the original market regime structure.")
	public int BlockSize;
	
	@Parameter(name="PreserveTimestamps", defaultValue="true")
	@Help("If true, adjusts timestamps to maintain chronological order after block randomization.")
	public boolean PreserveTimestamps;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	public void modifyTrades(IRandomGenerator rng, OrdersList originalOrders) throws Exception {
		
		if(originalOrders.size() < BlockSize) {
			Log.warn("Not enough trades ({}) for block size ({}). Skipping manipulation.", 
					originalOrders.size(), BlockSize);
			return;
		}
		
		// Create blocks of trades
		List<List<Order>> blocks = createTradeBlocks(originalOrders);
		
		if(blocks.size() < 2) {
			Log.warn("Not enough blocks ({}) created. Need at least 2 blocks for meaningful randomization.", 
					blocks.size());
			return;
		}
		
		// Randomize blocks
		List<List<Order>> randomizedBlocks = randomizeBlocks(rng, blocks);
		
		// Reconstruct the orders list
		reconstructOrdersList(originalOrders, randomizedBlocks);
		
		// Fix timestamps if requested
		if(PreserveTimestamps) {
			fixOrderTimestamps(originalOrders);
		}
		
		Log.debug("MACHR Block Randomization completed: {} trades in {} blocks", 
				originalOrders.size(), blocks.size());
	}

	//------------------------------------------------------------------------
	
	private List<List<Order>> createTradeBlocks(OrdersList originalOrders) {
		List<List<Order>> blocks = new ArrayList<>();
		
		for(int i = 0; i < originalOrders.size(); i += BlockSize) {
			List<Order> block = new ArrayList<>();
			
			// Add trades to current block
			for(int j = i; j < Math.min(i + BlockSize, originalOrders.size()); j++) {
				block.add(originalOrders.get(j));
			}
			
			blocks.add(block);
		}
		
		return blocks;
	}
	
	//------------------------------------------------------------------------
	
	private List<List<Order>> randomizeBlocks(IRandomGenerator rng, List<List<Order>> originalBlocks) {
		List<List<Order>> randomizedBlocks = new ArrayList<>();
		
		// Bootstrap method: sample blocks with replacement
		for(int i = 0; i < originalBlocks.size(); i++) {
			int randomBlockIndex = rng.nextInt(originalBlocks.size());
			List<Order> selectedBlock = originalBlocks.get(randomBlockIndex);
			
			// Create copies of orders to avoid reference issues
			List<Order> blockCopy = new ArrayList<>();
			for(Order order : selectedBlock) {
				blockCopy.add(new Order(order));
			}
			randomizedBlocks.add(blockCopy);
		}
		
		return randomizedBlocks;
	}
	
	//------------------------------------------------------------------------
	
	private void reconstructOrdersList(OrdersList originalOrders, List<List<Order>> randomizedBlocks) {
		// Clear original orders
		originalOrders.clear();
		
		// Add all orders from randomized blocks
		for(List<Order> block : randomizedBlocks) {
			for(Order order : block) {
				originalOrders.add(order);
			}
		}
	}
	
	//------------------------------------------------------------------------
	
	private void fixOrderTimestamps(OrdersList orders) {
		if(orders.size() == 0) return;
		
		// Calculate average time between orders and average order duration
		long totalTimeBetween = 0;
		long totalDuration = 0;
		int countBetween = 0;
		
		// First pass: calculate averages from original data
		for(int i = 0; i < orders.size(); i++) {
			Order order = orders.get(i);
			totalDuration += (order.CloseTime - order.OpenTime);
			
			if(i > 0) {
				Order prevOrder = orders.get(i-1);
				totalTimeBetween += Math.abs(order.OpenTime - prevOrder.CloseTime);
				countBetween++;
			}
		}
		
		long avgDuration = totalDuration / orders.size();
		long avgTimeBetween = countBetween > 0 ? totalTimeBetween / countBetween : avgDuration;
		
		// Second pass: assign new timestamps
		long currentTime = orders.get(0).OpenTime; // Start from first order's original open time
		
		for(int i = 0; i < orders.size(); i++) {
			Order order = orders.get(i);
			
			// Set new timestamps while preserving duration
			order.OpenTime = currentTime;
			order.CloseTime = currentTime + (order.CloseTime - order.OpenTime); // Preserve original duration
			
			// Update current time for next order
			currentTime = order.CloseTime + avgTimeBetween;
		}
	}
}