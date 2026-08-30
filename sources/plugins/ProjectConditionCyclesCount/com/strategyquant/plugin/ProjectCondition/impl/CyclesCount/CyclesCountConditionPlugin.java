package com.strategyquant.plugin.ProjectCondition.impl.CyclesCount;

import com.strategyquant.lib.L;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.conditions.Condition;
import com.strategyquant.tradinglib.project.IProjectCondition;
import com.strategyquant.tradinglib.project.ProjectConditionField;
import com.strategyquant.tradinglib.project.SQProject;
import com.strategyquant.tradinglib.taskImpl.ISQTask;
import java.util.List;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Author(name = "Tomas Brynda")
@Name(name = "CyclesCount Condition plugin")
@Category(name = "Project Condition")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class CyclesCountConditionPlugin implements IProjectCondition {
   public static final Logger Log = LoggerFactory.getLogger("CyclesCountConditionPlugin");
   private static final ProjectConditionField[] fields = new ProjectConditionField[]{
      new ProjectConditionField("comparator", "Comparator", "<"), new ProjectConditionField("number", "Value", "100")
   };
   private String comparator;
   private int value;
   private String lastCheckedValue = null;

   public String getName() {
      return "CyclesCount";
   }

   public String getTitle() {
      return L.tsq("Number of cycles (Project)");
   }

   public String getDescription() {
      return String.format(this.getDescriptionFormat(), this.comparator, this.value);
   }

   public String getDescriptionFormat() {
      return L.t("Number of cycles (Project) %s %d", new Object[0]);
   }

   public String getLastCheckedValue() {
      return this.lastCheckedValue;
   }

   public ProjectConditionField[] getFields() {
      return fields;
   }

   public void readSettings(Element var1) throws Exception {
      List var2 = var1.getChildren("Field");

      for (int var3 = 0; var3 < var2.size(); var3++) {
         Element var4 = (Element)var2.get(var3);
         String var5 = var4.getAttributeValue("type");
         if (var5 != null) {
            switch (var5) {
               case "comparator":
                  this.comparator = var4.getText();
                  break;
               case "number":
                  this.value = Integer.parseInt(var4.getText());
            }
         }
      }
   }

   public boolean isMet(SQProject var1, ISQTask var2) throws Exception {
      int var3 = var1.getRunCycles();
      this.lastCheckedValue = var3 + "";
      return Condition.validate(var3 - this.value, this.comparator);
   }

   public String getProduct() {
      return "SQUANT";
   }

   public int getPreferredPosition() {
      return 0;
   }

   public void initPlugin() throws Exception {
   }

   public IProjectCondition clone() {
      CyclesCountConditionPlugin var1 = new CyclesCountConditionPlugin();
      var1.comparator = this.comparator;
      var1.value = this.value;
      return var1;
   }
}
