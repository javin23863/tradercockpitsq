package SQ.PortfolioMasterFitness;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.PlTypes;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.portfolioMaster.PortfolioMasterFitness;

public class ReturnDD extends PortfolioMasterFitness {
   public ReturnDD() {
      super(L.tsq("Return / Drawdown ratio"), (byte)1, 0.0, -20.0, 20.0);
   }

   public double compute(ResultsGroup var1, byte var2) throws Exception {
      return var1.portfolio().stats((byte)0, (byte)10, var2).getDouble("ReturnDDRatio");
   }

   public String print(double var1) {
      return PlTypes.printPL(var1, (byte)10);
   }
}
