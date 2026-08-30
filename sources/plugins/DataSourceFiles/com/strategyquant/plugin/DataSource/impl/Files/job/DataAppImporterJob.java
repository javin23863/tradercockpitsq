package com.strategyquant.plugin.DataSource.impl.Files.job;

import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.gridlib.client.GridMessage;
import com.strategyquant.tradinglib.data.AppDataImporter;

public class DataAppImporterJob extends GridJob<Void> {
   private AppDataImporter importer;

   public DataAppImporterJob(String var1, String var2, String var3) {
      super(var1, 0, null);
      this.importer = new AppDataImporter(var2, var3);
   }

   public Void call() throws Exception {
      this.importer.start();
      return null;
   }

   public String validate() {
      return this.importer.validate();
   }

   public void messageReceived(GridMessage var1) {
      super.messageReceived(var1);
      switch (var1.getMessageID()) {
         case 4:
            this.importer.cancel();
      }
   }
}
