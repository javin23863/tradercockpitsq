/**
 * Info:
 * 
 * JS block object looks like this:
 *  {
 *      key : ''
 *      name : ''
 *      use : true/false
 *      weight : number
 *      params : []             - array of original parameters and their values
 *      customParams : []       - modified array of parameters - custom values
 *      
 *  // objects for exit types (for order types located in customParams):
 * 
 *      items: []               - array of possible options items, their parameters and values
 *  }
 * 
 */
angular.module('app.settings').service('BlocksService', function ($rootScope, $injector, $q, $timeout, BackendService, AppService, SQEvents, BlockConfigService, L, SQConstants, DataService) {
    var instance = this;

    function isVPBlockLocked(key) {
        var license = $rootScope.license;
        var vp = license && license.volumeProfile;
        if (!vp || vp.active) return false;
        if (!vp.blocks || !vp.blocks.length) return false;
        var parts = key.split('.');
        var className = parts[parts.length - 1];
        return vp.blocks.indexOf(className) >= 0;
    }

    this.init = function () {
        var deferred = $q.defer();
        dataPromise = deferred.promise;
        
        instance.gui.lastEngineWasStockpicker = null;

        getBuildingBlocks(function (processedBlocks) {
            loadToArray(instance.orderTypes, angular.copy(processedBlocks.orderTypes));
            loadToArray(instance.blocks, angular.copy(processedBlocks.blocks));
            loadToArray(instance.indicators, angular.copy(processedBlocks.indicators));
            loadToArray(instance.stopLimitBlocks, angular.copy(processedBlocks.stopLimitBlocks));
            loadToArray(instance.exitTypes, angular.copy(processedBlocks.exitTypes));
            loadToArray(instance.parameterSets, angular.copy(processedBlocks.parameterSets));

            //filter blocks by engine
            instance.filterBlocksByEngine(null, true);

            deferred.resolve();
        });
    }

    function getBuildingBlocks(callback) {
        if (buildingBlocks.loaded) {
            callback(buildingBlocks);
        }
        else {
            BackendService.sendRequest('blocks/list', null, function (result) {
                loadToArray(buildingBlocks.parameterSets, result.parameterSets);
                loadToArray(buildingBlocks.orderTypes, result.orderTypes);

                formulas.length = 0;
                buildingBlocks.blocks.length = 0;
                buildingBlocks.indicators.length = 0;
                buildingBlocks.stopLimitBlocks.length = 0;
                buildingBlocks.exitTypes.length = 0;

                createBlocks(formulas, result.formulas, "formula");

                createBlocks(buildingBlocks.blocks, result.simpleRules, "simpleRule");

                createBlocks(buildingBlocks.indicators, result.others, "other");
                createBlocks(buildingBlocks.indicators, result.priceValues, "priceValue");
                createBlocks(buildingBlocks.indicators, result.priceRanges, "priceRange");
                createBlocks(buildingBlocks.indicators, result.indicators, "indicator");
                createBlocks(buildingBlocks.indicators, result.operators, "operator");

                createBlocks(buildingBlocks.exitTypes, result.exitTypes), "exitType";

                buildingBlocks.exitTypes.push({
                    key: exitRuleKey,
                    name: "ExitRule",
                    probability: "50",
                    use: false
                });

                buildingBlocks.exitTypes.push({
                    key: eodRuleKey,
                    name: "Exit at End of Day",
                    probability: "50",
                    use: false
                });

                //Create artificial categories
                var numericBlocks = [];
                var priceBlocks = [];
                var priceBlocks2 = [];
                var priceRangeBlocks = [];

                //load custom blocks

                if(result.customBlocks){
                    for(var i=0; i<result.customBlocks.length; i++){
                        var customBlock = result.customBlocks[i];
                        
                        if(customBlock.groupKey == "Custom conditions"){
                            buildingBlocks.blocks.push(customBlock);
                        }
                        else if(customBlock.groupKey == "Custom price levels"){
                            priceBlocks.push(customBlock);
                        }
                        else {
                            //skip custom values
                        }
                    }
                }

                for (var i = buildingBlocks.indicators.length - 1; i >= 0; i--) {
                    var block = buildingBlocks.indicators[i];

                    switch (block.group) {
                        case 'Bar And Time':
                            if (block.returnType != 'number') {
                                buildingBlocks.blocks.push(block);
                                buildingBlocks.indicators.splice(i, 1);
                            }
                            continue;
                        case 'Bar Range':
                            if (block.key == "FixedPips") {
                                buildingBlocks.indicators.splice(i, 1);
                            }
                            break;
                        case 'Indicators':
                            if (block.key != "ATR" && block.key != "MTATR" && block.key != "TrueRange") {
                                block.group = "IndicatorsOld";
                            }
                            break;
                        case 'Price':
                            priceBlocks2.push(block);
                            break;
                        case 'Comparisons':
                            block.group = "Operators";
                            break;
                    }

                    if (block.returnType == 'number') {
                        numericBlocks.push(block);
                    } else if (block.returnType == 'price') {
                        priceBlocks.push(block);
                    } else if (block.returnType == 'pricerange') {
                        priceRangeBlocks.push(block);
                    }
                }

                for (var i = 0; i < numericBlocks.length; i++) {
                    var block = angular.copy(numericBlocks[i]);
                    block.group = "Indicators";
                    block.groupPriority = 1;
                    block.key = block.group + "." + block.key;
                    buildingBlocks.indicators.push(block);
                }

                for (var i = 0; i < priceBlocks.length; i++) {
                    var block = angular.copy(priceBlocks[i]);

                    if (block.group == "IndicatorsOld") {
                        block.group = "Indicators";
                        block.key = block.group + "." + block.key;
                        if (!getItem(buildingBlocks.indicators, 'key', block.key)) {
                            buildingBlocks.indicators.push(block);
                        }
                        block = angular.copy(priceBlocks[i]);
                    }

                    block.group = "Stop/Limit Price Levels";
                    block.key = block.group + "." + block.key;
                    buildingBlocks.stopLimitBlocks.push(block);
                }

                for (var i = 0; i < priceBlocks2.length; i++) {
                    var block = angular.copy(priceBlocks2[i]);
                    block.group = "Stop/Limit Price Levels";
                    block.key = block.group + "." + block.key;
                    if (!getItem(buildingBlocks.stopLimitBlocks, 'key', block.key)) {
                        buildingBlocks.stopLimitBlocks.push(block);
                    }

                    block = angular.copy(priceBlocks2[i]);
                    block.group = "Prices";
                    block.key = block.group + "." + block.key;
                    if (!getItem(buildingBlocks.indicators, 'key', block.key)) {
                        buildingBlocks.indicators.push(block);
                    }
                }

                for (var i = 0; i < priceRangeBlocks.length; i++) {
                    var block = angular.copy(priceRangeBlocks[i]);
                    block.group = "Stop/Limit Price Ranges";
                    block.key = block.group + "." + block.key;
                    buildingBlocks.stopLimitBlocks.push(block);
                }

                //remove original old categories

                for (var i = buildingBlocks.indicators.length - 1; i >= 0; i--) {
                    var block = buildingBlocks.indicators[i];

                    if (block.group == "Indicators"){
                        block.buildingBlockType = 'indicator';

                        if(block.key.indexOf("Indicators") < 0) {
                            block.key = "Indicators." + block.key;
                        }
                    }

                    if (block.group == 'Price' || block.group == 'IndicatorsOld' || block.group == 'Other') {
                        buildingBlocks.indicators.splice(i, 1);
                    }
                }

                buildingBlocks.loaded = true;

                callback(buildingBlocks);
            });
        }
    }

    function createBlocks(targetArray, sourceArray, buildingBlockType) {
        loadToArray(targetArray, sourceArray, true);

        for (var i = 0; i < targetArray.length; i++) {
            let item = targetArray[i];
            item.buildingBlockType = buildingBlockType;

            tryLoadDefaultParameterSets(item);
        }
    }

    function tryLoadDefaultParameterSets(blockObj) {
        blockObj.defaultParamSets = [];

        var block = getItem(buildingBlocks.parameterSets, 'block', blockObj.key);
        if (block && block.paramSets) {
            for (var i = 0; i < block.paramSets.length; i++) {
                var setParams = angular.copy(blockObj.params);
                var paramSet = block.paramSets[i];
                var params = paramSet.values.split(",");

                for (var p = 0; p < params.length; p++) {
                    var paramValue = params[p];
                    var isRandom = paramValue == '*';

                    if (setParams[p]) {
                        setParams[p].generation = isRandom ? instance.generationTypes.random : instance.generationTypes.fixed;
                        setParams[p].fixedValue = isRandom ? null : paramValue;
                    } else break;
                }

                var set = {
                    name: 'Default set ' + (i + 1),
                    params: setParams,
                    weight: paramSet.weight >= 0 ? paramSet.weight : 1
                }

                blockObj.defaultParamSets.push(set);
            }

            blockObj.paramSets = angular.copy(blockObj.defaultParamSets);
        }
    }

    function loadParamSets(blockObj, forceLoading) {
        if (!blockObj.paramSetsObj) {
            blockObj.paramSets = blockObj.paramSets || angular.copy(blockObj.defaultParamSets);
            saveParamSetsToBlock(blockObj, blockObj.paramSets, forceLoading);
            return;
        }

        if(!blockObj.defaultParamSets || !blockObj.defaultParamSets.length){
            blockObj.defaultParamSets = getPredefinedSets(blockObj);


            blockObj.paramSets = angular.copy(blockObj.defaultParamSets);
        }
        else {
            blockObj.paramSets = getPredefinedSets(blockObj);
        }
    }

    this.loadSettings = function (callback) {
        dataPromise.then(function () {
            instance.disableAllBlocks()

            var settingsElem = AppService.getTaskConfig();
            var blockSettingsElem = getChildElement(settingsElem, 'Blocks');
            var mode = getAttrValue(blockSettingsElem, 'type');
            instance.mode = mode == instance.modes.advanced ? instance.modes.advanced : instance.modes.simple;

            var calibrationElem = getChildElement(blockSettingsElem, 'Calibration');
            if (calibrationElem) {
                instance.gui.autoCalibrateBeforeStart = getAttrBooleanValue(calibrationElem, "calibrateBeforeStart", false);
                instance.gui.useCalibrationMaxSteps = getAttrBooleanValue(calibrationElem, "useMaxSteps", false);
                instance.gui.calibrationMaxSteps = getAttrIntValue(calibrationElem, "maxSteps", 50);
            }
            else {
                instance.gui.autoCalibrateBeforeStart = false;
                instance.gui.useCalibrationMaxSteps = true;
                instance.gui.calibrationMaxSteps = 50;
            }

            var buildingBlocksElem = getChildElement(blockSettingsElem, 'BuildingBlocks');
            loadBlocks(buildingBlocksElem);

            var orderTypesElem = getChildElement(blockSettingsElem, 'OrderTypes');
            loadBlocks(orderTypesElem);

            instance.loadCustomData();

            var exitTypesElem = getChildElement(blockSettingsElem, 'ExitTypes');
            if (exitTypesElem) {
                var blockElems = getChildElements(exitTypesElem, 'Block');
                for (var i = 0; i < blockElems.length; i++) {
                    var blockElem = blockElems[i];
                    var blockObj = getFormulaInfo(blockElem);
                    loadExitTypeInfo(blockObj);
                }
            }

            refreshGrids();

            if (instance.correctAllBlocksChartSettings()) {
                onSettingsChange();
            }

            //Check SLPT settings
            var whatToBuildElem = AppService.getCurrentTaskTabSettings("WhatToBuild");
            var SLPTOptionsElem = getChildElement(whatToBuildElem, "SLPTOptions");

            var SLRequired = getNodeBooleanValue(SLPTOptionsElem, "SLRequired", false);
            var SLIndicatorBased = getNodeBooleanValue(SLPTOptionsElem, "SLIndicatorBased", false);

            instance.changeSLPT({
                setting: 'Stop Loss',
                required: SLRequired,
                indicatorBased: SLIndicatorBased
            }, true);

            var PTRequired = getNodeBooleanValue(SLPTOptionsElem, "PTRequired", false);
            var PTIndicatorBased = getNodeBooleanValue(SLPTOptionsElem, "PTIndicatorBased", false);

            instance.changeSLPT({
                setting: 'Profit Target',
                required: PTRequired,
                indicatorBased: PTIndicatorBased
            }, false);

            refreshSelectedCounts();

            checkExitTypesDependencies();

            $timeout(function () {
                instance.gui.initialized = true;
                if (callback) callback();
            });
        });
    }

    this.disableAllBlocks = function() {
        if(instance.orderTypes && instance.orderTypes.length){
            instance.orderTypes.forEach(block => block.use = false);
        }
        if(instance.exitTypes && instance.exitTypes.length){
            instance.exitTypes.forEach(block => block.use = false);
        }
        if(instance.blocks && instance.blocks.length){
            instance.blocks.forEach(block => block.use = false);
        }
        if(instance.indicators && instance.indicators.length){
            instance.indicators.forEach(block => block.use = false);
        }
        if(instance.stopLimitBlocks && instance.stopLimitBlocks.length){
            instance.stopLimitBlocks.forEach(block => block.use = false);
        }
        if(instance.customData && instance.customData.length){
            instance.customData.forEach(block => block.use = false);
        }
    }

    this.loadSettingsFromFile = function (filePath) {
        BackendService.sendRequest('blocks/loadFromFile', {
            filePath: filePath
        }, function (result) {
            if (result.success) {
                applyBlockSettings(result.blockSettings);
            }
        }, 'POST');
    }

    function applyBlockSettings(blockSettings) {
        var settingsElem = AppService.getTaskConfig();
        var blockSettingsElem = getChildElement(settingsElem, 'Blocks');
        var newSettingsElem = xmlToObject(blockSettings).find("Blocks")[0];

        settingsElem.replaceChild(newSettingsElem, blockSettingsElem);

        AppService.updateTaskXML().then(function () {
            SQEvents.notifyListeners(SQEvents.get("SETTINGS_TAB_RELOAD"), "Building blocks");
            $rootScope.showSuccess(L.tsq("Block settings loaded"));
        });
    }

    this.saveSettingsToFile = function (filePath) {
        var settingsElem = AppService.getTaskConfig();
        var blockSettingsElem = getChildElement(settingsElem, 'Blocks');
        var blockSettings = xmlToString(blockSettingsElem, true);

        BackendService.sendRequest('blocks/saveToFile', {
            filePath: filePath,
            fileContent: blockSettings
        }, function (result) {
            if (result.success) {
                window.parent.hidePopup("#saveFileDialog");
                $rootScope.showSuccess(L.tsq("Block settings saved"));
            }
        }, 'POST');
    }

    function loadBlocks(parentElem) {
        var blockElems = getChildElements(parentElem, 'Block');

        for (var i = 0; i < blockElems.length; i++) {
            var blockElem = blockElems[i];
            var predefinedElem = getChildElement(blockElem, 'Predefined', true);
            var generatedElem = getChildElement(blockElem, 'Generated', true);
            var formulasElem = getChildElement(blockElem, 'Formulas', true);

            var blockObj = {
                key: getAttrValue(blockElem, 'key'),
                weight: getAttrValue(blockElem, 'weight'),
                use: getAttrValue(blockElem, 'use') == 'true',
                category: getAttrValue(blockElem, 'category'),
            }

            //correct category when loading old config files 
            if (blockObj.key && (blockObj.key.indexOf("Stop/Limit Price Levels") == 0 || blockObj.key.indexOf("Stop/Limit Price Ranges") == 0)) {
                blockObj.category = instance.categories.stopLimitBlocks;
            }

            if (getAttrValue(blockElem, "indicatorMin") != null) {
                blockObj.indicatorMin = getAttrValue(blockElem, "indicatorMin");
                blockObj.indicatorMax = getAttrValue(blockElem, "indicatorMax");
                blockObj.indicatorStep = getAttrValue(blockElem, "indicatorStep");
            }

            if (predefinedElem) {
                if (attributeExists(predefinedElem, "changed")) {
                    blockObj.paramSetsChanged = getAttrBooleanValue(predefinedElem, 'changed', false);
                } else {
                    loadParamSets(blockObj, true);
                    blockObj.paramSetsChanged = paramSetsChanged(blockObj, blockObj.paramSets);
                    predefinedElem.setAttribute("changed", !!blockObj.paramSetsChanged);
                }

                blockObj.paramSetsObj = angular.copy(predefinedElem);
            }

            if (generatedElem) {
                blockObj.generatedWeight = getAttrIntValue(generatedElem, "weight");
                blockObj.customParams = getGeneratedParams(generatedElem, 'Param');
            }

            if (formulasElem) {
                var formulaElems = getChildElements(formulasElem, 'Formula');
                if (formulaElems.length) {
                    blockObj.customParams = blockObj.customParams || [];

                    for (var a = 0; a < formulaElems.length; a++) {
                        var formulaElem = formulaElems[a];
                        var formulaInfo = getFormulaInfo(formulaElem);
                        var customParamFound = false;

                        for (var p = 0; p < blockObj.customParams.length; p++) {
                            var customParam = blockObj.customParams[p];
                            if (customParam.key == formulaInfo.key) {
                                formulaInfo.name = customParam.name;
                                formulaInfo.type = customParam.type;
                                formulaInfo.generation = customParam.generation;

                                blockObj.customParams[p] = formulaInfo;
                                customParamFound = true;
                                break;
                            }
                        }

                        if (!customParamFound) blockObj.customParams.push(formulaInfo);
                    }
                }
            }

            loadBlockInfo(blockObj);
        }
    }

    instance.loadCustomData = function (reloadGrid) {
        var timeframes = getMainSetupTimeframes();
        instance.customDataTimeframes.list = timeframes.join(', ');

        if (instance.forceDigest) instance.forceDigest();

        //load settings from project config
        var settingsElem = AppService.getTaskConfig();
        var blockSettingsElem = getChildElement(settingsElem, 'Blocks');

        var customDataElem = getChildElement(blockSettingsElem, 'CustomData');
        instance.gui.showAllCDataIndy = getAttrBooleanValue(customDataElem, "showAll", false);

        //load available custom data
        var availableCustomData = SQConstants.getConstants().customdata;

        var customData = getBlocksDataArray(instance.categories.customData);
        customData.length = 0;

        for (var i = 0; i < availableCustomData.length; i++) {
            var data = availableCustomData[i];

            if (data.rows == 0) continue;

            var disabled = false;

            if(!timeframes.includes(data.timeframe)) {
                if(instance.gui.showAllCDataIndy) {
                    disabled = true;
                } else {
                    continue;
                }
            }

            customData.push({
                use: false,
                key: data.id,
                name: data.name,
                timeframe: data.timeframe,
                weight: 1,
                category: instance.categories.customData,
                disabled: disabled
            })
        }

        customData.sort(function(a, b){
            return a.disabled - b.disabled;
        });

        //apply settings from project config
        var dataElems = getChildElements(customDataElem, 'Data');

        for (var i = 0; i < dataElems.length; i++) {
            var dataElem = dataElems[i];

            var key = getAttrValue(dataElem, 'key');

            var item = getItem(customData, 'key', key);
            if (item) {
                item.use = getAttrValue(dataElem, 'use') == 'true';
                item.weight = getAttrValue(dataElem, 'weight');
            }
        }

        if (reloadGrid && customDataGrid) {
            refreshGrid(customDataGrid);
        }
    }

    function getMainSetupTimeframes() {
        var timeframes = [];

        var mainSetup = DataService.config.setups[0];

        if (mainSetup) {
            timeframes.push(mainSetup.timeframe);

            if (mainSetup.subcharts) {
                for (var i = 0; i < mainSetup.subcharts.length; i++) {
                    timeframes.push(mainSetup.subcharts[i].timeframe);
                }
            }
        }

        return timeframes;
    }

    function getGeneratedParams(parentElem, paramElemName) {
        var customParams = [];
        var paramElems = getChildElements(parentElem, paramElemName);
        if (paramElems.length) {
            for (var a = 0; a < paramElems.length; a++) {
                var paramElem = paramElems[a];
                customParams.push(getParamXMLInfo(paramElem));
            }
        }
        return customParams;
    }

    function getPredefinedSets(blockObj) {
        var parentElem = blockObj.paramSetsObj;
        var paramSets = [];

        var setElems = getChildElements(parentElem, 'Params');

        if (setElems.length) {
            for (var a = 0; a < setElems.length; a++) {
                var setElem = setElems[a];
                var paramsElems = getChildElements(setElem, 'Param');
                var weight = getAttrIntValue(setElem, 'weight', 1);

                var setObj = {
                    name: getAttrValue(setElem, 'name'),
                    params: [],
                    weight: weight >= 0 ? weight : 1
                }

                if (paramsElems.length) {
                    for (var p = 0; p < paramsElems.length; p++) {
                        var paramsElem = paramsElems[p];
                        setObj.params.push(getParamXMLInfo(paramsElem));
                    }
                }

                //try add missing params
                for (var i = 0; i < blockObj.params.length; i++) {
                    var param = blockObj.params[i];
                    var setParam = getItem(setObj.params, 'key', param.key);

                    if (!setParam) {
                        setObj.params.push(angular.copy(param));
                    }
                }

                paramSets.push(setObj);
            }
        }
        return paramSets;
    }

    function getFormulaInfo(parentElem) {
        var info = {
            key: getAttrValue(parentElem, 'key'),
            probability: getAttrValue(parentElem, 'probability'),
            use: getAttrBooleanValue(parentElem, 'use', false),
            category: getAttrValue(parentElem, 'category'),
            exitMethod: getAttrValue(parentElem, 'exitMethod'),
            required: getAttrBooleanValue(parentElem, 'required', false),
            isChanged: true,
            items: []
        };

        if (valueFilled(info.probability)) {
            info.probability = getPercentageNumber(info.probability);
        }
        var valueElems = getChildElements(parentElem, 'Value');

        for (var i = 0; i < valueElems.length; i++) {
            var valueElem = valueElems[i];
            info.items.push({
                key: getAttrValue(valueElem, 'key'),
                use: getAttrBooleanValue(valueElem, 'use', false),
                params: [],
                customParams: [],
                paramSets: []
            });

            var generatedElem = getChildElement(valueElem, 'Generated', true);
            if (generatedElem) {
                info.items[i].generatedWeight = getAttrIntValue(generatedElem, "weight", 1);
                info.items[i].customParams = getGeneratedParams(generatedElem, 'Param');
            }

            var predefinedElem = getChildElement(valueElem, 'Predefined', true);
            if (predefinedElem) {
                info.items[i].paramSetsObj = predefinedElem;
            }
        }

        return info;
    }

    function getParamXMLInfo(paramElem) {
        let param = {
            key: getAttrValue(paramElem, 'key'),
            name: getAttrValue(paramElem, 'name'),
            type: getAttrValue(paramElem, 'type'),
            paramType: getAttrValue(paramElem, 'paramType'),
            generation: getAttrValue(paramElem, 'generation'),
            minValue: getAttrValue(paramElem, 'minValue'),
            maxValue: getAttrValue(paramElem, 'maxValue'),
            step: getAttrValue(paramElem, 'step'),
            fixedValue: getAttrValue(paramElem, 'defaultValue'),
            values: getAttrValue(paramElem, 'values'),
            allCharts: getAttrBooleanValue(paramElem, 'allCharts', false)
        };

        if(param.type === "period"){
            param.type = "int";
            param.paramType = "period";
        }

        return param;
    }

    function refreshGrids() {
        loadBlocksToGrid(instance.categories.signals);
        loadBlocksToGrid(instance.categories.indicators);
        loadBlocksToGrid(instance.categories.stopLimitBlocks);
        loadBlocksToGrid(instance.categories.orderTypes);
        loadBlocksToGrid(instance.categories.exitTypes);
        loadBlocksToGrid(instance.categories.customData);
    }

    this.refreshGrids = refreshGrids;

    function refreshGrid(grid) {
        if (grid == blocksGrid) loadBlocksToGrid(instance.categories.signals);
        if (grid == indicatorsGrid) loadBlocksToGrid(instance.categories.indicators);
        if (grid == stopLimitBlocksGrid) loadBlocksToGrid(instance.categories.stopLimitBlocks);
        else if (grid == orderTypesGrid) loadBlocksToGrid(instance.categories.orderTypes);
        else if (grid == exitTypesGrid) loadBlocksToGrid(instance.categories.exitTypes);

        if (grid == customDataGrid) loadBlocksToGrid(instance.categories.customData);
    }

    function loadBlockInfo(blockObj) {
        var targetBlockObj = instance.getBlockObjectByKey(blockObj.category, blockObj.key);
        if (!targetBlockObj) {
            targetBlockObj = instance.getBlockObjectByKey(instance.categories.indicators, blockObj.key);
        }

        if (targetBlockObj) {
            targetBlockObj.weight = blockObj.weight;
            targetBlockObj.generatedWeight = blockObj.generatedWeight;
            targetBlockObj.use = blockObj.use;
            targetBlockObj.customParams = blockObj.customParams;
            targetBlockObj.paramSets = blockObj.paramSets;
            targetBlockObj.paramSetsObj = blockObj.paramSetsObj;
            targetBlockObj.paramSetsChanged = blockObj.paramSetsChanged;
            targetBlockObj.indicatorMin = blockObj.indicatorMin != null ? blockObj.indicatorMin : targetBlockObj.indicatorMin;
            targetBlockObj.indicatorMax = blockObj.indicatorMax != null ? blockObj.indicatorMax : targetBlockObj.indicatorMax;
            targetBlockObj.indicatorStep = blockObj.indicatorStep != null ? blockObj.indicatorStep : targetBlockObj.indicatorStep;
        } 
        else {
            //block from XML config not found
            return;
        }

        tryLoadChartOptions(blockObj);
    }

    function loadExitTypeInfo(blockObj) {
        var targetBlockObj = instance.getBlockObjectByKey(blockObj.category, blockObj.key);
        if (targetBlockObj) {
            targetBlockObj.use = blockObj.use;
            targetBlockObj.probability = blockObj.probability;
            targetBlockObj.category = blockObj.category;
            targetBlockObj.exitMethod = blockObj.exitMethod;
            targetBlockObj.required = blockObj.required;

            if (arrayNotEmpty(targetBlockObj.items)) {
                for (var i = 0; i < targetBlockObj.items.length; i++) {
                    var item = targetBlockObj.items[i];

                    for (var a = 0; a < blockObj.items.length; a++) {
                        if (!item.key || item.key == blockObj.items[a].key) {
                            blockObj.items[a].name = item.name;
                            blockObj.items[a].type = item.type;
                            blockObj.items[a].generation = item.generation;
                            targetBlockObj.items[i].use = blockObj.items[a].use;
                            targetBlockObj.items[i].customParams = blockObj.items[a].customParams;
                            targetBlockObj.items[i].paramSets = blockObj.items[a].paramSets;
                            targetBlockObj.items[i].generatedWeight = blockObj.items[a].generatedWeight;
                            break;
                        }
                    }
                }
            }
        }
    }

    this.initOrderTypesGrid = function () {
        orderTypesGrid = new sqGrid("orderTypesGrid");

        var columns = [{
            title: L.tsq('ID'),
            type: "text",
            sort: 'text'
        },
        {
            title: L.tsq('Use'),
            type: "text",
            sort: 'text'
        },
        {
            title: "",
            type: "text",
            sort: 'text',
            align: "left"
        },
        {
            title: L.tsq('Weight'),
            type: "float2",
            sort: 'number'
        },
        {
            title: L.tsq('Parameters'),
            type: "text",
            sort: 'text'
        }
        ];

        var widths = [10, 30, '*', 60, 100];

        orderTypesGrid.setFirstColumnAsId(true, true);
        orderTypesGrid.setColumns(columns, !!orderTypesGrid);
        orderTypesGrid.setWidths(widths, !!orderTypesGrid);
        orderTypesGrid.setEmptyGridText(L.tsq('No order types defined.'));
        orderTypesGrid.disableSorting();

        orderTypesGrid.defineWidget('spinnerWidget', sqGridSpinner);
        orderTypesGrid.defineWidget('checkboxWidget', sqGridCheckbox);

        orderTypesGrid.cellEventHandler = function (rowIndex, cellIndex, eventName, args) {
            switch (eventName) {
                case "checkboxChecked":
                    onCheckboxChecked(orderTypesGrid, rowIndex, cellIndex, args[2]);
                    break;
                case "spinnerUpdated":
                    onSpinnerUpdated(orderTypesGrid, rowIndex, cellIndex, args[0]);
                    break;
                case "editParameters":
                    onEditParameters(orderTypesGrid, rowIndex);
                    break;
                case "resetParameters":
                    onResetParameters(orderTypesGrid, rowIndex);
                    break;
            }
        };

        orderTypesGrid.headerRedraw();
    }

    this.initExitTypesGrid = function () {
        exitTypesGrid = new sqGrid("exitTypesGrid");

        var columns = [{
            title: L.tsq('ID'),
            type: "text",
            sort: 'text'
        },
        {
            title: L.tsq('Use'),
            type: "text",
            sort: 'text'
        },
        {
            title: "",
            type: "text",
            sort: 'text',
            align: "left"
        },
        {
            title: L.tsq('Required'),
            type: "text",
            sort: 'text'
        },
        {
            title: L.tsq('Parameters'),
            type: "text",
            sort: 'text'
        }
        ];

        var widths = [10, 30, '*', 60, 100];

        exitTypesGrid.setFirstColumnAsId(true, true);
        exitTypesGrid.setColumns(columns, !!exitTypesGrid);
        exitTypesGrid.setWidths(widths, !!exitTypesGrid);
        exitTypesGrid.setEmptyGridText(L.tsq('No exit types defined.'));
        exitTypesGrid.disableSorting();

        exitTypesGrid.defineWidget('checkboxWidget', sqGridCheckbox);

        exitTypesGrid.cellEventHandler = function (rowIndex, cellIndex, eventName, args) {
            switch (eventName) {
                case "checkboxChecked":
                    onCheckboxChecked(exitTypesGrid, rowIndex, cellIndex, args[2]);
                    break;
                case "spinnerUpdated":
                    onSpinnerUpdated(exitTypesGrid, rowIndex, cellIndex, args[0]);
                    break;
                case "editParameters":
                    onEditParameters(exitTypesGrid, rowIndex);
                    break;
                case "resetParameters":
                    onResetParameters(exitTypesGrid, rowIndex);
                    break;
            }
        };

        exitTypesGrid.headerRedraw();
    }

    this.initBlocksGrid = function () {
        blocksGrid = new sqGrid("blocksGrid");

        var columns = [{
            title: L.tsq('ID'),
            type: "text",
            sort: 'text'
        },
        {
            title: L.tsq('Use'),
            type: "text",
            sort: 'text'
        },
        {
            title: "",
            type: "text",
            sort: 'text',
            align: "left"
        },
        {
            title: L.tsq('Weight'),
            type: "float2",
            sort: 'number'
        },
        {
            title: L.tsq('Parameters'),
            type: "text",
            sort: 'text'
        }
        ];

        var widths = [10, 30, '*', 60, 100];

        blocksGrid.setFirstColumnAsId(true, true);
        blocksGrid.setColumns(columns, !!blocksGrid);
        blocksGrid.setWidths(widths, !!blocksGrid);
        blocksGrid.setEmptyGridText(L.tsq('No signals defined.'));
        blocksGrid.disableSorting();

        blocksGrid.defineWidget('spinnerWidget', sqGridSpinner);
        blocksGrid.defineWidget('checkboxWidget', sqGridCheckbox);

        blocksGrid.cellEventHandler = function (rowIndex, cellIndex, eventName, args) {
            switch (eventName) {
                case "checkboxChecked":
                    onCheckboxChecked(blocksGrid, rowIndex, cellIndex, args[2]);
                    break;
                case "spinnerUpdated":
                    onSpinnerUpdated(blocksGrid, rowIndex, cellIndex, args[0]);
                    break;
                case "editParameters":
                    onEditParameters(blocksGrid, rowIndex);
                    break;
                case "resetParameters":
                    onResetParameters(blocksGrid, rowIndex);
                    break;
                case "showVolumeProfileAddon":
                    window.parent.showPopup('#volumeProfileAddonPopup');
                    break;
            }
        };

        blocksGrid.headerRedraw();
    }

    this.initIndicatorsGrid = function () {
        indicatorsGrid = new sqGrid("indicatorsGrid");

        var columns = [{
            title: L.tsq('ID'),
            type: "text",
            sort: 'text'
        },
        {
            title: L.tsq('Use'),
            type: "text",
            sort: 'text'
        },
        {
            title: "",
            type: "text",
            sort: 'text',
            align: "left"
        },
        {
            title: L.tsq('Weight'),
            type: "float2",
            sort: 'number'
        },
        {
            title: L.tsq('Parameters'),
            type: "text",
            sort: 'text'
        }
        ];

        var widths = [10, 30, '*', 60, 100];

        indicatorsGrid.setFirstColumnAsId(true, true);
        indicatorsGrid.setColumns(columns, !!indicatorsGrid);
        indicatorsGrid.setWidths(widths, !!indicatorsGrid);
        indicatorsGrid.setEmptyGridText(L.tsq('No indicators defined.'));
        indicatorsGrid.disableSorting();

        indicatorsGrid.defineWidget('spinnerWidget', sqGridSpinner);
        indicatorsGrid.defineWidget('checkboxWidget', sqGridCheckbox);

        indicatorsGrid.cellEventHandler = function (rowIndex, cellIndex, eventName, args) {
            switch (eventName) {
                case "checkboxChecked":
                    onCheckboxChecked(indicatorsGrid, rowIndex, cellIndex, args[2]);
                    break;
                case "spinnerUpdated":
                    onSpinnerUpdated(indicatorsGrid, rowIndex, cellIndex, args[0]);
                    break;
                case "editParameters":
                    onEditParameters(indicatorsGrid, rowIndex);
                    break;
                case "resetParameters":
                    onResetParameters(indicatorsGrid, rowIndex);
                    break;
                case "showVolumeProfileAddon":
                    window.parent.showPopup('#volumeProfileAddonPopup');
                    break;
            }
        };

        indicatorsGrid.headerRedraw();
    }


    this.initStopLimitBlocksGrid = function () {
        stopLimitBlocksGrid = new sqGrid("stopLimitBlocksGrid");

        var columns = [{
            title: L.tsq('ID'),
            type: "text",
            sort: 'text'
        },
        {
            title: L.tsq('Use'),
            type: "text",
            sort: 'text'
        },
        {
            title: "",
            type: "text",
            sort: 'text',
            align: "left"
        },
        {
            title: L.tsq('Weight'),
            type: "float2",
            sort: 'number'
        },
        {
            title: L.tsq('Parameters'),
            type: "text",
            sort: 'text'
        }
        ];

        var widths = [10, 30, '*', 60, 100];

        stopLimitBlocksGrid.setFirstColumnAsId(true, true);
        stopLimitBlocksGrid.setColumns(columns, !!stopLimitBlocksGrid);
        stopLimitBlocksGrid.setWidths(widths, !!stopLimitBlocksGrid);
        stopLimitBlocksGrid.setEmptyGridText(L.tsq('No Stop/Limit blocks defined.'));
        stopLimitBlocksGrid.disableSorting();

        stopLimitBlocksGrid.defineWidget('spinnerWidget', sqGridSpinner);
        stopLimitBlocksGrid.defineWidget('checkboxWidget', sqGridCheckbox);

        stopLimitBlocksGrid.cellEventHandler = function (rowIndex, cellIndex, eventName, args) {
            switch (eventName) {
                case "checkboxChecked":
                    onCheckboxChecked(stopLimitBlocksGrid, rowIndex, cellIndex, args[2]);
                    break;
                case "spinnerUpdated":
                    onSpinnerUpdated(stopLimitBlocksGrid, rowIndex, cellIndex, args[0]);
                    break;
                case "editParameters":
                    onEditParameters(stopLimitBlocksGrid, rowIndex);
                    break;
                case "resetParameters":
                    onResetParameters(stopLimitBlocksGrid, rowIndex);
                    break;
                case "showVolumeProfileAddon":
                    window.parent.showPopup('#volumeProfileAddonPopup');
                    break;
            }
        };

        stopLimitBlocksGrid.headerRedraw();
    }

    this.initCustomDataGrid = function () {
        customDataGrid = new sqGrid("customDataGridBB");

        var columns = [{
            title: L.tsq('ID'),
            type: "text",
            sort: 'text'
        },
        {
            title: L.tsq('Use'),
            type: "text",
            sort: 'text'
        },
        {
            title: "",
            type: "text",
            sort: 'text',
            align: "left"
        },
        {
            title: L.tsq('Timeframe'),
            type: "text",
            sort: 'text'
        },
        {
            title: L.tsq('Weight'),
            type: "float2",
            sort: 'number'
        },
        ];

        var widths = [10, 30, '*', 80, 60];

        customDataGrid.setFirstColumnAsId(true, true);
        customDataGrid.setColumns(columns, !!customDataGrid);
        customDataGrid.setWidths(widths, !!customDataGrid);
        customDataGrid.setEmptyGridText(L.tsq('No custom data defined.'));
        customDataGrid.disableSorting();

        customDataGrid.defineWidget('spinnerWidget', sqGridSpinner);
        customDataGrid.defineWidget('checkboxWidget', sqGridCheckbox);

        customDataGrid.cellEventHandler = function (rowIndex, cellIndex, eventName, args) {
            switch (eventName) {
                case "checkboxChecked":
                    onCheckboxChecked(customDataGrid, rowIndex, cellIndex, args[2], instance.categories.customData);
                    break;
                case "spinnerUpdated":
                    onSpinnerUpdated(customDataGrid, rowIndex, cellIndex, args[0]);
                    break;
            }
        };

        customDataGrid.headerRedraw();
    }

    function onResetParameters(grid, rowIndex) {
        if (rowIndex == 0) {
            resetAllBlocks(grid);
        } else if (grid.getUserData(rowIndex, 'group')) {
            resetWholeSimpleRulesGroup(grid, rowIndex);
        }

        onSettingsChange();
        updateGroups(grid);
        updateGlobalParamsValue(grid);
        grid.debouncedBodyRedrawWithResort();
    }

    function onEditParameters(grid, rowIndex) {
        var selectedBlockObj = getBlockObjectByRowIndex(grid, rowIndex);

        if (grid == exitTypesGrid) {
            instance.gui.editingNormalBlock = false;

            if (arrayNotEmpty(selectedBlockObj.items)) {
                instance.gui.editingNormalBlock = false;

                instance.showPossibleOptions(selectedBlockObj, true);

                $rootScope.$applyAsync();
            }
        }
        else {
            instance.gui.editingNormalBlock = true;

            if (arrayNotEmpty(selectedBlockObj.params)) {
                showParams(selectedBlockObj);

                showPopup('#blockParametersPopup', function () {
                    $timeout(function () {
                        $('#blockParametersPopup .tabs-header > div:not(.ng-hide)').eq(0).click();
                        SQEvents.notifyListeners(SQEvents.get("BLOCKS_PARAMS_RELOAD"), {});
                    }, 0, false);
                });
            }
        }
    }

    function onSpinnerUpdated(grid, rowIndex, colIndex, value) {
        if (rowIndex == 0) {
            for (var i = 1; i < grid.getNumberOfRows(); i++) {
                // grid.modifyRowByIndex("{{spinnerWidget value='" + value + "' min='1'}}", i, 3, true);
                sqGridSpinnerValueSet(grid, i, 3, value, true)
                if (grid.getUserData(i, "group")) continue;

                getBlockObjectByRowIndex(grid, i).weight = value;
            }
        } else if (grid.getUserData(rowIndex, "group")) {
            var categorySize = getCategorySize(grid, rowIndex);

            for (var i = rowIndex + 1; i <= rowIndex + categorySize; i++) {
                var blockObject = getBlockObjectByRowIndex(grid, i);
                blockObject.weight = value;
                // grid.modifyRowByIndex("{{spinnerWidget value='" + value + "' min='1'}}", i, 3, true);
                sqGridSpinnerValueSet(grid, i, 3, value, true)
            }
        } else {
            var blockObject = getBlockObjectByRowIndex(grid, rowIndex);
            blockObject.weight = value;
        }

        grid.debouncedBodyRedrawWithResort();

        onSettingsChange();
    }

    function onCheckboxChecked(grid, rowIndex, colIndex, checked, category) {
        var isGroup = grid.getUserData(rowIndex, "group");
        var blockObject = (isGroup || !rowIndex) ? null : getBlockObjectByRowIndex(grid, rowIndex);

        if (colIndex == 1) {
            if (rowIndex == 0) {
                instance.checkAllRows(grid, checked);
            }
            else if (isGroup) {
                var categorySize = getCategorySize(grid, rowIndex);

                for (var i = rowIndex + 1; i <= rowIndex + categorySize; i++) {
                    var curBlock = getBlockObjectByRowIndex(grid, i);
                    if(curBlock.empty){
                        sqGridCheckboxSetValue(grid, i, 1, false, true);
                        getBlockObjectByRowIndex(grid, i).use = false;
                    }
                    else {
                        sqGridCheckboxSetValue(grid, i, 1, checked, true);
                        getBlockObjectByRowIndex(grid, i).use = checked;
                    }
                }
            }
            else {
                blockObject.use = !blockObject.empty && checked;
            }
            
            if ((grid == blocksGrid && instance.signalsFilter.key == 'Checked') || (grid == indicatorsGrid || instance.indicatorsFilter.key == 'Checked')) {      
                //we must refresh the grid if checked items filter is active
                refreshGrid(grid);
            }
        }
        else {
            if (category == instance.categories.customData) { //custom data
               //do nothing
            }
            else { //exit types
                if (rowIndex == 0) {
                    for (var i = 1; i < grid.getNumberOfRows(); i++) {
                        setExitTypeRequired(grid, getBlockObjectByRowIndex(grid, i), i, checked);
                    }
                }
                else {
                    setExitTypeRequired(grid, blockObject, rowIndex, checked);
                }
            }
        }

        if (grid === exitTypesGrid) {
            checkExitTypesDependencies();
        }

        grid.bodyRedraw();

        onSettingsChange();
    }

    function checkExitTypesDependencies() {
        if (!exitTypesGrid) return;

        updateDependentBlock("MoveSL2BE.MoveSL2BE", "MoveSL2BE.SL2BEAddPips");
        updateDependentBlock("TrailingStop.TrailingStop", "TrailingStop.TrailingActivation");
    }

    function updateDependentBlock(mainBlockKey, dependentBlockKey) {
        var mainBlock = getItem(instance.exitTypes, "key", mainBlockKey);
        var dependentBlock = getItem(instance.exitTypes, "key", dependentBlockKey);

        if (mainBlock && dependentBlock) {
            var rowIndex = getBlockRowIndex(exitTypesGrid, dependentBlock.key);

            if (mainBlock.use) {
                exitTypesGrid.setCellDisabled(rowIndex, 1, false, true);
                exitTypesGrid.setCellDisabled(rowIndex, 3, false, true);
            }
            else {
                dependentBlock.use = false;
                dependentBlock.probability = 50;

                sqGridCheckboxSetValue(exitTypesGrid, rowIndex, 1, false, true);
                sqGridCheckboxSetValue(exitTypesGrid, rowIndex, 3, false, true);

                exitTypesGrid.setCellDisabled(rowIndex, 1, true, true);
                exitTypesGrid.setCellDisabled(rowIndex, 3, true, true);
            }
        }
    }

    this.showPossibleOptions = function (specialBlock, enableExitParams) {
        setSpecialBlockDetails(instance.specialBlock, specialBlock);

        var formula = getItem(formulas, 'key', specialBlock.name);
        //get additional info from original formula items

        if (arrayEmpty(instance.specialBlock.items)) {
            instance.specialBlock.items = angular.copy(formula.items);
        }

        if (arrayNotEmpty(instance.specialBlock.items)) {
            for (var i = 0; i < instance.specialBlock.items.length; i++) {
                var item = instance.specialBlock.items[i];
                item.enableExitParams = enableExitParams; //to let the blockConfig directive know it should show exitMethod params

                if (specialBlock.generation == 'formula') {
                    item.params = angular.copy(formula.items[i].params);
                    item.name = formula.items[i].name;
                }

                item.onResetToDefault = onResetToDefault;

                if (arrayEmpty(item.customParams)) {
                    item.customParams = angular.copy(item.params);
                }
                //set params' min/max and step values if missing (it can cause problems when changing fixed number value)
                else {
                    for (var a = item.customParams.length - 1; a >= 0; a--) {
                        var customParam = item.customParams[a];
                        var originalParam = item.params[a];

                        if (!originalParam) {
                            item.customParams.splice(a, 1);
                            continue;
                        }

                        if (!valueFilled(customParam.minValue) && valueFilled(originalParam.minValue)) {
                            customParam.minValue = originalParam.minValue;
                        }
                        if (!valueFilled(customParam.maxValue) && valueFilled(originalParam.maxValue)) {
                            customParam.maxValue = originalParam.maxValue;
                        }
                        if (!valueFilled(customParam.step) && valueFilled(originalParam.step)) {
                            customParam.step = originalParam.step;
                        }
                    }
                }
            }
        }

        showPopup('#possibleOptionsPopup', function () {
            $timeout(function () {
                $('#possibleOptionsPopup .tabs-header > div:not(.ng-hide)').eq(0).click();
                SQEvents.notifyListeners(SQEvents.get("BLOCKS_PARAMS_RELOAD"), {});
            }, 0, false);
        });
    }

    function setSpecialBlockDetails(targetObj, sourceObj) {
        targetObj.category = sourceObj.category;
        targetObj.key = sourceObj.key;
        targetObj.name = sourceObj.name;
        targetObj.generatedWeight = sourceObj.generatedWeight;
        targetObj.probability = sourceObj.probability;
        targetObj.items = angular.copy(sourceObj.items);
        targetObj.generation = sourceObj.generation;
        targetObj.minValue = sourceObj.minValue;
        targetObj.maxValue = sourceObj.maxValue;
        targetObj.step = sourceObj.step;
        targetObj.values = sourceObj.values;
        targetObj.use = sourceObj.use;
    }

    function setExitTypeRequired(grid, blockObject, rowIndex, required, isInit) {
        blockObject.probability = required ? 100 : 50;

        sqGridCheckboxSetValue(grid, rowIndex, 3, required, true);

        if (required) {
            blockObject.use = true;
            sqGridCheckboxSetValue(grid, rowIndex, 1, true, true);
        }

        grid.setCellDisabled(rowIndex, 1, required, true);

        if (!isInit && (blockObject.name == "Stop Loss" || blockObject.name == "Profit Target")) {
            SQEvents.notifyListeners(SQEvents.get("SLPT_SETTINGS_CHANGED"), {
                setting: blockObject.name,
                required: required,
                from: "BlocksService"
            });
        }
    }

    function getCategorySize(grid, categoryRowIndex) {
        var size = 0;
        var blockCount = grid.getNumberOfRows();

        while ((categoryRowIndex < blockCount - 1) && !grid.getUserData(++categoryRowIndex, "group")) {
            size++;
        }
        return size;
    }

    function getPercentageNumber(value) {
        var percIndex = value.indexOf('%');
        if (percIndex > 0) {
            return value.substr(0, percIndex);
        }
        return value;
    }

    function loadBlocksToGrid(category) {
        var grid = getBlocksGrid(category);
        if (!grid) return;
        var data = getBlocksDataArray(category, true);
        var allRowsChecked = true;
        var allWeightsEqual = true;
        var allRowsDefault = true;
        var commonWeight = -1;

        if (category == instance.categories.signals || category == instance.categories.indicators || category == instance.categories.stopLimitBlocks) {
            data.sort(compareBlocks);
        }

        if (grid.getNumberOfRows() > 0) {
            grid.removeAllRows(true, true, true);
        }

        if (category == instance.categories.customData) {
            if (data.length == 0) {
                grid.debouncedBodyRedrawWithResort();
                return;
            }

            grid.addRow(['all', "{{checkboxWidget}}", L.tsq('All'), null, null], true);

            sqGridCheckboxSetValue(grid, 0, 4, false, true);
        } else {
            grid.addRow(['all', "{{checkboxWidget}}", L.tsq('All'), category == instance.categories.exitTypes ? "{{checkboxWidget}}" : "{{spinnerWidget value='1' min='1'}}", createActionLink(L.tsq('Reset to default'), '', 'resetParameters')], true);
        }

        var rowIndex = 1;

        for (var i = 1; i <= data.length; i++) {
            var dataItem = data[i - 1];

            var rowId = 'r' + i;
            var checked = dataItem.use == true || dataItem.use == 'true';
            var weight = dataItem.weight;

            if (category == instance.categories.customData) { //custom data --------------------------------             
                var rowData = [
                    rowId,
                    "{{checkboxWidget}}",
                    dataItem.name,
                    dataItem.timeframe,
                    "{{spinnerWidget value='" + weight + "' min='1'}}"
                ]

                grid.addRow(rowData, true);

                grid.setRowDisabled(i, dataItem.disabled);

            } else { //others ------------------------------------------------------------------------------
                var parentRowId = 'group_' + dataItem.group;

                if (commonWeight == -1) {
                    commonWeight = weight;
                }

                var hasParams = false;
                var blockChanged = false;

                if (dataItem.key != "ProfitTarget.ProfitTarget" && dataItem.key != "StopLoss.StopLoss") {
                    hasParams = hasEditableParams(dataItem.params) || arrayNotEmpty(dataItem.items);
                    blockChanged = isBlockChanged(dataItem);
                }

                var parametersContent = hasParams ? createActionLink(blockChanged ? L.tsq('Custom') : L.tsq('Default'), blockChanged ? '' : "default-link", 'editParameters') : L.tsq('Default');
                var weightContent = valueFilled(dataItem.weight) ? "{{spinnerWidget value='" + dataItem.weight + "' min='1'}}" : "{{checkboxWidget}}";
                var required = dataItem.probability == 100;

                allRowsChecked = allRowsChecked && checked;
                allWeightsEqual = allWeightsEqual && weight == commonWeight;
                allRowsDefault = allRowsDefault && parametersContent.indexOf(L.tsq('Default')) >= 0;

                if (category == instance.categories.signals || category == instance.categories.indicators || category == instance.categories.stopLimitBlocks) {
                    if (!grid.rowExists(parentRowId)) {
                        grid.addRow([parentRowId, "{{checkboxWidget}}", L.tsq(dataItem.group), "{{spinnerWidget value='1' min='1'}}", createActionLink(L.tsq('Reset to default'), '', 'resetParameters')], true);
                        grid.setUserData(rowIndex, "group", true);
                        grid.setRowClass(rowIndex, "signal-group");
                        rowIndex++;
                    }
                }

                var blockTitle = (dataItem.name == "SL 2 BE Add Pips" || dataItem.name == "Trailing Activation" ? "└ " : "") + L.tsq(dataItem.name) + (dataItem.empty ? " <i>(block is empty)</i>" : "");

                var vpLocked = isVPBlockLocked(dataItem.key || '');
                if (vpLocked) {
                    blockTitle += ' <span style="float:right;">' + createActionLink('&#128274; ' + L.tsq('Volume & Market profile addon'), 'vp-addon-link', 'showVolumeProfileAddon') + '</span>';
                }

                grid.addRow([rowId, "{{checkboxWidget}}", blockTitle, weightContent, parametersContent], true);

                if (category == instance.categories.exitTypes) {
                    setExitTypeRequired(grid, dataItem, rowIndex, required, true);
                }
            }

            sqGridCheckboxSetValue(grid, rowIndex, 1, checked, true);

            grid.setUserData(rowIndex, "category", category);
            grid.setUserData(rowIndex, "key", dataItem.key);

            if(dataItem.empty){
                sqGridCheckboxSetValue(grid, rowIndex, 1, false, true);
                grid.setCellDisabled(rowIndex, 1, true, true);
            }

            if(vpLocked) {
                dataItem.use = false;
                sqGridCheckboxSetValue(grid, rowIndex, 1, false, true);
                grid.setCellDisabled(rowIndex, 1, true, true);
            }

            rowIndex++;
        }

        if (category == instance.categories.signals) {
            updateGroups(blocksGrid);
        }
        else if (category == instance.categories.indicators) {
            updateGroups(indicatorsGrid);
        }
        else if (category == instance.categories.stopLimitBlocks) {
            updateGroups(stopLimitBlocksGrid);
        }

        if (allRowsChecked) {
            sqGridCheckboxSetValue(grid, 0, 1, true, true);
        }

        if (allWeightsEqual && category != instance.categories.exitTypes && commonWeight >= 0) {
            grid.modifyRowByIndex("{{spinnerWidget value='" + commonWeight + "' min='1'}}", 0, 3, true);
        }

        if (allRowsDefault && category != instance.categories.customData) {
            grid.modifyRowByIndex("", 0, 4, true);
        }

        grid.debouncedBodyRedrawWithResort();
    }

    function compareBlocks(a, b) {
        var groupComparation = 0;
        try {
            groupComparation = a.group.localeCompare(b.group);
            if (groupComparation != 0) {
                var priority = getGroupPriority(a.group) - getGroupPriority(b.group);
                groupComparation = priority != 0 ? priority : groupComparation;
            }
        } catch (err) {
            console.error("Cannot get blocks group", a, b);
        }
        if (groupComparation != 0) return groupComparation;
        else return a.name.localeCompare(b.name);
    }

    function getGroupPriority(group) {
        switch (group) {
            case "Random Indicators Signals":
                return 1;
            case "Stop/Limit Price Levels":
                return 2;
            case "Stop/Limit Price Ranges":
                return 3;
            case "Operators":
                return 4;
            default:
                return 0;
        }
    }

    /**
     * If all group's subitems have equal weight, this weight is also set to the group.
     * The same principle applies on checked state.
     */
    function updateGroups(grid) {
        var allRowsChecked = true;
        var allWeightsEqual = true;
        var allParamsDefault = true;
        var commonWeight = null;
        var groupRowIndex = -1;

        for (var i = 1; i <= grid.getNumberOfRows(); i++) {
            if (i == grid.getNumberOfRows() || grid.getUserData(i, "group")) {
                if (groupRowIndex >= 0) {
                    if (allRowsChecked) {
                        sqGridCheckboxSetValue(grid, groupRowIndex, 1, true);
                    }

                    if (allWeightsEqual && commonWeight != null) {
                        grid.modifyRowByIndex(commonWeight, groupRowIndex, 3, true);
                    }

                    if (allParamsDefault) {
                        grid.modifyRowByIndex('', groupRowIndex, 4, true);
                    } else {
                        grid.modifyRowByIndex(createActionLink(L.tsq("Reset to default"), "parameters-cell", "resetParameters"), groupRowIndex, 4, true);
                    }
                }

                allRowsChecked = true;
                allWeightsEqual = true;
                allParamsDefault = true;
                groupRowIndex = i;
                commonWeight = null;
            } else {
                var checked = sqGridCheckboxGetValue(grid, i, 1);
                var weight = grid.getCellValue(i, 3);
                var params = grid.getCellValue(i, 4);

                if (commonWeight == null) {
                    commonWeight = weight;
                }

                allRowsChecked = allRowsChecked && checked;
                allWeightsEqual = allWeightsEqual && weight == commonWeight;
                allParamsDefault = allParamsDefault && params.indexOf(L.tsq('Default')) >= 0;
            }

        }
    }

    function tryLoadChartOptions(dataItem) {
        if (arrayNotEmpty(dataItem.params)) {
            for (var i = 0; i < dataItem.params.length; i++) {
                var param = dataItem.params[i];
                if (isChartParam(param)) {
                    param.values = BlockConfigService.getChartValues();
                    param.allCharts = valueFilled(param.allCharts) ? param.allCharts : true;
                }
            }
        }
    }

    function isBlockChanged(block) {
        if (paramsChanged(block) || block.paramSetsChanged) {
            return true;
        }

        if (arrayNotEmpty(block.items)) {
            for (var i = 0; i < block.items.length; i++) {
                var item = block.items[i];

                if ((valueFilled(item.generatedWeight) && parseInt(item.generatedWeight) != 1) ||
                    (block.items.length > 1 && valueFilled(item.use) && !item.use) ||
                    paramsChanged(item) ||
                    item.paramSetsChanged
                ) {
                    return true;
                }
            }
        }
    }

    function parameterChanged(param, defaultParam) {
        if (param.generation != defaultParam.generation) return true;

        if (isChartParam(param) && param.allCharts != defaultParam.allCharts) return true;

        if (param.generation == 'fixed') {
            if (param.fixedValue != defaultParam.fixedValue) return true;
        } else if (param.generation == 'random') {
            if (defaultParam.minValue && (param.minValue != defaultParam.minValue || param.maxValue != defaultParam.maxValue || param.step != defaultParam.step)) return true;
            if (defaultParam.values && param.values != defaultParam.values) return true;
        }
    }

    function paramsChanged(block) {
        var originalParams = block.params;
        var customParams = block.customParams;

        if (valueFilled(block.generatedWeight) && parseInt(block.generatedWeight) != 1) return true;

        if (checkIndicatorValuesRangeChanged(block)) return true;

        if (arrayNotEmpty(customParams)) {
            for (var i = 0; i < originalParams.length; i++) {
                var originalParam = originalParams[i];
                var customParam = customParams[i];

                if (!customParam) {
                    //console.error("Custom param not found", block, originalParam);
                    continue;
                }

                var isChart = isChartParam(customParam);

                if (isChart) {
                    if (customParam.allCharts) continue;
                    else {
                        return true;
                    }
                }

                if (originalParam.generation != customParam.generation ||
                    (originalParam.generation == BlockConfigService.generationTypes.random && optionsChanged(customParam)) ||
                    (BlockConfigService.getParamPossibleValues(block, originalParam) != BlockConfigService.getParamPossibleValues(block, customParam))) {
                    return true;
                }

                if (isBlockChanged(customParam)) {
                    return true;
                }
            }
        }
    }

    //Checks if indicatorMin/Max/Step was changed
    function checkIndicatorValuesRangeChanged(block) {
        if (valueFilled(block.originalIndicatorMin)) {
            if (block.indicatorMin != block.originalIndicatorMin ||
                block.indicatorMax != block.originalIndicatorMax ||
                block.indicatorStep != block.originalIndicatorStep
            ) {
                return true;
            }
        }

        return false;
    }

    function optionsChanged(customParam) {
        if (arrayNotEmpty(customParam.options)) {
            for (var i = 0; i < customParam.options.length; i++) {
                if (!customParam.options[i].checked) {
                    return true;
                }
            }
        }
    }

    function hasEditableParams(params) {
        if (arrayNotEmpty(params)) {
            for (var i = 0; i < params.length; i++) {
                var param = params[i];
                if (param.type != 'value' || param.generation != BlockConfigService.generationTypes.random) {
                    return true;
                }
            }
        }
        return false;
    }

    this.checkAllRows = function (grid, value) {
        for (var i = 1; i < grid.getNumberOfRows(); i++) {
            sqGridCheckboxSetValue(grid, i, 1, value, true);

            if (i && !grid.getUserData(i, "group")) {
                var blockObj = getBlockObjectByRowIndex(grid, i);
                if(blockObj.empty){
                    blockObj.use = false;
                    sqGridCheckboxSetValue(grid, i, 1, false, true);
                }
                else {
                    blockObj.use = value;
                }
            }
        }
        grid.debouncedBodyRedrawWithResort();
    }

    this.getBlockObjectByKey = function (category, key) {
        var targetBlocksDataArray = getBlocksDataArray(category);
        if (arrayNotEmpty(targetBlocksDataArray)) {
            for (var i = 0; i < targetBlocksDataArray.length; i++) {
                var block = targetBlocksDataArray[i];
                if (block.key == key) {
                    return block;
                }
            }
        } else {
            console.error("Cannot find block of type '" + category + "' with key '" + key + "'");
        }
    }

    function getBlockObjectByRowIndex(grid, rowIndex) {
        var category = grid.getUserData(rowIndex, "category");
        var key = grid.getUserData(rowIndex, "key");

        var targetArray = getBlocksDataArray(category, true);
        var targetObject = getItem(targetArray, 'key', key);

        if(!targetObject) return null;

        targetObject.category = category;
        return targetObject;
    }

    function getBlockRowIndex(grid, blockKey) {
        for (var i = 0; i < grid.getNumberOfRows(); i++) {
            var rowId = grid.getRowId(i);
            if (rowId == 'all') continue;

            if (grid.getUserData(i, "group")) continue;

            if (grid.getUserData(i, "key") == blockKey) {
                return i;
            }
        }
    }

    function getBlocksDataArray(type, useFilters) {
        switch (type) {
            case instance.categories.orderTypes:
                return instance.orderTypes;
            case instance.categories.exitTypes:
                return instance.exitTypes;
            case instance.categories.signals:
                return useFilters ? getFilteredItems(instance.blocks, instance.signalsFilter) : instance.blocks;
            case instance.categories.indicators:
                return useFilters ? getFilteredItems(instance.indicators, instance.indicatorsFilter) : instance.indicators;
            case instance.categories.stopLimitBlocks:
                return instance.stopLimitBlocks;
            case instance.categories.customData:
                return instance.customData;
            default:
                console.error("Unknown block type: '" + type + "'");
                return;
        }
    }

    function getFilteredItems(blocksArray, filter) {
        if (filter.key == 'None') return blocksArray;

        var filteredBlocks = [];

        for (var i = 0; i < blocksArray.length; i++) {
            var block = blocksArray[i];

            if (!block.key) continue;

            if (filter.key == 'Checked') {
                if (block.use) {
                    filteredBlocks.push(block);
                }
            }
            else {
                var key = block.key;

                if(key.indexOf("CBlock_") == 0){        //remove category from block's key
                    key = key.substr(7);
                }
                else {
                    key = key.split("\.")[block.key.indexOf(".") > 0 ? 1 : 0];  
                }
                
                if(key.indexOf("talib_") == 0){    //remove talib prefix from block's key
                    key = key.substr(6);
                    
                    if(key.indexOf("CDL") == 0){    
                        key = key.substr(3);
                    }
                }

                if (key.toLowerCase().indexOf(filter.key.toLowerCase()) == 0) {
                    filteredBlocks.push(block);
                }
            }
        }

        return filteredBlocks;
    }

    function getBlocksGrid(type) {
        switch (type) {
            case instance.categories.orderTypes:
                return orderTypesGrid;
            case instance.categories.exitTypes:
                return exitTypesGrid;
            case instance.categories.signals:
                return blocksGrid;
            case instance.categories.indicators:
                return indicatorsGrid;
            case instance.categories.stopLimitBlocks:
                return stopLimitBlocksGrid;
            case instance.categories.customData:
                return customDataGrid;
            default:
                console.error("Unknown block type: '" + type + "'");
                return;
        }
    }

    function showParams(blockObject) {
        loadParamSets(blockObject);
        setBlockDetails(instance.selectedBlock, blockObject);

        instance.selectedBlock.params = instance.selectedBlock.params || [];
        instance.selectedBlock.paramSets = instance.selectedBlock.paramSets || [];
        instance.selectedBlock.customParams = instance.selectedBlock.customParams || [];

        if (blockObject.category == "signals") {
            instance.selectedBlock.onApplyToAll = onApplyToAll;
        } else {
            instance.selectedBlock.onApplyToAll = null;
        }

        //add missing custom params (copies of original params)
        for (var i = 0; i < instance.selectedBlock.params.length; i++) {
            var originalParam = instance.selectedBlock.params[i];
            var paramFound = false;

            if (isChartParam(originalParam) && arrayEmpty(originalParam.options)) {
                originalParam.values = BlockConfigService.getChartValues();
                originalParam.options = BlockConfigService.getChartOptions();
                originalParam.isCombo = true;
            } else if (originalParam.type == 'boolean') {
                originalParam.options = [{
                    name: "true",
                    value: 'true',
                    checked: true
                },
                {
                    name: "false",
                    value: 'false',
                    checked: true
                }
                ]
            }

            for (var a = 0; a < instance.selectedBlock.customParams.length; a++) {
                var customParam = instance.selectedBlock.customParams[a];
                if (originalParam.key == customParam.key) {
                    customParam.isCombo = originalParam.isCombo || customParam.type == 'boolean';
                    paramFound = true;
                    break;
                }
            }

            if (!paramFound) {
                instance.selectedBlock.customParams.push(angular.copy(originalParam));
            }
        }

        if (!instance.selectedBlock || !instance.selectedBlock.params.length) {
            $rootScope.showError(L.tsq("Selected block has no parameters"));
        } else {
            $timeout(function () {
                SQEvents.notifyListeners(SQEvents.get("BLOCKS_PARAMS_RELOAD"), {});
            });
        }
    }

    function onApplyToAll(paramsOpen) {
        var count = 0;

        for (var i = 0; i < instance.blocks.length; i++) {
            var block = instance.blocks[i];
            if (block.group != instance.selectedBlock.group || !block.customParams) continue;

            count++;

            block.generatedWeight = instance.selectedBlock.generatedWeight

            if (paramsOpen) {
                for (var a = block.customParams.length - 1; a >= 0; a--) {
                    var param = block.customParams[a];
                    var selectedParam = getItem(instance.selectedBlock.customParams, 'key', param.key);

                    if (selectedParam) {
                        block.customParams.splice(a, 1, angular.copy(selectedParam));
                    }
                }

                block.isChanged = true;
            }
            else {
                var customizedParamSets = angular.copy(instance.selectedBlock.paramSets);

                for (var a = 0; a < customizedParamSets.length; a++) {
                    var paramSet = customizedParamSets[a];

                    //remove non-existing params from set
                    for (var p = paramSet.params.length - 1; p >= 0; p--) {
                        var setParam = paramSet.params[p];
                        if (!getItem(block.params, "key", setParam.key)) {
                            paramSet.params.splice(p, 1);
                        }
                    }

                    //add missing params
                    for (var p = 0; p < block.params.length; p++) {
                        var blockParam = block.params[p];

                        if (!getItem(paramSet.params, "key", blockParam.key)) {
                            blockParam = angular.copy(blockParam);
                            blockParam.generation = "random";

                            paramSet.params.splice(p, 0, blockParam);
                        }
                    }
                }

                block.paramSets = customizedParamSets;
                block.paramSetsObj = null;
                block.paramSetsChanged = true;

                saveParamSetsToBlock(block, block.paramSets, true);
            }

            block.category = instance.selectedBlock.category;
            updateParametersColValue(block);
        }

        $rootScope.showSuccess("Config applied to " + count + " blocks");
        onSettingsChange();

        instance.selectedBlock.isChanged = false; //disables showing close confirmation popup
        instance.selectedBlock.paramSetsChanged = false;
    }

    function onResetToDefault(selectedBlock, paramsOpen) {
        if (instance.gui.editingNormalBlock) {
            resetBlockConfig(selectedBlock, paramsOpen, !paramsOpen);
        } else {
            //reseting the special block's item
            if (paramsOpen) {
                resetParameters(selectedBlock);
            } else {
                resetParameterSets(selectedBlock);
            }
        }

        $timeout(function () {
            SQEvents.notifyListeners(SQEvents.get("BLOCKS_PARAMS_RELOAD"), {});
        }, 0, false);
    }

    function setBlockDetails(targetObj, sourceObj) {
        targetObj.category = sourceObj.category;
        targetObj.key = sourceObj.key;
        targetObj.name = sourceObj.name;
        targetObj.weight = sourceObj.weight;
        targetObj.originalWeight = sourceObj.originalWeight;
        targetObj.generatedWeight = valueFilled(sourceObj.generatedWeight) ? sourceObj.generatedWeight : 1;
        targetObj.params = angular.copy(sourceObj.params);
        targetObj.customParams = angular.copy(sourceObj.customParams);
        targetObj.paramSets = angular.copy(sourceObj.paramSets);
        targetObj.defaultParamSets = angular.copy(sourceObj.defaultParamSets);
        targetObj.paramSetsObj = sourceObj.paramSetsObj;
        targetObj.paramSetsChanged = sourceObj.paramSetsChanged;
        targetObj.group = sourceObj.group;
        targetObj.indicatorMin = sourceObj.indicatorMin;
        targetObj.indicatorMax = sourceObj.indicatorMax;
        targetObj.indicatorStep = sourceObj.indicatorStep;
        targetObj.originalIndicatorMin = sourceObj.originalIndicatorMin;
        targetObj.originalIndicatorMax = sourceObj.originalIndicatorMax;
        targetObj.originalIndicatorStep = sourceObj.originalIndicatorStep;
    }

    this.resetPossibleOptions = function () {
        var originalBlock = getOriginalBlock();
        if (arrayNotEmpty(originalBlock.items)) {
            for (var i = 0; i < originalBlock.items.length; i++) {
                var originalItem = originalBlock.items[i];
                var specialBlockItem = instance.specialBlock.items[i];
                specialBlockItem.generatedWeight = 1;
                specialBlockItem.use = true;
                specialBlockItem.customParams = angular.copy(originalItem.params);

                if (arrayNotEmpty(specialBlockItem.paramSets)) {
                    specialBlockItem.paramSets.length = 0;
                }
            }
        }

        $timeout(function () {
            SQEvents.notifyListeners(SQEvents.get("BLOCKS_PARAMS_RELOAD"), {});
        });
    }

    function resetAllBlocks(grid) {
        for (var i = 1; i < grid.getNumberOfRows(); i++) {
            if (grid.getUserData(i, "group")) {
                grid.modifyRowByIndex('', i, 4, true);
            } else {
                resetBlock(grid, i);
            }
        }
        grid.debouncedBodyRedrawWithResort();
    }

    function resetWholeSimpleRulesGroup(grid, rowIndex) {
        var categorySize = getCategorySize(grid, rowIndex);

        for (var i = rowIndex + 1; i <= rowIndex + categorySize; i++) {
            resetBlock(grid, i);
        }

        grid.modifyRowByIndex('', rowIndex, 4);
    }

    function resetBlock(grid, rowIndex) {
        var blockObj = getBlockObjectByRowIndex(grid, rowIndex);

        resetBlockConfig(blockObj, true, true);

        var hasParams = hasEditableParams(blockObj.params) || arrayNotEmpty(blockObj.items);
        var parametersContent = hasParams ? createActionLink(L.tsq('Default'), 'default-link', 'editParameters') : L.tsq('Default');

        grid.modifyRowByIndex(parametersContent, rowIndex, 4);
    }

    function resetBlockConfig(blockObj, resetParams, resetParamSets) {
        if (valueFilled(blockObj.originalIndicatorMin)) {
            blockObj.indicatorMin = blockObj.originalIndicatorMin;
            blockObj.indicatorMax = blockObj.originalIndicatorMax;
            blockObj.indicatorStep = blockObj.originalIndicatorStep;
        }

        if (resetParams) {
            resetParameters(blockObj);
        }
        if (resetParamSets) {
            resetParameterSets(blockObj);
        }

        if (arrayNotEmpty(blockObj.items)) {
            for (var i = 0; i < blockObj.items.length; i++) {
                var originalItem = blockObj.items[i];

                if (resetParams) {
                    originalItem.use = true;
                    originalItem.customParams = angular.copy(originalItem.params);
                }
                if (resetParamSets) {
                    resetParameterSets(originalItem);
                }
            }
        }

        blockObj.isChanged = false;
        blockObj.generatedWeight = 1;
    }

    function resetParameters(blockObj) {
        blockObj.customParams = angular.copy(blockObj.params);

        if (arrayNotEmpty(blockObj.customParams)) {
            for (var i = 0; i < blockObj.customParams.length; i++) {
                var customParam = blockObj.customParams[i];

                if (arrayNotEmpty(customParam.options)) {
                    for (var a = 0; a < customParam.options.length; a++) {
                        customParam.options[a].checked = true;
                    }
                }
            }
        }
    }

    /**
     * Restores default sets
     * @param {*} block 
     */
    function resetParameterSets(block) {
        block.paramSets = angular.copy(block.defaultParamSets);
        block.paramSetsObj = null;
        block.paramSetsChanged = false;
    }

    this.changeSLPT = function (args, dontSave) {
        if (args.from == "BlocksService" || instance.exitTypes.length == 0) return;

        var blockKey = args.setting == "Profit Target" ? "ProfitTarget.ProfitTarget" : "StopLoss.StopLoss";

        var block = getItem(instance.exitTypes, 'key', blockKey);
        if (!block) {
            console.error("Can't find exit type block '" + blockKey + "'");
            return;
        }

        for (var i = 0; i < block.items.length; i++) {
            var item = block.items[i];

            if (valueFilled(args.fixedValue) && item.key == "SQ.Formulas.SLPT.FixedValue") {
                item.use = args.fixedValue;
            } else if (valueFilled(args.pctValue) && item.key == "SQ.Formulas.SLPT.PctValue") {
                item.use = args.pctValue;
            } else if (valueFilled(args.atrBased) && item.key == "SQ.Formulas.SLPT.ATRBasedValue") {
                item.use = args.atrBased;
            }
            else if (valueFilled(args.indicatorBased) && item.key == "SQ.Formulas.SLPT.PriceLevel") {
                item.use = args.indicatorBased;
            }
        }

        if (exitTypesGrid) {
            var rowIndex = getBlockRowIndex(exitTypesGrid, blockKey);
            setExitTypeRequired(exitTypesGrid, block, rowIndex, args.required, true);
            exitTypesGrid.modifyRowByIndex(L.tsq("Default"), rowIndex, 4); //disables parameter editing
        }

        if (!dontSave) {
            onSettingsChange();
        }
    }

    this.savePossibleOptions = function () {
        var originalBlock = getOriginalBlock();

        originalBlock.items = angular.copy(instance.specialBlock.items);
        originalBlock.use = true;

        onSettingsChange();

        if (originalBlock.generation != 'formula') {
            updateParametersColValue(originalBlock);
        }

        hidePopup('#possibleOptionsPopup');
    }

    function getOriginalBlock() {
        if (instance.specialBlock.generation == 'formula') {
            return getItem(instance.selectedBlock.customParams, 'key', instance.specialBlock.key);
        } else {
            var blocksArray = getBlocksDataArray(instance.specialBlock.category);
            return getItem(blocksArray, 'name', instance.specialBlock.name);
        }
    }

    function onSettingsChange() {
        refreshSelectedCounts();

        instance.gui.settingsChanged = true;
        instance.saveConfig();
    }

    this.settingsChanged = onSettingsChange;

    function refreshSelectedCounts() {
        instance.selectedCounts.blocks = 0;
        for (var i = 0; i < instance.blocks.length; i++) {
            instance.selectedCounts.blocks += instance.blocks[i].use ? 1 : 0;
        }

        instance.selectedCounts.indicators = 0;
        for (var i = 0; i < instance.indicators.length; i++) {
            instance.selectedCounts.indicators += instance.indicators[i].use ? 1 : 0;
        }

        instance.selectedCounts.stopLimitBlocks = 0;
        for (var i = 0; i < instance.stopLimitBlocks.length; i++) {
            instance.selectedCounts.stopLimitBlocks += instance.stopLimitBlocks[i].use ? 1 : 0;
        }

        if (instance.forceDigest) instance.forceDigest();
    }

    this.onRandomChoice = function () {
        addRandomBlocks(100, true);
    }

    this.onAddRandomBlocks = function () {
        addRandomBlocks(10);
    }

    function addRandomBlocks(count, unselectAll) {
        if (!instance.randomButtons.enabled) return;

        instance.randomButtons.enabled = false;

        var freeIndexes = [];

        for (var i = 0; i < instance.blocks.length; i++) {
            var block = instance.blocks[i];
            if (unselectAll) {
                block.use = false;
            }
            if (!block.use) {
                freeIndexes.push(i);
            }
        }

        var freeBlocks = freeIndexes.length;

        for (var i = 0; i < instance.indicators.length; i++) {
            var block = instance.indicators[i];
            if (unselectAll) {
                block.use = false;
            }
            if (!block.use) {
                freeIndexes.push(i);
            }
        }

        if (freeIndexes.length < count) {
            $rootScope.showError(L.tsq("Cannot add random blocks - only %d blocks left unused", [freeIndexes.length]));
            return;
        }

        for (var i = 0; i < count; i++) {
            var randomNumber = Math.floor(Math.random() * freeIndexes.length);
            if (randomNumber > freeBlocks) {
                instance.indicators[freeIndexes[randomNumber]].use = true;
            } else {
                instance.blocks[freeIndexes[randomNumber]].use = true;
                freeBlocks--;
            }
            freeIndexes.splice(randomNumber, 1);
        }

        refreshGrid(blocksGrid);
        refreshGrid(indicatorsGrid);

        onSettingsChange();

        instance.randomButtons.enabled = true;
    }

    this.saveBlockConfig = function (modifiedBlockObj) {
        if (arrayNotEmpty(modifiedBlockObj.customParams) || arrayNotEmpty(modifiedBlockObj.items) || arrayNotEmpty(modifiedBlockObj.paramSets)) {
            var blocksArray = getBlocksDataArray(modifiedBlockObj.category);
            var originalBlock = getItem(blocksArray, 'key', modifiedBlockObj.key);

            //update original block
            originalBlock.customParams = angular.copy(modifiedBlockObj.customParams);
            originalBlock.paramSets = angular.copy(modifiedBlockObj.paramSets);
            originalBlock.paramSetsChanged = modifiedBlockObj.paramSetsChanged;
            originalBlock.items = angular.copy(modifiedBlockObj.items);
            originalBlock.generatedWeight = modifiedBlockObj.generatedWeight;
            originalBlock.probability = modifiedBlockObj.probability;
            originalBlock.use = !originalBlock.empty && (modifiedBlockObj.isChanged || originalBlock.use);
            originalBlock.indicatorMin = modifiedBlockObj.indicatorMin;
            originalBlock.indicatorMax = modifiedBlockObj.indicatorMax;
            originalBlock.indicatorStep = modifiedBlockObj.indicatorStep;
            originalBlock.isChanged = modifiedBlockObj.isChanged;
            originalBlock.generatedWeight = modifiedBlockObj.generatedWeight;

            //check if indicatorMin/Max/Step was changed
            originalBlock.isChanged = originalBlock.isChanged || checkIndicatorValuesRangeChanged(originalBlock);

            saveParamSetsToBlock(originalBlock, originalBlock.paramSets, true);

            //console.error("saveBlockConfig", angular.copy(originalBlock), modifiedBlockObj);
        }

        modifiedBlockObj.paramSetsChanged = paramSetsChanged(modifiedBlockObj, modifiedBlockObj.paramSets);

        onSettingsChange();

        updateParametersColValue(modifiedBlockObj, originalBlock.isChanged, false, originalBlock.empty);

        hidePopup("#blockParametersPopup");
    }

    function updateParametersColValue(block, isChanged, ignoreErrors, dontSelect) {
        var grid = getBlocksGrid(block.category);
        var rowIndex = getBlockRowIndex(grid, block.key);
        if (!rowIndex) {
            if (!ignoreErrors) {
                $rootScope.showError(L.tsq("Saving settings failed. Block '%s' not found in the grid.", [block.name]));
            }
            return;
        }

        var hasParams = hasEditableParams(block.params) || arrayNotEmpty(block.items);
        var blockChanged = isChanged || isBlockChanged(block);
        var parametersContent = hasParams ? createActionLink(blockChanged ? L.tsq('Custom') : L.tsq('Default'), blockChanged ? '' : "default-link", 'editParameters') : L.tsq('Default');
        grid.modifyRowByIndex(parametersContent, rowIndex, 4, true);

        if (blockChanged && !dontSelect) {
            sqGridCheckboxSetValue(grid, rowIndex, 1, true);
        }

        if (grid === blocksGrid || grid == indicatorsGrid || grid == stopLimitBlocksGrid) {
            updateGroups(grid);
        }

        updateGlobalParamsValue(grid);
        grid.debouncedBodyRedrawWithResort();
    }

    function updateGlobalParamsValue(grid) {
        for (var i = 1; i < grid.getNumberOfRows(); i++) {
            if (grid.getUserData(i, "group")) continue;

            var customSettings = grid.getCellValue(i, 4).indexOf('Custom') >= 0;
            if (customSettings) {
                grid.modifyRowByIndex(createActionLink(L.tsq("Reset to default"), "parameters-cell", "resetParameters"), 0, 4, true);
                return;
            }
        }

        grid.modifyRowByIndex('', 0, 4, true);
        grid.debouncedBodyRedrawWithResort();
    }

    function saveParamSetsToBlock(block, paramSets, forceSaving) {
        if (!paramSets || (!forceSaving && !paramSetsChanged(block, paramSets))) return false;

        var xmlDoc = AppService.xmlDoc;
        var predefinedElem = xmlDoc.createElement('Predefined');

        predefinedElem.setAttribute("changed", !!block.paramSetsChanged);

        for (var a = 0; a < paramSets.length; a++) {
            var set = paramSets[a];
            var setElem = createChild(predefinedElem, 'Params', xmlDoc, true);
            setElem.setAttribute('name', set.name);
            setElem.setAttribute('weight', set.weight);

            for (var p = 0; p < set.params.length; p++) {
                var param = set.params[p];
                var paramsElem = createChild(setElem, 'Param', xmlDoc, true);
                saveParamXML(paramsElem, param);
            }
        }

        block.paramSetsObj = predefinedElem;

        return true;
    }

    function paramSetsChanged(block, customSets) {
        var defaultSets = block.defaultParamSets || [];
        customSets = customSets || [];

        if (customSets.length == defaultSets.length) {
            for (var i = 0; i < customSets.length; i++) {
                var paramSet = customSets[i];
                var defaultParamSet = getItem(defaultSets, "name", paramSet.name);

                if (!defaultParamSet || paramSetParamsChanged(paramSet, defaultParamSet)) {
                    return true;
                }
            }
        } else {
            return true;
        }
    }

    function paramSetParamsChanged(paramSet, defaultParamSet) {
        if (paramSet.params.length != defaultParamSet.params.length) return false;

        for (var i = 0; i < paramSet.params.length; i++) {
            var param = paramSet.params[i];
            var defaultParam = defaultParamSet.params[i];

            if (parameterChanged(param, defaultParam)) {
                return true;
            }
        }
    }

    this.saveConfig = function () {
        var xmlDoc = AppService.xmlDoc;

        var settingsElem = AppService.getTaskConfig();
        if (!settingsElem) return;

        var blocksElem = createChild(settingsElem, 'Blocks', xmlDoc);
        blocksElem.setAttribute("type", instance.mode);

        removeAllChildren(blocksElem);

        var calibrationElem = createChild(blocksElem, 'Calibration', xmlDoc);
        calibrationElem.setAttribute("useMaxSteps", instance.gui.useCalibrationMaxSteps);
        calibrationElem.setAttribute("maxSteps", instance.gui.calibrationMaxSteps);
        calibrationElem.setAttribute("calibrateBeforeStart", instance.gui.isStockPicker ? false : instance.gui.autoCalibrateBeforeStart);

        addAllModifiedBlocks(instance.categories.signals, 'BuildingBlocks', blocksElem, xmlDoc);
        addAllModifiedBlocks(instance.categories.indicators, 'BuildingBlocks', blocksElem, xmlDoc);
        addAllModifiedBlocks(instance.categories.stopLimitBlocks, 'BuildingBlocks', blocksElem, xmlDoc);
        addAllModifiedBlocks(instance.categories.orderTypes, 'OrderTypes', blocksElem, xmlDoc);
        addAllModifiedBlocks(instance.categories.exitTypes, 'ExitTypes', blocksElem, xmlDoc);

        saveCustomData(blocksElem, xmlDoc)
    }

    function saveCustomData(blocksElem, xmlDoc) {
        var mainElem = getChildElement(blocksElem, 'CustomData', true);
        if (!mainElem) {
            mainElem = createChild(blocksElem, 'CustomData', xmlDoc, true);
        }

        mainElem.setAttribute("showAll", instance.gui.showAllCDataIndy);

        var customData = getBlocksDataArray(instance.categories.customData);

        for (var i = 0; i < customData.length; i++) {
            var data = customData[i];

            if(data.disabled) continue;

            var dataElem = createChild(mainElem, 'Data', xmlDoc, true);
            dataElem.setAttribute("key", data.key);
            dataElem.setAttribute("name", data.name);
            dataElem.setAttribute("use", data.use);
            dataElem.setAttribute("weight", data.weight);
        }
    }

    function addAllModifiedBlocks(category, childElemName, parentElem, xmlDoc) {
        var mainElem = getChildElement(parentElem, childElemName, true);
        if (!mainElem) {
            mainElem = createChild(parentElem, childElemName, xmlDoc, true);
        }

        var blocksArray = getBlocksDataArray(category);

        for (var i = 0; i < blocksArray.length; i++) {
            var block = blocksArray[i];
            block.isChanged = false;

            var blockElem = createChild(mainElem, 'Block', xmlDoc, true);
            blockElem.setAttribute("key", block.key);
            if (valueFilled(block.weight)) {
                blockElem.setAttribute("weight", block.weight);
            }

            if (block.required) {
                blockElem.setAttribute("use", true);
                blockElem.setAttribute("required", true);

                if (valueFilled(block.probability)) {
                    blockElem.setAttribute("probability", 100);
                }
            } else {
                blockElem.setAttribute("use", valueFilled(block.use) ? block.use : false);
                blockElem.removeAttribute("required");

                if (valueFilled(block.probability)) {
                    blockElem.setAttribute("probability", block.probability);
                }
            }

            if (block.indicatorMin != null) {
                blockElem.setAttribute("indicatorMin", block.indicatorMin);
                blockElem.setAttribute("indicatorMax", block.indicatorMax);
                blockElem.setAttribute("indicatorStep", block.indicatorStep);
            }

            blockElem.setAttribute("category", category);
            if (valueFilled(block.type)) {
                blockElem.setAttribute("type", block.type);
            }

            var generatedElem = createChild(blockElem, 'Generated', xmlDoc, true);
            generatedElem.setAttribute("weight", valueFilled(block.generatedWeight) ? block.generatedWeight : 1);

            if (arrayNotEmpty(block.customParams) || arrayNotEmpty(block.params)) {
                block.customParams = arrayNotEmpty(block.customParams) ? block.customParams : angular.copy(block.params);
                var formulasElem = null;

                for (var a = 0; a < block.customParams.length; a++) {
                    var param = block.customParams[a];
                    if (param.generation == "formula") {
                        formulasElem = formulasElem || createChild(blockElem, 'Formulas', xmlDoc, true);
                        var formulaElem = createChild(formulasElem, 'Formula', xmlDoc, true);
                        formulaElem.setAttribute("key", param.key);
                        formulaElem.setAttribute("probability", "100");
                        if (param.exitMethod) formulaElem.setAttribute("exitMethod", param.exitMethod);

                        if (arrayNotEmpty(param.items)) {
                            saveFormulaXML(formulaElem, param, xmlDoc);
                        }
                    }

                    var paramElem = createChild(generatedElem, 'Param', xmlDoc, true);
                    saveParamXML(paramElem, param);
                }
            }

            saveParamSetsXML(blockElem, block);

            if (arrayNotEmpty(block.items)) {
                saveFormulaXML(blockElem, block, xmlDoc);
            }
        }
    }

    function saveFormulaXML(parentElem, paramObj, xmlDoc) {
        for (var i = 0; i < paramObj.items.length; i++) {
            var item = paramObj.items[i];
            var valueElem = createChild(parentElem, 'Value', xmlDoc, true);
            valueElem.setAttribute("key", item.key);
            valueElem.setAttribute("use", item.use);

            if (arrayNotEmpty(item.customParams)) {
                var generatedElem = createChild(valueElem, 'Generated', xmlDoc, true);
                generatedElem.setAttribute("weight", item.generatedWeight);

                for (var a = 0; a < item.customParams.length; a++) {
                    var param = item.customParams[a];
                    var paramElem = createChild(generatedElem, 'Param', xmlDoc, true);
                    saveParamXML(paramElem, param);
                }
            }

            saveParamSetsXML(valueElem, item);
        }
    }

    function saveParamXML(paramElem, paramObj) {
        paramElem.setAttribute('key', paramObj.key);
        paramElem.setAttribute('name', paramObj.name);
        paramElem.setAttribute('type', paramObj.type);
        paramElem.setAttribute('paramType', paramObj.paramType);
        paramElem.setAttribute('generation', paramObj.generation);

        if (paramObj.generation == 'formula') return;
        else if (paramObj.generation == BlockConfigService.generationTypes.fixed) {
            paramElem.setAttribute('defaultValue', valueFilled(paramObj.fixedValue) ? paramObj.fixedValue : paramObj.defaultValue);
        } else if (arrayNotEmpty(paramObj.options) || paramObj.values) {
            paramElem.setAttribute('values', arrayNotEmpty(paramObj.options) ? BlockConfigService.getValuesFromOptions(paramObj.options) : paramObj.values);
        } else {
            paramElem.setAttribute('minValue', paramObj.minValue);
            paramElem.setAttribute('maxValue', paramObj.maxValue);
            paramElem.setAttribute('step', paramObj.step);
        }

        if (paramObj.allCharts) {
            paramElem.setAttribute('allCharts', 'true');
        }
    }

    function saveParamSetsXML(parentElem, block) {
        removeChild(parentElem, 'Predefined');

        if (!block.paramSetsObj) {
            loadParamSets(block, true);
        }

        if (block.paramSetsObj) {
            parentElem.appendChild(angular.copy(block.paramSetsObj));
        }
    }

    this.correctAllBlocksChartSettings = function () {
        var needsSaving = false;
        needsSaving = correctChartSettings(instance.orderTypes) ? true : needsSaving;
        needsSaving = correctChartSettings(instance.exitTypes) ? true : needsSaving;
        needsSaving = correctChartSettings(instance.blocks) ? true : needsSaving;
        needsSaving = correctChartSettings(instance.indicators) ? true : needsSaving;
        needsSaving = correctChartSettings(instance.stopLimitBlocks) ? true : needsSaving;

        if (needsSaving) console.log("Some of the blocks' chart settings were corrected");

        return needsSaving;
    }

    function correctChartSettings(blocks) {
        var needsSaving = false;

        for (var i = 0; i < blocks.length; i++) {
            var block = blocks[i];

            if (arrayNotEmpty(block.customParams)) {
                for (var a = 0; a < block.customParams.length; a++) {
                    var customParam = block.customParams[a];
                    var oldValues = customParam.values;

                    if (isChartParam(customParam)) {
                        var allChartOptions = BlockConfigService.getChartOptions();

                        if (customParam.generation == instance.generationTypes.random) {
                            if (valueFilled(customParam.values)) {
                                var paramOptions = BlockConfigService.getOptionsFromValues(customParam.values);
                                customParam.options = BlockConfigService.getCorrectedOptions(paramOptions, allChartOptions);
                                customParam.values = BlockConfigService.getValuesFromOptions(customParam.options);

                                //no selected charts available
                                if (customParam.values == "") {
                                    customParam.allCharts = true;
                                }

                                if (customParam.values != oldValues) needsSaving = true;
                            } else {
                                customParam.allCharts = true;
                            }
                        } else {
                            if (customParam.fixedValue > allChartOptions.length - 1) {
                                customParam.fixedValue = "" + (allChartOptions.length - 1);
                                customParam.defaultValue = "" + (allChartOptions.length - 1);
                            }
                        }
                        break;
                    }
                }
            }

            if (arrayNotEmpty(block.items)) {
                if (correctChartSettings(block.items)) {
                    needsSaving = true;
                }
            }
        }

        return needsSaving;
    }

    this.applyFilterToGrid = function (category, key) {
        if (category == instance.categories.signals) {
            instance.signalsFilter.key = key;
        }
        else {
            instance.indicatorsFilter.key = key;
        }

        loadBlocksToGrid(category);
    }

    this.calibrateBlocks = function (calibrationResults, dontSendXMLUpdate) {
        //console.error("calibrateBlocks", calibrationResults, instance.indicators);
        for (var i = 0; i < calibrationResults.length; i++) {
            var calibrationResult = calibrationResults[i];
            var mainRange = calibrationResult.ranges[0];

            //update indicator blocks
            for (var a = 0; a < instance.indicators.length; a++) {
                var indicatorObj = instance.indicators[a];

                if (indicatorObj.key == ("Indicators." + calibrationResult.key) || indicatorObj.key == ("Stop/Limit Price Ranges." + calibrationResult.key)) {
                    indicatorObj.indicatorMin = mainRange.minValue;
                    indicatorObj.indicatorMax = mainRange.maxValue;
                    indicatorObj.indicatorStep = mainRange.step;

                    indicatorObj.isChanged = true;
                    indicatorObj.category = instance.categories.indicators;
                    //console.error(indicatorObj);
                    updateParametersColValue(indicatorObj, false, true, true);
                    break;
                }
            }

            //update stop/limit blocks
            for (var a = 0; a < instance.stopLimitBlocks.length; a++) {
                var blockObj = instance.stopLimitBlocks[a];

                if (blockObj.key == ("Stop/Limit Price Ranges." + calibrationResult.key)) {
                    blockObj.indicatorMin = mainRange.minValue;
                    blockObj.indicatorMax = mainRange.maxValue;
                    blockObj.indicatorStep = mainRange.step;

                    blockObj.isChanged = true;
                    blockObj.category = instance.categories.stopLimitBlocks;
                    //console.error(blockObj);
                    updateParametersColValue(blockObj, false, true, true);
                    break;
                }
            }

            //update signal blocks
            for (var a = 0; a < instance.blocks.length; a++) {
                var blockObj = instance.blocks[a];

                if (blockObj.groupKey != calibrationResult.key || !blockObj.customParams.length) continue;

                for (var p = 0; p < blockObj.customParams.length; p++) {
                    var customParam = blockObj.customParams[p];
                    if (customParam.key == "#Level#") {
                        customParam.minValue = mainRange.minValue;
                        customParam.maxValue = mainRange.maxValue;
                        customParam.step = mainRange.step;

                        blockObj.isChanged = true;
                        blockObj.category = instance.categories.signals;
                        //console.error(blockObj);
                        updateParameterSetsWithCalibrationResult(blockObj, calibrationResult);
                        updateParametersColValue(blockObj, false, true, true);
                        break;
                    }
                }
            }
        }

        instance.saveConfig();
        
        if(!dontSendXMLUpdate){
            AppService.updateTaskXML();
        }
    }

    function updateParameterSetsWithCalibrationResult(blockObj, calibrationResult){
        loadParamSets(blockObj);

        var mainRange = calibrationResult.ranges[0];
        var somethingChanged = false;

        if(blockObj.paramSets){
            for(var i=0; i<blockObj.paramSets.length; i++){
                var paramSet = blockObj.paramSets[i];
                if(!paramSet || !paramSet.params) continue;

                for(var a=0; a<paramSet.params.length; a++){
                    var setParam = paramSet.params[a];

                    if(setParam.generation == "random" && setParam.key == "#Level#"){
                        setParam.minValue = mainRange.minValue;
                        setParam.maxValue = mainRange.maxValue;
                        setParam.step = mainRange.step;

                        somethingChanged = true;
                    }
                }
            }
        }

        if(somethingChanged){
            blockObj.paramSetsObj = null;
            blockObj.paramSetsChanged = true;

            saveParamSetsToBlock(blockObj, blockObj.paramSets, true);
        }
    }

    this.filterBlocksByEngine = function(engine, dontSaveChanges){
        if(!buildingBlocks.loaded) return;

        if(!engine){
            var dataElem = AppService.getCurrentTaskTabSettings("Data");
            if (dataElem) {
                var setupsElem = getChildElement(dataElem, 'Setups', true);
                if (setupsElem) {
                    var setupElems = getChildElements(setupsElem, 'Setup');
                    if (setupElems.length > 0) {
                        engine = getAttrValue(setupElems[0], 'engine');
                    }
                }
            }
        }
        
        var engineKey = SQConstants.getEngineKey(engine);        
        let isStockPicker = (engine === "Stockpicker" || engine === "Single-asset cloud strategy");
        this.gui.isStockPicker = isStockPicker;

        if(isStockPicker && this.gui.shownBlocks ==='signals'){
            this.gui.shownBlocks = 'indicators';
        }

        if(isStockPicker === this.gui.lastEngineWasStockpicker) return;
        
        //console.log("Filtering blocks by engine", engine, buildingBlocks);

        this.gui.lastEngineWasStockpicker = isStockPicker;

        this.blocks.length = 0;

        for(let i=0; i<buildingBlocks.blocks.length; i++){
            let block = buildingBlocks.blocks[i];

            let isCBlock = block.key && block.key.indexOf("CBlock_") === 0;
            if(isCBlock) {
                if((block.strategyType || "Standard") !== (isStockPicker ? "AWCloud" : "Standard")) continue;
            } else {
                if((isStockPicker && ((block.buildingBlockType !== 'simpleRule' || block.key.indexOf("talib_") < 0)) || (!isStockPicker && block.key.indexOf("talib_") >= 0))) continue;
                if(!checkForEngine(engineKey, block.forEngine)) continue;
            }

            this.blocks.push(block);
        }
        
        this.indicators.length = 0;

        for(let i=0; i<buildingBlocks.indicators.length; i++) {
            let indicator = buildingBlocks.indicators[i];

            if((isStockPicker && (indicator.customSnippet || indicator.buildingBlockType === 'simpleRule' || indicator.buildingBlockType === 'indicator') && indicator.key.indexOf("talib_") < 0) ||
                (!isStockPicker && indicator.key.indexOf("talib_") >= 0)
            ) continue;

            if(!checkForEngine(engineKey, indicator.forEngine)) continue;

            this.indicators.push(indicator);
        }

        this.stopLimitBlocks.length = 0;

        for(let i=0; i<buildingBlocks.stopLimitBlocks.length; i++){
            let block = buildingBlocks.stopLimitBlocks[i];
            let isCBlock = block.groupKey === "Custom price levels";

            if(isCBlock) {
                if((block.strategyType || "Standard") !== (isStockPicker ? "AWCloud" : "Standard")) continue;
            } else {
                if(!checkForEngine(engineKey, block.forEngine)) continue;
                if(isStockPicker && block.groupKey === "Indicators" && block.key.indexOf("talib_") < 0) continue;
                if(!isStockPicker && block.key.indexOf("talib_") >= 0) continue;
            }

            this.stopLimitBlocks.push(block);
        }

        this.orderTypes.length = 0;

        for(let i=0; i<buildingBlocks.orderTypes.length; i++){
            let block = buildingBlocks.orderTypes[i];
            if(!checkForEngine(engineKey, block.forEngine)) continue;

            this.orderTypes.push(block);
        }
        
        this.exitTypes.length = 0;
        
        for(let i=0; i<buildingBlocks.exitTypes.length; i++){
            let block = buildingBlocks.exitTypes[i];

            if(!isStockPicker && block.key === eodRuleKey) continue;

            if(!checkForEngine(engineKey, block.forEngine)) continue;
            
            this.exitTypes.push(block);
        }
        
        if(!dontSaveChanges){
            this.saveConfig();
        }

        refreshGrids();
        refreshSelectedCounts();
    }

    function applyDefaultStockpickerBlocks() {
        console.log("Aplying default stockpicker blocks");

        let blockSettings = InitializationData().settings.Blocks.stockpickerDefaultBlocks;
        applyBlockSettings(blockSettings);
    }

    var orderTypesGrid, exitTypesGrid, blocksGrid, indicatorsGrid, stopLimitBlocksGrid, customDataGrid;

    var buildingBlocks = {      //building blocks loaded from backend and processed
        orderTypes: [],
        exitTypes: [],
        blocks: [],
        indicators: [],
        stopLimitBlocks: [],
        customData: [],
        parameterSets: [],
        loaded: false
    };

    this.internalBlocks = buildingBlocks;

    this.orderTypes = [];
    this.exitTypes = [];
    this.blocks = [];
    this.indicators = [];
    this.stopLimitBlocks = [];
    this.customData = [];
    this.parameterSets = [];

    this.orderTypesPromise = null;
    this.exitTypesPromise = null;
    this.indicatorsPromise = null;
    this.priceRangesPromise = null;
    this.priceValuesPromise = null;
    this.operatorsPromise = null;
    this.simpleRulesPromise = null;
    this.othersPromise = null;

    this.categories = BlockConfigService.categories;

    this.modes = {
        simple: 'simple',
        advanced: 'advanced'
    }

    this.mode = this.modes.simple;
    this.generationTypes = BlockConfigService.generationTypes;

    this.config = {};

    this.gui = {
        initialized: false,
        editingNormalBlock: true,
        settingsChanged: false,
        shownBlocks: "signals",
        useCalibrationMaxSteps: true,
        maxCalibrationSteps: 50,
        autoCalibrateBeforeStart: false,
        showAllCDataIndy: false,
        isStockPicker: false
    }

    this.selectedBlock = {
        onResetToDefault: onResetToDefault
    };
    this.specialBlock = {};
    this.selectedParam = BlockConfigService.selectedParam;
    this.paramConfig = {};

    this.randomButtons = {
        enabled: true
    };

    this.signalsFilter = { key: 'None' };
    this.indicatorsFilter = { key: 'None' };

    this.selectedCounts = {
        blocks: 0,
        indicators: 0,
        stopLimitBlocks: 0
    };

    this.forceDigest = null;

    this.customDataTimeframes = {
        list: null
    }

    var exitRuleKey = "_ExitRule_";
    var eodRuleKey = "_ExitEOD_";
    var dataPromise;

    var formulas = [];

    var lastEngineWasStockpicker = null;

});