package com.strategyquant.gridlib.sync.updater;

import com.strategyquant.gridlib.FileHelper;
import com.strategyquant.gridlib.message.JmsConnectionInfo;
import java.io.File;
import java.io.IOException;
import javax.jms.JMSException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;

public class SolidFolderUpdater extends AbstractFolderUpdater {
   private String hash;

   public SolidFolderUpdater(String var1, String var2, JmsConnectionInfo var3) throws IOException, JMSException {
      super(var1, var2, var3);
   }

   @Override
   public void updateHash() throws IOException {
      this.hash = FileHelper.countHash(this.getFolder());
      this.hash = DigestUtils.md5Hex(this.hash);
   }

   @Override
   public HandleHashResult hashChanged(String var1) {
      boolean var2 = !var1.equals(this.hash);
      if (var2) {
         this.setDirty(true);
      }

      return new HandleHashResult(var2, null);
   }

   @Override
   protected void prepareForNewData() throws IOException {
      super.prepareForNewData();
      FileUtils.cleanDirectory(new File(this.getFolder()));
   }
}
