package SQ.Internal;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.ExitMethod;
import com.strategyquant.tradinglib.StrategyBase;
import java.lang.reflect.Field;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ActionBlock extends StandardBlock {
   public static final Logger Log = LoggerFactory.getLogger("ValueBlock");

   public abstract void OnAction() throws TradingException;

   public double evaluateBlock() throws TradingException {
      throw new TradingException("This shouldn't be called!");
   }

   public double evaluateBlock(int var1) throws TradingException {
      throw new TradingException("This shouldn't be called!");
   }

   @Override
   protected void initialize(StrategyBase var1, Element var2) throws BlockDefinitionException {
      super.initialize(var1, var2);
      Class var3 = this.getClass();

      for (Field var7 : var3.getFields()) {
         if (var7.getType().toString().equals("class [Lcom.strategyquant.tradinglib.ExitMethod;")) {
            try {
               ExitMethod[] var8 = (ExitMethod[])var7.get(this);
               if (var8 == null) {
                  var8 = new ExitMethod[0];
                  var7.set(this, var8);
               }
            } catch (IllegalArgumentException | IllegalAccessException var9) {
               var9.printStackTrace();
               Log.error("Error initializing ActionBlock", var9);
            }
         }
      }
   }

   public void OnApplyExits() throws TradingException {
   }
}
