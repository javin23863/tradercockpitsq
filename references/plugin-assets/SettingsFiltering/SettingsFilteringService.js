angular.module('app.settings').service('SettingsFilteringService', function($q, BackendService, AppService, L, DatabankService, SQEvents) {

    function loadDatabanks() {
        DatabankService.loadVisibleDatabanks(instance.config.availableDatabanks);
    }

    this.saveSettings = function() {
        var xmlDoc = AppService.xmlDoc;
        var filteringObj = AppService.getCurrentTaskTabSettings("Filtering");

        if(!filteringObj || !instance.conditions.obj) return;

        removeAllChildren(filteringObj);

        addNode('ActionType', instance.config.actionType, filteringObj, xmlDoc);
        addNode('MaxStrategies', instance.config.maxStrategiesLimited == 1 ? instance.config.maxStrategies : 0, filteringObj, xmlDoc);

        //conditions
        addNode('ConditionsType', instance.config.conditionsType, filteringObj, xmlDoc);

        filteringObj.appendChild(instance.conditions.obj);

        instance.conditions.count = getTotalConditions(instance.conditions.obj);

        //databanksObj
        var databanksObj = AppService.getCurrentTaskTabSettings("Databanks");
        var databankObjs = getChildElements(databanksObj, 'Databank');

        for(var i=0; i<databankObjs.length; i++) {
            var databankObj = databankObjs[i];

            var databankName = getAttrValue(databankObj, "name", null);

            if(databankName=="Source") { databankObj.setAttribute("value", instance.config.databankSource); }
            if(databankName=="Target") { databankObj.setAttribute("value", instance.config.databankTarget); }
        }
    }
    
    this.loadSettings = function() {
        var xmlDoc = AppService.xmlDoc;

        var filteringObj = AppService.getCurrentTaskTabSettings("Filtering");
        if(!filteringObj) return;

        instance.config.actionType = getNodeIntValue(filteringObj, 'ActionType', 0);
        
        instance.config.maxStrategies = getNodeIntValue(filteringObj, 'MaxStrategies', 0);
        if(instance.config.maxStrategies>0) {
            instance.config.maxStrategiesLimited = 1;
        } else {
            instance.config.maxStrategiesLimited = 0;
            instance.config.maxStrategies = 100;
        }

        //conditions
        instance.config.conditionsType = getNodeIntValue(filteringObj, 'ConditionsType', 0);
  
        createChild(filteringObj, "Conditions", xmlDoc);
        instance.conditions.obj = getChildElement(filteringObj, "Conditions");      

        instance.conditions.count = getTotalConditions(instance.conditions.obj);

        var databanksObj = AppService.getCurrentTaskTabSettings("Databanks");
        var databankObjs = getChildElements(databanksObj, 'Databank');

        for(var i=0; i<databankObjs.length; i++) {
            var databankObj = databankObjs[i];

            var databankName = getAttrValue(databankObj, "name", null);
            var databankValue = getAttrValue(databankObj, "value", null);
            
            if(databankName=="Source") { instance.config.databankSource = databankValue; }
            if(databankName=="Target") { instance.config.databankTarget = databankValue; }
        }
    }

    function getTotalConditions(conditionsObj) {
        var totalConditions = 0;

        if (conditionsObj) {
            var conditionObjs = getChildElements(conditionsObj, "Condition");

            for (var i = 0; i < conditionObjs.length; i++) {
                var conditionObj = conditionObjs[i];

                if(correctAttrValue(conditionObj.attributes['use'], true)) {
                    totalConditions++;
                }
            }
        }

        return totalConditions;
    }

    var instance = this;

    this.config = {
        conditionsType: 0,
        actionType: 0,
        databankSource: null,
        databankTarget: null,
        availableDatabanks: [],
        atri : L.tsq("default value"),
        maxStrategiesLimited: 0,
        maxStrategies: 100
    }

    this.conditions = {
        obj: {},
        count: 0
    }

    function onEvent(event, data){
        if(event == SQEvents.get("RELOAD_DATABANKS")){
            loadDatabanks();
        }
    }

    SQEvents.addListener("SettingsFilteringService", [SQEvents.get('RELOAD_DATABANKS')], onEvent);
});