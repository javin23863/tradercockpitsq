package com.strategyquant.plugin.Task.impl.AutomaticPortfolioBuilder.genetic;

import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.uncommons.util.concurrent.ConfigurableThreadFactory;
import org.uncommons.util.id.IDSource;
import org.uncommons.util.id.IntSequenceIDSource;
import org.uncommons.util.id.StringPrefixIDSource;
import org.uncommons.watchmaker.framework.EvaluatedCandidate;

public class FitnessEvaluationWorker {
   private static final IDSource<String> WORKER_ID_SOURCE = new StringPrefixIDSource("FitnessEvaluationWorker", new IntSequenceIDSource());
   private final LinkedBlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>();
   private final ThreadPoolExecutor executor;

   FitnessEvaluationWorker() {
      this(true);
   }

   private FitnessEvaluationWorker(boolean var1) {
      ConfigurableThreadFactory var2 = new ConfigurableThreadFactory((String)WORKER_ID_SOURCE.nextID(), 5, var1);
      this.executor = new ThreadPoolExecutor(
         Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().availableProcessors(), 60L, TimeUnit.SECONDS, this.workQueue, var2
      );
      this.executor.prestartAllCoreThreads();
   }

   public <T> Future<EvaluatedCandidate<T>> submit(FitnessEvalutationTask<T> var1) {
      return this.executor.submit(var1);
   }

   public static void main(String[] var0) {
      new FitnessEvaluationWorker(false);
   }

   @Override
   protected void finalize() throws Throwable {
      this.executor.shutdown();
      super.finalize();
   }
}
