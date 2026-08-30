package com.strategyquant.gridlib.message;

public enum MessageKind {
   registerListener,
   unregisterListener,
   registerListenerResult,
   sendCustomJobMessage,
   registerTask,
   pauseTasks,
   restartTasks,
   stopTasks,
   resultOfTask,
   taskComputed,
   needTask,
   tasksQueueNotEmpty,
   computeTask,
   checkFileDataStatus,
   dataStatus,
   needFileData,
   sendingFileData,
   nodeHeartbeat,
   sendingTopology,
   refreshTopology;
}
