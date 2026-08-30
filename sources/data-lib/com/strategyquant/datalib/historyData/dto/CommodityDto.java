package com.strategyquant.datalib.historyData.dto;

import java.math.BigDecimal;

public class CommodityDto {
   private Long id;
   private String code;
   private String name;
   private BigDecimal commision;
   private BigDecimal defaultSpread;
   private BigDecimal pointValue;
   private BigDecimal tickStep;
   private BigDecimal tickSize;
   private BigDecimal orderSizeMulti;
   private BigDecimal orderSizeStep;

   public Long getId() {
      return this.id;
   }

   public void setId(Long var1) {
      this.id = var1;
   }

   public String getCode() {
      return this.code;
   }

   public void setCode(String var1) {
      this.code = var1;
   }

   public BigDecimal getCommision() {
      return this.commision;
   }

   public void setCommision(BigDecimal var1) {
      this.commision = var1;
   }

   public BigDecimal getDefaultSpread() {
      return this.defaultSpread;
   }

   public void setDefaultSpread(BigDecimal var1) {
      this.defaultSpread = var1;
   }

   public BigDecimal getPointValue() {
      return this.pointValue;
   }

   public void setPointValue(BigDecimal var1) {
      this.pointValue = var1;
   }

   public BigDecimal getTickStep() {
      return this.tickStep;
   }

   public void setTickStep(BigDecimal var1) {
      this.tickStep = var1;
   }

   public BigDecimal getTickSize() {
      return this.tickSize;
   }

   public void setTickSize(BigDecimal var1) {
      this.tickSize = var1;
   }

   public String getName() {
      return this.name;
   }

   public void setName(String var1) {
      this.name = var1;
   }

   public BigDecimal getOrderSizeMulti() {
      return this.orderSizeMulti;
   }

   public void setOrderSizeMulti(BigDecimal var1) {
      this.orderSizeMulti = var1;
   }

   public BigDecimal getOrderSizeStep() {
      return this.orderSizeStep;
   }

   public void setOrderSizeStep(BigDecimal var1) {
      this.orderSizeStep = var1;
   }
}
