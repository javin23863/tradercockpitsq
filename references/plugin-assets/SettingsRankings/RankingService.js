angular.module('app.settings').service('RankingService', function($rootScope, $q, $injector, BackendService, AppService, SQConstants, L, SQEvents) {
    
    this.getDefaultWFConditions = function(data, callOnSuccess) {
        BackendService.sendRequest('rankingsettings/getDefaultWFConditions', data, callOnSuccess, 'POST');
    }  

    this.applySettings = function() {
        var settingsObj = AppService.getTaskConfig();
        var rankingsObj = getChildElement(settingsObj, "Rankings");
                
        //Fitness
        var fcObj = getChildElement(rankingsObj, 'FitnessCriteria');

        instance.fitness.method = getAttrValue(fcObj, "method");
        instance.fitness.xml = getChildElement(fcObj, 'Settings');
        instance.fitness.useFitnessByIndex = getAttrBooleanValue(fcObj, "useFitnessByIndex", false);
        instance.config.fitnessInstance.applySettings(instance.fitness.method, instance.fitness.xml);

        //StrategyAcceptanceConditions
        instance.config.conditionsType = getNodeIntValue(rankingsObj, 'ConditionsType', 1);

        if(AppService.getTask().type == 'Retest' || AppService.getTask().type == 'AutomaticRetest') {
            instance.config.deleteFailedStrategies = getNodeBooleanValue(rankingsObj, 'DeleteFailedStrategies', false);
            instance.config.forceRunCrossChecks = getNodeBooleanValue(rankingsObj, 'ForceRunCrossChecks', false);
        }

        if(AppService.getTask().type == 'Build') {
            instance.config.dismissTooSimilarStrategies = getNodeBooleanValue(rankingsObj, 'DismissTooSimilarStrategies', true);
        }

        instance.config.maxStrategies = getNodeIntValue(rankingsObj, 'MaxStrategies', 500);
  
        var conditionsObj = getChildElement(rankingsObj, "Conditions");    
        instance.config.condGridInstance.setConditionsObj(conditionsObj);

        if(AppService.getTask().type == 'Optimize') {
            var optimizationObj = getChildElement(settingsObj, "Optimization");
            instance.config.optimizationType = getAttrStringValue(optimizationObj, "type", instance.optimizationTypes.simple);
            
            var soObj = getChildElement(optimizationObj, "SimpleOptimization");
            instance.config.simpleOptimizationType = getAttrIntValue(soObj, "type", instance.simpleOptimizationTypes.storeBest);

            var wfObj = getChildElement(rankingsObj, 'WalkForward');
            
            var conditionsObj = getChildElement(wfObj, "Conditions");    
            instance.config.wfCondGridInstance.setConditionsObj(conditionsObj);

            instance.config.thresholdPct = getAttrIntValue(conditionsObj, "thresholdPct", 80);
            instance.config.wfRobRows = getAttrIntValue(conditionsObj, "robCombRows", 3);
            instance.config.wfRobCols = getAttrIntValue(conditionsObj, "robCombCols", 3);
            instance.config.wfRobComb = getAttrIntValue(conditionsObj, "robMinComb", 7);

            if(wfObj) {
                instance.config.wfDontSaveOriginalStr = getNodeBooleanValue(wfObj, 'DontSaveOriginalStr', false);
                instance.config.wfDeleteFailedStr = getNodeBooleanValue(wfObj, 'DeleteFailedStr', false);
            }
            else {
                var xmlDoc = AppService.xmlDoc;
                wfObj = createChild(rankingsObj, 'WalkForward', xmlDoc, true);

                conditionsObj = instance.config.wfCondGridInstance.getConditionsObj();
                wfObj.appendChild(conditionsObj);

                conditionsObj.setAttribute("thresholdPct", instance.config.thresholdPct);
                conditionsObj.setAttribute("robCombRows", instance.config.wfRobRows);
                conditionsObj.setAttribute("robCombCols", instance.config.wfRobCols);
                conditionsObj.setAttribute("robMinComb", instance.config.wfRobComb);
            }
        }

        if(AppService.getTask().type == 'Build' || AppService.getTask().type == 'Retest' || AppService.getTask().type == 'AutomaticRetest') {
            loadStrategyDismissalSettings(rankingsObj);
        }

        var stopConditionObj = getChildElement(rankingsObj, "StopCondition");
        instance.config.stopCondition.type = getAttrStringValue(stopConditionObj, 'type', instance.stopConditionTypes.never);
        instance.config.stopCondition.passedStrategies = getAttrIntValue(stopConditionObj, 'passedStrategies', 1000);
        instance.config.stopCondition.restartCount = getAttrIntValue(stopConditionObj, 'restartCount', 5);
        instance.config.stopCondition.days = getAttrIntValue(stopConditionObj, 'days', 0);
        instance.config.stopCondition.hours = getAttrIntValue(stopConditionObj, 'hours', 0);
        instance.config.stopCondition.minutes = getAttrIntValue(stopConditionObj, 'minutes', 0);

        var fitPortfolioElem = getChildElement(rankingsObj, "FitPortfolio");
        instance.config.fitPortfolio.active = getAttrBooleanValue(fitPortfolioElem, 'active', false);
        instance.config.fitPortfolio.databank = getAttrStringValue(fitPortfolioElem, 'databank', SQConstants.getConstants().gedatabanks.existingPortfolio);

        var correlationElem = getChildElement(fitPortfolioElem, 'Correlation');
        instance.config.fitPortfolio.correlation.max = getAttrFloatValue(correlationElem, 'max', 0.3);
        instance.config.fitPortfolio.correlation.type = getAttrStringValue(correlationElem, 'type', 'ProfitLoss');
        instance.config.fitPortfolio.correlation.period = getAttrIntValue(correlationElem, 'period', 10);
        instance.config.fitPortfolio.correlation.allowNegative = getAttrBooleanValue(correlationElem, 'allowNegative', false);
        instance.config.fitPortfolio.correlation.addEmptyPeriods = getAttrBooleanValue(correlationElem, 'addEmptyPeriods', false);

        var caElem = getChildElement(rankingsObj, "CustomAnalysis");
        instance.config.caMethod = getAttrStringValue(caElem, 'method', "none");
        instance.config.caInputArgs = getAttrStringValue(caElem, 'inputArgs', '');
        instance.config.caFilter = getAttrBooleanValue(caElem, 'filter', false);
    }

    function loadStrategyDismissalSettings(rankingsObj){
        var problems = SQConstants.getConstants().strategyProblems;
        instance.config.strategyProblems = angular.copy(problems);

        var autoDismissalElem = getChildElement(rankingsObj, 'AutomaticDismissal');
        instance.config.autoFiltersWarning =  getAttrBooleanValue(autoDismissalElem, 'warnings', false);

        if(!autoDismissalElem){
            autoDismissalElem = createChild(rankingsObj, 'AutomaticDismissal', AppService.xmlDoc, true);
            autoDismissalElem.setAttribute("warnings", false);

            for(var i=0; i<instance.config.strategyProblems.length; i++){
                var problem = instance.config.strategyProblems[i];
                problem.dismiss = true;
            }

            return;
        }

        var problemElems = getChildElements(autoDismissalElem, 'Problem');

        for(var i=0; i<problemElems.length; i++){
            var problemElem = problemElems[i];
            var dismiss = getAttrBooleanValue(problemElem, 'dismiss');
            var code = getAttrIntValue(problemElem, 'code');

            var problem = getItem(instance.config.strategyProblems, 'code', code);
            if(problem) {
                problem.dismiss = dismiss;
            }
        }
    }

    this.isDatabankInput = function(){
        var WhatToBuildService = $injector.get("WhatToBuildService");
        if(WhatToBuildService){
            WhatToBuildService.loadConfig();

            if(WhatToBuildService.config.strategyType == WhatToBuildService.strategyTypes.improve &&
                WhatToBuildService.config.improveType == WhatToBuildService.improveTypes.databank
            ){
                instance.inputDatabankName = WhatToBuildService.config.improveDatabank;
                return true;
            }
        }

        var OptimizationService = $injector.get("OptimizationService");
        if(OptimizationService){
            OptimizationService.loadConfig();

            if(OptimizationService.config.source == OptimizationService.constants.sources.databank){
                instance.inputDatabankName = OptimizationService.config.sourceDatabank;
                return true;
            }
        }

        return false;
    }

    this.onMaxStrategiesChanged = function(maxRecords, callback){
        var args = {
            projectName : AppService.getProject(),
            taskName : AppService.getTask().name,
            databankName : $rootScope.activeDatabankTab.title,
            maxRecords : maxRecords
        }

        BackendService.sendRequest('project/changeMaxRecords', args, callback);
    }

    this.saveConfig = function() {
        var xmlObj = AppService.xmlConfig;
        var xmlDoc = AppService.xmlDoc;
        
        var settingsObj = AppService.getTaskConfig();
        if(!settingsObj) return;

        var rankingsObj = getChildElement(settingsObj, 'Rankings');
        if(!rankingsObj){
            rankingsObj = createChild(settingsObj, 'Rankings', xmlDoc, true);
        } else {
            removeAllChildren(rankingsObj);
        }
        
        addNode('MaxStrategies', instance.config.maxStrategies, rankingsObj, xmlDoc);

        //Rank strategies
        var fitnessCriteriaElem = createChild(rankingsObj, 'FitnessCriteria', xmlDoc, true);
        fitnessCriteriaElem.setAttribute("method", instance.fitness.method);
        fitnessCriteriaElem.setAttribute("useFitnessByIndex", instance.fitness.useFitnessByIndex);
        if(instance.fitness.xml) {
            fitnessCriteriaElem.appendChild(instance.fitness.xml);
        }

        //StrategyAcceptanceConditions
        addNode('ConditionsType', instance.config.conditionsType, rankingsObj, xmlDoc);

        if(AppService.getTask().type == 'Build' || AppService.getTask().type == 'Retest' || AppService.getTask().type == 'AutomaticRetest') {
            var conditionsObj = instance.config.condGridInstance.getConditionsObj();
            rankingsObj.appendChild(conditionsObj);
        }

        if(AppService.getTask().type == 'Retest' || AppService.getTask().type == 'AutomaticRetest') {
           addNode('DeleteFailedStrategies', instance.config.deleteFailedStrategies, rankingsObj, xmlDoc);  
           addNode('ForceRunCrossChecks', instance.config.forceRunCrossChecks, rankingsObj, xmlDoc);    
        }

        if(AppService.getTask().type == 'Build') {
            addNode('DismissTooSimilarStrategies', instance.config.dismissTooSimilarStrategies, rankingsObj, xmlDoc);  
         }

        if(AppService.getTask().type == 'Build' || AppService.getTask().type == 'Retest' || AppService.getTask().type == 'AutomaticRetest') {
            saveStrategyDismissalSettings(rankingsObj, xmlDoc);
        }

        if(AppService.getTask().type == 'Optimize') {
            var wfObj = createChild(rankingsObj, 'WalkForward', xmlDoc, true);

            var conditionsObj = instance.config.wfCondGridInstance.getConditionsObj();
            wfObj.appendChild(conditionsObj);

            conditionsObj.setAttribute("thresholdPct", instance.config.thresholdPct);
            conditionsObj.setAttribute("robCombRows", instance.config.wfRobRows);
            conditionsObj.setAttribute("robCombCols", instance.config.wfRobCols);
            conditionsObj.setAttribute("robMinComb", instance.config.wfRobComb);

            addNode('DontSaveOriginalStr', instance.config.wfDontSaveOriginalStr, wfObj, xmlDoc);
            addNode('DeleteFailedStr', instance.config.wfDeleteFailedStr, wfObj, xmlDoc);
        }

        var stopConditionObj = createChild(rankingsObj, 'StopCondition', xmlDoc, true);
        stopConditionObj.setAttribute("type", instance.config.stopCondition.type);
        stopConditionObj.setAttribute("passedStrategies", instance.config.stopCondition.passedStrategies);
        stopConditionObj.setAttribute("restartCount", instance.config.stopCondition.restartCount);
        stopConditionObj.setAttribute("days", instance.config.stopCondition.days);
        stopConditionObj.setAttribute("hours", instance.config.stopCondition.hours);
        stopConditionObj.setAttribute("minutes", instance.config.stopCondition.minutes);

        var fitPortfolioElem = createChild(rankingsObj, 'FitPortfolio', xmlDoc, true);
        fitPortfolioElem.setAttribute("active", instance.config.fitPortfolio.active);
        fitPortfolioElem.setAttribute("databank", instance.config.fitPortfolio.databank);

        var correlationElem = createChild(fitPortfolioElem, 'Correlation', xmlDoc, true);
        correlationElem.setAttribute("max", instance.config.fitPortfolio.correlation.max);
        correlationElem.setAttribute("type", instance.config.fitPortfolio.correlation.type);
        correlationElem.setAttribute("period", instance.config.fitPortfolio.correlation.period);
        correlationElem.setAttribute("allowNegative", instance.config.fitPortfolio.correlation.allowNegative);
        correlationElem.setAttribute("addEmptyPeriods", instance.config.fitPortfolio.correlation.addEmptyPeriods);

        var caElem = createChild(rankingsObj, 'CustomAnalysis', xmlDoc, true);
        caElem.setAttribute("method", instance.config.caMethod);
        caElem.setAttribute("filter", instance.config.caFilter);
        caElem.setAttribute("inputArgs", instance.config.caInputArgs);
        //instance.checkFitPortfolioSettings();
    }

    function saveStrategyDismissalSettings(rankingsObj, xmlDoc){
        var autoDismissalElem = getChildElement(rankingsObj, 'AutomaticDismissal');
        if(!autoDismissalElem){
            autoDismissalElem = createChild(rankingsObj, 'AutomaticDismissal', xmlDoc, true);
        }

        autoDismissalElem.setAttribute("warnings", instance.config.autoFiltersWarning);

        for(var i=0; i<instance.config.strategyProblems.length; i++){
            var strategyProblem = instance.config.strategyProblems[i];
            
            var problemElem = createChild(autoDismissalElem, 'Problem', xmlDoc, true);
            problemElem.setAttribute("code", strategyProblem.code);
            problemElem.setAttribute("dismiss", valueFilled(strategyProblem.dismiss) ? strategyProblem.dismiss : false);
        }
    }

    /*
        this.checkFitPortfolioSettings = function() {
            var settingsObj = AppService.getTaskConfig();
            var rankingsObj = getChildElement(settingsObj, "Rankings");

            var fitPortfolioElem = getChildElement(rankingsObj, "FitPortfolio");
            var showExistingPortfolioTab = getAttrBooleanValue(fitPortfolioElem, 'active', false);

            if(AppService.getTask().type == 'Build') {
                SQEvents.notifyListeners(SQEvents.get("DATABANK_DISPLAY"), {name : SQConstants.getConstants().gedatabanks.existingPortfolio, show : showExistingPortfolioTab});
            } else {
                SQEvents.notifyListeners(SQEvents.get("DATABANK_DISPLAY"), {name : SQConstants.getConstants().gedatabanks.existingPortfolio, show : false});
            }
        }

        function onEvent(event, data) {
            if(event == SQEvents.get("DATABANKS_LOADED")) {
                if(!AppService.isTaskManager()) instance.checkFitPortfolioSettings();

            } else if(event == SQEvents.get("SETTINGS_TAB_RELOAD") && data == "all") {        //controller may not be initialized
                instance.checkFitPortfolioSettings();
            } 
        }

        SQEvents.addListener('SettingsRankingsService', [SQEvents.get("DATABANKS_LOADED"), SQEvents.get('SETTINGS_TAB_RELOAD')], onEvent);
    */

    function init() {
        //Custom analysis
        instance.config.caAvailableMethods.length = 0;

        var noneMethod = {"name": L.tsq("None"), "value": "none"}
        instance.config.caAvailableMethods.push(noneMethod);

        var methods = InitializationData().settings.CustomAnalysis.methods;        
        for (var i = 0; i < methods.length; i++) {
            let method = methods[i];

            if(method.type==20 || method.type==30) instance.config.caAvailableMethods.push(method);
        }
    }

    var instance = this;

    this.fitness = {
        method : null,
        xml : null,
        useFitnessByIndex : false
    }

    this.methods = [];
    this.inputDatabankName = '';

    this.projectStates = SQConstants.getConstants().runningStatuses; 
    this.optimizationTypes = SQConstants.getConstants().optimization.optimizations;
    this.simpleOptimizationTypes = SQConstants.getConstants().optimization.simpleOptimizations;
    this.stopConditionTypes = SQConstants.getConstants().buildStopConditionTypes;
    this.generationTypes = SQConstants.getConstants().buildGenerationTypes;

    this.config = {
        condGridInstance: {},
        fitnessInstance: {},
        maxStrategies: 500,

        wfCondGridInstance: {},
        thresholdPct: 80,
        wfRobRows : 3,
        wfRobCols : 3,
        wfRobComb : 7,
        wfDontSaveOriginalStr: false,
        wfDeleteFailedStr: false,

        optimizationType : instance.optimizationTypes.simple,
        simpleOptimizationType: instance.simpleOptimizationTypes.storeBest,
        
        stopCondition : {
            type: instance.stopConditionTypes.never,
            passedStrategies: 1000,
            restartCount: 5,
            days : 0,
            hours : 0,
            minutes : 0
        },

        strategyProblemsAll: false,
        deleteFailedStrategies: false,
        autoFiltersWarning: false,
        dismissTooSimilarStrategies: true,

        fitPortfolio: {
            active: false,
            databank: SQConstants.getConstants().gedatabanks.existingPortfolio,
            correlation: {
                max: 0.3,
                type: 'ProfitLoss',
                period: 10,
                allowNegative: true,
                addEmptyPeriods: false
            }
        },

        caAvailableMethods: [],
        caMethod: "none",
        caInputArgs: "",
        caFilter: false,

        forceRunCrossChecks: false
    }
    
    init();
});