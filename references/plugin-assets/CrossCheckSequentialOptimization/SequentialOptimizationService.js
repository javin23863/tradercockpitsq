angular.module('app').service('SequentialOptimizationService', function(BackendService, AppService, CrossChecksService, L) {
    
    this.applySettings = function(settingsElem, config) {
        if(!settingsElem) {
            return;
        }

        var elParameterSettings = getChildElement(settingsElem, "ParameterSettings", true);
        if (elParameterSettings) {
            config.distributionUp = getNodeIntValue(elParameterSettings, "DistributionUp", 50);
            config.distributionDown = getNodeIntValue(elParameterSettings, "DistributionDown", 50);
            config.steps = getNodeIntValue(elParameterSettings, "Steps", 50);
            config.applyToStrategy = getNodeBooleanValue(elParameterSettings, 'ApplyToStrategy', true);
        }

        var whatToParametrizeObj = getChildElement(settingsElem, "WhatToParametrize", true);
        if (whatToParametrizeObj) {
            config.symmetricVariables = getAttrBooleanValue(whatToParametrizeObj, 'symmetricVariables', config.symmetricVariables);

            config.parametrizeType = getNodeBooleanValue(whatToParametrizeObj, 'Recommended', true) ? 0 : 1;
            config.periodParams = getNodeBooleanValue(whatToParametrizeObj, 'Periods', config.periodParams);
            config.shiftParams = getNodeBooleanValue(whatToParametrizeObj, 'Shifts', config.shiftParams);
            config.constantsParams = getNodeBooleanValue(whatToParametrizeObj, 'Constants', config.constantsParams);
            config.otherParams = getNodeBooleanValue(whatToParametrizeObj, 'OtherParams', config.otherParams);
            config.entryParams = getNodeBooleanValue(whatToParametrizeObj, 'EntryParams', config.entryParams);
            config.entryLogic = getNodeBooleanValue(whatToParametrizeObj, 'EntryLogic', config.entryLogic);
            config.exitParamsUsed = getNodeBooleanValue(whatToParametrizeObj, 'ExitParamsUsed', config.exitParamsUsed);
            config.exitParamsUnused = getNodeBooleanValue(whatToParametrizeObj, 'ExitParamsUnused', config.exitParamsUnused);
            config.booleanParams = getNodeBooleanValue(whatToParametrizeObj, 'BooleanParams', config.booleanParams);
        }
    }

    this.loadSettings = function(settingsElem, config) {
        var xmlDoc = AppService.xmlDoc;

        var elParameterSettings = createChild(settingsElem, 'ParameterSettings', xmlDoc, false);
        addNode('DistributionUp', config.distributionUp, elParameterSettings, xmlDoc);
        addNode('DistributionDown', config.distributionDown, elParameterSettings, xmlDoc);
        addNode('Steps', config.steps, elParameterSettings, xmlDoc);
        addNode('ApplyToStrategy', config.applyToStrategy, elParameterSettings, xmlDoc);

        var whatToParametrizeObj = createChild(settingsElem, 'WhatToParametrize', xmlDoc, false);
        whatToParametrizeObj.setAttribute("type", config.parametrizeType);
        whatToParametrizeObj.setAttribute("symmetricVariables", config.parametrizeType==0 ? false : config.symmetricVariables);

        addNode('Recommended', config.parametrizeType==0 ? true : false, whatToParametrizeObj, xmlDoc);
        addNode('Periods', config.parametrizeType==0 ? false : config.periodParams, whatToParametrizeObj, xmlDoc);
        addNode('Shifts', config.parametrizeType==0 ? false : config.shiftParams, whatToParametrizeObj, xmlDoc);
        addNode('Constants', config.parametrizeType==0 ? false : config.constantsParams, whatToParametrizeObj, xmlDoc);
        addNode('OtherParams', config.parametrizeType==0 ? false : config.otherParams, whatToParametrizeObj, xmlDoc);
        addNode('EntryParams', config.parametrizeType==0 ? false : config.entryParams, whatToParametrizeObj, xmlDoc);
        addNode('EntryLogic', config.parametrizeType==0 ? false : config.entryLogic, whatToParametrizeObj, xmlDoc);
        addNode('ExitParamsUsed', config.parametrizeType==0 ? false : config.exitParamsUsed, whatToParametrizeObj, xmlDoc);
        addNode('ExitParamsUnused', config.parametrizeType==0 ? false : config.exitParamsUnused, whatToParametrizeObj, xmlDoc);
        addNode('BooleanParams', config.parametrizeType==0 ? false : config.booleanParams, whatToParametrizeObj, xmlDoc);
    }
    
    this.getInfo = function(settingsElem) {
        var config = {
            distributionUp: 50,
            distributionDown: 50,
            steps: 50,
            applyToStrategy: false
        };

        if(settingsElem) instance.applySettings(settingsElem, config);
        
        return L.tsq("Distribution: Up: %d%, Down: %d%, Steps: %d, Apply to strategy: %s", [config.distributionUp, config.distributionDown, config.steps, config.applyToStrategy ? L.tsq("yes") : L.tsq("no")]);
    }

    this.getShortInfo = function(settingsElem) {
        return instance.getInfo(settingsElem);
    }

    this.getAverageDuration = function(settingsElem){
        var numberOfSimulations = getNodeValue(settingsElem, "MaxTests", 0);
        if(!numberOfSimulations) return 0;

        var duration = CrossChecksService.getAverageDuration(numberOfSimulations, true) * 2;
        return duration < 0.5 ? 0.5 : duration;
    }

    var instance = this;
    
});