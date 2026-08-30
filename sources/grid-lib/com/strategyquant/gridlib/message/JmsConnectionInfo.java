package com.strategyquant.gridlib.message;

import java.util.UUID;

public class JmsConnectionInfo {
   private String ident;
   private String target;

   public JmsConnectionInfo(String var1) {
      this.target = var1;
      this.ident = UUID.randomUUID().toString().replace("-", "");
   }

   public String getIdent() {
      return this.ident;
   }

   public void setIdent(String var1) {
      this.ident = var1;
   }

   public String getTarget() {
      return this.target;
   }

   public void setTarget(String var1) {
      this.target = var1;
   }
}
