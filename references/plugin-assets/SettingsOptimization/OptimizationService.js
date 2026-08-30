angular.module('app.settings').service('OptimizationService', function ($rootScope, $q, BackendService, AppService, SQConstants, SQEvents, DatabankService, SQWebSocketService, L) {

    this.loadConfig = function (callback, forceReload) {
        if (!loaded || forceReload) {
            instance.loadSettings(callback);
            loaded = true;
        } else if (callback) {
            callback();
        }
    }

    this.loadSettings = function (callback) {
        var settingsObj = AppService.getTaskConfig();
        var optimizationObj = getChildElement(settingsObj, "Optimization");

        instance.config.optimization = getAttrIntValue(optimizationObj, "type", instance.constants.optimizations.simple);
        instance.config.maxOptimizations = getAttrIntValue(optimizationObj, 'maxOptimizations', instance.config.maxOptimizations);

        var sourceObj = getChildElement(optimizationObj, "Source");
        instance.config.source = getAttrIntValue(sourceObj, "type", instance.constants.sources.file);
        instance.config.filePath = getNodeValue(optimizationObj, "Source", "");

        instance.config.sourceDatabank = DatabankService.getTaskInputDatabank();
        instance.config.outputDatabank = DatabankService.getTaskOutputDatabank();

        //simple optimization -------------------------------------------------------------------------------
        if (instance.config.optimization == instance.constants.optimizations.simple || instance.config.optimization == instance.constants.optimizations.sequential) {
            var soObj = getChildElement(optimizationObj, "SimpleOptimization");
            instance.config.simpleType = getAttrIntValue(soObj, "type", instance.constants.simpleOptimizations.storeBest);
            instance.config.pctToPass = getAttrIntValue(soObj, "pctToPass", 80);
            instance.config.resultsCount = getAttrIntValue(soObj, "resultsCount", 5);
            instance.config.stabilityRange = getAttrIntValue(soObj, "stabilityRange", 20);
        } 
        else {
            var wfObj = getChildElement(optimizationObj, "WalkForward");
            if (wfObj) {
                instance.config.wfType = getAttrStringValue(wfObj, "type", '' + instance.constants.wfType.WF_TYPE_SIMIS_SIMOOS);
                instance.config.periodType = getAttrIntValue(wfObj, "period", instance.constants.periodTypes.percent);
                instance.config.optimizationType = getAttrStringValue(wfObj, "optimization", instance.constants.optimizationTypes.floating);
            }

            var param1Obj = getChildElement(wfObj, "Param1");
            var param2Obj = getChildElement(wfObj, "Param2");

            //walk-forward optimization ------------------------------------------------------------------------
            if (instance.config.optimization == instance.constants.optimizations.walk_fo) {
                instance.config.param1 = getAttrFloatValue(param1Obj, "value", instance.config.param1);
                instance.config.param2 = getAttrFloatValue(param2Obj, "value", instance.config.param2);
            }
            //walk-forward matrix ----------------------------------------------------------------------
            else if (instance.config.optimization == instance.constants.optimizations.walk_fm) {
                instance.config.param1Start = getAttrFloatValue(param1Obj, "start", instance.config.param1Start);
                instance.config.param1Stop = getAttrFloatValue(param1Obj, "stop", instance.config.param1Stop);
                instance.config.param1Step = getAttrFloatValue(param1Obj, "step", instance.config.param1Step);

                instance.config.param2Start = getAttrFloatValue(param2Obj, "start", instance.config.param2Start);
                instance.config.param2Stop = getAttrFloatValue(param2Obj, "stop", instance.config.param2Stop);
                instance.config.param2Step = getAttrFloatValue(param2Obj, "step", instance.config.param2Step);
            }
        }

        var optimizationMethodObj = getChildElement(optimizationObj, "OptimizationMethod");
        if (optimizationMethodObj) {
            instance.config.mode = getAttrStringValue(optimizationMethodObj, 'settings', instance.config.mode);
            instance.config.symmetricVariables = getAttrBooleanValue(optimizationMethodObj, 'symmetricVariables', instance.config.symmetricVariables);
            instance.gui.symmetryDisabled = getAttrBooleanValue(optimizationMethodObj, "symmetryDisabled", instance.gui.symmetryDisabled);

            if (instance.config.source == SQConstants.getConstants().optimization.sources.databank) {
                instance.config.mode = instance.modes.automatic;
                instance.gui.automaticOnly = true;
            } else {
                var filePath = getNodeValue(optimizationMethodObj, "Source", "");
                instance.gui.automaticOnly = valueFilled(filePath);

                let elSource = getChildElement(optimizationMethodObj, "Source");
                instance.config.relativePath = getAttrBooleanValue(elSource, 'relativePath', instance.config.relativePath);
            }
        }

        var autoSettingsObj = getChildElement(optimizationObj, "AutomaticSettings", true);
        if (autoSettingsObj) {
            instance.config.distributionUp = getAttrIntValue(autoSettingsObj, "distributionUp", instance.config.distributionUp);
            instance.config.distributionDown = getAttrIntValue(autoSettingsObj, "distributionDown", instance.config.distributionDown);
            instance.config.maxSteps = getAttrIntValue(autoSettingsObj, 'maxSteps', instance.config.maxSteps);
        }

        var whatToParametrizeObj = getChildElement(optimizationObj, "WhatToParametrize", true);
        if (whatToParametrizeObj) {
            instance.config.parametrizeType = getNodeBooleanValue(whatToParametrizeObj, 'Recommended', true) ? 0 : 1;
            instance.config.periodParams = getNodeBooleanValue(whatToParametrizeObj, 'Periods', instance.config.periodParams);
            instance.config.shiftParams = getNodeBooleanValue(whatToParametrizeObj, 'Shifts', instance.config.shiftParams);
            instance.config.constantsParams = getNodeBooleanValue(whatToParametrizeObj, 'Constants', instance.config.constantsParams);
            instance.config.otherParams = getNodeBooleanValue(whatToParametrizeObj, 'OtherParams', instance.config.otherParams);
            instance.config.entryParams = getNodeBooleanValue(whatToParametrizeObj, 'EntryParams', instance.config.entryParams);
            instance.config.entryLogic = getNodeBooleanValue(whatToParametrizeObj, 'EntryLogic', instance.config.entryLogic);
            instance.config.exitParamsUsed = getNodeBooleanValue(whatToParametrizeObj, 'ExitParamsUsed', instance.config.exitParamsUsed);
            instance.config.exitParamsUnused = getNodeBooleanValue(whatToParametrizeObj, 'ExitParamsUnused', instance.config.exitParamsUnused);
            instance.config.booleanParams = getNodeBooleanValue(whatToParametrizeObj, 'BooleanParams', instance.config.booleanParams);

            instance.config.tradingOptionsParams = getNodeBooleanValue(whatToParametrizeObj, 'TradingOptions', instance.config.tradingOptionsParams);   
        }

        instance.checkFileExists(optimizationObj, callback);
        checkFileExists();
    }

    this.checkFileExists = function (optimizationObj, callback) {
        if (!optimizationObj) {
            var settingsObj = AppService.getTaskConfig();
            optimizationObj = getChildElement(settingsObj, "Optimization");
        }

        if (instance.config.source == instance.constants.sources.file) {
            fileExistsRequestDeferred.promise.then(function () {
                if (optimizationObj && instance.config.filePath == '') {
                    setSource(optimizationObj);
                }
                if (callback) {
                    callback(instance.config.filePath);
                }
            });
        } else if (callback) {
            callback(null);
        }
    }

    function checkFileExists() {
        BackendService.getPromise("main/checkFileExists", {
            filePath: instance.config.filePath
        }).then(function (result) {
            if (result.error || !result.data.exists) {
                instance.config.filePath = '';
            }
            fileExistsRequestDeferred.resolve();
        });
    }

    this.onSelectFile = function (callback, simpleSettingsRequest) {
        instance.simpleSettingsRequest = simpleSettingsRequest;

        instance.config.source = instance.constants.sources.file;

        $rootScope.showFilePicker(L.tsq('Select file to load'), "loadResultToOptimize", true, false, null, fileExtension, null, function (paths) {
            if (paths) {
                let path = paths[0].replace(/[\\]/g, "/");
                let appPath = (SQConstants.getSettings()["appPath"] || "").replace(/[\\]/g, "/");

                if(appPath && path.indexOf(appPath) === 0){     //it is a relative path
                    instance.config.relativePath = true;
                    path = path.substr(appPath.length);
                }
                else {
                    instance.config.relativePath = false;
                }

                instance.config.filePath = path;

                instance.onLoadStrategy(instance.config.filePath, callback);
            } else {
                instance.simpleSettingsRequest = false;
            }
        });
    }

    this.onLoadStrategy = function (filePath, callback) {
        DatabankService.databankList(function () {
            if (DatabankService.databanks[0].records > 0) {
                $rootScope.showConfirm(
                    L.tsq("Loading strategy file"),
                    L.tsq("You are going to load a new strategy to Optimizer.<br>Do you want to delete all existing results in the Optimizer databank?"),
                    function (confirmed) {
                        loadStrategy(filePath, confirmed, callback);
                    },
                    true
                );
            } else {
                loadStrategy(filePath, true, callback);
            }
        });
    }

    function loadStrategy(filePath, clearResults, callback) {
        instance.loadStrategy(filePath, clearResults).then(function (result) {
            if (result.data.success) {
                $rootScope.showSuccess(result.data.success);
            } else {
                $rootScope.showError(rsesult.data.error);
            }

            var data = {
                filePath: filePath
            };

            if (result.data.lastSettingsXml) {
                data.lastSettingsXml = xmlToObject(result.data.lastSettingsXml).find('Settings')[0];
            }

            SQEvents.notifyListeners(SQEvents.get("OPTIMIZATION_FILE_CHANGED"), data);

            if (callback) callback();
            instance.simpleSettingsRequest = false;
        });
    }

    this.loadStrategy = function (filePath, clearResults) {
        var args = {
            projectName: AppService.getProject(),
            taskName: AppService.getTask().name,
            filePath: filePath,
            clearResults: clearResults,
            relativePath: instance.config.relativePath
        }

        return BackendService.getPromise('optimization/loadStrategyToOptimize', args);
    }

    this.checkOptimizationSource = function () {
        var defaultDatabank = databankTypes.strategiesToOptimize;
        var showDatabank = false;

        var curTaskName = AppService.getTask().name;

        var taskElems = getTasks(AppService.getProjectConfig());
        for(var i=0; i<taskElems.length; i++){
            var taskElem = taskElems[i];
            var taskName = getAttrStringValue(taskElem, "name");

            var settingsObj = AppService.getTaskConfig();
            var optimizationObj = getChildElement(settingsObj, "Optimization");
            if (!optimizationObj) continue;

            var sourceObj = getChildElement(optimizationObj, "Source");
            var source = getAttrStringValue(sourceObj, "type", instance.constants.sources.file);

            if(source == instance.constants.sources.databank) {
                if(instance.isOptimizer){
                    showDatabank = true;
                    break;
                }
                else {      
                    //correct config in custom project - Strategies to optimize databank doesn't exist there
                    var databanksObj = getChildElement(settingsObj, "Databanks");
                    var databankObjs = getChildElements(databanksObj, 'Databank');

                    for(var d=0; d<databankObjs.length; d++){
                        var inputDBElem = databankObjs[d];
                        var dbName = getAttrValue(inputDBElem, "name", null);

                        if(dbName == "Input") {
                            if (inputDBElem && getAttrStringValue(inputDBElem, 'value', databankTypes.results) == defaultDatabank) {
                                inputDBElem.setAttribute("value", databankTypes.results);
                                
                                if(taskName == curTaskName){
                                    instance.config.sourceDatabank = databankTypes.results;
                                }
                            }
                            break;
                        }
                    }
                }
            }
        }

        SQEvents.notifyListeners(SQEvents.get("DATABANK_DISPLAY"), {
            name: defaultDatabank,
            show: showDatabank,
            displayFirst: true
        });
    }

    this.getNumberOfTests = function (callback) {
        var databanks = DatabankService.getTaskDatabanks();
        var databank = getItem(databanks, 'name', 'Input');

        var args = {
            projectName: AppService.getProject(),
            databankName: databank ? databank.value : databankTypes.results,
            taskConfig: xmlToString(AppService.getTaskConfig())
        }

        BackendService.sendRequest("optimization/getNumberOfTests", args, function (result) {
            instance.gui.testsCount = formatNumberWithCommas(result.numberOfTests);

            if (callback) {
                callback(result.settingsXML);
            }
        }, 'POST');
    }

    this.isAllStrategiesOptionSet = function () {
        var optimizationObj = AppService.getCurrentTaskTabSettings("Optimization");
        if (optimizationObj) {
            var sourceObj = getChildElements(optimizationObj, "Source")[0];
            if (sourceObj) {
                var type = correctAttrValue(sourceObj.attributes['type']);
                if (type == 'databank') return true;
            }
        }
    }

    this.getWhatToParametrizeText = function () {
        var all = true;
        var text = "";

        if (instance.config.periodParams) {
            text += (text != "" ? ", " : "") + L.tsq("Periods");
        } else {
            all = false;
        }
        if (instance.config.shiftParams) {
            text += (text != "" ? ", " : "") + L.tsq("Shifts");
        } else {
            all = false;
        }
        if (instance.config.constantsParams) {
            text += (text != "" ? ", " : "") + L.tsq("Constants");
        } else {
            all = false;
        }
        if (instance.config.exitParamsUsed) {
            text += (text != "" ? ", " : "") + L.tsq("Exit params used");
        } else {
            all = false;
        }
        if (instance.config.exitParamsUnused) {
            text += (text != "" ? ", " : "") + L.tsq("Exit params unused");
        } else {
            all = false;
        }
        if (instance.config.tradingOptionsParams) {
            text += (text != "" ? ", " : "") + L.tsq("Trading options");
        } else {
            all = false;
        }
        if (instance.config.booleanParams) {
            text += (text != "" ? ", " : "") + L.tsq("Booleans");
        } else {
            all = false;
        }
        if (instance.config.otherParams) {
            text += (text != "" ? ", " : "") + L.tsq("Others");
        } else {
            all = false;
        }
        if (instance.config.entryParams) {
            text += (text != "" ? ", " : "") + L.tsq("Entry (levels)");
        } else {
            all = false;
        }
        if (instance.config.entryLogic) {
            text += (text != "" ? ", " : "") + L.tsq("Entry (logic)");
        } else {
            all = false;
        }        

        return all ? "All" : text;
    }

    this.saveConfig = function () {
        var xmlDoc = AppService.xmlDoc;

        var settingsObj = AppService.getTaskConfig();
        if (!settingsObj) return;

        var optimizationObj = getChildElement(settingsObj, "Optimization");
        if (!optimizationObj) {
            optimizationObj = createChild(settingsObj, "Optimization", xmlDoc, true);
        } else {
            removeAllChildren(optimizationObj);
        }

        optimizationObj.setAttribute("type", instance.config.optimization);
        optimizationObj.setAttribute("maxOptimizations", instance.config.maxOptimizations ? instance.config.maxOptimizations : 100);

        setSource(optimizationObj);

        if (instance.config.optimization == instance.constants.optimizations.simple || instance.config.optimization == instance.constants.optimizations.sequential) {
            var soObj = createChild(optimizationObj, "SimpleOptimization", xmlDoc, true);
            soObj.setAttribute("type", instance.config.simpleType || instance.constants.simpleOptimizations.storeBest);
            soObj.setAttribute("pctToPass", instance.config.pctToPass);
            soObj.setAttribute("resultsCount", instance.config.resultsCount);
            soObj.setAttribute("stabilityRange", instance.config.stabilityRange);
        } 
        else {
            var wfObj = createChild(optimizationObj, "WalkForward", xmlDoc, true);
            wfObj.setAttribute("type", instance.config.wfType);
            wfObj.setAttribute("period", instance.config.periodType);
            wfObj.setAttribute("optimization", instance.config.optimizationType);

            var param1Obj = createChild(wfObj, "Param1", xmlDoc, true);
            var param2Obj = createChild(wfObj, "Param2", xmlDoc, true);

            if (instance.config.optimization == instance.constants.optimizations.walk_fo) {
                param1Obj.setAttribute("value", instance.config.param1);
                param2Obj.setAttribute("value", instance.config.param2);
            } else {
                param1Obj.setAttribute("start", instance.config.param1Start);
                param1Obj.setAttribute("stop", instance.config.param1Stop);
                param1Obj.setAttribute("step", instance.config.param1Step);

                param2Obj.setAttribute("start", instance.config.param2Start);
                param2Obj.setAttribute("stop", instance.config.param2Stop);
                param2Obj.setAttribute("step", instance.config.param2Step);
            }
        }

        var optimizationMethodObj = createChild(optimizationObj, 'OptimizationMethod', xmlDoc);

        optimizationMethodObj.setAttribute("settings", instance.config.mode);
        optimizationMethodObj.setAttribute("symmetricVariables", instance.config.parametrizeType==0 ? false : instance.config.symmetricVariables);
        optimizationMethodObj.setAttribute("symmetryDisabled", instance.gui.symmetryDisabled);

        if (instance.config.mode == instance.modes.automatic) {
            createAutomaticXMLConfig(xmlDoc, optimizationObj);
        } else {
            createManualXMLConfig(xmlDoc, optimizationObj);
        }

        var whatToParametrizeObj = getChildElement(optimizationObj, "WhatToParametrize", true);
        if (!whatToParametrizeObj) {
            whatToParametrizeObj = createChild(optimizationObj, 'WhatToParametrize', xmlDoc, true);
        }

        whatToParametrizeObj.setAttribute("type", instance.config.parametrizeType);

        addNode('Recommended', instance.config.parametrizeType==0 ? true : false, whatToParametrizeObj, xmlDoc);
        addNode('Periods', instance.config.parametrizeType==0 ? false : instance.config.periodParams, whatToParametrizeObj, xmlDoc);
        addNode('Shifts', instance.config.parametrizeType==0 ? false : instance.config.shiftParams, whatToParametrizeObj, xmlDoc);
        addNode('Constants', instance.config.parametrizeType==0 ? false : instance.config.constantsParams, whatToParametrizeObj, xmlDoc);
        addNode('OtherParams', instance.config.parametrizeType==0 ? false : instance.config.otherParams, whatToParametrizeObj, xmlDoc);
        addNode('EntryParams', instance.config.parametrizeType==0 ? false : instance.config.entryParams, whatToParametrizeObj, xmlDoc);
        addNode('EntryLogic', instance.config.parametrizeType==0 ? false : instance.config.entryLogic, whatToParametrizeObj, xmlDoc);
        addNode('ExitParamsUsed', instance.config.parametrizeType==0 ? false : instance.config.exitParamsUsed, whatToParametrizeObj, xmlDoc);
        addNode('ExitParamsUnused', instance.config.parametrizeType==0 ? false : instance.config.exitParamsUnused, whatToParametrizeObj, xmlDoc);
        addNode('BooleanParams', instance.config.parametrizeType==0 ? false : instance.config.booleanParams, whatToParametrizeObj, xmlDoc);

        addNode('TradingOptions', instance.config.tradingOptionsParams, whatToParametrizeObj, xmlDoc);

        instance.checkOptimizationSource();
    }

    function setSource(optimizationObj) {
        var sourceText = instance.config.source == instance.constants.sources.file ? instance.config.filePath : "";
        var sourceObj = addNode("Source", sourceText, optimizationObj, AppService.xmlDoc);
        sourceObj.setAttribute("type", instance.config.source);
        sourceObj.setAttribute("relativePath", instance.config.relativePath);

        DatabankService.setTaskInputDatabank(instance.config.sourceDatabank);
    }

    function createManualXMLConfig(xmlDoc, parentObj) {
        var manualSettingsObj = getChildElements(parentObj, "ManualSettings")[0];
        if (!manualSettingsObj) {
            manualSettingsObj = createChild(parentObj, 'ManualSettings', xmlDoc, true);
        }

        var paramsObj = getChildElements(manualSettingsObj, "Params")[0];
        if (!paramsObj) {
            paramsObj = createChild(manualSettingsObj, 'Params', xmlDoc, true);
        }
    }

    function createAutomaticXMLConfig(xmlDoc, parentObj) {
        var autoSettingsObj = createChild(parentObj, 'AutomaticSettings', xmlDoc);

        autoSettingsObj.setAttribute("distributionUp", instance.config.distributionUp);
        autoSettingsObj.setAttribute("distributionDown", instance.config.distributionDown);
        autoSettingsObj.setAttribute("maxSteps", instance.config.maxSteps);
    }

    //handling strategies loading
    function onWebSocketData(args) {
        if(!instance.gui.active) return;

        if (args.projectData && args.projectData.channels) {
            var databanks = DatabankService.getTaskDatabanks();
            var databank = getItem(databanks, 'name', 'Input');
            var inputDatabank = databank ? databank.value : databankTypes.results;

            for (var i = 0; i < args.projectData.channels.length; i++) {
                var channel = args.projectData.channels[i];
                if (channel.name == SQConstants.getConstants().channels.databanks) {
                    if (channel.data) {
                        for (var a = 0; a < channel.data.length; a++) {
                            if (channel.data[a].name == inputDatabank) {
                                tryUpdateNumberOfTests();
                                break;
                            }
                        }
                    }
                    break;
                }
            }
        }
    }

    function tryUpdateNumberOfTests(){
        if(timeoutHandle){
            clearTimeout(timeoutHandle);
        }

        timeoutHandle = setTimeout(function(){
            timeoutHandle = null;
            instance.getNumberOfTests();
        }, 2000);
    }

    function onEvent(event, data) {
        if (event == SQEvents.get("DATABANKS_LOADED")) {
            instance.checkOptimizationSource();
        } 
        else if (event == SQEvents.get('OPTIMIZATION_SRC_CHANGED')) {
            instance.gui.automaticOnly = data;
            if (data) {
                instance.config.mode = instance.modes.automatic;
            }
        }
    }

    var instance = this;
    
    var loaded = false;
    var timeoutHandle;

    var databankTypes = SQConstants.getConstants().gedatabanks;

    var fileExtension = {
        name: 'sq4,str,sqx,sqw',
        description: 'SQ X Strategy Files'
    }

    this.constants = SQConstants.getConstants().optimization;
    this.wfTypes = objectToList(this.constants.wfTypes);
    this.wfTypes.sort(function (a, b) {
        return a.value - b.value;
    });
    this.helpText = L.tsq("Select the strategy to optimize and type of optimization");
    this.isOptimizer = window.appConfig.product == 'OPTIMIZER';
    this.simpleSettingsRequest = false;

    this.modes = {
        manual: 'manual',
        automatic: 'automatic'
    }

    this.paramTypes = [{
            name: "unknown",
            value: 0
        },
        {
            name: "int",
            value: 1
        },
        {
            name: "boolean",
            value: 2
        },
        {
            name: "string",
            value: 3
        },
        {
            name: "double",
            value: 4
        }
    ]

    this.comparators = ['>', '<', '=', '>=', '<=', '<>'];

    this.autoPreset = {
        distributionUp: 20,
        distributionDown: 20,
        maxSteps: 10
    }

    this.config = {
        optimization: instance.constants.optimizations.simple,
        source: instance.constants.sources.file,
        relativePath: false,
        outputDatabank: databankTypes.results,
        wfType: '' + instance.constants.wfType.WF_TYPE_SIMIS_SIMOOS,
        periodType: instance.constants.periodTypes.percent,
        optimizationType: instance.constants.optimizationTypes.floating,
        simpleType: instance.constants.simpleOptimizations.storeBest,
        filePath: "",
        param1: 20,
        param2: 10,
        param1Start: 10,
        param1Stop: 40,
        param1Step: 10,
        param2Start: 5,
        param2Stop: 20,
        param2Step: 5,
        //Parameters settings
        mode: instance.modes.manual,
        symmetricVariables: true,
        distributionUp: 20,
        distributionDown: 20,
        presetDistribution: 30,
        maxSteps: 20,
        maxOptimizations: "100",
        periodParams: true,
        shiftParams: true,
        constantsParams: true,
        otherParams: true,
        entryParams: true,
        entryLogic: true,
        exitParamsUsed: true,
        exitParamsUnused: true,
        tradingOptionsParams: true,
        parametrizeType: 0,
        pctToPass: 80,
        resultsCount: 5,
        stabilityRange: 20
    };
    this.maxOptimizationValues = [
        {value:100, name: '100'},
        {value:500, name: '500'},
        {value:1000, name: '1.000'},
        {value:3000, name: '3.000'},
        {value:7500, name: '7.500'},
        {value:10000, name: '10.000'},
        {value:15000, name: '15.000'},
        {value:50000, name: '50.000'},
        {value:100000, name: '100.000'},
        {value:200000, name: '200.000'},
        {value:500000, name: '500.000'},
        {value:1000000, name: '1.000.000'},
        {value:10000000, name: '10.000.000'},
        {value:100000000, name: '100.000.000'},
        {value:1000000000, name: '1.000.000.000'},
        {value:1000000001, name: L.tsq('Exhaustive (all combinations)')}
    ];
    
    this.printMaxTestsLabel = function(value) {
        var item = getItem(instance.maxOptimizationValues, "value", value);
        return item ? item.name : value;
    }

    this.gui = {
        automaticOnly: false,
        symmetryDisabled: false,
        testsCount: 1,
        settingsChanged: false,
        active: false
    }

    this.printParamLabel = function(param, type) {
        let types = instance.constants.periodTypes;

        if(param == 1) {
            switch (type) {
                case types.percent: return L.tsq("Out of Sample %");
                case types.days: return L.tsq("In Sample days");
                case types.bars: return L.tsq("In Sample bars");
            }
        } else {
            switch (type) {
                case types.percent: return L.tsq("Walk Forward runs");
                case types.days: return L.tsq("Out of Sample days");
                case types.bars: return L.tsq("Out of Sample bars");
            }
        }
    }

    var fileExistsRequestDeferred = $q.defer();

    SQWebSocketService.subscribeGeneral("OptimizationService" + "-" + window.appConfig.appCode, onWebSocketData);

    SQEvents.addListener("OptimizationService", [
        SQEvents.get("DATABANKS_LOADED"),
        SQEvents.get('OPTIMIZATION_SRC_CHANGED')
    ], onEvent);

});