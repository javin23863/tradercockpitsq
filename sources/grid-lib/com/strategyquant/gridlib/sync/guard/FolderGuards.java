package com.strategyquant.gridlib.sync.guard;

import com.strategyquant.gridlib.message.JmsConnectionInfo;
import com.strategyquant.gridlib.message.sync.FolderGuardFactory;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import javax.jms.JMSException;

public class FolderGuards implements AutoCloseable {
   private List<IFolderGuard> guards = new LinkedList<>();
   private String path;
   private JmsConnectionInfo connection;

   public FolderGuards(String var1, JmsConnectionInfo var2) throws JMSException {
      this.path = var1;
      this.connection = var2;
   }

   @Override
   public void close() throws Exception {
      for (IFolderGuard var2 : this.guards) {
         var2.close();
      }
   }

   public void create(String var1, boolean var2) throws IOException, JMSException {
      this.guards.add(FolderGuardFactory.getGuard(var1, this.path + "/" + var1, var2, this.connection));
   }

   public void updateHash() throws IOException, JMSException {
      for (IFolderGuard var2 : this.guards) {
         var2.updateHash();
      }
   }
}
