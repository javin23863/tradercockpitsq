package com.strategyquant.tradinglib.gp;

import com.strategyquant.lib.IRandomGenerator;
import java.io.Serializable;
import org.jdom2.Element;

public abstract class AbstractFactory<T extends IGPNode> implements Serializable {
   public abstract T generateRandomCandidate(IRandomGenerator var1) throws Exception;

   public abstract AbstractFactory<T> clone();

   public abstract void destroy();

   public abstract Element mutateATM(Element var1, IRandomGenerator var2) throws Exception;
}
