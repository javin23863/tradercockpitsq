package SQ.Internal.RulesImpl;

import SQ.Internal.ActionBlock;
import SQ.Internal.ITradingOptionsEvaluator;
import SQ.Internal.Rule;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.IBlock;
import java.util.ArrayList;
import org.jdom2.Element;

public class IfThen extends Rule {
   private IBlock condition = null;
   private ArrayList<ActionBlock> actions = null;
   boolean everyTick = true;

   @Override
   public void evaluateRule(int var1, ITradingOptionsEvaluator var2, String var3) throws Exception {
      if (this.everyTick || var1 == this.barEventType || var3.equals("OnInit") || var3.equals("OnDeinit")) {
         double var4 = 0.0;
         if (this.condition != null) {
            var4 = this.condition.evaluateBlock();
         }

         if (var4 > 0.0 && this.actions != null) {
            for (int var6 = 0; var6 < this.actions.size(); var6++) {
               ActionBlock var7 = this.actions.get(var6);
               if (var7.getReturnType() != 8 || var2.continueBarUpdate()) {
                  var7.OnAction();
               }
            }
         }

         if (this.Strategy.getApplyExitsAtTheEndOfRule()) {
            this.applyExits();
         }
      }
   }

   private void applyExits() throws TradingException {
      if (this.actions != null) {
         for (int var1 = 0; var1 < this.actions.size(); var1++) {
            if (this.actions.get(var1).getReturnType() == 8) {
               this.actions.get(var1).OnApplyExits();
            }
         }
      }
   }

   @Override
   protected void parseXml(Element var1) throws BlockDefinitionException {
      super.parseXml(var1);
      ArrayList var2 = this.getBlocks("If");
      if (var2.size() > 1) {
         throw new BlockDefinitionException("IF part cannot have more than one sub-block!");
      }

      if (var2 != null && var2.size() != 0) {
         this.condition = (IBlock)var2.get(0);
      }

      var2 = this.getBlocks("Then");
      if (var2 != null && var2.size() != 0) {
         this.actions = new ArrayList<>();

         for (int var3 = 0; var3 < var2.size(); var3++) {
            IBlock var4 = (IBlock)var2.get(var3);
            if (!(var4 instanceof ActionBlock)) {
               throw new BlockDefinitionException(String.format("Block '%s' in THEN part is not an ActionBlock!", var4.getClass().getSimpleName()));
            }

            this.actions.add((ActionBlock)var4);
         }
      }

      String var6 = var1.getAttributeValue("everyTick");
      if (var6 != null && var6.equals("false")) {
         this.everyTick = false;
      } else {
         this.everyTick = true;
         if (this.condition != null) {
            this.everyTick = !this.hasIsBarOpenCondition(this.condition);
         }
      }
   }
}
