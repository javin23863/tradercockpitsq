package SQ.ExitMethods;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.ClassConfig;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.ExitMethod;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.IActionEventListener;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.IFormula;
import com.strategyquant.tradinglib.ILiveOrder;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.exit.TrailingStopHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ClassConfig(name = "Trailing Stop")
@SortOrder(400)
@ForEngine("*,-SP,-SA")
public class TrailingStop extends ExitMethod {
   public static final Logger Log = LoggerFactory.getLogger("TrailingStop");
   @Parameter(name = "Trailing Stop", category = "Advanced", showIfDefault = false)
   @Editor(type = 200, formulaName = "RangeLevel")
   public IFormula TrailingStop;
   @Parameter(name = "TS Activation Level", category = "Advanced", showIfDefault = false)
   @Editor(type = 200, formulaName = "Range")
   public IFormula TrailingActivation;
   private double trailingSL = -9.9999999E7;
   private boolean debugTrailingStops = false;

   public void setForOrder(final ILiveOrder var1, final StrategyBase var2) throws TradingException {
      if (this.TrailingStop != null && !this.TrailingStop.isNoneValue()) {
         this.trailingSL = -9.9999999E7;
         var1.registerEvent(
            2,
            new IActionEventListener() {
               public void OnActionEvent(StrategyBase var1x) throws TradingException {
                  if (var1.isMarketOpen()) {
                     TrailingStop.this.trailingSL = TrailingStopHandler.checkTrailingStop(
                        var1,
                        TrailingStop.this.TrailingStop,
                        TrailingStop.this.TrailingActivation,
                        var2,
                        TrailingStop.this.trailingSL,
                        TrailingStop.this.debugTrailingStops,
                        (byte)-1
                     );
                  }
               }
            }
         );
      }
   }

   public double computeValue(byte var1, StrategyBase var2, String var3, double var4) throws TradingException {
      throw new TradingException("This method shouldn't be called!");
   }

   public boolean setExit(ILiveOrder var1, StrategyBase var2) throws TradingException {
      return TrailingStopHandler.setExit(var1, var2, this.trailingSL, this.debugTrailingStops);
   }

   public IBlock clone(boolean var1, StrategyBase var2) throws BlockDefinitionException {
      TrailingStop var3 = new TrailingStop();
      var3.TrailingStop = this.TrailingStop;
      var3.TrailingActivation = this.TrailingActivation;
      return var3;
   }
}
