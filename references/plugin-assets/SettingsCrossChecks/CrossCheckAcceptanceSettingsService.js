angular.module('app.settings').service('CrossCheckAcceptanceSettingsService', function ($injector, BackendService, AppService, SQConstants, L) {

    this.getDefaultWFConditions = function(data, callOnSuccess) {
        BackendService.sendRequest('rankingsettings/getDefaultWFConditions', data, callOnSuccess, 'POST');
    }  

    this.getDefaultSPPConditions = function(callOnSuccess) {
        BackendService.sendRequest('optProfileSysParamPerm/getDefaultSPPConditions', null, callOnSuccess);
    }  

    this.applySettings = function (acceptSettingsElem, config, crosscheckName) {
        var xmlDoc = AppService.xmlDoc;
        
        var conditionsObj = getChildElement(acceptSettingsElem, "Conditions");
        if(config.condGridInstance && config.condGridInstance.setConditionsObj){
            config.condGridInstance.setConditionsObj(conditionsObj);
        }

        if(crosscheckName=='WalkForwardOptimization') {
            config.thresholdPct = getAttrIntValue(conditionsObj, "thresholdPct", 80);

        } else if(crosscheckName=='WalkForwardMatrix') {
            config.thresholdPct = getAttrIntValue(conditionsObj, "thresholdPct", 80);
            config.robRows = getAttrIntValue(conditionsObj, "robCombRows", 3);
            config.robCols = getAttrIntValue(conditionsObj, "robCombCols", 3);
            config.robComb = getAttrIntValue(conditionsObj, "robMinComb", 7);

        } else if(crosscheckName=='OptProfileSysParamPermutation') {
            config.profitOptPct = getNodeValue(acceptSettingsElem, "ProfitOptPct", 30);
            config.avgProfit = getNodeValue(acceptSettingsElem, "AvgProfit", 0);
            config.uniformDistrChanges = getNodeValue(acceptSettingsElem, "UniformDistrChanges", 5);
            config.stdevAvgProfit = getNodeValue(acceptSettingsElem, "StdevAvgProfit", 1);
            config.evalProfitOptCheck = getNodeBooleanValue(acceptSettingsElem, "EvalProfitOptCheck", false);
            config.evalAvgProfitCheck = getNodeBooleanValue(acceptSettingsElem, "EvalAvgProfitCheck", false);
            config.evalUniformDistrCheck = getNodeBooleanValue(acceptSettingsElem, "EvalUniformDistrCheck", false);
            config.evalTopProfitCheck = getNodeBooleanValue(acceptSettingsElem, "EvalTopProfitCheck", false);
        
        } else if(crosscheckName=='RetestOnAdditionalMarkets') {
            config.condCheck = getNodeValue(acceptSettingsElem, "Check", 'all');
            config.minConditions = getNodeValue(acceptSettingsElem, "MinConditions", 2);
            config.minMarkets = getNodeValue(acceptSettingsElem, "MinMarkets", 1);

        } else if(crosscheckName=='SequentialOptimization') {
            config.pctToPass = getNodeValue(acceptSettingsElem, "PctToPass", 80);
            config.resultsCount = getNodeValue(acceptSettingsElem, "ResultsCount", 5);
            config.stabilityRange = getNodeValue(acceptSettingsElem, "StabilityRange", 20);
        }
        
    }

    this.loadSettings = function (acceptSettingsElem, config, crosscheckName) {
        var xmlDoc = AppService.xmlDoc;
        
        var conditionsObj = config.condGridInstance && config.condGridInstance.getConditionsObj ? config.condGridInstance.getConditionsObj() : null;

        if(crosscheckName=='WalkForwardOptimization') {
            conditionsObj.setAttribute("thresholdPct", config.thresholdPct);

        } else if(crosscheckName=='WalkForwardMatrix') {
            conditionsObj.setAttribute("thresholdPct", config.thresholdPct);
            conditionsObj.setAttribute("robCombRows", config.robRows);
            conditionsObj.setAttribute("robCombCols", config.robCols);
            conditionsObj.setAttribute("robMinComb", config.robComb);

        } else if(crosscheckName=='OptProfileSysParamPermutation') {
            addNode('ProfitOptPct', config.profitOptPct, acceptSettingsElem, xmlDoc);
            addNode('AvgProfit', config.avgProfit, acceptSettingsElem, xmlDoc);
            addNode('UniformDistrChanges', config.uniformDistrChanges, acceptSettingsElem, xmlDoc);
            addNode('StdevAvgProfit', config.stdevAvgProfit, acceptSettingsElem, xmlDoc);
            addNode('EvalProfitOptCheck', config.evalProfitOptCheck, acceptSettingsElem, xmlDoc);
            addNode('EvalAvgProfitCheck', config.evalAvgProfitCheck, acceptSettingsElem, xmlDoc);
            addNode('EvalUniformDistrCheck', config.evalUniformDistrCheck, acceptSettingsElem, xmlDoc);
            addNode('EvalTopProfitCheck', config.evalTopProfitCheck, acceptSettingsElem, xmlDoc);

        } else if(crosscheckName=='RetestOnAdditionalMarkets') {
            addNode('Check', config.condCheck, acceptSettingsElem, xmlDoc);
            addNode('MinConditions', config.minConditions, acceptSettingsElem, xmlDoc);
            addNode('MinMarkets', config.minMarkets, acceptSettingsElem, xmlDoc);

        } else if(crosscheckName=='SequentialOptimization') {
            addNode('PctToPass', config.pctToPass, acceptSettingsElem, xmlDoc);
            addNode('ResultsCount', config.resultsCount, acceptSettingsElem, xmlDoc);
            addNode('StabilityRange', config.stabilityRange, acceptSettingsElem, xmlDoc);
        }
        
        if(conditionsObj){
            acceptSettingsElem.appendChild(conditionsObj);
        }
    }

    this.getAcceptanceSettingsInfo = function (acceptSettingsElem, crosscheckName) {
        var totalConditions = 0;

        if (acceptSettingsElem) {
            var conditionsObj = getChildElement(acceptSettingsElem, "Conditions");
            var conditionObjs = getChildElements(conditionsObj, "Condition");

            for (var i = 0; i < conditionObjs.length; i++) {
                    var conditionObj = conditionObjs[i];

                    if(correctAttrValue(conditionObj.attributes['use'], true)) {
                        totalConditions++;
                    }
            }
        }

        if(crosscheckName=='OptProfileSysParamPermutation') {
            if(getNodeBooleanValue(acceptSettingsElem, "EvalProfitOptCheck", false)) totalConditions++;
            if(getNodeBooleanValue(acceptSettingsElem, "EvalAvgProfitCheck", false)) totalConditions++;
            if(getNodeBooleanValue(acceptSettingsElem, "EvalUniformDistrCheck", false)) totalConditions++;
            if(getNodeBooleanValue(acceptSettingsElem, "EvalTopProfitCheck", false)) totalConditions++;
        }

        return ((totalConditions > 0) ? L.tsq("%d conditions", [totalConditions]) : L.tsq("none"));
    }
    
    function printSetupSymbol(value) {
        try {
            var service = $injector.get("RetestOnAdditionalMarketsService");
            return service.config.setups[value-1].symbol;
        } catch(err) {
            console.error("Cannot print symbol of additional market "+value, err);
            return "Market "+value;
        }
    }

    function printBacktestType(type) {
        switch (type) {
            case instance.chartSetups.main: return "Main data";
            case instance.chartSetups.portfolio: return "Portfolio";
            case instance.chartSetups.crossCheck: return "Cross check data";
            case instance.chartSetups.chart: return "Market";
            case instance.chartSetups.everyChart: return "Every market";
        }

        return "?";
    }

    this.chartSetups = SQConstants.getConstants().resultTypes;

    var instance = this;
});