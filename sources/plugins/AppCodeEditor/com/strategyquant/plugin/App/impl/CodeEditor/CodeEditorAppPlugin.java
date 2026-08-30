package com.strategyquant.plugin.App.impl.CodeEditor;

import com.strategyquant.lib.L;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.hw.OperatingSystem;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.pluginlib.program.IProgram;
import com.strategyquant.pluginlib.program.Program;
import com.strategyquant.tradinglib.plugindef.app.IAppPlugin;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Author(name = "Mark Fric")
@Name(name = "Trader")
@Category(name = "App")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class CodeEditorAppPlugin implements IAppPlugin, IProgram {
   public static final Logger Log = LoggerFactory.getLogger(CodeEditorAppPlugin.class);

   public String getName() {
      return "CodeEditor";
   }

   public String getProduct() {
      return "SQUANT";
   }

   public int getPreferredPosition() {
      return 2;
   }

   public void initPlugin() throws Exception {
      Program.register("CodeEditor", this);
   }

   public String getContextPath() {
      return "/editor";
   }

   public String getAppCode() {
      return "SQEDITOR";
   }

   public String getTooltip() {
      return "CodeEditor";
   }

   public String getProject() {
      return null;
   }

   public String getDefaultTaskType() {
      return null;
   }

   public String getDefaultTaskName() {
      return null;
   }

   public Object call(String var1, Object... var2) throws Exception {
      if (var1.equals("open")) {
         (new Thread() {
            @Override
            public void run() {
               try {
                  String[] var1x = null;
                  OperatingSystem var2x = new OperatingSystem();
                  if (var2x.isWindows()) {
                     var1x = new String[]{"cmd", "/c", "start", "\"\"", MainApp.getDataPath() + "CodeEditor.exe"};
                  } else if (var2x.isMac()) {
                     var1x = new String[]{"./CodeEditor"};
                  } else {
                     if (!var2x.isUnix()) {
                        throw new Exception(L.t("Operating system not recognized", new Object[0]));
                     }

                     var1x = new String[]{"./CodeEditor"};
                  }

                  Runtime.getRuntime().exec(var1x);
               } catch (Exception var3) {
                  CodeEditorAppPlugin.Log.error("Failed to open CodeEditor.", var3);
               }
            }
         }).start();
      }

      return null;
   }
}
