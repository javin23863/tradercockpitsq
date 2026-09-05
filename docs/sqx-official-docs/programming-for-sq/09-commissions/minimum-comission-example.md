# Minimum Commission Example

- Source: <https://strategyquant.com/doc/programming-for-sq/minimum-comission-example/>
- Section: Programming for StrategyQuant X › Commissions
- Fetched: 2026-09-04

---

This is simple method of commissions computation – it uses commissions in $ per lot and multiplies it by actual trade size. If final commission is below treshold then minimum commission is used. You can download snippet [here](https://strategyquant.com/codebase/size-based-minimum-comission/).

## Step 1 – Clone new Trade column

Open CodeEditor, in the Navigator under Builtin/Snippets/Trading/Commissions, find the SizeBased snippet, right-click it, and clone it. In the User/Snippets/Trading/Commission folder, you will see the cloned snippet that you can edit.

[![](https://strategyquant.com/wp-content/uploads/2021/12/1-1-750x402.jpg)](https://strategyquant.com/wp-content/uploads/2021/12/1-1-750x402.jpg)

## Step 2 – Define Parameters

At this point you need to specify the parameters to be used for the calculation. In our case, we specify a commission for the entire lot and a minimum commission in case the amount of the commission is less than the specified minimum.

```
@ClassConfig(name="Size Based Comission with Minimum Value", display="#Commission# with Minimum #MinimumComission#")
@Help("<b>Comission per Trade with Minimum value</b><br/>Used mainly for stocks, or Forex.")
public class MinimumComission extends CommissionsMethod {

    @Parameter(defaultValue="0", minValue=-100000d, name="Commission", maxValue=100000d, step=1d, category="Default")
    @Help("Commission in $ per complete lot")
    public double Commission;

    @Parameter(defaultValue="0.5", minValue=0, name="MinimumComission", maxValue=100000d, step=1d, category="Default")
    @Help("Minimum Commission in $ per Trade")
    public double MinimumComission;
```

## Step 3 – Implement computeCommissionsOnOpen method

At this point you have two choices. Either calculate the commission when you open a trade, using the computeCommissionsOnOpen method, or calculate the commission when you close a trade, using the computeCommissionsOnClose method. Both methods have as arguments: ILiveOrder order , double tickSize, double pointValue.

In our case, we use the first option and modify this method to compute the minimum commission when the computed commission is lower than the minimum we set.

```
    @Override
    public double computeCommissionsOnOpen(ILiveOrder order, double tickSize, double pointValue) throws Exception {
        // commission is in $ per full lot
        double result = 0;
        double comission  = Commission * order.getSize();
        if(comission<MinimumComission){result = MinimumComission;}
        else result = Commission * order.getSize();
        
        return result; 
    }
    //------------------------------------------------------------------------

    @Override
    public double computeCommissionsOnClose(ILiveOrder order, double tickSize, double pointValue) throws Exception {
        return 0;
    }
    
}
```

## Step 4 – Compile snippet and using it

After restarting StrategyQuant X, we see the new method for calculating commissions in the Data section.

[![](https://strategyquant.com/wp-content/uploads/2021/12/2-750x403.jpg)](https://strategyquant.com/wp-content/uploads/2021/12/2-750x403.jpg)

In the figure below, you can see that the commission per lot should actually be $10, but since we set the minimum commission above, the final commission is $20 per trade.

[![](https://strategyquant.com/wp-content/uploads/2021/12/3-750x393.png)](https://strategyquant.com/wp-content/uploads/2021/12/3-750x393.png)

## Full code of the snippet

```
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
package SQ.Trading.Commissions;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@ClassConfig(name="Size Based Comission with Minimum Value", display="$ #Commission# per trade")
@Help("<b>Comission per Trade with Minimum value</b><br/>Used mainly for stocks, or Forex.")
public class MinimumComission extends CommissionsMethod {

    @Parameter(defaultValue="0", minValue=-100000d, name="Commission", maxValue=100000d, step=1d, category="Default")
    @Help("Commission in $ per complete lot")
    public double Commission;

    @Parameter(defaultValue="0.5", minValue=0, name="Minimum Comission", maxValue=100000d, step=1d, category="Default")
    @Help("Minimum Commission in $ per Trade")
    public double MinimumComission;

    //------------------------------------------------------------------------
    //------------------------------------------------------------------------
    //------------------------------------------------------------------------
    
    @Override
    public double computeCommissionsOnOpen(ILiveOrder order, double tickSize, double pointValue) throws Exception {
        // commission is in $ per full lot
        double result = 0;
        double comission  = Commission * order.getSize();
        if(comission<MinimumComission){result = MinimumComission;}
        else result = Commission * order.getSize();
        
        return result; 
    }
    //------------------------------------------------------------------------

    @Override
    public double computeCommissionsOnClose(ILiveOrder order, double tickSize, double pointValue) throws Exception {
        return 0;
    }
    
}
```
