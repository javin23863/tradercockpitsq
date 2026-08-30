package com.strategyquant.webguilib.servlet;

import java.util.ArrayList;

public class PathInfo {
   String name;
   boolean isDirectory;
   ArrayList<Integer> subFilesIds;
   ArrayList<String> subFilesNames;
   ArrayList<Boolean> subDirs;
   ArrayList<Boolean> projectSubDirs;

   public String getName() {
      return this.name;
   }

   public void setName(String var1) {
      this.name = var1;
   }

   public boolean isDirectory() {
      return this.isDirectory;
   }

   public void setDirectory(boolean var1) {
      this.isDirectory = var1;
   }

   public ArrayList<Integer> getSubFilesIds() {
      return this.subFilesIds;
   }

   public void setSubFilesIds(ArrayList<Integer> var1) {
      this.subFilesIds = var1;
   }

   public ArrayList<String> getSubFilesNames() {
      return this.subFilesNames;
   }

   public void setSubFilesNames(ArrayList<String> var1) {
      this.subFilesNames = var1;
   }

   public ArrayList<Boolean> getSubDirs() {
      return this.subDirs;
   }

   public void setSubDirs(ArrayList<Boolean> var1) {
      this.subDirs = var1;
   }

   public ArrayList<Boolean> getProjectSubDirs() {
      return this.projectSubDirs;
   }

   public void setProjectSubDirs(ArrayList<Boolean> var1) {
      this.projectSubDirs = var1;
   }
}
