angular.module('app.tmtasks', ['sqplugin', 'dndLists']).config(function(sqPluginProvider, SQEventsProvider) {

    SQEventsProvider.add("TASK_ADDED", 'task-added'); 
    SQEventsProvider.add("TASK_REMOVED", 'task-removed'); 

    sqPluginProvider.plugin("MainPanel", 5, {
        dataItem: 'tmtasks',
        templateUrl: '../../../plugins/TaskManagerTasks/tasks.html',
        controller: 'TMTasksCtrl',
        product: 'TASKMANAGER',
        class: 'sqn-tasksflow',
        name: 'tasksflow',
    });
});
