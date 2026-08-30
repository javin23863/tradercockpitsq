angular.module('app').service('MonteCarloManipulationService', function(BackendService, AppService, L) {
    
    this.list = function(callOnSuccess) {
        BackendService.sendRequest('monteCarloManipulation/list', null, callOnSuccess);
    } 

    this.applySettings = function(settingsElem, config) {
        if(!settingsElem) {
            return;
        }

        var xmlDoc = AppService.xmlDoc;

        var methodsElem = getChildElement(settingsElem, 'Methods');
        var methods = parseSettingsMethods(methodsElem);

        config.settingsTableInstance.applySettings(methods);

        config.numberOfSimulations = getNodeValue(settingsElem, "NumberOfSimulations", 10);
        config.useFullSample = getNodeBooleanValue(settingsElem, "MCUseFullSample", false);
    }

    this.loadSettings = function(settingsElem, config) {
        var xmlDoc = AppService.xmlDoc;

        var methodsElem = xmlDoc.createElement('Methods');
        var methodsObj = settingsElem.appendChild(methodsElem);

        config.settingsTableInstance.getValuesXml(xmlDoc, methodsObj); //save setttings

        //Number of simulations
        addNode('NumberOfSimulations', config.numberOfSimulations, settingsElem, xmlDoc);
        addNode('MCUseFullSample', config.useFullSample, settingsElem, xmlDoc);
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

        return 0.1;     //fixed value - 0.1s it does trades manipulation only
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