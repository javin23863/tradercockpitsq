package com.strategyquant.plugin.Task.impl.NeuralNetworkTrainer;

import com.strategyquant.lib.L;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.project.ProgressEngine;
import com.strategyquant.tradinglib.taskImpl.AbstractTask;
import com.strategyquant.tradinglib.taskImpl.ISQTask;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;

@Author(name = "Tamas Takacs")
@Name(name = "Neural network trainer plugin")
@Category(name = "Task")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class NeuralNetworkTrainerTask extends AbstractTask {
   public NeuralNetworkTrainerTask() throws Exception {
      super(null, null);
   }

   public NeuralNetworkTrainerTask(String var1, ProgressEngine var2) throws Exception {
      super(var1, var2);
   }

   public String getType() {
      return "NeuralNetworkTrainer";
   }

   public String getName() {
      return L.tsq("Neural Network Trainer");
   }

   public ISQTask clone(String var1, ProgressEngine var2) throws Exception {
      NeuralNetworkTrainerTask var3 = new NeuralNetworkTrainerTask(var1, var2);
      var3.settingTabPlugins = this.settingTabPlugins;
      return var3;
   }

   public void start() throws Exception {
   }

   protected int getRunningStatus() {
      return 0;
   }

   public String getPluginFolderName() {
      return "TaskNeuralNetworkTrainer";
   }

   public int getPreferredPosition() {
      return 5;
   }

   public String[] getSettings() {
      return new String[]{"Data", "Options", "Rankings", "Notes"};
   }

   protected Databank[] getUsedDatabanks() {
      return null;
   }

   protected Databank getOutputDatabank() {
      return null;
   }

   public String getProduct() {
      return "SQUANT";
   }
}
