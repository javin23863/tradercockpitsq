package com.strategyquant.tradinglib.gp;

import com.strategyquant.lib.IRandomGenerator;
import java.util.List;

public interface IEvolutionaryOperator<T extends IGPNode> {
   List<T> apply(List<T> var1, IRandomGenerator var2, AbstractFactory<T> var3, int var4, int var5) throws Exception;
}
