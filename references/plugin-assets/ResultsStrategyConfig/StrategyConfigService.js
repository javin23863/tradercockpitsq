angular.module('app.resultstabs.strategyconfig').service('StrategyConfigService', function(BackendService, AppService, SQConstants) {
    
    this.print = function(data, callback) {
        BackendService.sendRequest('strategyconfig/print', data, callback, 'GET', true);
    }

    this.loadStrategySettings = function(data, callOnSuccess) {
        BackendService.sendRequest('strategyconfig/loadStrategySettings', data, callOnSuccess);
    }

    this.loadTasks = function(isTaskManager, projectName, callback) {
        instance.tasks.length = 0;

        if(isTaskManager) {
            let projectObj = SQConstants.getProjectConfig(projectName, true);
            if(projectObj) {
                var tasksObj = getChildElement(projectObj, 'Tasks');
                var taskObjs = getChildElements(tasksObj, 'Task');

                for(var i=0; i<taskObjs.length; i++) {
                    var taskObj = taskObjs[i];
                    var taskName = getAttrValue(taskObj, "name", null);
                    
                    var task = {
                        type: taskName,
                        name: taskName,
                        title: getAttrStringValue(taskObj, "title", taskName),
                    }

                    instance.tasks.push(task);
                }
            }

            callback();
        } else {
            instance.tasks.push({type: 'BUILDER', name: 'Builder', taskName: 'Build', title: 'Builder'});
            instance.tasks.push({type: 'RETESTER', name: 'Retester', taskName: 'Retest', title: 'Retester'});
            instance.tasks.push({type: 'OPTIMIZER', name: 'Optimizer', taskName: 'Optimize', title: 'Optimizer'});

            callback();
        }
    }

    var instance = this;
    this.tasks = [];
});