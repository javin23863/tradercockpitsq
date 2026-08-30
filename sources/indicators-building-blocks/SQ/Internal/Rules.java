package SQ.Internal;

import com.strategyquant.lib.snippets.CustomClassesLoader;
import com.strategyquant.tradinglib.strategy.xml.XmlStrategyException;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Rules {
   public static final Logger Log = LoggerFactory.getLogger("Rules");
   private static Rules instance;
   private Int2ObjectOpenHashMap<Rule> mapClasses = new Int2ObjectOpenHashMap();

   static Rules getInstance() throws XmlStrategyException {
      if (instance == null) {
         instance = new Rules();
      }

      return instance;
   }

   private Rules() throws XmlStrategyException {
      this.initClassesMap("Internal/RulesImpl");
   }

   private void initClassesMap(String var1) throws XmlStrategyException {
      this.mapClasses.clear();
      CustomClassesLoader var2 = new CustomClassesLoader(var1);

      while (var2.hasNext()) {
         String var3 = var2.getNext();
         Rule var4 = (Rule)var2.createInstance(var3);
         if (var4 == null) {
            throw new XmlStrategyException("Cannot instantiate rule class '" + var3 + "'");
         }

         this.mapClasses.put(var4.getClass().getSimpleName().hashCode(), var4);
      }
   }

   public static Rule get(String var0) throws XmlStrategyException {
      return getInstance()._get(var0);
   }

   private Rule _get(String var1) throws XmlStrategyException {
      int var2 = var1.hashCode();
      if (!this.mapClasses.containsKey(var2)) {
         throw new XmlStrategyException("Cannot find rule with class '" + var1 + "'");
      } else {
         return (Rule)this.mapClasses.get(var2);
      }
   }

   public static void reload() throws Exception {
      instance = new Rules();
   }
}
