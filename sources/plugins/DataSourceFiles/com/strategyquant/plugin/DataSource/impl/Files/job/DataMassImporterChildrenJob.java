package com.strategyquant.plugin.DataSource.impl.Files.job;

import com.strategyquant.datalib.data.io.MassImportDataInfo;
import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.gridlib.client.GridMessage;
import java.io.File;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataMassImporterChildrenJob extends GridJob<String> {
   public static final Logger Log = LoggerFactory.getLogger(DataMassImporterChildrenJob.class);
   private static final long serialVersionUID = 1L;
   private DataMassImporter importer;
   private File file;

   public DataMassImporterChildrenJob(String var1, File var2, MassImportDataInfo var3, Set<String> var4) {
      super(var1, 0, null);
      this.file = var2;
      this.importer = new DataMassImporter(var3, var2, var4);
   }

   public String call() throws Exception {
      try {
         return this.importer.performImport();
      } catch (Exception var2) {
         Log.error("Error while importing file: " + this.file.getName(), var2);
         throw var2;
      }
   }

   public void messageReceived(GridMessage var1) {
      super.messageReceived(var1);
      switch (var1.getMessageID()) {
         case 4:
            this.importer.cancel();
      }
   }
}
