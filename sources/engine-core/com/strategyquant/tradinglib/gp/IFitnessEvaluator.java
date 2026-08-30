package com.strategyquant.tradinglib.gp;

import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.lib.utils.ISQCloneable;
import java.io.Serializable;

public interface IFitnessEvaluator<T extends IGPNode> extends Serializable, ISQCloneable<IFitnessEvaluator<? super T>> {
   T evaluateCandidate(T var1, int var2, GridJob var3, boolean var4);

   void destroy();

   void stop();
}
