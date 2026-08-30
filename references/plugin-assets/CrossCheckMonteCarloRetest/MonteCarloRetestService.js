angular.module('app').service('MonteCarloRetestService', function(BackendService, AppService, CrossChecksService, L) {
    
    this.list = function(callOnSuccess) {
        BackendService.sendRequest('monteCarloRetest/list', null, callOnSuccess);
    } 

    this.applySettings = function(settingsElem, config) {
        if(!settingsElem) {
            return;
        }

        var xmlDoc = AppService.xmlDoc;

        var methodsElem = getChildElements(settingsElem, 'Methods')[0];
        var methods = parseSettingsMethods(methodsElem);

        config.settingsTableInstance.applySettings(methods);

        config.numberOfSimulations = getNodeValue(settingsElem, "NumberOfSimulations", 10);

        config.useFullSample = getNodeBooleanValue(settingsElem, "MCUseFullSample", false);

        var precision = getNodeIntValue(settingsElem, "MCBacktestPrecision", -1);
        config.usePrecision = precision != -1;
        config.precision = precision;
    }

    this.loadSettings = function(settingsElem, config) {
        var xmlDoc = AppService.xmlDoc;

        var methodsElem = xmlDoc.createElement('Methods');
        var methodsObj = settingsElem.appendChild(methodsElem);

        config.settingsTableInstance.getValuesXml(xmlDoc, methodsObj); //save setttings

        //Number of simulations
        addNode('NumberOfSimulations', config.numberOfSimulations, settingsElem, xmlDoc);
        
        addNode('MCUseFullSample', config.useFullSample, settingsElem, xmlDoc);
        addNode('MCBacktestPrecision', config.usePrecision ? config.precision : -1, settingsElem, xmlDoc);
    }
    
    this.getInfo = function(settingsElem) {
        if(!settingsElem) {
            return L.tsq("N/A");
        }

        var numberOfTests = getNumberOfSimulations(settingsElem);
        var numberOfSimulations = getNodeValue(settingsElem, "NumberOfSimulations", 10);
        
        return L.tsq("%d tests with %d simulations", [numberOfTests, numberOfSimulations]);
    }

    this.getShortInfo = function(settingsElem) {
        return instance.getInfo(settingsElem);
    }

    this.getAverageDuration = function(settingsElem){
        var numberOfSimulations = getNodeValue(settingsElem, "NumberOfSimulations", 0);
        if(!numberOfSimulations) return 0;

        var duration = CrossChecksService.getAverageDuration(numberOfSimulations, true) * 2;
        return duration < 0.5 ? 0.5 : duration;
    }

    function getNumberOfSimulations(settingsElem) {
        var methodsElem = getChildElement(settingsElem, 'Methods');
        var methods = parseSettingsMethods(methodsElem);

        var count = 0;

        for(var i=0; i<methods.length; i++) {
            if(methods[i].use) {
                count++;
            }
        }

        return count;
    }

    var instance = this;
    
});