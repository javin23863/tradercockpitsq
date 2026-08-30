package SQ.Internal;

import SQ.Blocks.Comparisons.AND;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.Blocks;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.debug.Debugger;
import com.strategyquant.tradinglib.strategy.xml.XmlStrategyException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import org.jdom2.Element;

public abstract class Rule extends Debugger {
   public XmlStrategy Strategy = null;
   private HashMap<String, ArrayList<IBlock>> blocksMap = new HashMap<>();
   protected int barEventType;

   public abstract void evaluateRule(int var1, ITradingOptionsEvaluator var2, String var3) throws Exception;

   protected double evaluateActions(String var1) throws TradingException {
      double var2 = 0.0;
      if (this.blocksMap.containsKey(var1)) {
         for (IBlock var5 : this.blocksMap.get(var1)) {
            if (var5 instanceof ActionBlock var6) {
               var6.OnAction();
            }
         }
      }

      return var2;
   }

   protected ArrayList<IBlock> getBlocks(String var1) {
      return this.blocksMap.containsKey(var1) ? this.blocksMap.get(var1) : null;
   }

   public Rule newInstance(XmlStrategy var1, Element var2) throws XmlStrategyException {
      try {
         Constructor var3 = null;
         var3 = this.getClass().getConstructor();
         Rule var4 = (Rule)SQUtils.invokeUnchecked(var3, new Object[0]);
         var4.initialize(var1, var2);
         return var4;
      } catch (NoSuchMethodException | SecurityException var5) {
         throw new XmlStrategyException("Exception cloning block! " + var5.getMessage(), var5);
      }
   }

   private void initialize(XmlStrategy var1, Element var2) throws XmlStrategyException {
      this.Strategy = var1;
      this.barEventType = this.Strategy.getBarEventType();
      String var3 = "";

      try {
         var3 = XMLUtil.getStringAttr(var2, "name", "???");
         this.parseXml(var2);
      } catch (BlockDefinitionException var5) {
         throw new XmlStrategyException(String.format("Cannot create strategy from XML! Error while parsing rule '%s' - %s ", var3, var5.getMessage()), var5);
      }
   }

   public void deinitialize() {
      if (this.blocksMap != null && !this.blocksMap.isEmpty()) {
         for (String var2 : this.blocksMap.keySet()) {
            ArrayList var3 = this.blocksMap.get(var2);

            for (int var4 = 0; var4 < var3.size(); var4++) {
               IBlock var5 = (IBlock)var3.get(var4);
               if (var5 instanceof StandardBlock) {
                  ((StandardBlock)var5).deinitialize();
               }
            }
         }
      }
   }

   protected void parseXml(Element var1) throws BlockDefinitionException {
      for (Element var3 : var1.getChildren()) {
         if (!var3.getName().contains("Description")) {
            this.parseBlocksInRulePart(var3);
         }
      }
   }

   private void parseBlocksInRulePart(Element var1) throws BlockDefinitionException {
      ArrayList var2 = new ArrayList();

      for (Element var4 : var1.getChildren()) {
         if (!var4.getName().equals("Item")) {
            throw new BlockDefinitionException("Block has an unallowed name '" + var4.getName() + "'");
         }

         IBlock var5 = Blocks.getBlockObject(var4.getAttributeValue("key"), this.Strategy, var4);
         BuildingBlock var6 = var5.getClass().getAnnotation(BuildingBlock.class);
         if (var6 != null) {
            var5.setReturnType(var6.returnType());
         }

         var2.add(var5);
      }

      this.blocksMap.put(var1.getName(), var2);
   }

   protected boolean hasIsBarOpenCondition(IBlock var1) {
      String var2 = var1.getClass().getSimpleName();
      if (var2.equals("IsBarOpen")) {
         return true;
      }

      if (var2.equals("AND")) {
         AND var3 = (AND)var1;

         for (IBlock var7 : var3.Children) {
            if (this.hasIsBarOpenCondition(var7)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }
}
