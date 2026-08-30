package SQ.Internal;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.Blocks;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.blocks.CustomBlocks;
import java.util.HashMap;
import java.util.List;
import org.jdom2.Element;

public class CBlock extends StandardBlock {
   @Parameter
   public IBlock Contents;
   private Element elBlock;
   private String cbKey;
   private HashMap<String, CBlock.CParamType> cbParams = null;

   public double evaluateBlock(int var1) throws TradingException {
      if (this.Contents == null) {
         Log.error("Cannot evaluate custom block, it has no contents!");
         return 0.0;
      } else if (this.Contents instanceof ComparisonBlock) {
         boolean var3 = ((ComparisonBlock)this.Contents).OnEvaluateComparison();
         return var3 ? 1 : 0;
      } else if (this.Contents instanceof ConditionBlock) {
         boolean var2 = ((ConditionBlock)this.Contents).OnBlockEvaluate();
         return var2 ? 1 : 0;
      } else {
         return this.Contents.evaluateBlock(var1);
      }
   }

   public double evaluateBlock() throws TradingException {
      return this.evaluateBlock(0);
   }

   @Override
   public IBlock newInstance(StrategyBase var1, Element var2) throws BlockDefinitionException {
      CBlock var3 = (CBlock)this.clone(true, var1);
      var3.elBlock = var2.clone();
      var3.initialize(var1, var2);
      if (var1 instanceof XmlStrategy) {
         var3.Strategy = (XmlStrategy)var1;
      }

      return var3;
   }

   private void initializeCBParams(Element var1) {
      this.cbParams = new HashMap<>();
      List var2 = var1.getChildren("Param");

      for (int var3 = 0; var3 < var2.size(); var3++) {
         Element var4 = (Element)var2.get(var3);
         String var5 = var4.getAttributeValue("key");
         CBlock.CParamType var6 = new CBlock.CParamType(var4);
         this.cbParams.put(var5, var6);
      }
   }

   @Override
   protected void parseXml(Element var1) throws BlockDefinitionException {
      this.cbKey = var1.getAttributeValue("key");
      this.initializeCBParams(var1);
      StrategyBase var2 = this.getStrategy();
      Element var3 = CustomBlocks.getElement(this.cbKey);
      if (var3 != null && var2 != null) {
         var3 = var3.clone();
      }

      if (var3 == null) {
         throw new BlockDefinitionException("Custom block '" + this.cbKey + "' not found in map!");
      }

      if (this.elBlock == null) {
         this.elBlock = var3;
      }

      Element var4 = var3.getChild("Contents").getChild("Item");
      if (var4 == null) {
         throw new BlockDefinitionException("Custom block '" + this.cbKey + "' has no contents!");
      }

      this.applyActualParameters(var4);
      String var5 = var4.getAttributeValue("key");
      this.Contents = Blocks.getBlockObject(var5, var2, var4);
   }

   @Override
   public IBlock clone(boolean var1, StrategyBase var2) throws BlockDefinitionException {
      CBlock var3 = (CBlock)super.clone(var1, var2);
      var3.cbKey = this.cbKey;
      if (this.elBlock != null) {
         var3.elBlock = this.elBlock.clone();
      }

      if (this.cbParams != null) {
         var3.cbParams = new HashMap<>(this.cbParams);
      }

      return var3;
   }

   private void applyActualParameters(Element var1) throws BlockDefinitionException {
      if (this.cbParams != null) {
         this.replaceParameterWithValue(var1);
      }
   }

   private void replaceParameterWithValue(Element var1) {
      if (var1.getName().equals("Param")) {
         String var2 = var1.getText();
         CBlock.CParamType var3 = this.cbParams.get(var2);
         if (var3 != null && var3.value != null) {
            var1.setText(var3.value);
            if (var3.variable != null) {
               var1.setAttribute("variable", var3.variable);
            }

            if (var3.variableType != null) {
               var1.setAttribute("variableType", var3.variableType);
               String var4 = var1.getAttributeValue("key");
               if (var3.variableType.equals("data") && var4 != null && var4.equals("#Symbol#")) {
                  int var5 = -1;

                  try {
                     var5 = Integer.parseInt(var3.value);
                  } catch (Exception var7) {
                  }

                  if (var5 >= 0 && this.Strategy != null) {
                     var1.setText(this.Strategy.Symbol);
                  }
               }
            }
         }

         var1.removeAttribute("customParam");
      }

      List var8 = var1.getChildren();

      for (int var9 = 0; var9 < var8.size(); var9++) {
         this.replaceParameterWithValue((Element)var8.get(var9));
      }
   }

