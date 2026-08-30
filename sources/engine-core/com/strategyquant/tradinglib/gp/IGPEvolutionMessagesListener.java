package com.strategyquant.tradinglib.gp;

import java.io.Serializable;

public interface IGPEvolutionMessagesListener<T extends IGPNode> {
   void newCandidate(T var1);

   void evolutionMessage(GPEvolutionMessage var1);

   void lastPopulation(GPEvolutionPopulationMessage var1);

   void evolutionException(Serializable var1);
}
