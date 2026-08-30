package SQ.PortfolioMasterFitness;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.PlTypes;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.portfolioMaster.PortfolioMasterFitness;

public class NetProfit extends PortfolioMasterFitness {
   public NetProfit() {
      super(L.tsq("Net Profit (Return)"), (byte)1, 0.0, -10000.0, 10000.0);
   }

   public double compute(ResultsGroup var1, byte var2) throws Exception {
      return var1.portfolio().stats((byte)0, (byte)10, var2).getDouble("NetProfit");
   }

   public String print(double var1) throws Exception {
      return PlTypes.printPL(var1, (byte)10);
   }
}
