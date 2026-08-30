angular.module('app.settings').service('ComplexitiesService', function ($rootScope, $q, BackendService, AppService, SQConstants) {

    function init() {
        instance.loadComplexities();
    }

    this.loadComplexities = function () {
        if(!AppService.getTask()) return;
        
        updateShifts();

        var settingsElem = AppService.getTaskConfig();
        var buildTypeElem = getChildElement(settingsElem, 'WhatToBuild', true);

        instance.settings.complexities.length = 1;

        if (AppService.getTask().type == "Build" && buildTypeElem) {
            var rulesComplexityElem = getChildElement(buildTypeElem, 'RulesComplexity');

            if (rulesComplexityElem) {
                var chartElems = getChildElements(rulesComplexityElem, 'Chart');

                instance.settings.useDifferentSettings = getAttrBooleanValue(rulesComplexityElem, "useDifferentSettings", instance.settings.useDifferentSettings);
                instance.settings.complexities.length = chartElems.length > 0 ? 0 : 1;

                for (var i = 0; i < chartElems.length; i++) {
                    var chartElem = chartElems[i];
                    instance.settings.complexities.push({
                        chartName: getAttrValue(chartElem, 'name'),
                        minConditions: getAttrIntValue(chartElem, 'minConditions'),
                        maxConditions: getAttrIntValue(chartElem, 'maxConditions'),
                        minExitConditions: getAttrIntValue(chartElem, 'minExitConditions', getAttrIntValue(chartElem, 'minConditions')),
                        maxExitConditions: getAttrIntValue(chartElem, 'maxExitConditions', getAttrIntValue(chartElem, 'maxConditions')),
                        minExitTypes: getAttrIntValue(chartElem, 'minExitTypes', 1),
                        maxExitTypes: getAttrIntValue(chartElem, 'maxExitTypes', 5),
                        minPeriod: getAttrIntValue(chartElem, 'minPeriod'),
                        maxPeriod: getAttrIntValue(chartElem, 'maxPeriod'),
                        minShift: instance.minShift.value,
                        maxShift: getAttrIntValue(chartElem, 'maxShift')
                    });
                }
            }
        }
        else {
            var dataElem = getChildElement(settingsElem, 'Data', true);
            if (dataElem) {
                var setupsElem = getChildElement(dataElem, 'Setups', true);
                if (setupsElem) {
                    var setupElem = getChildElement(setupsElem, 'Setup', true);
                    if (setupElem) {
                        var chartElems = getChildElements(setupElem, 'Chart');
                        instance.settings.complexities.length = chartElems.length > 0 ? chartElems.length : 1;
                    }
                }
            }
        }

        instance.correctComplexities();
        complexitiesDeferred.resolve();
    }

    this.correctComplexities = function () {
        for (var i = 0; i < instance.settings.complexities.length; i++) {
            instance.settings.complexities[i] = instance.settings.complexities[i] || {};
            var complexity = instance.settings.complexities[i];

            complexity.chartName = getSetupName(i);
            complexity.minConditions = valueFilled(complexity.minConditions) ? complexity.minConditions : 0;
            complexity.maxConditions = valueFilled(complexity.maxConditions) ? complexity.maxConditions : 2;
            complexity.minExitConditions = valueFilled(complexity.minExitConditions) ? complexity.minExitConditions : 0;
            complexity.maxExitConditions = valueFilled(complexity.maxExitConditions) ? complexity.maxExitConditions : 2;
            complexity.minExitTypes = valueFilled(complexity.minExitTypes) ? complexity.minExitTypes : 1;
            complexity.maxExitTypes = valueFilled(complexity.maxExitTypes) ? complexity.maxExitTypes : 5;
            complexity.minPeriod = valueFilled(complexity.minPeriod) ? complexity.minPeriod : 15;
            complexity.maxPeriod = valueFilled(complexity.maxPeriod) ? complexity.maxPeriod : 100;
            complexity.minShift = instance.minShift.value;
            complexity.maxShift = valueFilled(complexity.maxShift) ? complexity.maxShift : (complexity.minShift + 2);
        }

        var xmlDoc = AppService.xmlDoc;
        var settingsElem = AppService.getTaskConfig();

        //update Data settings

        correctSetups(xmlDoc, settingsElem, 'Data');

        instance.saveComplexities();
    }

    this.loadFromTemplate = function (chartSettings) {
        instance.settings.complexities.length = chartSettings.length;
        instance.correctComplexities();

        setMainSetupData(chartSettings);
    }

    function setMainSetupData(chartSettings) {
        var settingsElem = AppService.getTaskConfig();
        var dataElem = getChildElement(settingsElem, 'Data', true);
        if (dataElem) {
            var setupsElem = getChildElement(dataElem, 'Setups', true);
            if (setupsElem) {
                var setupElem = getChildElement(setupsElem, 'Setup');
                var chartElems = getChildElements(setupElem, 'Chart');

                for (var i = 1; i < chartElems.length; i++) {       //main chart should remain the same
                    var chartElem = chartElems[i];
                    var templateSymbol = chartSettings[i].symbol;

                    var symbol, timeframe;
                    
                    var symbolData = getItem(SQConstants.getConstants().data, 'symbol', templateSymbol);
                    if(!symbolData){
                        symbol = "Same as main chart";
                        timeframe = chartSettings[i].timeframe;
                    }	
                    else {
                        symbol = templateSymbol;
                        timeframe = getNewTimeframe(symbolData.timeframe, chartSettings[i].timeframe);
                    }

                    chartElem.setAttribute('symbol', symbol);
                    chartElem.setAttribute('timeframe', timeframe);
                }
            }
        }
    }

    function updateShifts(engine){
        if(engine){
            instance.minShift.value = (engine == "Tradestation" || engine == "MultiCharts") ? 0 : 1;
            //instance.maxShift.value = engine == "Tradestation" ? 2 : 3;
            return;
        }

        var settingsElem = AppService.getTaskConfig();
        var dataElem = getChildElement(settingsElem, 'Data', true);
        if (dataElem) {
            var setupsElem = getChildElement(dataElem, 'Setups', true);
            if (setupsElem) {
                var setupElem = getChildElement(setupsElem, 'Setup');
                if(setupElem){
                    var curEngine = getAttrValue(setupElem, "engine");
                    instance.minShift.value = (engine == "Tradestation" || engine == "MultiCharts") ? 0 : 1;
                    //instance.maxShift.value = curEngine == "Tradestation" ? 2 : 3;
                }
            }
        }
    }

    this.setShiftsForCurrentEngine = function(engine){
        updateShifts(engine);
        instance.saveComplexities();
    }

    this.resetComplexity = function (complexity) {
        complexity.minConditions = 1;
        complexity.maxConditions = 4;
        complexity.minExitConditions = 1;
        complexity.maxExitConditions = 4;
        complexity.minExitTypes = 1;
        complexity.maxExitTypes = 5;
        complexity.minPeriod = 2;
        complexity.maxPeriod = 100;
        complexity.minShift = instance.minShift.value;
        complexity.maxShift = complexity.minShift + 2;
    }

    function getSetupName(index) {
        return index == 0 ? 'Main chart' : 'Additional chart ' + (index);
    }

    this.saveComplexities = function () {
        var xmlDoc = AppService.xmlDoc;

        var task = AppService.getTask();
        var settingsElem = AppService.getTaskConfig();

        if (task && task.type == 'Build') {
            var buildTypeElem = createChild(settingsElem, 'WhatToBuild', xmlDoc);
            var rulesComplexityElem = createChild(buildTypeElem, 'RulesComplexity', xmlDoc);
            rulesComplexityElem.setAttribute("useDifferentSettings", instance.settings.useDifferentSettings);

            removeAllChildren(rulesComplexityElem);

            for (var i = 0; i < instance.settings.complexities.length; i++) {
                var complexity = instance.settings.complexities[i];
                var chartElem = createChild(rulesComplexityElem, 'Chart', xmlDoc, true);
                chartElem.setAttribute('name', complexity.chartName);
                chartElem.setAttribute('minConditions', complexity.minConditions);
                chartElem.setAttribute('maxConditions', complexity.maxConditions);
                chartElem.setAttribute('minExitConditions', complexity.minExitConditions);
                chartElem.setAttribute('maxExitConditions', complexity.maxExitConditions);

                if(i == 0) { //only for main chart
                    chartElem.setAttribute('minExitTypes', complexity.minExitTypes);
                    chartElem.setAttribute('maxExitTypes', complexity.maxExitTypes);
                }

                chartElem.setAttribute('minPeriod', complexity.minPeriod);
                chartElem.setAttribute('maxPeriod', complexity.maxPeriod);
                chartElem.setAttribute('minShift', instance.minShift.value);
                chartElem.setAttribute('maxShift', complexity.maxShift);
            }
        }
    }

    function correctSetups(xmlDoc, settingsElem, settingName) {
        var dataElem = getChildElement(settingsElem, settingName, true);
        if (dataElem) {
            var setupsElem = getChildElement(dataElem, 'Setups', true);
            if (setupsElem) {
                var setupElems = getChildElements(setupsElem, 'Setup');
                var chartCount = instance.settings.complexities.length;

                for (var a = 0; a < setupElems.length; a++) {
                    var setupElem = setupElems[a];
                    var chartElems = getChildElements(setupElem, 'Chart');

                    if (chartElems.length > chartCount) {
                        //remove overabundant charts
                        for (var i = chartElems.length - 1; i >= chartCount; i--) {
                            removeChildAtIndex(setupElem, "Chart", i)
                        }
                    }
                    else if (chartElems.length < chartCount) {
                        var spread = chartElems.length > 0 ? getAttrIntValue(chartElems[0], "spread", 3) : 3;
                        //add missing charts
                        for (var i = chartElems.length; i < chartCount; i++) {
                            var chartElem = createChild(setupElem, 'Chart', xmlDoc, true);
                            chartElem.setAttribute('symbol', "Same as main chart");
                            chartElem.setAttribute('timeframe', "H1");
                            chartElem.setAttribute('spread', spread);
                        }
                    }
                }
            }
        }
    }

    var instance = this;
    var complexitiesDeferred = $q.defer();

    this.settings = { 
        useDifferentSettings : true,
        complexities : [] 
    };

    this.minShift = {
        value : 1
    };

    this.complexitiesPromise = complexitiesDeferred.promise;

    init();

});