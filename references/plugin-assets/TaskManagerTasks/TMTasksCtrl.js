angular.module('app.tmtasks').controller('TMTasksCtrl', function ($rootScope, $scope, sqPlugin, $timeout, TMTasksService, AppService, SQConstants, SQEvents, SQWebSocketService, L, usSpinnerService) {
    console.log("TMTasks controller initialized");

    $scope.onAddNewTask = function (index) {
        if ($rootScope.project.state == $scope.projectStates.running) {
            return;
        }

        $scope.config.allowCopyFromTask = false;
        $scope.addIndex = index;
        if ($scope.tasks.length > 0) {
            $scope.config.copyFromTask = $scope.tasks[0].taskName;
        }
        AppService.updateTaskXML().then(() => {
            showPopup('#newTaskModal');
        })
    }

    $scope.onCreateTask = function (task, index) {
        if(!$scope.disableAddTaskButtons){
            usSpinnerService.spin("addNewTaskSpinner");
            $scope.disableAddTaskButtons = true;
    
            var data = {
                projectName: $scope.projectName,
                taskType: task.type,
                taskName: generateTaskName(task),
                index: $scope.addIndex
            }
    
            TMTasksService.addTask(data, function (result) {
                reloadTasks(data);
    
                if ($scope.config.allowCopyFromTask) {
                    applyConfig($scope.config.copyFromTask, data.taskName, function () {
                        SQEvents.notifyListeners(SQEvents.get("TASK_ADDED"));
                        usSpinnerService.stop("addNewTaskSpinner");
                        $scope.disableAddTaskButtons = false;
                    });
                } else {
                    SQEvents.notifyListeners(SQEvents.get("TASK_ADDED"));
                    usSpinnerService.stop("addNewTaskSpinner");
                    $scope.disableAddTaskButtons = false;
                }
            });
        }
    }

    $scope.onMovedTask = function (item, index, event, external) {
        if ($rootScope.project.state == $scope.projectStates.running) {
            $rootScope.showError(L.tsq("Cannot change tasks flow while the project is running"));
            return;
        }

        var data = {
            projectName: $scope.projectName,
            taskName: item.taskName,
            toIndex: index
        }
        
        AppService.updateTaskXML().then(() => {
            TMTasksService.moveTask(data, function (result) {
                console.log("Task moved");
                reloadTasks();
            });
        })

        return true;
    }

    function generateTaskName(task) {
        let tname = L.tsq(task.name);

        if (!TMTasksService.findTask(tname, task.type)) {
            return tname;
        }

        for (var i = 2; i < 1000; i++) {
            var taskName = tname+ " " + i;

            if (!TMTasksService.findTask(taskName, task.type)) {
                return taskName;
            }
        }
    }

    function onEvent(event, data) {
        if (event == SQEvents.get('APP_PROJECT_CHANGED')) {
            $scope.tasks.length = 0;

            if (data != null) {
                $scope.projectName = AppService.getProject();

                reloadTasks();
            }
            try {
                $scope.$digest();
            } catch (err) { }
        }
    }

    function reloadTasks(taskToSelect) {
        TMTasksService.loadTasks();

        for (var i = 0; i < $scope.tasks.length; i++) {
            $scope.tasks[i].selectTask = selectTask;
            $scope.tasks[i].activateTask = activateTask;
        }

        if ($scope.tasks.length > 0) {
            if (taskToSelect) {
                selectTask(taskToSelect.taskType, taskToSelect.taskName);
            } else {
                selectTask($scope.tasks[0].taskType, $scope.tasks[0].taskName);
            }
        }
    }

    function selectTask(taskType, taskName) {
        var taskTitle = null;
        var taskXMLFile = null;

        for (var i = 0; i < $scope.tasks.length; i++) {
            var task = $scope.tasks[i];

            if (task.taskName == taskName) {
                task.selected = true;
                taskTitle = task.taskTitle;
                taskXMLFile = task.taskXMLFile;
            } 
            else {
                task.selected = false;
            }
        }

        let taskTypeOrig = null;
        if(taskType=='OptimizeConfigurable') {
            taskType = "Optimize";
            taskTypeOrig = 'OptimizeConfigurable';
        }
 
        AppService.setTask(taskType, taskName, taskTitle, taskTypeOrig, taskXMLFile);
    }

    function onWebSocketData(data) {
        if(data.ExitApp){
            AppService.updateTaskXML()
        }

        if (!data || !data.customProjectStats) {
            return;
        }

        for (var i = 0; i < data.customProjectStats.length; i++) {
            var projectStats = data.customProjectStats[i];
            if (projectStats.projectName == $scope.projectName) {
                setTaskStats(projectStats.activeTask, projectStats.tasksIterations, projectStats.runningStatus);
                break;
            }
        }

        try {
            $scope.$digest();
        } catch (err) { }
    }

    function setTaskStats(activeTask, tasksIterations, runningStatus) {
        for (var i = 0; i < $scope.tasks.length; i++) {
            var task = $scope.tasks[i];

            task.finished = false;
            task.inprogress = false;

            var item = getItem(tasksIterations, "taskName", task.taskName);
            if (item) {
                task.iterations = item.iterations;
            }
        }

        if (!activeTask || runningStatus === $scope.projectStates.stopped) {
            return;
        }

        for (var i = 0; i < $scope.tasks.length; i++) {
            var task = $scope.tasks[i];

            if (task.taskName == activeTask) {
                task.finished = false;
                task.inprogress = true;

                break;
            } else {
                task.finished = true && task.active;
                task.inprogress = false;
            }
        }
    }

    $scope.$on('$destroy', function () {
        SQEvents.removeListener(listenerId);
    });

    $scope.tasks = TMTasksService.tasks;
    $scope.availableTasks = TMTasksService.availableTasks;

    $scope.projectName = null;
    $scope.projectStates = SQConstants.getConstants().runningStatuses;

    var listenerId = "TMTasksCtrl";
    SQEvents.addListener(listenerId, [SQEvents.get('APP_PROJECT_CHANGED')], onEvent);

    SQWebSocketService.subscribeGeneral("TaskManagerTasks", onWebSocketData);

    $('.sq-app-taskmanager').removeClass('sqn-hide-header');

    $(document.body).append($(".dropdown-custom-tasks").detach());

    $scope.rename = {
        task: null,
        newName: null
    }

    function renameTask(task) {
        $scope.rename.task = task;
        $scope.rename.newName = task.taskTitle;

        showPopup("#renameTaskModal")
    }

    function copyConfigFromTask(task) {
        $scope.config.selectedTask = task;

        $scope.config.tasksCopyFrom.length = 0;

        for (var i = 0; i < $scope.tasks.length; i++) {
            var item = $scope.tasks[i];
            if (item.taskName != task.taskName) {
                $scope.config.tasksCopyFrom.push(item);
            }
        }

        if ($scope.config.tasksCopyFrom.length > 0) {
            $scope.config.copyFromTask = $scope.config.tasksCopyFrom[0].taskName;
        }

        showPopup("#copyFromTaskModal")
    }

    function copyConfigToTasks(task) {
        $scope.config.selectedTask = task;
        $scope.config.tasksCopyFrom.length = 0;

        for (var i = 0; i < $scope.tasks.length; i++) {
            var item = $scope.tasks[i];
            if (item.taskName != task.taskName) {
                item.selected = false;
                $scope.config.tasksCopyFrom.push(item);
            }
        }

        showPopup("#copyToTasksModal")
    }

    function activateTask(task) {
        var data = {
            projectName: $scope.projectName,
            taskType: task.taskType,
            taskName: task.taskName,
            active: task.active
        }

        TMTasksService.activateTask(data);
    }

    function cloneTask(task, position) {
        var data = {
            projectName: $scope.projectName,
            taskType: task.taskType,
            taskName: task.taskName,
            position: position
        }

        AppService.updateTaskXML().then(() => {
            TMTasksService.cloneTask(data, function (result) {
                reloadTasks(result);
            });
        })
    }

    $scope.onRenameTaskConfirm = function () {
        var task = $scope.rename.task;

        var data = {
            projectName: $scope.projectName,
            taskType: task.taskType,
            taskName: task.taskName,
            taskTitle: $scope.rename.newName,
        }

        AppService.updateTaskXML().then(() => {
            TMTasksService.renameTask(data, function (result) {
                $scope.rename.task.taskTitle = $scope.rename.newName;

                selectTask(task.taskType, task.taskName);
            })
        })

        hidePopup("#renameTaskModal")
    }

    $scope.onCopyFromTaskConfirm = function () {
        AppService.updateTaskXML().then(() => {
            var task = getItem($scope.tasks, "taskName", $scope.config.copyFromTask);
            var config = xmlToString(preprocessConfig(task, $scope.config.selectedTask, getTaskTemplate(AppService, SQConstants, task.taskType, task.taskName)));

            $rootScope.applyConfigToTask(config, $scope.config.selectedTask.taskName, function () {
                $rootScope.showSuccess(L.tsq("Task config loaded"));

                hidePopup("#copyFromTaskModal");
            });
        })
    }

    $scope.onCopyToTasksConfirm = function () {
        AppService.updateTaskXML().then(() => {
            let loaded = 0;
            for (var i = 0; i < $scope.config.tasksCopyFrom.length; i++) {
                let task = $scope.config.tasksCopyFrom[i];
                if (!task.selected) {
                    continue;
                }
                let config = xmlToString(preprocessConfig($scope.config.selectedTask, task, getTaskTemplate(AppService, SQConstants, $scope.config.selectedTask.taskType, $scope.config.selectedTask.taskName)));
                $rootScope.applyConfigToTask(config, task.taskName);
                loaded++;
            }
            
            if (loaded > 0) {
                $rootScope.showSuccess(loaded == 1 ? L.tsq("Task config loaded") : L.tsq("Task configs loaded"));
            }

            hidePopup("#copyToTasksModal");

        });
    }

    function preprocessConfig(fromTask, toTask, config){
        if(fromTask.taskType == toTask.taskType){
            return config
        }
        for(var i=0; i<config.childNodes[0].childNodes.length; i++){
            if(config.childNodes[0].childNodes[i].nodeName == "Databanks"){
                config.childNodes[0].removeChild(config.childNodes[0].childNodes[i]);
            }
        }
        return config
    }

    function applyConfig(fromTask, toTask, callback) {
        var task = getItem($scope.tasks, "taskName", fromTask);
        let elTaskCloned = getTaskTemplate(AppService, SQConstants, task.taskType, task.taskName);

        $rootScope.applyConfigToTask(xmlToString(elTaskCloned), toTask, function () {
            callback();
        });
    }

    $scope.getTaskLabel = function () {
        var task = getItem($scope.tasks, "taskName", $scope.config.copyFromTask);
        return task ? task.taskTitle : '';
    }

    $scope.onCloneTask = function (position) {
        cloneTask(TMTasksService.clickedTask, position);
    }

    $scope.onManuallyMoveTask = function (moveBy) {
        var newIndex = TMTasksService.clickedTask.taskNumber + moveBy;
        if (newIndex < 0) {
            newIndex = 0;
        }
        if (newIndex > $scope.tasks.length) {
            newIndex = $scope.tasks.length;
        }

        var data = {
            projectName: $scope.projectName,
            taskName: TMTasksService.clickedTask.taskName,
            toIndex: newIndex
        }

        AppService.updateTaskXML().then(() => {
            TMTasksService.moveTask(data, function (result) {
                console.log("Task moved");
                reloadTasks();
            });
        })
    }

    $scope.onRenameTask = function () {
        renameTask(TMTasksService.clickedTask);
    }

    $scope.onCopyFromTask = function () {
        copyConfigFromTask(TMTasksService.clickedTask);
    }

    $scope.onCopyToTasks = function () {
        copyConfigToTasks(TMTasksService.clickedTask);
    }

    $scope.applyMassConfig = function () {
        if(TMTasksService.clickedTask.taskType=="ApplyMassConfig") {
            return;
        }
        
        AppService.updateTaskXML().then(() => {
            $scope.clickedTaskname = TMTasksService.clickedTask.taskName

            $timeout(function () {
                $scope.applyMassConfigDialog.show()  
            }, 100);
        });
    }    

    $scope.onRemoveTask = function (task) {
        var data = {
            projectName: AppService.getProject(),
            taskType: TMTasksService.clickedTask.taskType,
            taskName: TMTasksService.clickedTask.taskName
        }

        $rootScope.showConfirm("Remove task", "Are you sure you want to remove '" + TMTasksService.clickedTask.taskTitle + "' task?", function (confirmed) {
            if (!confirmed) {
                return;
            }

            AppService.updateTaskXML().then(() => {
                TMTasksService.removeTask(data, function (data) {
                    SQEvents.notifyListeners(SQEvents.get("TASK_REMOVED"));
                    reloadTasks();
                });
            })
        }, true);
    }

    $scope.onRunProjectFromHere = function () {
        AppService.updateTaskXML().then(() => {
            SQEvents.notifyListeners(SQEvents.get("START_PROJECT"), {
                taskName: TMTasksService.clickedTask.taskName,
                action: 'runProjectFromHere'
            });
        })
    }

    $scope.onRunThisTaskOnly = function () {
        AppService.updateTaskXML().then(() => {
            SQEvents.notifyListeners(SQEvents.get("START_PROJECT"), {
                taskName: TMTasksService.clickedTask.taskName,
                action: 'runThisTaskOnly'
            });
        })
    }

    $scope.onTaskActivate = function (active) {
        TMTasksService.clickedTask.active = active

        activateTask(TMTasksService.clickedTask);
    }

    $scope.config = {
        copyFromTask: null,
        allowCopyFromTask: false,
        selectedTask: null,
        tasksCopyFrom: []
    }

    $scope.applyMassConfigDialog = {};

    $scope.disableAddTaskButtons = false;

    $scope.scrollRight = function () {
        let container = document.getElementById('ulResizeDiv');
        container.scrollLeft = 300;
    }

    $scope.scrollLeft = function () {
        let container = document.getElementById('ulResizeDiv');
        container.scrollLeft -= 300;
    }
    
    $scope.onChangeDivSize = function (smaller) {
        let container = document.querySelector('.sqn-tasksflow');
        let ulResizeDiv = document.getElementById('ulResizeDiv');
        let gridConatiner = document.querySelector('.tm-tasks-list-wrapper');
        let rows = Math.ceil(ulResizeDiv.clientHeight /75);
        let taskLength = $scope.tasks.length;
        let containerMaxWidth = (taskLength/rows)*300;
        containerMaxWidth = Math.ceil(containerMaxWidth/300)*300;
        if(smaller){
            container.style.width = "300px";
            gridConatiner.style.gridTemplateRows = `repeat(${taskLength}, 70px)`;
        } else {
            container.style.width = `${containerMaxWidth}px`;
            gridConatiner.style.gridAutoFlow = "column";
            gridConatiner.style.gridTemplateRows = `repeat(${rows}, 70px)`;
        }
    }

    $scope.onResize = function ($event) {
        window.addEventListener('mousemove', Resize, false);
        window.addEventListener('mouseup', stopResize, false);
    }

    function Resize(e) {
        let container = document.querySelector('.sqn-tasksflow');
        let gridConatiner = document.querySelector('.tm-tasks-list-wrapper');
        let ulResizeDiv = document.getElementById('ulResizeDiv');
        let taskLength = $scope.tasks.length;
        container.style.width = (e.clientX - container.offsetLeft) + 'px';
        
        let width = e.clientX - container.offsetLeft;
        let numberOfCols = Math.round(width/300);
        gridConatiner.style.gridTemplateRows = `repeat(${Math.ceil(taskLength/numberOfCols)}, 70px)`;
        gridConatiner.style.gridAutoFlow = "column";
        if(numberOfCols !== 1){
            ulResizeDiv.style.overflowX = 'auto';
        } else {
            ulResizeDiv.style.overflowX = 'hidden';
        }
    }

    function stopResize(e) {
        window.removeEventListener('mousemove', Resize, false);
        window.removeEventListener('mouseup', stopResize, false);
    }

});