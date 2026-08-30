package com.strategyquant.tradinglib.gp;

import com.strategyquant.lib.IRandomGenerator;
import java.util.List;

public interface ISelectionStrategy<T extends IGPNode> {
   <T extends IGPNode> List<T> select(List<T> var1, boolean var2, int var3, IRandomGenerator var4, List<T> var5);
}
