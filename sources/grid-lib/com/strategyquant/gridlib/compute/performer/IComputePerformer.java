package com.strategyquant.gridlib.compute.performer;

import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.gridlib.client.GridMessage;
import com.strategyquant.gridlib.compute.common.ExecuteOptions;
import com.strategyquant.gridlib.topology.GridTopology;

public interface IComputePerformer extends AutoCloseable {
   void addJobFinishedHandler(IJobStatusChangedHandler var1);

   void execute(GridJob<?> var1, String var2, ExecuteOptions var3) throws Exception;

   void commit() throws Exception;

   void pause(String var1) throws Exception;

   void pause(String var1, String var2) throws Exception;

   void stop(String var1) throws Exception;

   void stop(String var1, String var2) throws Exception;

   void restart(String var1) throws Exception;

   void restart(String var1, String var2) throws Exception;

   boolean sendMessageToJob(String var1, String var2, GridMessage var3) throws Exception;

   GridTopology getGridTopology();

   String getGridDescriptions();

   void setProgress(String var1, String var2, int var3);

   void setGroupRestrictions(String var1, int var2);

   void check();

   void setJobWaiting(String var1, String var2, boolean var3);

   int getComputedThread();

   int getUsedComputedThreads();

   void setUsedComputedThreads(int var1);
}
