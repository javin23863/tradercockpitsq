angular.module('app.settings').service('ApplyMassConfigService', function($q, BackendService, AppService, L, SQEvents) {

    this.loadSettings = function () {
        instance.loadTasks(instance.config);

        var applyMassConfigObj = AppService.getCurrentTaskTabSettings("ApplyMassConfig");
        instance.loadSettingsObj(applyMassConfigObj, instance.config);
    }     
    
    this.saveSettings = function () {
        var applyMassConfigObj = AppService.getCurrentTaskTabSettings("ApplyMassConfig");
        if(!applyMassConfigObj) return;

        instance.saveSettingsObj(applyMassConfigObj, instance.config);
    }

    this.loadSettingsObj = function (applyMassConfigObj, config) {
        var loadFromObj = getChildElement(applyMassConfigObj, "LoadFrom");
        config.loadFrom.type = getNodeValue(loadFromObj, 'Type', 'file');
        config.loadFrom.file = getNodeValue(loadFromObj, 'File', '');
        config.loadFrom.task = getNodeValue(loadFromObj, 'Task', null);

        if(config.availableTasks.length>0) {
            if(!config.loadFrom.task) {
                config.loadFrom.task = config.availableTasks[0].taskName;

            } else {
                let found = false;

                for(var j=0; j<config.availableTasks.length; j++) {
                    if(config.availableTasks[j].taskName == config.loadFrom.task) {
                        found = true;
    
                        break;
                    }
                }

                if(!found) {
                    config.loadFrom.task = config.availableTasks[0].taskName;
                }
            }
        }

        var whatToApplyObj = getChildElement(applyMassConfigObj, "WhatToApply");
        config.whatToApply.data = getNodeBooleanValue(whatToApplyObj, 'Data', false);
        config.whatToApply.dataTradingEngine = getNodeBooleanValue(whatToApplyObj, 'DataTradingEngine', false);
        config.whatToApply.dataBacktestData = getNodeBooleanValue(whatToApplyObj, 'DataBacktestData', false);
        config.whatToApply.dataTestParameters = getNodeBooleanValue(whatToApplyObj, 'DataTestParameters', false);
        config.whatToApply.dataDataRangeParts = getNodeBooleanValue(whatToApplyObj, 'DataDataRangeParts', false);

        config.whatToApply.tradingOptions = getNodeBooleanValue(whatToApplyObj, 'TradingOptions', false);

        config.whatToApply.blocks = getNodeBooleanValue(whatToApplyObj, 'Blocks', false);
        config.whatToApply.blocksSignals = getNodeBooleanValue(whatToApplyObj, 'BlocksSignals', false);
        config.whatToApply.blocksIndicators = getNodeBooleanValue(whatToApplyObj, 'BlocksIndicators', false);
        config.whatToApply.blocksStopLimitEntry = getNodeBooleanValue(whatToApplyObj, 'BlocksStopLimitEntry', false);
        config.whatToApply.blocksOrderTypes = getNodeBooleanValue(whatToApplyObj, 'BlocksOrderTypes', false);
        config.whatToApply.blocksExitTypes = getNodeBooleanValue(whatToApplyObj, 'BlocksExitTypes', false);

        config.whatToApply.atm = getNodeBooleanValue(whatToApplyObj, 'Atm', false);
        config.whatToApply.moneyManagement = getNodeBooleanValue(whatToApplyObj, 'MoneyManagement', false);
        config.whatToApply.crossChecks = getNodeBooleanValue(whatToApplyObj, 'CrossChecks', false);

        config.whatToApply.whatToBuild = getNodeBooleanValue(whatToApplyObj, 'WhatToBuild', false);
        config.whatToApply.whatToRetest = getNodeBooleanValue(whatToApplyObj, 'WhatToRetest', false);
        config.whatToApply.partsToImprove = getNodeBooleanValue(whatToApplyObj, 'PartsToImprove', false);
        config.whatToApply.geneticOptions = getNodeBooleanValue(whatToApplyObj, 'GeneticOptions', false);
        config.whatToApply.optimization = getNodeBooleanValue(whatToApplyObj, 'Optimization', false);
        config.whatToApply.ranking = getNodeBooleanValue(whatToApplyObj, 'Ranking', false);

        for(var j=0; j<config.availableTasks.length; j++) {
            config.availableTasks[j].apply = false;
        }

        let tasks = getNodeValue(applyMassConfigObj, 'TargetTasks', '').split(',');
        for(var i=0; i<tasks.length; i++) {
            let task = tasks[i];

            for(var j=0; j<config.availableTasks.length; j++) {
                if(config.availableTasks[j].taskName == task) {
                    config.availableTasks[j].apply = true;

                    break;
                }
            }
        }
    }

    this.saveSettingsObj = function (applyMassConfigObj, config) {
        var xmlDoc = AppService.xmlDoc;

        var loadFromObj = createChild(applyMassConfigObj, "LoadFrom", xmlDoc);
        addNode('Type', config.loadFrom.type, loadFromObj, xmlDoc);
        addNode('File', config.loadFrom.file, loadFromObj, xmlDoc);
        addNode('Task', config.loadFrom.task, loadFromObj, xmlDoc);

        var whatToApplyObj = createChild(applyMassConfigObj, "WhatToApply", xmlDoc);
        addNode('Data', config.whatToApply.data, whatToApplyObj, xmlDoc);
        addNode('DataTradingEngine', config.whatToApply.dataTradingEngine, whatToApplyObj, xmlDoc);
        addNode('DataBacktestData', config.whatToApply.dataBacktestData, whatToApplyObj, xmlDoc);
        addNode('DataTestParameters', config.whatToApply.dataTestParameters, whatToApplyObj, xmlDoc);
        addNode('DataDataRangeParts', config.whatToApply.dataDataRangeParts, whatToApplyObj, xmlDoc);

        addNode('TradingOptions', config.whatToApply.tradingOptions, whatToApplyObj, xmlDoc);

        addNode('Blocks', config.whatToApply.blocks, whatToApplyObj, xmlDoc);
        addNode('BlocksSignals', config.whatToApply.blocksSignals, whatToApplyObj, xmlDoc);
        addNode('BlocksIndicators', config.whatToApply.blocksIndicators, whatToApplyObj, xmlDoc);
        addNode('BlocksStopLimitEntry', config.whatToApply.blocksStopLimitEntry, whatToApplyObj, xmlDoc);
        addNode('BlocksOrderTypes', config.whatToApply.blocksOrderTypes, whatToApplyObj, xmlDoc);
        addNode('BlocksExitTypes', config.whatToApply.blocksExitTypes, whatToApplyObj, xmlDoc);
        
        addNode('Atm', config.whatToApply.atm, whatToApplyObj, xmlDoc);
        addNode('MoneyManagement', config.whatToApply.moneyManagement, whatToApplyObj, xmlDoc);
        addNode('CrossChecks', config.whatToApply.crossChecks, whatToApplyObj, xmlDoc);

        addNode('WhatToBuild', config.whatToApply.whatToBuild, whatToApplyObj, xmlDoc);
        addNode('WhatToRetest', config.whatToApply.whatToRetest, whatToApplyObj, xmlDoc);
        addNode('PartsToImprove', config.whatToApply.partsToImprove, whatToApplyObj, xmlDoc);
        addNode('GeneticOptions', config.whatToApply.geneticOptions, whatToApplyObj, xmlDoc);
        addNode('Optimization', config.whatToApply.optimization, whatToApplyObj, xmlDoc);
        addNode('Ranking', config.whatToApply.ranking, whatToApplyObj, xmlDoc);

        let tasks = [];
        for(var i=0; i<config.availableTasks.length; i++) {
            if(config.availableTasks[i].apply) {
                tasks.push(config.availableTasks[i].taskName);
            }
        }

        addNode('TargetTasks', tasks.length>0 ? tasks.join() : "", applyMassConfigObj, xmlDoc);
    }

    this.loadTasks = function (config) {
        var projectObj = AppService.getProjectConfig();
        var tasksObj = getChildElement(projectObj, 'Tasks');
        var taskObjs = getChildElements(tasksObj, 'Task');

        config.availableTasks.length = 0;

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

            if (taskType != 'ApplyMassConfig') {
                config.availableTasks.push(task);
            }
        }
    }

    var instance = this;

    this.config = {
        loadFrom : {
            type: 'file',
            file: null,
            task: null
        },
        whatToApply: {
            data: false,
            dataTradingEngine: false,
            dataBacktestData: false,
            dataTestParameters: false,
            dataDataRangeParts: false,
            
            tradingOptions: false,

            blocks: false,
            blocksSignals: false,
            blocksIndicators: false,
            blocksStopLimitEntry: false,
            blocksOrderTypes: false,
            blocksExitTypes: false,
            
            atm: false,
            moneyManagement: false,
            crossChecks: false,

            whatToBuild: false,
            partsToImprove: false,
            geneticOptions: false,
            optimization: false,
            ranking: false
        },
        availableTasks: []
    }

});