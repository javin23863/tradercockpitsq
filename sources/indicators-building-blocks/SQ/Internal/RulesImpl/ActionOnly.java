package SQ.Internal.RulesImpl;

import SQ.Internal.ActionBlock;
import SQ.Internal.ITradingOptionsEvaluator;
import SQ.Internal.Rule;
import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.IBlock;
import java.util.ArrayList;
import org.jdom2.Element;

public class ActionOnly extends Rule {
   private ArrayList<ActionBlock> actions = null;

   @Override
   public void evaluateRule(int var1, ITradingOptionsEvaluator var2, String var3) throws Exception {
      if (this.actions != null) {
         for (int var4 = 0; var4 < this.actions.size(); var4++) {
            if (this.actions.get(var4).getReturnType() != 8 || var2.continueBarUpdate()) {
               this.actions.get(var4).OnAction();
            }
         }
      }
   }

   @Override
   protected void parseXml(Element var1) throws BlockDefinitionException {
      super.parseXml(var1);
      ArrayList var2 = null;
      var2 = this.getBlocks("Then");
      if (var2 == null) {
         var2 = this.getBlocks("Actions");
      }

      if (var2 != null && var2.size() != 0) {
         this.actions = new ArrayList<>();

         for (IBlock var4 : var2) {
            if (!(var4 instanceof ActionBlock)) {
               throw new BlockDefinitionException(String.format("Block '%' in THEN part is not an ActionBlock!", var4.getClass().getSimpleName()));
            }

            this.actions.add((ActionBlock)var4);
         }
      }
   }
}
