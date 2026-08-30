angular.module('app.tmtasks').service('TMTasksService', function($rootScope, $q, BackendService, AppService, $timeout, SQConstants) {

    this.loadAvailableTasks = function() {
        BackendService.sendRequest('taskmanager/getAvailableTasks', null, function(result) {
            let tasks = result.tasks;
            tasks.sort(function (a, b) {
                return a.name.localeCompare(b.name);
            });

            loadToArray(instance.availableTasks, tasks);
        });
    }       

    this.addTask = function(data, callback) {
        addCurrentProjectConfig(data);

        BackendService.sendRequest('taskmanager/addTask', data, function(result) {
            let projectConfigObj = SQConstants.getProjectConfigObj(AppService.getProject());
            projectConfigObj.xml = result.xmlConfig;
            projectConfigObj.tasks = projectConfigObj.tasks || {};
            projectConfigObj.tasks[result.taskXMLFile] = result.taskXML;

            SQConstants.setProjectConfigObj(AppService.getProject(), projectConfigObj);

            AppService.setProject(AppService.getProject(), callback);  
        }, 'POST');
    }   

    this.renameTask = function(data, callback) {
        addCurrentProjectConfig(data);

        BackendService.sendRequest('taskmanager/renameTask', data, function(result) {
            SQConstants.setProjectConfig(AppService.getProject(), result.xmlConfig);

            AppService.reloadConfig(function(result2) {
                callback();
            });  
        }, 'POST');
    }   

    this.activateTask = function(data) {
        addCurrentProjectConfig(data);

        BackendService.sendRequest('taskmanager/activateTask', data, function(result) {
            let taskElem = null;

            try {
                var tasksObj = getChildElement(AppService.getProjectConfig(), 'Tasks');
                var taskObjs = getChildElements(tasksObj, 'Task');

                for(var i=0; i<taskObjs.length; i++) {
                    let taskObj = taskObjs[i]; 
                    let name = getAttrValue(taskObj, 'name', false);

                    if(name === data.taskName) {
                        taskElem = taskObj;
                        break;
                    }
                }

                taskElem.setAttribute("active", data.active);

                SQConstants.setProjectConfig(data.projectName, xmlToString(AppService.getProjectConfig()));
            }
            catch(err){
                console.error(err);
                $rootScope.showError("Unable to " + (data.active ? "activate" : "deactivate") + " task - " + err.message);
            }
        }, 'POST');
    }   

    this.cloneTask = function(data, callback) {
        addCurrentProjectConfig(data);

        BackendService.sendRequest('taskmanager/cloneTask', data, function(result) {
            let projectConfigObj = SQConstants.getProjectConfigObj(AppService.getProject());
            projectConfigObj.xml = result.xmlConfig;
            projectConfigObj.tasks = projectConfigObj.tasks || {};
            projectConfigObj.tasks[result.taskXMLFile] = result.taskXML;

            SQConstants.setProjectConfigObj(AppService.getProject(), projectConfigObj);

            AppService.setProject(AppService.getProject(), callback);  
        }, 'POST');
    }  

    this.removeTask = function(data, callback) {
        addCurrentProjectConfig(data);

        BackendService.sendRequest('taskmanager/removeTask', data, function(result) {
            let projectConfigObj = SQConstants.getProjectConfigObj(AppService.getProject());
            projectConfigObj.xml = result.xmlConfig;
            delete projectConfigObj.tasks[result.taskXMLFile];

            SQConstants.setProjectConfigObj(AppService.getProject(), projectConfigObj);

            AppService.reloadConfig(function(result2) {
                if(AppService.getTask().taskXMLFile === result.taskXMLFile){
                    AppService.setFirstTask();
                }
                callback();
            });  
        }, 'POST');
    }   

    this.moveTask = function(data, callback) {
        addCurrentProjectConfig(data);

        BackendService.sendRequest('taskmanager/moveTask', data, function(result) {
            SQConstants.setProjectConfig(AppService.getProject(), result.xmlConfig);

            AppService.reloadConfig(function(result2) {
                callback();
            });  
        }, 'POST');
    } 

    this.loadTasks = function() {
        instance.tasks.length = 0;

        var projectObj = AppService.getProjectConfig();
        var tasksObj = getChildElement(projectObj, 'Tasks');
        var taskObjs = getChildElements(tasksObj, 'Task');

        for(var i=0; i<taskObjs.length; i++) {
            var taskObj = taskObjs[i];

            var taskName =  getAttrValue(taskObj, "name", null);

            var task = {
                taskNumber: i+1,
                projectName: instance.projectName,
                taskType: getAttrValue(taskObj, "type", null),
                taskName: taskName,
                taskTitle: getAttrStringValue(taskObj, "title", taskName),
                taskXMLFile: getAttrStringValue(taskObj, "taskXMLFile", null),
                active: getAttrBooleanValue(taskObj, "active", true),
                iterations: 0
            }

            instance.tasks.push(task);
        }
    }

    this.findTask = function(taskName, taskType) {
        for(var i=0; i<instance.tasks.length; i++) {
            var task = instance.tasks[i];  

            if(task.taskType==taskType && task.taskName==taskName) {
                return task;
            }
        }

        return null;
    }

    function addCurrentProjectConfig(dataObj){
        var xmlConfig = AppService.getProjectConfig();
        dataObj.projectXML = xmlConfig ? xmlToString(xmlConfig) : null;
    }

    var instance = this;

    this.tasks = [];
    this.availableTasks = [];
    this.projectName = null;
    this.clickedTask = null;

    instance.loadAvailableTasks();
});