package com.strategyquant.plugin.App.impl.TaskManager;

import com.strategyquant.tradinglib.project.ProjectEngine;
import com.strategyquant.tradinglib.project.SQProject;
import com.strategyquant.tradinglib.project.websocket.DataToSend;
import com.strategyquant.tradinglib.project.websocket.SQWebSocketManager;
import com.strategyquant.tradinglib.project.websocket.SynchronizedWebSocketPublisher;
import com.strategyquant.tradinglib.taskImpl.ISQTask;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public class TaskManagerProgressPublisher extends SynchronizedWebSocketPublisher {
   private JSONArray projectsStats = new JSONArray();
   private DataToSend data = new DataToSend("customProjectStats", this.projectsStats);
   private String lastData;

   public TaskManagerProgressPublisher() {
      SQWebSocketManager.getInstance().addAppPublisher("TASKMANAGER", this);
   }

   public DataToSend getData() {
      List var1 = ProjectEngine.list();

      for (int var2 = this.projectsStats.length() - 1; var2 >= 0; var2--) {
         this.projectsStats.remove(var2);
      }

      for (SQProject var3 : var1) {
         if (!var3.isAppProject()) {
            JSONObject var4 = var3.getInfo();
            var4.put("activeTask", var3.getActiveTaskName());
            var4.put("runningStatus", var3.getRunningStatus());
            JSONArray var5 = new JSONArray();
            var4.put("tasksIterations", var5);

            try {
               for (int var6 = 0; var6 < var3.getTasksHashes().size(); var6++) {
                  try {
                     ISQTask var7 = var3.getTaskByHash(var3.getTasksHashes().getInt(var6));
                     var5.put(new JSONObject().put("taskName", var7.getCustomName()).put("iterations", var7.getNumberOfRuns()));
                  } catch (Exception var8) {
                  }
               }
            } catch (Exception var9) {
            }

            this.projectsStats.put(var4);
         }
      }

      if (this.lastData != null && this.projectsStats.toString().equals(this.lastData)) {
         return null;
      }

      this.lastData = this.projectsStats.toString();
      return this.data;
   }

   public void resetLastData() {
      this.lastData = null;
   }
}
