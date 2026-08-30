package com.strategyquant.wizard.desktop.loader;

import com.strategyquant.lib.utils.SQFileChooserFactory;
import com.strategyquant.wizard.desktop.indicators.CustomIndicatorFileImporter;
import com.strategyquant.wizard.desktop.settings.Settings;
import com.strategyquant.wizard.desktop.utils.FileUtils;
import java.awt.Component;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

public class CustomIndicatorLoader {
   private Component parent;

   public CustomIndicatorLoader(Component var1) {
      this.parent = var1;
   }

   public String loadFile(String var1) {
      File var2 = new File(FileUtils.getAppFolder() + "/" + "config" + "/" + var1);
      if (!var2.exists()) {
         return null;
      }

      CustomIndicatorFileImporter var3 = new CustomIndicatorFileImporter(this.parent);
      return var3.load(var2);
   }

   public String loadFile() {
      String var1 = Settings.get().get("indicatorFolder");
      JFileChooser var2 = SQFileChooserFactory.createPersistingJFileChooser(var1.isEmpty() ? null : var1).getJFileChooser();
      FileNameExtensionFilter var3 = new FileNameExtensionFilter("MQ4", "*.mq4");
      var2.setFileFilter(var3);
      int var4 = var2.showOpenDialog(this.parent);
      if (var4 == 0) {
         File var5 = var2.getSelectedFile();
         Settings.get().set("indicatorFolder", var5.getParent());
         CustomIndicatorFileImporter var6 = new CustomIndicatorFileImporter(this.parent);
         return var6.load(var5);
      } else {
         return null;
      }
   }

   public void dispose() {
      this.parent = null;
   }
}
