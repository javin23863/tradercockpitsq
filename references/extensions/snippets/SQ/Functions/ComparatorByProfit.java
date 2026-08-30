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

import java.util.Comparator;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

public class ComparatorByProfit implements Comparator<Order> {

	private byte type = PlTypes.Money;

    public ComparatorByProfit(byte type) {
        this.type = type;
    }

    public int compare(Order order1, Order order2) {
        if(type == PlTypes.Pips) {	
            if(order1.PipsPL > order2.PipsPL) return -1;
            if(order1.PipsPL < order2.PipsPL) return 1;
        } else if(type == PlTypes.Percent) {	
			if(order1.PctPL > order2.PctPL) return -1;
			if(order1.PctPL < order2.PctPL) return 1;
		} else {
			if(order1.PL > order2.PL) return -1;
			if(order1.PL < order2.PL) return 1;			
		}
	
		return 0;
	}
}    