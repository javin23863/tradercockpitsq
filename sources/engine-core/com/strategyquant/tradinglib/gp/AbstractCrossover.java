package com.strategyquant.tradinglib.gp;

import com.strategyquant.lib.IRandomGenerator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractCrossover<T extends IGPNode> implements IEvolutionaryOperator<T> {
   public static final Logger Log = LoggerFactory.getLogger("AbstractCrossover");
   private final int crossoverPoints;
   private final double crossoverProbability;

   protected AbstractCrossover(int var1) {
      this(var1, 1.0);
   }

   protected AbstractCrossover(int var1, double var2) {
      this.crossoverPoints = var1;
      this.crossoverProbability = var2;
      if (var1 <= 0) {
         throw new IllegalArgumentException("Number of cross-over points must be positive.");
      }
   }

   @Override
   public List<T> apply(List<T> var1, IRandomGenerator var2, AbstractFactory<T> var3, int var4, int var5) {
      ArrayList var6 = new ArrayList(var1);
      Collections.shuffle(var6, var2.getRandom());
      ArrayList var7 = new ArrayList(var1.size());
      Iterator var8 = var6.iterator();

      while (var8.hasNext()) {
         IGPNode var9 = (IGPNode)var8.next();
         if (var8.hasNext()) {
            IGPNode var10 = (IGPNode)var8.next();
            int var11;
            if (var2.probability(this.crossoverProbability)) {
               if (this.crossoverPoints == 1) {
                  var11 = 1;
               } else {
                  var11 = 1 + var2.nextInt(this.crossoverPoints);
               }
            } else {
               var11 = 0;
            }

            if (var11 > 0) {
               var7.addAll(this.mate((T)var9, (T)var10, var11, var2, var4, var5));
            } else {
               var7.add(var9);
               var7.add(var10);
            }
         } else {
            var7.add(var9);
         }
      }

      return var7;
   }

   protected abstract List<T> mate(T var1, T var2, int var3, IRandomGenerator var4, int var5, int var6);
}
