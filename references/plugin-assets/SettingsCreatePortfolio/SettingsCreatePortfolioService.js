angular.module('app.settings').service('SettingsCreatePortfolioService', function($q, BackendService, AppService, L, DatabankService, SQEvents, SQConstants) {

    function loadDatabanks() {
        DatabankService.loadVisibleDatabanks(instance.config.availableDatabanks);
    }

    this.saveSettings = function() {
        var xmlDoc = AppService.xmlDoc;
        var createPortfolioObj = AppService.getCurrentTaskTabSettings("CreatePortfolio");
        
        addNode('MaxStrategies', instance.config.maxStrategies, createPortfolioObj, xmlDoc);

        DatabankService.setTaskInputDatabank(instance.config.databankSource);
        DatabankService.setTaskOutputDatabank(instance.config.databankTarget);
    }
    
    this.loadSettings = function() {
        var xmlDoc = AppService.xmlDoc;

        var createPortfolioObj = AppService.getCurrentTaskTabSettings("CreatePortfolio");
        if(!createPortfolioObj) return;

        instance.config.maxStrategies = getNodeIntValue(createPortfolioObj, 'MaxStrategies', 100);

        instance.config.databankSource = DatabankService.getTaskInputDatabank();
        instance.config.databankTarget = DatabankService.getTaskOutputDatabank();
    }

    function loadInstruments() {
        loadToArray(instance.instruments, SQConstants.getConstants().instruments);

        if(instance.instruments.length>0) {
            instance.config.instrument = instance.instruments[0].instrument;
        }
    }

    var instance = this;

    this.config = {
        databankSource: "Results",
        databankTarget: "Results",
        availableDatabanks: [],
        maxStrategies: 100,
    }

    function onEvent(event, data){
        if(event == SQEvents.get("RELOAD_DATABANKS")){
            loadDatabanks();
        }
    }

    SQEvents.addListener("SettingsLoadFromFilesService", [SQEvents.get('RELOAD_DATABANKS')], onEvent);
});