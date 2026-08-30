angular.module('app.settings').controller('OptimizationCtrl', function ($rootScope, $scope, $timeout, L, SQEvents, SQConstants, OptimizationService, DatabankService, AppService, ParametersGridService) {

    function init(dontLoadConfig, forceReload, callback) {
        loaded = false;

        OptimizationService.checkOptimizationSource();
        DatabankService.databankList(function () {
            $scope.databanks.length = 0;
            var allDatabanks = angular.copy(DatabankService.databanks);

            for (var i = 0; i < allDatabanks.length; i++) {
                var databank = allDatabanks[i];
                if ((databank.name != databankTypes.results || !$scope.isOptimizer) && databank.display !== false) {
                    $scope.databanks.push(databank);
                }
            }

            if (!dontLoadConfig) {
                OptimizationService.loadConfig(function () {
                    if (forceReload) {
                        $scope.parametersGrid.reloadVariables();
                    }

                    $timeout(function () {
                        loaded = true;
                        if(callback) callback();
                    }, 0, false);
                }, forceReload);
            } else {
                loaded = true;
            }
        });

        $timeout(function () {
            initialized = true;
            $('[data-toggle="tooltip"]').tooltip({
                "trigger": "hover",
            });
        }, 0, false);
    }

    $scope.onSelectFile = function () {
        if ($scope.config.source != $scope.constants.sources.file) return;

        OptimizationService.onSelectFile(function () {
            onSettingsChange();
        });
    }

    $scope.resizeGrid = function () {
        $timeout(function () {
            window.dispatchEvent(new Event('resize'));
        }, 0, false)
    }

    $scope.getHelpText = function () {
        return L.tsq("Optimization explanation - if you choose Maximum optimizations value bigger than number of Total combinations,\nSQ will use Brute force method to test all the combinations.\nOtherwise it will use Genetic optimization method configured to roughly use desired maximum optimizations.\n\nMaximum optimizations recommended value is 5.000 - 20.000.")
    }

    $scope.onAutoPreset = function () {
        var variables = $scope.parametersGrid.getVariables();
        var selected = getItem(variables, 'use', true);

        if (!selected) {
            $rootScope.showError(L.tsq("No parameters set to be used. Please check at least one"));
            return;
        }

        showPopup("#autoPresetPopup");
    }

    $scope.onSaveAutoPreset = function () {
        $scope.parametersGrid.autoPreset($scope.autoPreset);
        hidePopup("#autoPresetPopup");
    }

    $scope.optimizationTypeChanged = function (type) {
        SQEvents.notifyListeners(SQEvents.get("OPTIMIZATION_TYPE_CHANGED"), type);
    }

    $scope.optimizationSimpleTypeChanged = function (type) {
        SQEvents.notifyListeners(SQEvents.get("OPTIMIZATION_SIMPLE_TYPE_CHANGED"), type);
    }

    $scope.onParametersChange = function () {
        $scope.gui.settingsChanged = true;
        settingsChanged = true;
    }

    function checkValue(param, value, paramType) {
        var valueParamRowId = getRowIdOfParam(value);
        if (valueParamRowId) { //parameter used as value
            var valueParamType = getParamType(valueParamRowId);
            if (paramType != valueParamType) {
                $rootScope.showError(L.tsq("Different parameter types used. Parameter '%s' is of type %s and '%s' is of type %s", [param, paramType, value, valueParamType]));
                return false;
            }
        } else {
            if (value == '') {
                $rootScope.showError(L.tsq("Value cannot be empty"));
                return false;
            }
            switch (paramType) {
                case 'boolean':
                    if (value != 'true' && value != 'false') {
                        $rootScope.showError(L.tsq("Incorrect value '%s'. Expected 'true' or 'false'", [value]));
                        return false;
                    }
                    break;
                case 'int':
                    if (!checkIntegerValue(value)) {
                        $rootScope.showError(L.tsq("Incorrect value '%s'. Expected integer value", [value]));
                        return false;
                    }
                    break;
                case 'double':
                    if (!checkDoubleValue(value)) {
                        $rootScope.showError(L.tsq("Incorrect value '%s'. Expected numeric value", [value]));
                        return false;
                    }
                    break;
            }
        }

        return true;
    }

    function shouldReloadVariables(newValue, oldValue) {
        return oldValue.parametrizeType != newValue.parametrizeType ||
            oldValue.periodParams != newValue.periodParams ||
            oldValue.shiftParams != newValue.shiftParams ||
            oldValue.constantsParams != newValue.constantsParams ||
            oldValue.otherParams != newValue.otherParams ||
            oldValue.entryParams != newValue.entryParams ||
            oldValue.entryLogic != newValue.entryLogic ||
            oldValue.exitParamsUsed != newValue.exitParamsUsed ||
            oldValue.exitParamsUnused != newValue.exitParamsUnused ||
            oldValue.tradingOptionsParams != newValue.tradingOptionsParams ||
            oldValue.booleanParams != newValue.booleanParams;
    }

    function save() {
        OptimizationService.saveConfig();
        ParametersGridService.save(true);

        if (sendSrcChangedEvent) {
            sendSrcChangedEvent = false;
            SQEvents.notifyListeners(SQEvents.get("OPTIMIZATION_SRC_CHANGED"), $scope.config.source == $scope.constants.sources.databank);
        }
    }

    //- Event Handlers --------------------------------------------------------------

    $scope.tab.shouldSaveSettings = function () {
        var shouldSaveSettings = settingsChanged || $scope.gui.settingsChanged;
        $scope.gui.settingsChanged = false;
        settingsChanged = false;
        return shouldSaveSettings;
    }

    function onSettingsChange() {
        settingsChanged = true;
        $scope.gui.settingsChanged = false;
        save();
    }

    function onEvent(event, data) {
        if (event == SQEvents.get('SETTINGS_TAB_ACTIVE')) {
            $scope.gui.active = data.title == $scope.tab.title;
            if ($scope.gui.active && !loaded) {
                init();
            }
        } 
        else if (event == SQEvents.get('SETTINGS_TAB_RELOAD')) {
            if ($scope.tab.display && (data == $scope.tab.title || data == 'all')) {
                init(false, true, OptimizationService.checkOptimizationSource);
            }
        } 
        else if (event == SQEvents.get("RELOAD_DATABANKS") && !$scope.isOptimizer) { //databank created
            init(true);
            return;
        } 
        else if (event == SQEvents.get("OPTIMIZATION_FILE_CHANGED")) {
            onSettingsChange();
        } 
        else return;

        try {
            $scope.$digest();
        } catch (er) {}
    }

    var destroyConfigWatch = $scope.$watch('config', function (newValue, oldValue) {
        if (loaded && !disableWatch) {
            if (newValue.source != oldValue.source && !OptimizationService.simpleSettingsRequest) {
                sendSrcChangedEvent = true;

                OptimizationService.checkOptimizationSource();

                if (newValue.source == $scope.constants.sources.databank) {
                    if ($scope.isOptimizer) {
                        $scope.config.sourceDatabank = databankTypes.strategiesToOptimize;
                    } else {
                        $scope.config.sourceDatabank = $scope.databanks[0].name;
                        $scope.config.outputDatabank = $scope.databanks[0].name;
                    }
                } 
                else if (newValue.source == $scope.constants.sources.file) {
                    if (valueFilled($scope.config.filePath)) {
                        OptimizationService.onLoadStrategy($scope.config.filePath, function () {
                            $timeout(function () {
                                SQEvents.notifyListeners(SQEvents.get('PARAMETERS_GRID_RELOAD'));
                            }, 0, false);
                        });
                    }
                }
            }

            if (valueFilled(newValue.outputDatabank) && newValue.outputDatabank != oldValue.outputDatabank) {
                DatabankService.setTaskOutputDatabank(newValue.outputDatabank);
            }

            if (oldValue.symmetricVariables != newValue.symmetricVariables) {
                $scope.parametersGrid.reloadVariables();
            } 
            else if (shouldReloadVariables(newValue, oldValue)) {
                $scope.gui.settingsChanged = false;
                settingsChanged = false;

                OptimizationService.saveConfig();
                ParametersGridService.save(true);

                AppService.updateTaskXML().then(function () {
                    $scope.parametersGrid.reloadVariables();
                });
                return;
            }

            if (oldValue.mode != newValue.mode ||
                oldValue.distributionUp != newValue.distributionUp ||
                oldValue.distributionDown != newValue.distributionDown ||
                oldValue.maxSteps != newValue.maxSteps
            ) {
                OptimizationService.saveConfig();

                $timeout(function(){            //timeout needed, because changes need to take effect
                    OptimizationService.getNumberOfTests(function () {
                        try { $scope.$digest(); } catch (err) {}
                    });
                }, 0, false);
            }

            onSettingsChange();
        }

        if (disableWatch) {
            disableWatch = false;
        }
    }, true);

    $scope.getWFTypeLabel = function () {
        var type = getItem($scope.wfTypes, "value", $scope.config.wfType);
        return type ? type.name : '';
    }

    $scope.$on("$destroy", function () {
        destroyConfigWatch();
        SQEvents.removeListener(listenerId);
    });

    //- Initialization --------------------------------------------------------------

    var loaded = false;
    var sendSrcChangedEvent = false;
    var disableWatch = false;

    var settingsChanged = false;

    var databankTypes = SQConstants.getConstants().gedatabanks;

    $scope.databanks = [];
    $scope.isOptimizer = OptimizationService.isOptimizer;
    $scope.constants = OptimizationService.constants;
    $scope.wfTypes = OptimizationService.wfTypes
    $scope.config = OptimizationService.config;

    $scope.parametersGrid = {};

    $scope.modes = OptimizationService.modes;
    $scope.paramTypes = OptimizationService.paramTypes;
    $scope.comparators = OptimizationService.comparators;
    $scope.maxOptimizationValues = OptimizationService.maxOptimizationValues;
    $scope.printMaxTestsLabel = OptimizationService.printMaxTestsLabel;
    
    $scope.parameters = [];

    $scope.config = OptimizationService.config;
    $scope.gui = OptimizationService.gui;

    $scope.autoPreset = OptimizationService.autoPreset;

    $scope.variablesMenu = {};

    $scope.onSave = OptimizationService.saveVariablesSettings;

    $scope.printParamLabel = OptimizationService.printParamLabel;

    init();

    var listenerId = "OptimizationCtrl";
    SQEvents.addListener(listenerId, [
        SQEvents.get('SETTINGS_TAB_ACTIVE'),
        SQEvents.get('SETTINGS_TAB_RELOAD'),
        SQEvents.get("RELOAD_DATABANKS"),
        SQEvents.get("OPTIMIZATION_FILE_CHANGED")
    ], onEvent);

});