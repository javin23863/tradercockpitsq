package com.strategyquant.gridlib.message.sync;

import com.strategyquant.gridlib.message.JmsConnectionInfo;
import com.strategyquant.gridlib.sync.guard.CommonFolderGuard;
import com.strategyquant.gridlib.sync.guard.IFolderGuard;
import com.strategyquant.gridlib.sync.updater.IFolderUpdater;
import com.strategyquant.gridlib.sync.updater.SingleFolderUpdater;
import com.strategyquant.gridlib.sync.updater.SolidFolderUpdater;
import java.io.IOException;
import javax.jms.JMSException;

public class FolderGuardFactory {
   public static IFolderGuard getGuard(String var0, String var1, boolean var2, JmsConnectionInfo var3) throws IOException, JMSException {
      return new CommonFolderGuard(var0, var1, var3, var2);
   }

   public static IFolderUpdater getUpdater(String var0, String var1, boolean var2, JmsConnectionInfo var3) throws IOException, JMSException {
      return var2 ? new SolidFolderUpdater(var0, var1, var3) : new SingleFolderUpdater(var0, var1, var3);
   }
}
