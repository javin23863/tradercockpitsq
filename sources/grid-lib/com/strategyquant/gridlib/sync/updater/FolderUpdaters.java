package com.strategyquant.gridlib.sync.updater;

import com.strategyquant.gridlib.message.JmsConnectionInfo;
import com.strategyquant.gridlib.message.sync.FolderGuardFactory;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import javax.jms.JMSException;

public class FolderUpdaters implements AutoCloseable {
   private List<IFolderUpdater> updaters = new LinkedList<>();
   private String path;
   private List<IFolderUpdatedListener> listeners = new LinkedList<>();
   private FolderUpdaters.FolderUpdatedListener listener = new FolderUpdaters.FolderUpdatedListener();
   private JmsConnectionInfo connection;

   public FolderUpdaters(String var1, JmsConnectionInfo var2) throws JMSException {
      this.path = var1;
      this.connection = var2;
   }

   public void createUpdater(String var1, boolean var2) throws IOException, JMSException {
      IFolderUpdater var3 = FolderGuardFactory.getUpdater(var1, this.path + "/" + var1, var2, this.connection);
      var3.addFolderUpdatedListener(this.listener);
      this.updaters.add(var3);
   }

   public void addFolderUpdatedListener(IFolderUpdatedListener var1) {
      this.listeners.add(var1);
   }

   @Override
   public void close() throws Exception {
      for (IFolderUpdater var2 : this.updaters) {
         var2.close();
      }
   }

   public boolean isDirty() {
      for (IFolderUpdater var2 : this.updaters) {
         if (var2.isDirty()) {
            return true;
         }
      }

      return false;
   }

   private class FolderUpdatedListener implements IFolderUpdatedListener {
      private FolderUpdatedListener() {
      }

      @Override
      public void onUpdated() {
         if (!FolderUpdaters.this.isDirty()) {
            for (IFolderUpdatedListener var2 : FolderUpdaters.this.listeners) {
               var2.onUpdated();
            }
         }
      }
   }
}
