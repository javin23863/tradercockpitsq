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
package SQ.Functions;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;

public class StatFunctions {

	/**
	 * computes average of values in given array.
	 * If to parameter is set to -1 it will use whole array
	 * 
	 * @param values
	 * @param from
	 * @param to
	 * @return
	 */
	public static double computeAverage(DoubleArrayList values) {
		return StatFunctions.computeAverage(values, 0, -1);
	}

	public static double computeAverage(DoubleArrayList values, int from, int to) {
		if(values == null || values.size() == 0) {
			return 0;
		}
		
		double sumPL = 0, pl = 0;
        
		if(to == -1) {
			to = values.size();
		}
        
    	for(int i=from; i<to; i++) {
        	pl = values.getDouble(i);
        	sumPL += pl;
    	}
    	
        return SQUtils.safeDivide(sumPL, to - from);
		
	}

	
	public static double computeStdev(double mean, DoubleArrayList values) {
		return StatFunctions.computeStdev(mean, values, 0, -1);
	}

	public static double computeStdev(double mean, DoubleArrayList values, int from, int to) {
		if(values == null || values.size() == 0) {
			return 0;
		}
    	
		if(to == -1) {
			to = values.size();
		}
    	
    	double sum = 0, stdev, pl;
    	for (int i = from; i < to; i++) {
    		pl = values.getDouble(i);
    		sum += Math.pow((pl - mean), 2d);
    	}
    	stdev = (double) Math.sqrt(sum / ((double) (to - from)));
    	return (stdev);
	}	
}    