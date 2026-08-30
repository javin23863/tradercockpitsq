package com.strategyquant.tradinglib;

import com.strategyquant.tradinglib.optimization.NonexistingVariableException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import org.jdom2.Element;

public class Variables extends ArrayList<Variable> {
   private Element xml;

   private Variables() {
   }

   public Variables(Element var1) {
      this.xml = var1;
      this.parseXmlVariables();
   }

   public Variables(Object var1) {
      this.xml = null;
      Class var2 = var1.getClass();

      for (Field var6 : var2.getFields()) {
         for (Annotation var10 : var6.getAnnotations()) {
            if (var10 instanceof Parameter) {
               Variable var11 = new Variable(var1, var6);
               this.add(var11);
               break;
            }
         }
      }
   }

   private void parseXmlVariables() {
      if (this.xml != null) {
         Element var1 = this.xml.getChild("Strategy").getChild("Variables");
         if (var1 != null) {
            for (Element var3 : var1.getChildren("variable")) {
               Variable var4 = new Variable(var3);
               this.add(var4);
            }
         }
      }
   }

   public Variable get(String var1) {
      for (int var2 = 0; var2 < this.size(); var2++) {
         Variable var3 = this.get(var2);
         if (var3.getName().equals(var1)) {
            return var3;
         }
      }

      return null;
   }

   public Variable getById(String var1) {
      for (int var2 = 0; var2 < this.size(); var2++) {
         Variable var3 = this.get(var2);
         if (var3.getId().equals(var1)) {
            return var3;
         }
      }

      return null;
   }

   public Variable getByField(Object var1, String var2) {
      for (int var3 = 0; var3 < this.size(); var3++) {
         Variable var4 = this.get(var3);
         if (var4.isObjectRegistered(var1, var2)) {
            return var4;
         }
      }

      return null;
   }

   public void sortByName() {
      this.sort(new Comparator<Variable>() {
         public int compare(Variable var1, Variable var2) {
            return var1.getName().compareTo(var2.getName());
         }
      });
   }

   public void setValue(String var1, double var2) throws NonexistingVariableException {
      for (int var4 = 0; var4 < this.size(); var4++) {
         Variable var5 = this.get(var4);
         if (var5.getName().equals(var1)) {
            switch (var5.getInternalType()) {
               case 1:
               case 5:
                  var5.setValue((int)var2);
                  break;
               case 2:
                  var5.setValue(var2 != 0.0);
                  break;
               case 3:
               default:
                  throw new NonexistingVariableException(String.format("Cannot set value for variable %s with type %d", var5.getName(), var5.getInternalType()));
               case 4:
                  var5.setValue(var2);
            }

            return;
         }
      }

      throw new NonexistingVariableException(String.format("Variable '%s' doesn't exist!", var1));
   }

   public boolean canSetValue(String var1) throws NonexistingVariableException {
      for (int var2 = 0; var2 < this.size(); var2++) {
         Variable var3 = this.get(var2);
         if (var3.getName().equals(var1)) {
            switch (var3.getInternalType()) {
               case 1:
               case 2:
               case 4:
               case 5:
                  return true;
               case 3:
               default:
                  return false;
            }
         }
      }

      throw new NonexistingVariableException(String.format("Variable '%s' doesn't exist!", var1));
   }

   public void setValueInXml(String var1, double var2) throws NonexistingVariableException {
      for (int var4 = 0; var4 < this.size(); var4++) {
         Variable var5 = this.get(var4);
         if (var5.getName().equals(var1)) {
            switch (var5.getInternalType()) {
               case 1:
               case 5:
                  var5.setValueInXml((int)var2);
                  break;
               case 2:
                  var5.setValueInXml(var2);
                  break;
               case 3:
               default:
                  throw new NonexistingVariableException(String.format("Cannot set value for variabe %s with type %d", var5.getName(), var5.getInternalType()));
               case 4:
                  var5.setValueInXml(var2);
            }

            return;
         }
      }

      throw new NonexistingVariableException(String.format("Variable '%s' doesn't exist!", var1));
   }
}
