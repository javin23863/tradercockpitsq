angular.module('app.settings').service('TaskAutoRetestService', function ($rootScope, SQConstants, SQEvents, AppService, DatabankService) {
   
    this.loadSettings = function() {
        DatabankService.loadVisibleDatabanks(instance.config.availableDatabanks);

        instance.config.sourceDatabank = DatabankService.getTaskInputDatabank();
        instance.config.targetDatabank = DatabankService.getTaskOutputDatabank();
    }

    this.saveSettings = function() {
        DatabankService.setTaskInputDatabank(instance.config.sourceDatabank);
        DatabankService.setTaskOutputDatabank(instance.config.targetDatabank);

        AppService.updateTaskXML();
    }

    function onEvent(event, data){
        if(event == SQEvents.get("DATABANK_DISPLAY")){
            DatabankService.loadVisibleDatabanks(instance.config.availableDatabanks);
        }
        else if(event == SQEvents.get("RELOAD_DATABANKS")){
            DatabankService.loadVisibleDatabanks(instance.config.availableDatabanks);
        }
    }

    var instance = this;

    this.constants = SQConstants.getConstants();

    this.config = {
        availableDatabanks : [],
        sourceDatabank : this.constants.gedatabanks.results,
        targetDatabank : this.constants.gedatabanks.results
    }

    SQEvents.addListener("TaskAutoRetestService", [
        SQEvents.get("DATABANK_DISPLAY"), 
        SQEvents.get('RELOAD_DATABANKS')
    ], onEvent);

});