angular.module('app').service('OptProfileSysParamPermutationService', function(BackendService, AppService, CrossChecksService, L, OptimizationService) {
    
    this.applySettings = function(settingsElem, config) {
        if(!settingsElem) {
            return;
        }

        var xmlDoc = AppService.xmlDoc;

        config.maxTests = getNodeIntValue(settingsElem, "MaxTests", 1000);

        config.distributionUp = getNodeIntValue(settingsElem, "DistributionUp", 100);
        config.distributionDown = getNodeIntValue(settingsElem, "DistributionDown", 100);
        config.steps = getNodeIntValue(settingsElem, "Steps", 25);

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

        addNode('MaxTests', config.maxTests ? config.maxTests : 1000, settingsElem, xmlDoc);

        addNode('DistributionUp', config.distributionUp, settingsElem, xmlDoc);
        addNode('DistributionDown', config.distributionDown, settingsElem, xmlDoc);
        addNode('Steps', config.steps, settingsElem, xmlDoc);

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
        var config = {maxTests: 1000};

        if(settingsElem) instance.applySettings(settingsElem, config);
        return L.tsq("Max optimizations: %s", [OptimizationService.printMaxTestsLabel(config.maxTests)]);
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