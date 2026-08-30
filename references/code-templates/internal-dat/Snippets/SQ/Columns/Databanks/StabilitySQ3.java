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
package SQ.Columns.Databanks;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.Directions;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.PlTypes;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;
import com.strategyquant.tradinglib.ValueTypes;
import com.strategyquant.tradinglib.correlation.CorrelationComputer;

import SQ.Functions.DailyEquityComputer;

/**
 * Stability is computed as R-Squared^2 between equity in time, and line created from first to last trade.
 * 
 * @author Mark Fric
 *
 */
public class StabilitySQ3 extends DatabankColumn {
	public static final Logger Log = LoggerFactory.getLogger("StabilitySQ3");
    
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	public StabilitySQ3() {
		super(L.tsq("Stability SQ3"), DatabankColumn.Decimal2, ValueTypes.Maximize, 0, 0, 1);

		setTooltip(L.tsq("Stability - how straight and steep is the equity curve (SQ3 version)"));
		
		// restrict stability computation only for money PL Type (we'll not compute separate stability for % or pips results)
		setPLTypeRestrictions(PlTypes.Money);
		
		// restrict computation only for Both direction, we'll not compute it for Long only or Short only results
		setDirectionRestrictions(Directions.Both); 		
	}

	//------------------------------------------------------------------------

	@Override
	public double compute(SQStats stats, StatsTypeCombination combination, OrdersList ordersList, SettingsMap settings, SQStats statsLong, SQStats statsShort) throws Exception {
		if(ordersList.size() == 0) {
			return 0;
		}
		
        // stability as computed in SQ 3
        double stabilityTopValue = 0;
        double stabilityLastValue = 0;
        
		int size = ordersList.size();
		double totalPL = 0;
		double maxPL = -10000000;
		double sumPL = 0;
		
		int period = 6; 
		if(period > size/2) {
			period = size/2;
		}		
		for(int i=0; i<size; i++) {
            Order order = ordersList.get(i);

            totalPL += getPLByStatsType(order, combination);
            
        	if(totalPL > maxPL) {
        		maxPL = totalPL;
        	}

   			if(i > (ordersList.size()-1-period)) {
   				sumPL += totalPL;
   			}
		}
	
		// topvalue
		if(maxPL == 0f) {
			stabilityTopValue = 10000f;
		} else {
			stabilityTopValue = maxPL;
		}
		
		
		// last value
		stabilityLastValue = sumPL / ((double) period); 

        if(stabilityLastValue <= 0 || stabilityTopValue <= 0) {
        	
        }
        
    	double topTanA = stabilityLastValue / (((double) size));
        double tenPct = stabilityLastValue * 0.1f;
        
        double tanA = 10000 / ((double) ordersList.size()*10.0);
        double angle = Math.atan(tanA);
        if(angle > Math.PI/2.0) {
        	angle = Math.PI/2.0;
        }         
        
		//--------------------------------
        // start
        double eXY = 0;
        double eX = 0;
        double eX2 = 0;
        double eY = 0;
        double eY2 = 0;
        double N = 0;
        double upper = 0;
        double lowerLeft = 0;
        double lowerRight = 0;
        double stabilitySum = 0;
        totalPL = 0;
        
        for(int i=0; i<ordersList.size(); i++) {
            Order order = ordersList.get(i);

            totalPL += getPLByStatsType(order, combination);
            
            double yVal = ((double) i) * (double) tanA;

            eXY += totalPL*yVal;
            eX += totalPL;
            eX2 += totalPL*totalPL;
            eY += yVal;
            eY2 += yVal*yVal;
            N += 1;

            double yValTop = ((double) i) * (double) topTanA;
            stabilitySum += Math.abs(totalPL - yValTop) / tenPct;

        }

        if(N > 0) {
        	upper = eXY - (eX*eY/N);
        	lowerLeft = eX2 - (eX*eX)/N;
        	lowerRight = eY2 - (eY*eY)/N;
        } else {
        	upper = 0;
        	lowerLeft = 0;
        	lowerRight = 0;
        }

        if(lowerLeft == 0 || lowerRight == 0 || N == 0) {
        	return 0;
        	
        } else {
        	
        	double correlation = upper / ((double) Math.sqrt(lowerLeft*lowerRight)); // correlation in range -1 to 1
        	double stability = Math.abs(stabilitySum / ((double) N)); // compute stability
        	stability = 5f / (5f + stability); // change to 0-1 range, where 1 is the best, 0 is the worst
        	
        	stability = (correlation + 2d*stability) / 3d;
        	if(stability <= 0 || stability >= 1d) {
        		stability = 0.01d;
        	}

        	if(N > 0 && N<100) {
        		// decrease correlation and stability if number of trades is too low
        		double coef = (1f - 100f/(((double) N)+100))*2f;
        		
        		stability *= coef;
        	}

        	return round2(stability);
        } 
	}
	
}