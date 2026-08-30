angular.module('app.settings').service('TaskClearDatabanksService', function ($rootScope, SQConstants, SQEvents, AppService, DatabankService, L) {
   
    this.loadSettings = function() {
        var elClearDatabanks = AppService.getCurrentTaskTabSettings("ClearDatabanks");
        var databankElems = getChildElements(elClearDatabanks, "Databank");

        instance.config.databanks.length = 0;

        for(var i=0; i<databankElems.length; i++){
            var elDatabank = databankElems[i];
            instance.config.databanks.push(getAttrStringValue(elDatabank, "name", "Results"));
        }

        reloadDatabanks();
    }

    function reloadDatabanks(){
        var databanks = instance.config.databanks;
        var configChanged = false;

        DatabankService.loadVisibleDatabanks(instance.availableDatabanks, function() {
            for(var i=databanks.length-1; i>=0; i--){
                var databank = instance.config.databanks[i];
    
                //delete non-existing databanks from list
                if(!getItem(instance.availableDatabanks, "name", databank) && databank!='all'){
                    databanks.splice(i, 1);
                    configChanged = true;
                }
            }

            instance.availableDatabanks.push({ title: L.tsq('All databanks'), name: "all" });
    
            if(configChanged){
                instance.saveSettings();
            }
        });
    }

    this.saveSettings = function(dontSendRequest) {
        var elClearDatabanks = AppService.getCurrentTaskTabSettings("ClearDatabanks");
        if(!elClearDatabanks) return;

        removeAllChildren(elClearDatabanks);

        for(var i=0; i<instance.config.databanks.length; i++){
            var databank = instance.config.databanks[i];

            var elDatabank = createChild(elClearDatabanks, "Databank", AppService.xmlDoc, true);
            elDatabank.setAttribute("name", databank);
        }

        if(!dontSendRequest){
            AppService.updateTaskXML();
        }
    }

    function onEvent(event, data){
        if(event == SQEvents.get("DATABANK_DISPLAY")){
            reloadDatabanks();
        }
        else if(event == SQEvents.get("RELOAD_DATABANKS")){
            reloadDatabanks();
        }
    }

    var instance = this;

    this.constants = SQConstants.getConstants();

    this.config = {
        databanks : []
    }

    this.availableDatabanks = [];

    SQEvents.addListener("TaskAutoRetestService", [
        SQEvents.get("DATABANK_DISPLAY"), 
        SQEvents.get('RELOAD_DATABANKS')
    ], onEvent);

});