   @Override
   public Element getCustomBlockXml(int var1) throws BlockDefinitionException {
      if (this.cbKey == null) {
         throw new BlockDefinitionException("Custom block key doesn't exist!");
      }

      if (var1 == 3) {
         return CustomBlocks.getElement(this.cbKey);
      }

      Element var2;
      if (this.elBlock != null) {
         var2 = this.elBlock;
      } else {
         var2 = CustomBlocks.getElement(this.cbKey);
      }

      if (var1 == 0) {
         return var2.clone();
      }

      var2 = var2.clone();
      var2.removeChild("Contents");
      return var2;
   }

   public CBlock getOppositeBlock() throws BlockDefinitionException {
      if (this.cbKey == null) {
         throw new BlockDefinitionException("Custom block key doesn't exist!");
      }

      Element var1 = null;
      if (this.elBlock == null) {
         this.elBlock = CustomBlocks.getElement(this.cbKey);
         if (this.elBlock == null) {
            throw new BlockDefinitionException("Custom block '" + this.cbKey + "' element not found in map!");
         }

         var1 = this.elBlock;
         this.elBlock = this.elBlock.clone();
      }

      if (var1 == null) {
         var1 = CustomBlocks.getElement(this.cbKey);
      }

      if (var1 == null) {
         throw new BlockDefinitionException("Custom block '" + this.cbKey + "' element not found in map!");
      }

      String var2 = var1.getAttributeValue("oppositeBlockKey");
      if (var2 == null || var2.equals("") || var2.equals("CBlock_null") || var2.equals("null") || var2.equals("CBlock_")) {
         var2 = this.cbKey;
      }

      Element var3 = CustomBlocks.getElement(var2);
      if (var3 == null) {
         throw new BlockDefinitionException("Custom (opposite) block '" + var2 + "' element not found in map!");
      }

      var3 = var3.clone();
      this.applyCBParams(var3);
      this.fixEmptyCBParams(var3);
      CBlock var4 = new CBlock();
      var4.elBlock = var3;
      var4.initialize(this.getStrategy(), var3);
      return var4;
   }

   private void applyCBParams(Element var1) {
      if (this.cbParams != null) {
         List var2 = var1.getChildren("Param");

         for (int var3 = 0; var3 < var2.size(); var3++) {
            Element var4 = (Element)var2.get(var3);
            String var5 = var4.getAttributeValue("key");
            CBlock.CParamType var6 = this.cbParams.get(var5);
            if (var6 != null && var6.value != null) {
               var4.setText(var6.value);
               if (var6.variable != null) {
                  var4.setAttribute("variable", var6.variable);
               }

               if (var6.variableType != null) {
                  var4.setAttribute("variableType", var6.variableType);
               }
            }
         }
      }
   }

   private void fixEmptyCBParams(Element var1) {
      List var2 = var1.getChildren("Param");

      for (int var3 = 0; var3 < var2.size(); var3++) {
         Element var4 = (Element)var2.get(var3);
         String var5 = var4.getText();
         if (var5.isEmpty()) {
            String var6 = var4.getAttributeValue("defaultValue");
            if (var6 != null && !var6.isEmpty()) {
               var4.setText(var6);
            }
         }
      }
   }

   public void applyParamChange(double var1, double var3) {
      if (this.cbParams != null) {
         List var5 = this.elBlock.getChildren("Param");

         for (int var6 = 0; var6 < var5.size(); var6++) {
            Element var7 = (Element)var5.get(var6);
            String var8 = var7.getAttributeValue("key");
            if (!var8.contains("Chart")) {
               CBlock.CParamType var9 = this.cbParams.get(var8);
               if (var9 != null && var9.value != null) {
                  double var10 = Double.parseDouble(var9.value);
                  if (var10 == var1) {
                     var9.value = Double.toString(var3);
                     var7.setText(var9.value);
                     if (var9.variable != null) {
                        var7.setAttribute("variable", var9.variable);
                     }

                     if (var9.variableType != null) {
                        var7.setAttribute("variableType", var9.variableType);
                     }
                  }
               }
            }
         }
      }
   }

   public IBlock getContents() {
      return this.Contents;
   }

   class CParamType {
      String value = null;
      String variable = null;
      String variableType = null;

      public CParamType(Element nullx) {
         this.value = nullx.getText();
         this.variable = nullx.getAttributeValue("variable");
         this.variableType = nullx.getAttributeValue("variableType");
         String var3 = nullx.getAttributeValue("type");
         if (this.variableType == null && var3 != null && var3.equals("data")) {
            this.variableType = "data";
         }
      }
   }
}
