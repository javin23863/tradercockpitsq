package com.strategyquant.tradinglib.gp;

import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.gridlib.client.GridMessage;
import java.io.Serializable;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GPEvaluationJob<T extends IGPNode> extends GridJob<T> {
   private static final Logger Log = LoggerFactory.getLogger("GPEvaluationJob");
   private IFitnessEvaluator<? super T> evaluator;
   private T candidate;
   private int populationType;
   private boolean sendLastPopulation;

   public GPEvaluationJob(String var1, Map<String, Serializable> var2) {
      super(var1, 1, var2);
      this.evaluator = (IFitnessEvaluator<? super T>)var2.get("evaluator");
      this.candidate = (T)var2.get("candidate");
      this.populationType = (Integer)((Serializable)var2.get("populationType"));
      this.sendLastPopulation = (Boolean)((Serializable)var2.get("sendLastPopulation"));
   }

   public T call() throws Exception {
      return (T)this.evaluator.evaluateCandidate(this.candidate, this.populationType, this, this.sendLastPopulation);
   }

   public void messageReceived(GridMessage var1) {
      if (var1.getMessageID() == 4) {
         this.evaluator.stop();
         this.destroy();
      }
   }

   public void destroy() {
      if (Log.isDebugEnabled()) {
         Log.debug("Job {} destroyed", this.getJobId());
      }

      if (this.evaluator != null) {
         if (Log.isDebugEnabled()) {
            Log.debug(" - Job {} evaluator destroyed", this.getJobId());
         }

         this.evaluator.destroy();
      }
   }
}
