angular.module('app.settings').service('CrossChecksService', function($rootScope, $q, $injector, SQConstants, SQEvents, BackendService, AppService, DataService, L) {
   
    var instance = this;
    var loaded = false;

    this.config = {
        crosschecks: [],
        disabled: false,
        evaluateAll: false
    }

    this.list = function(callOnSuccess, forceReload) {
        if(loaded && !forceReload) {
            callOnSuccess();
            return;
        }
        
        loaded = false;

        var crosschecks = InitializationData().settings.CrossChecks.crosschecks;
        for (var i = 0; i < crosschecks.length; i++) {
            var crosscheck = crosschecks[i];

            instance.config.crosschecks.push(crosscheck);
        }

        callOnSuccess();
        
        loaded = true;
    }   

    this.loadConfig = function() {
        var xmlDoc = AppService.xmlDoc;
        var settingsObj = AppService.getTaskConfig();
        if(!settingsObj){
            let projectConfigObj = SQConstants.getProjectConfigObj(AppService.getProject());
            if(Object.keys(projectConfigObj.tasks).length !== 0){   //non-empty project
                console.error("XML element 'Settings' not found");
                $rootScope.showError(L.tsq("App XML error"), L.tsq("XML element 'Settings' not found"));
            }
            return;
        }

        var crossChecksObj = getChildElement(settingsObj, 'CrossChecks');
        if(!crossChecksObj){
            var crossChecksElem = xmlDoc.createElement('CrossChecks');
            crossChecksObj = settingsObj.appendChild(crossChecksElem);
        }

        instance.config.disabled = $rootScope.license.isProVersion ? !getAttrBooleanValue(crossChecksObj, 'use', true) : true;
        instance.config.evaluateAll = getAttrBooleanValue(crossChecksObj, 'evaluateAll', true);

        for(var i=0; i<instance.config.crosschecks.length; i++) {
            var crosscheck = instance.config.crosschecks[i];
            
            var crossCheckObj = createChild(crossChecksObj, crosscheck.name, xmlDoc);
            crosscheck.use = getAttrBooleanValue(crossCheckObj, 'use', false);
        }
    }

    this.saveConfig = function() {
        var xmlDoc = AppService.xmlDoc;
        var settingsObj = AppService.getTaskConfig();
        
        if(!settingsObj){
            console.error("Corrupted XML - element 'Settings' not found");
            $rootScope.showError(L.tsq("Task XML error while saving config"), L.tsq("Corrupted XML - element 'Settings' not found"));
            return;
        }
        
        var crossChecksObj = createChild(settingsObj, 'CrossChecks', xmlDoc);
        crossChecksObj.setAttribute("use", !instance.config.disabled);
        crossChecksObj.setAttribute("evaluateAll", instance.config.evaluateAll);

        for(var i=0; i<instance.config.crosschecks.length; i++) {
            var crossCheckObj = createChild(crossChecksObj, instance.config.crosschecks[i].name, xmlDoc);
            crossCheckObj.setAttribute("use", instance.config.crosschecks[i].use);
        }
    }

    this.saveSettings = function(crossCheckName, settingsElem, acceptSettingsElem) {
        var xmlDoc = AppService.xmlDoc;
        var settingsObj = AppService.getTaskConfig();
        
        if(!settingsObj){
            console.error("Corrupted XML - element 'Settings' not found");
            $rootScope.showError(L.tsq("Task XML error while saving settings"), L.tsq("Corrupted XML - element 'Settings' not found"));
            return;
        }

        var crossChecksObj = getChildElement(settingsObj, 'CrossChecks');
        if(!crossChecksObj) {
            return;
        }

        var crossCheckObj = createChild(crossChecksObj, crossCheckName, xmlDoc);
        removeAllChildren(crossCheckObj);
        crossCheckObj.appendChild(settingsElem);
        crossCheckObj.appendChild(acceptSettingsElem);

        console.log("Cross check settings changed", crossCheckName, settingsElem, acceptSettingsElem);
    }

    this.createElement = function(name) {
        var xmlDoc = AppService.xmlDoc;
        return xmlDoc.createElement(name);
    }

    this.getCrossChecksElem = function(){
        if(!AppService.getTaskXMLConfig()) return null;

        var settingsObj = AppService.getTaskConfig();
        if(!settingsObj){
            return null;
        }

        return getChildElement(settingsObj, 'CrossChecks');
    }

    this.getSettingsElem = function(crossCheckName, settingName) {
        var crossChecksObj = this.getCrossChecksElem();
        if(!crossChecksObj) {
            return;
        }

        var crossCheckObj = getChildElement(crossChecksObj, crossCheckName);
        if(!crossCheckObj) {
            return;
        }

        return getChildElement(crossCheckObj, settingName);
    }

    this.getAverageDuration = function(backtestsCount, useMainSetup, elSetups, customPrecision){
        var mainSetup = null;
        var elData = AppService.getCurrentTaskTabSettings("Data");
        let elMainSetups = getChildElement(elData, "Setups");
        
        if(elMainSetups){
            var mainSetups = [];
            DataService.loadSetup(mainSetups, getChildElement(elMainSetups, "Setup"));
            mainSetup = mainSetups[0];
        }
        
        if(useMainSetup){
            elSetups = elMainSetups;
        }

        if(!elSetups) return 0;

        var totalTicksCount = 0;

        var constants = SQConstants.getConstants();
        var setupElems = getChildElements(elSetups, "Setup");
        var setups = [];

        for(var i=0; i<setupElems.length; i++){
            DataService.loadSetup(setups, setupElems[i]);
            var setup = setups[i];

            try {
                var dataInfo = getItem(constants.data, 'symbol', setup.symbol);

                var originalDataLength = dataInfo.dateTo - dataInfo.dateFrom;
                var usedDataLength = getDate(setup.dateTo).getTime() - getDate(setup.dateFrom).getTime();

                var realTimeframe = setup.timeframe != "Same as main data" ? setup.timeframe : mainSetup.timeframe;

                var dataTicksCount = getTicksCount(dataInfo, valueFilled(customPrecision) ? customPrecision : setup.precision, realTimeframe);
                if(dataTicksCount == -1) continue;

                var usedTicksCount = dataTicksCount * usedDataLength / originalDataLength;
                totalTicksCount += usedTicksCount;
            } 
            catch(err){
                console.error("Cannot calculate number of ticks. " + err, setup);
                continue;
            }
        }
        return parseFloat(backtestsCount) * totalTicksCount * constants.benchmarkTimePerTick;
    }

    function getTicksCount(dataInfo, precision, timeframe){
        switch(precision){
            case 1:     //Selected TF
                return dataInfo.secondsRecords / getTFSeconds(timeframe);
            case 2:     //Base TF - 1 minute data
                return dataInfo.secondsRecords / 60;
            case 3:     //Custom spread
            case 4:     //Real spread
                return dataInfo.rows;
            default: return -1;
        }
    }

    this.get = function(crossCheckName){
        return $injector.get(crossCheckName + "Service");
    }

    //broadcast REFRESH_CC_TIMES event coming from main app
    if(!isMainApp() && !isAlgoWizard()){
        window.parent.addListener("CrossCheckService-" + window.appConfig.product, [ SQEvents.get('REFRESH_CC_TIMES') ], function(event, data){
            SQEvents.notifyListeners(SQEvents.get('REFRESH_CC_TIMES'));
        });
    }

});