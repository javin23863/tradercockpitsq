angular.module('app.settings').service('BuildTaskService', function ($rootScope, $q, $timeout, $injector, SQEvents, SQConstants, AppService, SettingsService, BackendService, MoneyManagementService, RankingService, L) {

    this.loadSettings = function () {
        MoneyManagementService.getMMSettings();
        MoneyManagementService.getRMSettings();
    }

    this.calibrate = function (symbols, timeframes, maxSteps, engine, callback) {
        let taskName = null;
        if(window.parent.activeApp == "TASKMANAGER"){
            taskName = AppService.getTask().title;
            taskName = valueFilled(taskName) ? taskName : AppService.getTask().name;
        }
        
        BackendService.sendRequest("indyTester/calibrate", { 
            projectName : AppService.getProject(), 
            taskName : taskName, 
            symbols: symbols, 
            timeframes: timeframes, 
            maxSteps : maxSteps,
            engine: engine
        }, callback, 'POST');
    }

    this.saveSettings = function (config) {
        MoneyManagementService.saveConfig(true);
        AppService.updateTaskXML();
    }

    this.getStrategyTypeInfo = function (strategyType) {
        switch (strategyType) {
            case instance.strategyTypes.defaultForex: return L.tsq('Default build config for forex');
            case instance.strategyTypes.defaultFutures: return L.tsq('Default build config for futures');
            case instance.strategyTypes.defaultStockpicker: return L.tsq('Default build config for stockpicker');
            case instance.strategyTypes.market: return L.tsq('Strategies that open at market price, they use almost all availablle signals and can produce a variety of trading approaches');
            case instance.strategyTypes.trendFollowing: return L.tsq('Trend following (breakout) strategies that use stop orders to catch breakouts and go with the trend');
            case instance.strategyTypes.meanReversal: return L.tsq('Strategies that are exploiting reversal of price from extreme values to mean');
            case instance.strategyTypes.fuzzy: return L.tsq('Strategies using fuzzy logic rules - fuzzy rule has multiple conditions, but only a defined % of them must be valid in order for rule to be triggered');
            case instance.strategyTypes.daily: return L.tsq('Daily strategies that place high emphasis on their robustness in multiple markets');
            default: return L.tsq('Custom build settings are used.') + '<br/>' + L.tsq('Choose one of the types above to apply standard settings.');
        }
    }

    function calibrateSingleTask(callback, delay){
        window.parent.callAction('calibrateBeforeStart', $injector.get('BlocksService'), $injector.get('DataService'), function(){
            if(delay){
                setTimeout(callback, delay);
            }
            else {
                callback();
            }
        });
    }

    async function calibrateWholeProject(projectName, callback){
        AppService.updateTaskXML();
        
        if(AppService.getProject() !== projectName){
            AppService.setProject(projectName, function() { calibrateWholeProject(projectName, callback); });
            return;
        }   

        let projectConfig = AppService.getProjectConfig();
        let elTasks = getChildElement(projectConfig, "Tasks");
        let taskElems = getChildElements(elTasks, "Task");

        let originalTask = angular.copy(AppService.getTask());

        let taskChanged = false;

        for(let i=0; i<taskElems.length; i++){
            let elTask = taskElems[i];

            if(getAttrStringValue(elTask, "type") != "Build" || !getAttrBooleanValue(elTask, "active")) continue;

            let taskXMLFile = getAttrStringValue(elTask, "taskXMLFile");

            let elSettings = SQConstants.getTaskConfig(AppService.getProject(), taskXMLFile, true);
            let elBlocks = getChildElement(elSettings, "Blocks");
            let elCalibration = getChildElement(elBlocks, "Calibration");

            if(!getAttrBooleanValue(elCalibration, "calibrateBeforeStart")) continue;

            let taskType = getAttrStringValue(elTask, "type");

            let taskTypeOrig = null;
            if(taskType=='OptimizeConfigurable') {
                taskType = "Optimize";
                taskTypeOrig = 'OptimizeConfigurable';
            }
            
            AppService.setTask(taskType, getAttrStringValue(elTask, "name"), getAttrStringValue(elTask, "title"), taskTypeOrig, taskXMLFile);
            
            taskChanged = true;

            await new Promise(resolve => $timeout(function(){ calibrateSingleTask(resolve, 1000); }, 1000, false));
        }

        if(taskChanged){
            AppService.setTask(originalTask.type, originalTask.name, originalTask.title, originalTask.typeOrig, originalTask.taskXMLFile);
        }

        if(callback) callback();
    }

    this.config = MoneyManagementService.config;
    this.configRanking = RankingService.config;
    this.strategyTypes = SettingsService.strategyTypes;

    var instance = this;

    if(window.appConfig.product == "BUILDER"){
        SQEvents.addListener("Builder-beforeStart-listener", [SQEvents.get('PROJECT_BEFORE_START')], function(event, data){
            var deferred = $q.defer();

            $timeout(function(){
                calibrateSingleTask(deferred.resolve);
            }, 0, false);

            if(data.addPromise) {
                data.addPromise(deferred.promise);
            }
        });
    }
    if(window.appConfig.product == "TASKMANAGER"){
        SQEvents.addListener("TaskManager-beforeStart-listener", [SQEvents.get('PROJECT_BEFORE_START')], function(event, data){
            var deferred = $q.defer();
            
            $timeout(function(){
                calibrateWholeProject(data.projectName, deferred.resolve);
            }, 0, false);

            if(data.addPromise) {
                data.addPromise(deferred.promise);
            }
        });
    }

});