angular.module('app.settings').service('SettingsCustomAnalysisService', function($q, BackendService, AppService, L, DatabankService, SQEvents) {


    function init() {
        instance.config.availableMethods.length = 0;
        instance.config.availableMethodsPerStrategy.length = 0;
        instance.config.availableMethodsFullDatabank.length = 0;

        var noneMethod = {"name": L.tsq("None"), "value": "none"}
        instance.config.availableMethods.push(noneMethod);
        instance.config.availableMethodsPerStrategy.push(noneMethod);
        instance.config.availableMethodsFullDatabank.push(noneMethod);

        var methods = InitializationData().settings.CustomAnalysis.methods;        
        for (var i = 0; i < methods.length; i++) {
            let method = methods[i];
            instance.config.availableMethods.push(method);

            if(method.type==20 || method.type==30) instance.config.availableMethodsPerStrategy.push(method);
            if(method.type==10 || method.type==30) instance.config.availableMethodsFullDatabank.push(method);
        }
    }

    function loadDatabanks() {
        DatabankService.loadVisibleDatabanks(instance.config.availableDatabanks);
    }

    this.saveSettings = function() {
        var xmlDoc = AppService.xmlDoc;
        var caObj = AppService.getCurrentTaskTabSettings("CustomAnalysis");
       
        addNode('PerStrategy1', instance.config.perStrategy1, caObj, xmlDoc);
        addNode('PerStrategy2', instance.config.perStrategy2, caObj, xmlDoc);
        addNode('FullDatabank1', instance.config.fullDatabank1, caObj, xmlDoc);
        addNode('FullDatabank2', instance.config.fullDatabank2, caObj, xmlDoc);
        addNode('Filter', instance.config.filter, caObj, xmlDoc);

        addNode('InputArgsPerStrategy1', instance.config.inputArgsPerStrategy1, caObj, xmlDoc);
        addNode('InputArgsPerStrategy2', instance.config.inputArgsPerStrategy2, caObj, xmlDoc);
        addNode('InputArgsFullDatabank1', instance.config.inputArgsFullDatabank1, caObj, xmlDoc);
        addNode('InputArgsFullDatabank2', instance.config.inputArgsFullDatabank2, caObj, xmlDoc);

        DatabankService.setTaskInputDatabank(instance.config.databankSource);
        DatabankService.setTaskOutputDatabank(instance.config.databankTarget);
    }
    
    this.loadSettings = function() {
        var xmlDoc = AppService.xmlDoc;

        var caObj = AppService.getCurrentTaskTabSettings("CustomAnalysis");

        instance.config.perStrategy1 = getNodeValue(caObj, 'PerStrategy1', 'none');
        instance.config.perStrategy2 = getNodeValue(caObj, 'PerStrategy2', 'none');
        instance.config.fullDatabank1 = getNodeValue(caObj, 'FullDatabank1', 'none');
        instance.config.fullDatabank2 = getNodeValue(caObj, 'FullDatabank2', 'none');
        instance.config.filter = getNodeBooleanValue(caObj, 'Filter', false);
        instance.config.inputArgsPerStrategy1 = getNodeValue(caObj, 'InputArgsPerStrategy1', '');
        instance.config.inputArgsPerStrategy2 = getNodeValue(caObj, 'InputArgsPerStrategy2', '');
        instance.config.inputArgsFullDatabank1 = getNodeValue(caObj, 'InputArgsFullDatabank1', '');
        instance.config.inputArgsFullDatabank2 = getNodeValue(caObj, 'InputArgsFullDatabank2', '');

        instance.config.databankSource = DatabankService.getTaskInputDatabank();
        instance.config.databankTarget = DatabankService.getTaskOutputDatabank();
    }

    var instance = this;

    this.config = {
        perStrategy1: "none",
        perStrategy2: "none",
        fullDatabank1: "none",
        fullDatabank2: "none",
        databankSource: "Results",
        databankTarget: "Results",
        filter: false,

        inputArgsPerStrategy1: "",
        inputArgsPerStrategy2: "",
        inputArgsFullDatabank1: "",
        inputArgsFullDatabank2: "",

        availableDatabanks: [],

        availableMethods: [],
        availableMethodsPerStrategy: [],
        availableMethodsFullDatabank: [],
    }

    function onEvent(event, data){
        if(event == SQEvents.get("RELOAD_DATABANKS")){
            loadDatabanks();
        }
    }

    init();

    SQEvents.addListener("SettingsCustomAnalysisService", [SQEvents.get('RELOAD_DATABANKS')], onEvent);
});