package com.strategyquant.tradinglib.gp;

import com.strategyquant.gridlib.client.JobDetails;
import java.io.Serializable;

public interface IGPNode extends Serializable {
   void setFitness(double var1, byte var3);

   double getFitness(byte var1);

   void destroy();

   void setGPIDs(GPIDs var1);

   GPIDs getGPIDs();

   IGPNode clonePassed();

   IGPNode cloneDismissed(int var1);

   IGPNode cloneFull();

   IGPNode cloneForMigration();

   int getDismissalReason();

   boolean passedEvaluation();

   void setJobDetails(JobDetails var1);

   JobDetails getJobDetails();

   int getHash();

   String getAsString();

   void setFitness(IGPNode var1);

   int getFingerprint();

   Exception getException();

   String getErrorMsg();
}
