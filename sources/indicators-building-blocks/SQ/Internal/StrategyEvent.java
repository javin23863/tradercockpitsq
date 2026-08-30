package SQ.Internal;

import com.strategyquant.tradinglib.strategy.xml.XmlStrategyException;
import java.util.ArrayList;
import org.jdom2.Element;

public class StrategyEvent {
   private XmlStrategy strategy;
   private String eventName;
   private Rule[] rules = null;

   public StrategyEvent(XmlStrategy var1, Element var2) throws XmlStrategyException {
      this.strategy = var1;
      this.parseXml(var2);
   }

   public String getEventName() {
      return this.eventName;
   }

   public void evaluateEvent(int var1, ITradingOptionsEvaluator var2) throws Exception {
      if (this.rules != null) {
         for (Rule var6 : this.rules) {
            var6.evaluateRule(var1, var2, this.eventName);
         }
      }
   }

   private void parseXml(Element var1) throws XmlStrategyException {
      this.eventName = var1.getAttributeValue("key");
      ArrayList var2 = null;

      for (Element var4 : var1.getChildren()) {
         String var5 = var4.getAttributeValue("type");
         Rule var6 = Rules.get(var5);
         var6 = var6.newInstance(this.strategy, var4);
         if (var2 == null) {
            var2 = new ArrayList();
         }

         var2.add(var6);
      }

      if (var2 != null) {
         this.rules = new Rule[var2.size()];
         int var7 = 0;

         for (Rule var9 : var2) {
            this.rules[var7++] = var9;
         }

         var2.clear();
      }
   }

   public void deinitialize() {
      if (this.rules != null) {
         for (int var1 = 0; var1 < this.rules.length; var1++) {
            this.rules[var1].deinitialize();
         }
      }
   }
}
