angular.module('app').service('WhatIfService', function(BackendService, AppService, CrossChecksService, L) {
    
    this.list = function(callOnSuccess) {
        BackendService.sendRequest('whatIf/list', null, callOnSuccess);
    } 

    this.applySettings = function(settingsElem, config) {
        if(!settingsElem) {
            return;
        }

        var xmlDoc = AppService.xmlDoc;

        var methodsElem = getChildElements(settingsElem, 'Methods')[0];
        var methods = parseSettingsMethods(methodsElem);

        config.settingsTableInstance.applySettings(methods);
    }

    this.loadSettings = function(settingsElem, config) {
        var xmlDoc = AppService.xmlDoc;

        var methodsElem = xmlDoc.createElement('Methods');
        var methodsObj = settingsElem.appendChild(methodsElem);

        config.settingsTableInstance.getValuesXml(xmlDoc, methodsObj); //save setttings
    }
    
    this.getInfo = function(settingsElem) {
        if(!settingsElem) {
            return L.tsq("N/A");
        }

        var numberOfTests = getNumberOfSimulations(settingsElem);
        
        return L.tsq("%d simulations", [numberOfTests]);
    }

    this.getShortInfo = function(settingsElem) {
        return instance.getInfo(settingsElem);
    }

    this.getAverageDuration = function(settingsElem){
       return 0.5;
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