angular.module('app.settings').service('SettingsLoadFromFilesService', function($q, BackendService, AppService, L, DatabankService, SQEvents, SQConstants) {

    this.loadDatabanks = function() {
        DatabankService.loadVisibleDatabanks(instance.config.availableDatabanks);
    }

    this.saveSettings = function() {
        var xmlDoc = AppService.xmlDoc;
        var loadFromFilesObj = AppService.getCurrentTaskTabSettings("LoadFromFiles");
        
        addNode('SourceDirectory', instance.config.directory, loadFromFilesObj, xmlDoc);
        addNode('IncludeSubdirectories', instance.config.includeSubdirectories, loadFromFilesObj, xmlDoc);
        addNode('UnrecognizedInstrumentAction', instance.config.noInstrumentAction, loadFromFilesObj, xmlDoc);
        addNode('UnrecognizedInstrumentValue', instance.config.instrument, loadFromFilesObj, xmlDoc);

        DatabankService.setTaskInputDatabank(instance.config.databank);
    }
    
    this.loadSettings = function() {
        var xmlDoc = AppService.xmlDoc;

        var loadFromFilesObj = AppService.getCurrentTaskTabSettings("LoadFromFiles");
        if(!loadFromFilesObj) return;

        instance.config.directory = getNodeValue(loadFromFilesObj, 'SourceDirectory', '');
        instance.config.includeSubdirectories = getNodeBooleanValue(loadFromFilesObj, 'IncludeSubdirectories', false);
        instance.config.databank = DatabankService.getTaskInputDatabank();

        instance.config.noInstrumentAction = getNodeValue(loadFromFilesObj, 'UnrecognizedInstrumentAction', '0');
        instance.config.instrument = getNodeValue(loadFromFilesObj, 'UnrecognizedInstrumentValue', null);

        var instrument = getItem(instance.instruments, 'instrument', instance.config.instrument);
        if(!instrument && instance.instruments.length>0) {
            instance.config.instrument = instance.instruments[0].instrument;
        }
    }

    function loadInstruments() {
        loadToArray(instance.instruments, SQConstants.getConstants().instruments);

        if(instance.instruments.length>0) {
            instance.config.instrument = instance.instruments[0].instrument;
        }
    }

    var instance = this;

    this.config = {
        databank: "Results",
        availableDatabanks: [],
        includeSubdirectories: false,
        directory: null,
        noInstrumentAction: "0",
        instrument: null
    }

    this.instruments = [];

    loadInstruments();

    function onEvent(event, data){
        if(event == SQEvents.get("RELOAD_DATABANKS")) {
            instance.loadDatabanks();

        } else if(event == SQEvents.get('INSTRUMENTS_CHANGED')) {
            loadInstruments();
        } 
    }

    SQEvents.addListener("SettingsLoadFromFilesService", [SQEvents.get('RELOAD_DATABANKS'), SQEvents.get('INSTRUMENTS_CHANGED')], onEvent);
});