package com.strategyquant.tradinglib.gp.strategies;

import com.strategyquant.lib.IRandomGenerator;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.atm.ATMGenerateConfig;
import com.strategyquant.tradinglib.generator.GenerateException;
import com.strategyquant.tradinglib.gp.AbstractFactory;
import com.strategyquant.tradinglib.gp.GPIDs;
import com.strategyquant.tradinglib.gp.IEvolutionaryOperator;
import com.strategyquant.tradinglib.gp.SelectedBlockGroups;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.ArrayList;
import java.util.List;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeMutation implements IEvolutionaryOperator<Node> {
   public static final Logger Log = LoggerFactory.getLogger("NodeMutation");
   private final double mutationProbability;
   private final double atmMutationProbability = 20.0;
   private boolean LimitSLPTRRR;
   private double LimitSLPTRRRFromCoef;
   private double LimitSLPTRRRToCoef;
   private SelectedBlockGroups selectedBlockGroups;
   private ATMGenerateConfig atmConfig;

   public NodeMutation(double var1, BuildSettings var3, SelectedBlockGroups var4) {
      this.mutationProbability = var1;
      this.selectedBlockGroups = var4;
      SettingsMap var5 = var3.getBuildSettingsMap();
      this.atmConfig = var3.getATMGenerateConfig();
      this.LimitSLPTRRR = var5.getBoolean("LimitSLPTRRR");
      if (this.LimitSLPTRRR) {
         double var6 = var5.getInt("LimitSLPTRRRFrom");
         double var8 = var5.getInt("LimitSLPTRRRTo");
         this.LimitSLPTRRRFromCoef = 1.0 / (var6 / 100.0);
         this.LimitSLPTRRRToCoef = 1.0 / (var8 / 100.0);
      }
   }

   @Override
   public List<Node> apply(List<Node> var1, IRandomGenerator var2, AbstractFactory<Node> var3, int var4, int var5) throws Exception {
      ArrayList var6 = new ArrayList(var1.size());

      for (int var7 = 0; var7 < var1.size(); var7++) {
         Node var8 = (Node)var1.get(var7);
         Node var9 = var8.clone();
         Element var10 = this.mutate(var9, var2, var3);
         if (var10 != null && GenEvoChecker.passedRRRCheck(var10, this.LimitSLPTRRR, this.LimitSLPTRRRToCoef, this.LimitSLPTRRRFromCoef)) {
            Node var11 = new Node(var10);
            GPIDs var12 = var9.getGPIDs();
            GPIDs var13 = new GPIDs();
            var13.islandIndex = var4;
            var13.generationIndex = var5;
            var13.generationType = "Mutation";
            var13.parent1 = var12.toShortString();
            var11.setGPIDs(var13);
            var11.setModified();
            var6.add(var11);
         } else {
            var9 = var8.clone();
            var6.add(var9);
         }
      }

      for (int var14 = 0; var14 < var1.size(); var14++) {
         Node var15 = (Node)var1.get(var14);
         var15.clearGeneratedObjectsMap();
      }

      return var6;
   }

   private Element mutate(Node var1, IRandomGenerator var2, AbstractFactory<Node> var3) throws Exception {
      GeneratedObjectsMap var4 = var1.getGeneratedObjectsMap();
      IntArrayList var5 = new IntArrayList();

      for (int var6 = 0; var6 < var4.size(); var6++) {
         if (var2.probability(this.mutationProbability)) {
            GeneratedObject var7 = var4.get(var6);
            if (!this.isObjectDependent(var7, var5)) {
               Element var8 = null;

               try {
                  var8 = var7.mutate(var2, var3);
               } catch (GenerateException var10) {
                  if (!var10.getMessage().contains("Cannot find block ")) {
                     throw var10;
                  }
               }

               var8 = GenEvoChecker.fixZeroPeriodsAndExits(var8);
               if (var8 != null) {
                  var1.replaceObjectById(var7.getGID(), var8);
                  var8.setAttribute("genop", "mutated");
                  var5.add(var7.getGID());
               }
            }
         }
      }

      if (this.atmConfig != null && var2.probability(20.0)) {
         Element var11 = var1.getStrategy();
         Element var12 = var3.mutateATM(var11, var2);
         if (var12 != null) {
            return var12;
         }
      }

      return var5.isEmpty() ? null : var1.getStrategy();
   }

   private boolean isObjectDependent(GeneratedObject var1, IntArrayList var2) {
      int var3 = var1.getParentGID();
      if (var3 == 0) {
         return false;
      }

      for (int var4 = 0; var4 < var2.size(); var4++) {
         if (var3 == var2.getInt(var4)) {
            return true;
         }
      }

      return false;
   }
}
