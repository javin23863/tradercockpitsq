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
package SQ.Columns.Trades;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

public class Type extends TradelistColumn {
    
	public Type() {
		super(L.tsq("Type"), Text);
	}

	@Override
	public Object getValue(Order order) {
		return order.Type;
	}
	
	@Override
	public String getFormattedValue(Object value) {
		byte type = (byte) value;
		
		switch(type){
			case OrderTypes.Any: return L.t("Any");
			case OrderTypes.Balance: return L.t("Balance");
			case OrderTypes.Buy: return L.t("Buy");
			case OrderTypes.BuyLimit: return L.t("BuyLimit");
			case OrderTypes.BuyStop: return L.t("BuyStop");
			case OrderTypes.BuyStopLimit: return L.t("BuyStopLimit");
			case OrderTypes.Deposit: return L.t("Deposit");
			case OrderTypes.Sell: return L.t("Sell");
			case OrderTypes.SellLimit: return L.t("SellLimit");
			case OrderTypes.SellStop: return L.t("SellStop");
			case OrderTypes.SellStopLimit: return L.t("SellStopLimit");
			case OrderTypes.Withdrawal: return L.t("Withdrawal");
			default: return L.t("N/A");
		}
	}
}