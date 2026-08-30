package com.strategyquant.gridlib.topology;

import java.io.Serializable;

public class NodeInfo implements Serializable {
   private static final long serialVersionUID = 1L;
   private String ip;
   private Long id;
   private int totalCores;
   private int usedCores;
   private Long age;
   private Long lastHeartBeat;
   private int runningThreads;
   private String osVersion;
   private double totalMemory;
   private double usedMemory;
   private int cpuUsage;

   public String getIp() {
      return this.ip;
   }

   public void setIp(String var1) {
      this.ip = var1;
   }

   public Long getAge() {
      return this.age;
   }

   public void setAge(Long var1) {
      this.age = var1;
   }

   public int getRunningThreads() {
      return this.runningThreads;
   }

   public void setRunningThreads(int var1) {
      this.runningThreads = var1;
   }

   public String getOsVersion() {
      return this.osVersion;
   }

   public void setOsVersion(String var1) {
      this.osVersion = var1;
   }

   public double getTotalMemory() {
      return this.totalMemory;
   }

   public void setTotalMemory(double var1) {
      this.totalMemory = var1;
   }

   public int getCpuUsage() {
      return this.cpuUsage;
   }

   public void setCpuUsage(int var1) {
      this.cpuUsage = var1;
   }

   public Long getLastHeartBeat() {
      return this.lastHeartBeat;
   }

   public void setLastHeartBeat(Long var1) {
      this.lastHeartBeat = var1;
   }

   public double getUsedMemory() {
      return this.usedMemory;
   }

   public void setUsedMemory(double var1) {
      this.usedMemory = var1;
   }

   public int getTotalCores() {
      return this.totalCores;
   }

   public void setTotalCores(int var1) {
      this.totalCores = var1;
   }

   public int getUsedCores() {
      return this.usedCores;
   }

   public void setUsedCores(int var1) {
      this.usedCores = var1;
   }

   public Long getId() {
      return this.id;
   }

   public void setId(Long var1) {
      this.id = var1;
   }
}
