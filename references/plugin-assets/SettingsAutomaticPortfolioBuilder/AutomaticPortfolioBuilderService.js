angular.module('app.settings').service('AutomaticPortfolioBuilderService', function(AppService, SQEvents, DatabankService, BackendService, SQConstants) {
    
    function loadDatabanks() {
        DatabankService.loadVisibleDatabanks(instance.config.availableDatabanks);
    }

    this.getNumberOfPossiblePortfolios = function(data, callOnSuccess){
        return BackendService.sendRequest('/pmsettings/getNumberOfPossiblePortfolios', data, callOnSuccess);
    }    

    this.saveSettings = function() {
        
        var xmlDoc = AppService.xmlDoc;
        var pmObj = AppService.getCurrentTaskTabSettings("AutomaticPortfolioBuilder");
        if(!pmObj) return;
        
        //search type
        addNode('SearchType', instance.config.searchType, pmObj, xmlDoc);
        
        //databanks
        DatabankService.setTaskInputDatabank(instance.config.inputDatabank);
        DatabankService.setTaskOutputDatabank(instance.config.outputDatabank);
        DatabankService.setRetestSelected(instance.config.onlySelected);

        //genetic options
        addNode('GenPopulationSize', instance.config.populationSize, pmObj, xmlDoc);
        addNode('GenGenerations', instance.config.generations, pmObj, xmlDoc);
        addNode('GenRunContinually', instance.config.runContinually, pmObj, xmlDoc);
        addNode('GenRestartAfterStagnation', instance.config.restartAfterStagnation, pmObj, xmlDoc);
        addNode('GenGenerationLimit', instance.config.generationLimit, pmObj, xmlDoc);

        //portfolio
        addNode('MinStrategies', instance.config.minStrategies, pmObj, xmlDoc);
        addNode('MaxStrategies', instance.config.maxStrategies, pmObj, xmlDoc);
        addNode('MaxStrategiesDatabank', instance.config.maxStrategiesDatabank, pmObj, xmlDoc);
        addNode('MaxStrategiesSector', instance.config.maxStrategiesSector, pmObj, xmlDoc);
        addNode('DateRange', instance.config.dateRange, pmObj, xmlDoc);
        addNode('DateRangeFrom', instance.config.dateRangeFrom, pmObj, xmlDoc);
        addNode('DateRangeTo', instance.config.dateRangeTo, pmObj, xmlDoc);
        addNode('DateRangeIsPct', instance.config.dateRangeIsPct, pmObj, xmlDoc);
        addNode('DateRangeReverse', instance.config.reverseSample, pmObj, xmlDoc);
        addNode('FitnessType', instance.config.fitnessType, pmObj, xmlDoc);

        //correlation
        addNode('CorrMax', instance.config.correlation.max, pmObj, xmlDoc);
        addNode('CorrType', instance.config.correlation.type, pmObj, xmlDoc);
        addNode('CorrSampleType', instance.config.correlation.sampleType, pmObj, xmlDoc);
        addNode('CorrPeriod', instance.config.correlation.period, pmObj, xmlDoc);
        addNode('CorrAllowNegative', instance.config.correlation.allowNegative, pmObj, xmlDoc);
        addNode('CorrAddEmptyPeriods', instance.config.correlation.addEmptyPeriods, pmObj, xmlDoc);

        var conditionsObj = instance.config.condGridInstance.getConditionsObj();
        pmObj.appendChild(conditionsObj);

        //money management
        var mmObj = createChild(pmObj, "MoneyManagement", xmlDoc, false);
        addNode('InitialCapital', instance.config.initialCapital, mmObj, xmlDoc);

        console.log("Saving MM settings");

        addNode('MMMethodOptional', instance.config.mmMethodOptional, pmObj, xmlDoc);

        var type = instance.config.mmPropGridInstance.getSelectedType();
        var mmMethodObj = createChild(mmObj, "Method", xmlDoc, false);
        mmMethodObj.setAttribute("type", type);
        mmMethodObj.setAttribute("use", 'true');
        instance.config.mmPropGridInstance.setPropertyValues(xmlDoc, mmMethodObj);
        mmObj.appendChild(mmMethodObj);
        pmObj.appendChild(mmObj);
    }
    
    this.loadSettings = function() {
        var pmObj = AppService.getCurrentTaskTabSettings("AutomaticPortfolioBuilder");
        if(!pmObj) return;

        //search type
        instance.config.searchType = getNodeValue(pmObj, 'SearchType', 'bruteforce');

        //databanks
        instance.config.inputDatabank = DatabankService.getTaskInputDatabank();
        instance.config.outputDatabank = DatabankService.getTaskOutputDatabank();
        instance.config.onlySelected = DatabankService.getRetestSelected();

        //genetic options
        instance.config.populationSize = getNodeIntValue(pmObj, 'GenPopulationSize', 100);
        instance.config.generations = getNodeIntValue(pmObj, 'GenGenerations', 700);
        instance.config.runContinually = getNodeBooleanValue(pmObj, 'GenRunContinually', false);
        instance.config.restartAfterStagnation = getNodeBooleanValue(pmObj, 'GenRestartAfterStagnation', true);
        instance.config.generationLimit = getNodeIntValue(pmObj, 'GenGenerationLimit', 500);

        //portfolio
        instance.config.minStrategies = getNodeIntValue(pmObj, 'MinStrategies', 2);
        instance.config.maxStrategies = getNodeIntValue(pmObj, 'MaxStrategies', 8);
        instance.config.maxStrategiesDatabank = getNodeIntValue(pmObj, 'MaxStrategiesDatabank', 100);
        instance.config.maxStrategiesSector = getNodeIntValue(pmObj, 'MaxStrategiesSector', 3);
        instance.config.dateRange = getNodeValue(pmObj, 'DateRange', "full");
        instance.config.dateRangeFrom = getNodeValue(pmObj, 'DateRangeFrom', timeToDateString(new Date().getTime()));
        instance.config.dateRangeTo = getNodeValue(pmObj, 'DateRangeTo', timeToDateString(new Date().getTime()));
        instance.config.dateRangeIsPct = getNodeIntValue(pmObj, 'DateRangeIsPct', 70);
        instance.config.reverseSample = getNodeBooleanValue(pmObj, 'DateRangeReverse', false);
        instance.config.fitnessType = getNodeValue(pmObj, 'FitnessType', "NetProfitIS");

        //check if the fitness type is valid
        if(instance.fitnessTypes && instance.fitnessTypes.length > 0) {
            let item = getItem(instance.fitnessTypes, "type", instance.config.fitnessType);
            if(!item) {
                instance.config.fitnessType = instance.fitnessTypes[0].type;
            }
        }

        //correlation
        instance.config.correlation.max = getNodeFloatValue(pmObj, 'CorrMax', 0.3);
        instance.config.correlation.type = getNodeValue(pmObj, 'CorrType', 'ProfitLoss');
        instance.config.correlation.sampleType = getNodeIntValue(pmObj, 'CorrSampleType', SQConstants.getConstants().sampleTypes.in);
        instance.config.correlation.period = getNodeIntValue(pmObj, 'CorrPeriod', 10);
        instance.config.correlation.allowNegative = getNodeBooleanValue(pmObj, 'CorrAllowNegative', true);
        instance.config.correlation.addEmptyPeriods = getNodeBooleanValue(pmObj, 'CorrAddEmptyPeriods', false);

        var conditionsObj = getChildElement(pmObj, "Conditions");    
        instance.config.condGridInstance.setConditionsObj(conditionsObj);

        //money management
        instance.config.mmMethodOptional = getNodeBooleanValue(pmObj, 'MMMethodOptional', false);
        var mmObj = getChildElement(pmObj, "MoneyManagement"); 
        if(mmObj) {
            instance.config.initialCapital = getNodeFloatValue(mmObj, 'InitialCapital', instance.config.initialCapital);

            var mmMethodObj = getChildElement(mmObj, 'Method');
            instance.config.mmType = getAttrStringValue(mmMethodObj, 'type', instance.config.mmType);
            instance.config.mmSettings = parseMethodSettings(mmMethodObj);
        }
    }

    var instance = this;

    this.config = {
        //search type
        searchType: 'bruteforce',

        //databanks
        inputDatabank: 'Simple strategies',
        outputDatabank: 'Results',
        onlySelected: false,
        availableDatabanks: [],

        //genetic options
        populationSize: 100,
        generations: 700, 
        runContinually: false,
        restartAfterStagnation: true,
        generationLimit: 500,

        //portfolio
        minStrategies: 2,
        maxStrategies: 8, 
        maxStrategiesDatabank: 100,
        maxStrategiesSector: 3,
        dateRange: "full",
        dateRangeFrom: 0,
        dateRangeTo: 0,
        dateRangeIsPct: 70,
        reverseSample: false,

        //correlation
        correlation: {
            max: 0.3,
            type: 'ProfitLoss',
            sampleType: SQConstants.getConstants().sampleTypes.in,
            period: 10,
            allowNegative: true,
            addEmptyPeriods: false
        },

        //filtering
        condGridInstance: {},

        fitnessType: 'NetProfitIS',
        fitnessTypes: [],

        numberOfPossiblePortfolios: 0,

        //money management
        initialCapital: 10000,
        mmMethodOptional: false,
        mmType: 'FixedSize',
        mmSettings: null,
        mmPropGridInstance: {}
    }

    this.fitnessTypes = InitializationData().settings.AutomaticPortfolioBuilder.fitnessTypes;

    loadDatabanks();

    function onEvent(event, data){
        if(event == SQEvents.get("RELOAD_DATABANKS")){
            loadDatabanks();
        }
    }

    SQEvents.addListener("AutomaticPortfolioBuilderService", [SQEvents.get('RELOAD_DATABANKS')], onEvent);
});