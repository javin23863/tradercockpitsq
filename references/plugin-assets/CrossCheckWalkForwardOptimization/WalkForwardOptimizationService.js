angular.module('app').service('WalkForwardOptimizationService', function($rootScope, SQEvents, $q, BackendService, SQConstants, AppService, CrossChecksService, L) {

    this.applySettings = function(settingsElem, config) {
        if(!settingsElem) {
            return;
        }

        var constants = SQConstants.getConstants().optimization;

        var xmlDoc = AppService.xmlDoc;

        var wfObj = getChildElement(settingsElem, "WalkForward");
        if(wfObj) {
            config.wfType = getAttrStringValue(wfObj, "type", '' +  constants.wfType.WF_TYPE_SIMIS_SIMOOS);
            config.periodType = getAttrIntValue(wfObj, "period", constants.periodTypes.percent);
            config.optimizationType = getAttrStringValue(wfObj, "optimization", constants.optimizationTypes.floating);
            
            config.distributionUp = getAttrIntValue(wfObj, "distributionUp", 30);
            config.distributionDown = getAttrIntValue(wfObj, "distributionDown", 30);
            config.maxSteps = getAttrIntValue(wfObj, "maxSteps", 10);
        }
        
        var param1Obj = getChildElement(wfObj, "Param1");
        var param2Obj = getChildElement(wfObj, "Param2");

        config.param1 = getAttrFloatValue(param1Obj, "value", 20);
        config.param2 = getAttrFloatValue(param2Obj, "value", 10);

        config.maxTests = getNodeIntValue(settingsElem, "MaxTests", 100);

        var whatToParametrizeObj = getChildElement(settingsElem, "WhatToParametrize");
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

        var wfObj = createChild(settingsElem, "WalkForward", xmlDoc);
        wfObj.setAttribute("type", config.wfType);
        wfObj.setAttribute("period", config.periodType);
        wfObj.setAttribute("optimization", config.optimizationType);
        
        wfObj.setAttribute("distributionUp", config.distributionUp);
        wfObj.setAttribute("distributionDown", config.distributionDown);
        wfObj.setAttribute("maxSteps", config.maxSteps);

        var param1Obj = createChild(wfObj, "Param1", xmlDoc);
        var param2Obj = createChild(wfObj, "Param2", xmlDoc);

        param1Obj.setAttribute("value", config.param1);
        param2Obj.setAttribute("value", config.param2);

        addNode('MaxTests', config.maxTests ? config.maxTests : 100, settingsElem, xmlDoc);

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
        if(!settingsElem) {
            return L.tsq("N/A");
        }
        
        var constants = SQConstants.getConstants().optimization;

        var config = {};
        instance.applySettings(settingsElem, config);

        if(config.periodType==constants.periodTypes.percent) {
            return L.tsq("Out of sample: %f %, runs: %d", [config.param1, config.param2]);
        } else {
            return L.tsq("In sample: %d days, Out of sample: %d days", [config.param1, config.param2]);
        }
    }

    this.getShortInfo = function(settingsElem) {
        if(!settingsElem) {
            return L.tsq("N/A");
        }

        var constants = SQConstants.getConstants().optimization;
        
        var config = {};
        instance.applySettings(settingsElem, config);

        if(config.periodType==constants.periodTypes.percent) {
            return L.tsq("OOS: %f %, runs: %d", [config.param1, config.param2]);
        } else {
            return L.tsq("IS: %d days, OOS: %d days", [config.param1, config.param2]);
        }
    }

    this.getAverageDuration = function(settingsElem){
        var maxTests = getNodeIntValue(settingsElem, "MaxTests", 0);
        if(!maxTests) return 0;

        var duration = CrossChecksService.getAverageDuration(maxTests, true) * 3;
        return duration < 0.5 ? 0.5 : duration;
    }

    var instance = this;
});