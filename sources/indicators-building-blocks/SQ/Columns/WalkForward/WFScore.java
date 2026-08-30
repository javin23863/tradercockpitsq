package SQ.Columns.WalkForward;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.WalkForwardColumn;
import com.strategyquant.tradinglib.WalkForwardResult;

public class WFScore extends WalkForwardColumn {
   public WFScore() {
      super("WFScore", L.tsq("WF Score"), "Decimal2Pct", (byte)1);
   }

   public double compute(WalkForwardResult var1) {
      return var1.scorePerc;
   }
}
