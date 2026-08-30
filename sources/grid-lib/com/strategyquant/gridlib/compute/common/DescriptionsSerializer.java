package com.strategyquant.gridlib.compute.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class DescriptionsSerializer<T> {
   private StringBuilder sb;

   public String getJson(
      Map<Integer, LinkedHashSet<Integer>> var1, Collection<Integer> var2, Map<Integer, JobQueue.JobInfo<?>> var3, RingBuffer<JobQueue.JobInfo<?>> var4
   ) {
      this.sb = new StringBuilder();
      this.beginObject();
      this.addJobs("running", DescriptionsSerializer.SerializedPart.RUNNING, this.getRunning(var2, var3), var3);
      this.addSeparator();
      this.addJobs("waiting", DescriptionsSerializer.SerializedPart.WAITING, this.getWaiting(var2, var1, var3), var3);
      if (var4 != null) {
         this.addSeparator();
         this.addFinishedJobs("finished", var4);
      }

      this.endObject();
      return this.sb.toString();
   }

   private List<Integer> getRunning(Collection<Integer> var1, Map<Integer, JobQueue.JobInfo<?>> var2) {
      LinkedList var3 = new LinkedList();

      for (Integer var5 : var1) {
         JobQueue.JobInfo var6 = (JobQueue.JobInfo)var2.get(var5);
         if (var6 != null && !var6.waiting) {
            var3.add(var5);
         }
      }

      return var3;
   }

   private List<Integer> getWaiting(Collection<Integer> var1, Map<Integer, LinkedHashSet<Integer>> var2, Map<Integer, JobQueue.JobInfo<?>> var3) {
      LinkedList var4 = new LinkedList();

      for (Integer var6 : var1) {
         JobQueue.JobInfo var7 = (JobQueue.JobInfo)var3.get(var6);
         if (var7 != null && var7.waiting) {
            var4.add(var6);
         }
      }

      for (int var8 = 9; var8 >= 0; var8--) {
         LinkedHashSet var9 = (LinkedHashSet)var2.get(var8);
         if (var9 != null) {
            var4.addAll(var9);
         }
      }

      return var4;
   }

   private void addFinishedJobs(String var1, RingBuffer<JobQueue.JobInfo<?>> var2) {
      this.addAttrOnly(var1);
      ArrayList var3 = new ArrayList(var2.getSize());

      for (JobQueue.JobInfo var5 : var2) {
         var3.add(var5);
      }

      Collections.sort(var3, new Comparator<JobQueue.JobInfo>() {
         public int compare(JobQueue.JobInfo var1, JobQueue.JobInfo var2x) {
            long var3x = var1.getFinishTime();
            long var5 = var2x.getFinishTime();
            if (var3x == var5) {
               return 0;
            } else {
               return var3x > var5 ? -1 : 1;
            }
         }
      });
      this.beginArray();
      boolean var7 = true;

      for (JobQueue.JobInfo var6 : var3) {
         if (!var7) {
            this.addSeparator();
         }

         this.beginObject();
         this.addCommonsJobsInfo(var6);
         this.addSeparator();
         this.addAttribute("duration", var6.getDuration());
         this.addSeparator();
         this.addAttribute("durationWithWait", var6.getDurationWithWait());
         this.addSeparator();
         this.addAttribute("startTime", var6.getExecuteTime());
         this.addSeparator();
         this.addAttribute("finishTime", var6.getExecuteTime());
         this.addSeparator();
         this.addAttribute("success", var6.isSuccess());
         if (!var6.isSuccess()) {
            this.addSeparator();
            this.addAttribute("message", var6.getMessage());
         }

         this.endObject();
         var7 = false;
      }

      this.endArray();
   }

   private void addCommonsJobsInfo(JobQueue.JobInfo<?> var1) {
      this.addAttribute("jobId", var1.getId());
      this.addSeparator();
      this.addAttribute("jobGroupId", var1.getJobGroupId());
      this.addSeparator();
      this.addAttribute("createTime", var1.getCreationTime());
   }

   private void addJobs(String var1, DescriptionsSerializer.SerializedPart var2, Collection<Integer> var3, Map<Integer, JobQueue.JobInfo<?>> var4) {
      this.addAttrOnly(var1);
      this.beginArray();
      boolean var5 = true;

      for (Integer var7 : var3) {
         JobQueue.JobInfo var8 = (JobQueue.JobInfo)var4.get(var7);
         if (var8 != null) {
            if (!var5) {
               this.addSeparator();
            }

            this.beginObject();
            this.addCommonsJobsInfo(var8);
            if (var2 == DescriptionsSerializer.SerializedPart.RUNNING) {
               long var9 = System.currentTimeMillis();
               this.addSeparator();
               this.addAttribute("progress", var8.getProgress());
               this.addSeparator();
               this.addAttribute("duration", var9 - var8.getExecuteTime());
               this.addSeparator();
               this.addAttribute("durationWithWait", var9 - var8.getCreationTime());
               this.addSeparator();
               this.addAttribute("startTime", var8.getExecuteTime());
               this.addSeparator();
               this.addAttribute("waiting", var8.isWaiting());
            }

            this.endObject();
            var5 = false;
         }
      }

      this.endArray();
   }

   private void beginArray() {
      this.sb.append("[");
   }

   private void endArray() {
      this.sb.append("]");
   }

   private void beginObject() {
      this.sb.append("{");
   }

   private void endObject() {
      this.sb.append("}");
   }

   private void addSeparator() {
      this.sb.append(",");
   }

   private void addAttrOnly(String var1) {
      this.sb.append("\"" + var1 + "\":");
   }

   private void addAttribute(String var1, String var2) {
      var2 = var2.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
      this.addAttrOnly(var1);
      this.sb.append("\"" + var2 + "\"");
   }

   private void addAttribute(String var1, boolean var2) {
      this.addAttrOnly(var1);
      this.sb.append(var2);
   }

   private void addAttribute(String var1, long var2) {
      this.addAttrOnly(var1);
      this.sb.append(var2);
   }

   private void addAttribute(String var1, int var2) {
      this.addAttrOnly(var1);
      this.sb.append(var2);
   }

   private enum SerializedPart {
      FINISHED,
      WAITING,
      RUNNING;
   }
}
