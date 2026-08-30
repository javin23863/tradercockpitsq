angular.module('app.settings').service('SettingsGoToTaskService', function ($q, $timeout, AppService, L, ProjectConditionsService) {

    this.loadSettings = function () {
        instance.gui.loading = true;

        loadTasks();

        var goToTaskObj = AppService.getCurrentTaskTabSettings("GoToTask");

        instance.config.task = getAttrStringValue(goToTaskObj, 'task', '');
        if(!instance.config.task && instance.config.tasks.length > 0) {
            instance.config.task = instance.config.tasks[0].taskName;

            goToTaskObj.setAttribute('task', instance.config.task);
        }

        instance.config.resetEvaluatedCycles = getAttrBooleanValue(goToTaskObj, 'resetEvaluatedCycles', false);
        instance.config.resetActivatedCycles = getAttrBooleanValue(goToTaskObj, 'resetActivatedCycles', false);

        instance.config.conditions = ProjectConditionsService.loadConditions(goToTaskObj);

        $timeout(function(){ instance.gui.loading = false; }, 0, false);
    }           

    function loadTasks() {
        var projectObj = AppService.getProjectConfig();
        var tasksObj = getChildElement(projectObj, 'Tasks');
        var taskObjs = getChildElements(tasksObj, 'Task');

        instance.config.tasks.length = 0;

        for (var i = 0; i < taskObjs.length; i++) {
            var taskObj = taskObjs[i];
            var taskName = getAttrStringValue(taskObj, 'name');
            var taskType = getAttrStringValue(taskObj, 'type');
            var taskTitle = getAttrStringValue(taskObj, "title", taskName);

            var task = {
                taskType: taskType,
                taskName: taskName,
                taskTitle: taskTitle
            }

            if (taskType != 'GoToTask') {
                instance.config.tasks.push(task);
            }
        }
    }

    this.saveSettings = function () {
        var xmlDoc = AppService.xmlDoc;
        var goToTaskObj = AppService.getCurrentTaskTabSettings("GoToTask");
        if(!goToTaskObj) return;

        goToTaskObj.setAttribute('task', instance.config.task);
        goToTaskObj.setAttribute('resetEvaluatedCycles', instance.config.resetEvaluatedCycles);
        goToTaskObj.setAttribute('resetActivatedCycles', instance.config.resetActivatedCycles);

        removeAllChildren(goToTaskObj);

        ProjectConditionsService.saveConditions(goToTaskObj, instance.config.conditions, xmlDoc);
    }


    var instance = this;

    this.config = {
        conditions: [],
        tasks: [],
        task: null,
        resetEvaluatedCycles: false,
        resetActivatedCycles: false
    }

    this.gui = {
        loading : false
    }

});