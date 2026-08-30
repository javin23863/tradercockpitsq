package com.strategyquant.tradinglib.blocks.random;

import com.strategyquant.tradinglib.generator.GenerateException;
import java.util.List;
import org.jdom2.Element;

public class FormulaValue {
   public String key;
   public int weight;
   public boolean isNoneValue;
   public ReplacementParameterGroups parameters = new ReplacementParameterGroups();
   public BlockDefinition blockDef;
   public double exitBasedType = 0.0;

   public FormulaValue(ReplacementsConfig var1, Element var2, BlocksConfig var3, ReplacementConfig var4) throws GenerateException {
      this.key = var2.getAttributeValue("key");
      String var5 = var2.getAttributeValue("weight");
      this.weight = var5 == null ? 1 : Integer.parseInt(var5);
      this.isNoneValue = var2.getName().equals("ValueNone");
      this.blockDef = var3.getFormula(this.key);
      if (var2.getChild("Generated") != null) {
         ReplacementParameters var6 = new ReplacementParameters((byte)1, var1, var3, this.blockDef, var4, var2.getChild("Generated"), null);
         this.parameters.add(var6);
      }

      if (var2.getChild("Predefined") != null) {
         List var10 = var2.getChild("Predefined").getChildren();

         for (int var7 = 0; var7 < var10.size(); var7++) {
            Element var8 = (Element)var10.get(var7);
            ReplacementParameters var9 = new ReplacementParameters((byte)2, var1, var3, this.blockDef, var4, var8, null);
            this.parameters.add(var9);
         }
      }

      if (this.key.contains("ATRBasedValue")) {
         this.exitBasedType = -1.0;
      } else if (this.key.contains("FixedValue")) {
         this.exitBasedType = 1.0;
      }
   }
}
