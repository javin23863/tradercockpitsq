package com.strategyquant.plugin.App.impl.NeuralNetwork;

import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.plugindef.app.IAppPlugin;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Author(name = "Tomas Brynda")
@Name(name = "Neural Network Trainer")
@Category(name = "App")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class NeuralNetworkAppPlugin implements IAppPlugin {
   public static final Logger Log = LoggerFactory.getLogger(NeuralNetworkAppPlugin.class);

   public String getName() {
      return "Neural Network Trainer";
   }

   public String getProduct() {
      return "SQUANT";
   }

   public int getPreferredPosition() {
      return 10;
   }

   public void initPlugin() throws Exception {
   }

   public String getContextPath() {
      return "/NEURALNETWORK";
   }

   public String getAppCode() {
      return "NEURALNETWORK";
   }

   public String getTooltip() {
      return "Neural Network Trainer";
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
}
