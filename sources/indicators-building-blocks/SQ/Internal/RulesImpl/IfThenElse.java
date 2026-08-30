package SQ.Internal.RulesImpl;

import SQ.Internal.ActionBlock;
import SQ.Internal.ITradingOptionsEvaluator;
import SQ.Internal.Rule;
import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.IBlock;
import java.util.ArrayList;
import org.jdom2.Element;

public class IfThenElse extends Rule {
   private IBlock condition = null;
   private ArrayList<ActionBlock> actionsIf = null;
   private ArrayList<ActionBlock> actionsElse = null;
   boolean everyTick = true;

   @Override
   public void evaluateRule(int var1, ITradingOptionsEvaluator var2, String var3) throws Exception {
      if (this.everyTick || var1 == this.barEventType || var3.equals("OnInit") || var3.equals("OnDeinit")) {
         double var4 = 0.0;
         if (this.condition != null) {
            var4 = this.condition.evaluateBlock();
         }

         if (var4 > 0.0) {
            if (this.actionsIf != null) {
               for (int var6 = 0; var6 < this.actionsIf.size(); var6++) {
                  if (this.actionsIf.get(var6).getReturnType() != 8 || var2.continueBarUpdate()) {
                     this.actionsIf.get(var6).OnAction();
                  }
               }
            }
         } else if (this.actionsElse != null) {
            for (int var7 = 0; var7 < this.actionsElse.size(); var7++) {
               if (this.actionsElse.get(var7).getReturnType() != 8 || var2.continueBarUpdate()) {
                  this.actionsElse.get(var7).OnAction();
               }
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
         this.actionsIf = new ArrayList<>();

         for (IBlock var4 : var2) {
            if (!(var4 instanceof ActionBlock)) {
               throw new BlockDefinitionException(String.format("Block '%' in THEN part is not an ActionBlock!", var4.getClass().getSimpleName()));
            }

            this.actionsIf.add((ActionBlock)var4);
         }
      }

      var2 = this.getBlocks("Else");
      if (var2 != null && var2.size() != 0) {
         this.actionsElse = new ArrayList<>();

         for (int var7 = 0; var7 < var2.size(); var7++) {
            IBlock var9 = (IBlock)var2.get(var7);
            if (!(var9 instanceof ActionBlock)) {
               throw new BlockDefinitionException(String.format("Block '%' in ELSE part is not an ActionBlock!", var9.getClass().getSimpleName()));
            }

            this.actionsElse.add((ActionBlock)var9);
         }
      }

      String var8 = var1.getAttributeValue("everyTick");
      if (var8 != null && var8.equals("false")) {
         this.everyTick = false;
      } else {
         this.everyTick = true;
         if (this.condition != null) {
            this.everyTick = !this.hasIsBarOpenCondition(this.condition);
         }
      }
   }
}
