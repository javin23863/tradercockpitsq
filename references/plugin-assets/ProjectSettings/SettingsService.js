angular.module('app.settings').service('SettingsService', function($rootScope, $q, $injector, sqPlugin, SQEvents, BackendService, AppService, SQConstants, L) {
    
    this.getTaskInfoPanels = function(customGroups) {
        var taskType = AppService.getTask().type;
        var taskPlugin = getItem(sqPlugin.getPlugins("SimpleTaskSettings"), 'taskType', taskType);
        if(!taskPlugin){
            console.error("Unknown task type '" + taskType + "'");
            return [];
        }

        var xmlConfig = AppService.getTaskConfig();

        if(customGroups){
            var settingsPlugins = sqPlugin.getPlugins('SettingsTab');

            for(var i=0; i<customGroups.length; i++){
                var customGroup = customGroups[i];

                for(var a=0; a<customGroup.settings.length; a++){
                    var setting = customGroup.settings[a];

                    if(setting.configElemName){
                        setting.value = getTaskInfoPanelSettings(xmlConfig, getItem(settingsPlugins, 'configElemName', setting.configElemName), $injector);
                    }
                }
            }

            return customGroups;
        }
        else return taskPlugin.getInfoPanels(xmlConfig, $injector);
    }   

    this.getStrategyType = function(){
        var taskObj = getTask(AppService.getProjectConfig(), AppService.getTask()); 
        var sampleName = getAttrStringValue(taskObj, 'sampleName', instance.strategyTypes.custom);

        switch(sampleName){
            case instance.strategyTypes.market:
            case instance.strategyTypes.trendFollowing:
            case instance.strategyTypes.meanReversal:
            case instance.strategyTypes.fuzzy:
            case instance.strategyTypes.daily:
            case instance.strategyTypes.defaultForex:
            case instance.strategyTypes.defaultFutures:
                return sampleName;
            default: 
                return instance.strategyTypes.custom;
        }
    }

    this.applySimpleTemplate = function(strategyType, callback){
        //$rootScope.showConfirm(L.tsq("Settings reset"), L.tsq("Do you want to reset your settings to '%s' strategy type?", [strategyType]), function (confirmed) {
        //    if (confirmed) {
                var args = { 
                    projectName : AppService.getProject(), 
                    taskName : AppService.getTask().name, 
                    strategyType : strategyType 
                };
                
                BackendService.sendRequest('project/applySimpleTemplate', args, function(result){
                    try {
                        AppService.xmlConfig = xmlToObject(result.xmlConfig);
                        AppService.xmlDoc = getXMLDoc(result.xmlConfig);

                        SQConstants.setProjectConfig(AppService.getProject(), result.xmlConfig);

                        AppService.taskXMLConfig = xmlToObject(result.taskXML);
                        AppService.taskXMLDoc = getXMLDoc(result.taskXML);

                        SQConstants.setTaskConfig(AppService.getProject(), result.taskXMLFile, result.taskXML);

                        AppService.lastConfigHash = MurmurHash3(result.xmlConfig).result();
                        AppService.lastTaskConfigHash = MurmurHash3(result.taskXML).result();
                        
                        SQEvents.notifyListeners(SQEvents.get("SETTINGS_TAB_RELOAD"), 'all');
                        SQEvents.notifyListeners(SQEvents.get("TASK_INFO_UPDATE"), { simpleSettings : { strategyType : strategyType }});
        
                        if(callback) callback();
                    } 
                    catch (error) {
                        $rootScope.showError(L.tsq("Cannot load template '%s' - %s", [strategyType, error]));
                    }
                });   
        //    }
        //}, true);      
    }

    var instance = this; 

    this.strategyTypes = SQConstants.getConstants().simpleStrategyTypes;

});