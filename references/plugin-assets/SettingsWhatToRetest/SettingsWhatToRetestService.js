angular.module('app.settings').service('SettingsWhatToRetestService', function($q, $timeout, SQEvents, DatabankService, AppService, SQConstants) {

    this.loadAvailableDatabanks = function(callback) {
        DatabankService.loadVisibleDatabanks(instance.availableDatabanks, function(){
            if(window.appConfig.product == "RETESTER"){
                instance.availableDatabanks.splice(0, 0, { name : "<currently selected>" });
            }

            if(callback) callback();
        });
    }

    this.loadSettings = function() {
        instance.config.inputDatabank = DatabankService.getTaskInputDatabank();
        instance.config.outputDatabank = DatabankService.getTaskOutputDatabank();
        instance.config.retestSelected = DatabankService.getRetestSelected();
    }

    this.saveSettings = function() {
        DatabankService.setTaskInputDatabank(instance.config.inputDatabank);
        DatabankService.setTaskOutputDatabank(instance.config.outputDatabank);
        DatabankService.setRetestSelected(instance.config.retestSelected);
    }

    var instance = this;

    this.predefinedDatabanks = SQConstants.getConstants().gedatabanks;
    this.availableDatabanks = [];

    this.config = {
        inputDatabank: instance.predefinedDatabanks.results,
        outputDatabank: instance.predefinedDatabanks.results,
        retestSelected: false
    };

    this.isTaskManager = window.appConfig.product == 'TASKMANAGER';

    if(window.appConfig.product == "RETESTER"){
        SQEvents.addListener("Retester-beforeStart-listener", [SQEvents.get('PROJECT_BEFORE_START'), SQEvents.get('SETTINGS_TAB_RELOAD')], function(event, data){
            if(event == SQEvents.get('PROJECT_BEFORE_START')){
                if(instance.config.inputDatabank == "<currently selected>"){
                    DatabankService.setTaskInputDatabank(DatabankService.databankTabs.activeTab.title);
                }
        
                if(instance.config.outputDatabank == "<currently selected>"){
                    DatabankService.setTaskOutputDatabank(DatabankService.databankTabs.activeTab.title);
                }
            }
            else if(event == SQEvents.get('SETTINGS_TAB_RELOAD')){
                $timeout(function(){ instance.saveSettings(); }, 0, false); //preserve databank settings
            }
        });
    }

});