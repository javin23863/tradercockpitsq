angular.module('app.settings').controller('WhatToBuildCtrl', function ($rootScope, $scope, $timeout, $element, WhatToBuildService, ComplexitiesService, OptionsService, DatabankService, AppService, SQConstants, SQEvents, L, DataService) {
    $scope.broker = SQConstants.getConstants().broker;
    $scope.isBrazilianEdition = SQConstants.getConstants().isBrazilianEdition;

    function init() {
        WhatToBuildService.init();
        WhatToBuildService.loadConfig();

        ComplexitiesService.loadComplexities();
        $scope.complexities = ComplexitiesService.settings.complexities;

        for (var i = 0; i < $scope.settingsPlugins.length; i++) {
            $scope.settingsPlugins[i].loadSettings();
        }

        try { $scope.$digest(); } catch (err) { }

        $timeout(function () {
            initialized = true;
            $scope.dataConfig.engine = DataService.config.setups[0].engine;
        }, 0, false);
    }

    $scope.reloadTemplateFile = function () {
        WhatToBuildService.checkTemplateResources($scope.config.templateFile);

        WhatToBuildService.loadTemplate($scope.config.templateFile, true, function (result) {
            if(result.strategyTemplateFiles){
                loadToArray($scope.strategyTemplateFiles, result.strategyTemplateFiles);
            }

            ComplexitiesService.loadFromTemplate(result.chartSettings);
            SQEvents.notifyListeners(SQEvents.get('SETTINGS_TAB_RELOAD'), 'Data');
            $rootScope.showSuccess(Ltsq("Template loaded"));
        });
    }

    $scope.openTemplateFilesDir = function () {
        var pathName = "loadBuildTypeTemplateFile";
        var defaultPath = $scope.config.templateFile || SQConstants.getSettings()["strategyTemplatesPath"];
        var fileExtension = { name: 'sqx,sq4', description: L.tsq('SQ X Template Files') };

        openFilePicker(pathName, defaultPath, fileExtension, function (filePath) {
            $scope.config.templateFile = '';            //enables subcharts refreshing when loading modified template with the same name
            try { $scope.$digest(); } catch (err) { }

            $scope.config.templateFile = filePath;
            try { $scope.$digest(); } catch (err) { }

            WhatToBuildService.checkTemplateResources(filePath);
        });
    }

    $scope.openStrategyFilesDir = function () {
        var pathName = "loadBuildTypeStrategyFile";
        var defaultPath = $scope.config.strategyFile || SQConstants.getSettings()["strategiesPath"];
        var fileExtension = { name: 'sqx,sq4,sqw,str', description: L.tsq('SQ X Strategy Files') };

        openFilePicker(pathName, defaultPath, fileExtension, function (filePath) {
            $scope.config.strategyFile = filePath;
            try { $scope.$digest(); } catch (err) { }
        });
    }

    function openFilePicker(pathName, defaultPath, extension, callback) {
        var path = SQConstants.getSettings()[pathName];

        $rootScope.showFilePicker(L.tsq('Select config file to load'), pathName, true, false, defaultPath, extension, null, function (paths) {
            if (paths) {
                callback(paths[0]);
            }
        });
    }

    $scope.openSettingsPopup = function (plugin, setting) {
        $scope.activeSettings.setting = setting;
        $scope.activeSettings.plugin = plugin;
        $scope.activeSettings.plugin.loadSettings();

        showPopup('#additionalConfigPopup');
    }

    $scope.onSaveAdditionalConfig = function () {
        if (!$scope.activeSettings.plugin.checkSettings()) {
            return;
        }

        hidePopup('#additionalConfigPopup');

        var whatToBuildElem = AppService.getCurrentTaskTabSettings("WhatToBuild");
        $scope.activeSettings.plugin.saveSettings(whatToBuildElem);

        AppService.updateTaskXML();
    }

    $scope.onResetToDefault = function () {
        $scope.activeSettings.plugin.resetSettings();
    }

    function onStrategyTypeChange() {
        dontSave = true;

        if ($scope.config.strategyType == $scope.strategyTypes.multiTf) {
            $scope.complexities.length = parseInt($scope.config.additionalCharts) + 1;
        }
        else if ($scope.config.strategyType == $scope.strategyTypes.template) {
            if (valueFilled($scope.config.templateFile)) {
                WhatToBuildService.loadTemplate($scope.config.templateFile, false, function (result) {
                    ComplexitiesService.loadFromTemplate(result.chartSettings);
                    SQEvents.notifyListeners(SQEvents.get('SETTINGS_TAB_RELOAD'), 'Data');
                    $rootScope.showSuccess(Ltsq("Template loaded"));
                });
                return;
            }
        }
        else if ($scope.config.strategyType == $scope.strategyTypes.improve) {
            if ($scope.config.improveType == $scope.improveTypes.strategy) {
                if (valueFilled($scope.config.strategyFile)) {
                    WhatToBuildService.loadTemplate($scope.config.strategyFile, false, function (result) {
                        OptionsService.replaceBTOptions(result.lastSettings);
                        SQEvents.notifyListeners(SQEvents.get('SETTINGS_TAB_RELOAD'), 'Trading options');
                        $rootScope.showSuccess(Ltsq("Strategy settings loaded"));
                    });
                    return;
                }
            }
            else if ($scope.config.improveType == $scope.improveTypes.databank && $scope.isBuilder) {
                //for builder set default improve databank
                $scope.config.improveDatabank = SQConstants.getConstants().gedatabanks.strategiesToImprove
            }
        }
        else {
            $scope.complexities.length = 1;
        }

        ComplexitiesService.correctComplexities();
        SQEvents.notifyListeners(SQEvents.get('SETTINGS_TAB_RELOAD'), 'Data');
    }

    $scope.onHelp = function () {
        AppService.openHelp("what-to-build");
    }

    $scope.onClose = function () {
        $scope.activeSettings.plugin.resetSettings(true);        //to revert settings text when closing settings popup
    }

    $scope.isSettingDisabled = function(setting) {
        return setting === 'Strategy style' && ($scope.dataConfig.engine === 'Stockpicker' || $scope.dataConfig.engine === 'Single-asset cloud strategy');
    }

    //- Event Handlers --------------------------------------------------------------

    $scope.tab.shouldSaveSettings = function () {
        var shouldSaveSettings = settingsChanged;
        settingsChanged = false;
        return shouldSaveSettings;
    }

    $scope.tab.getSettings = function () {
        return [{
            name: "What to build setting",
            value: 12345
        }];
    }

    function onSettingsChange() {
        settingsChanged = true;
        WhatToBuildService.saveConfig();
    }

    var destroyConfigWatch = $scope.$watch("config", function (newValue, oldValue) {
        if (initialized) {
            if (newValue.strategyType != oldValue.strategyType ||
                newValue.additionalCharts != oldValue.additionalCharts ||
                newValue.templateFile != oldValue.templateFile ||
                newValue.strategyFile != oldValue.strategyFile ||
                newValue.improveType != oldValue.improveType
            ) {
                if (newValue.templateFile instanceof Array) {
                    $scope.config.templateFile = newValue.templateFile[0];
                }
                onStrategyTypeChange();
            }

            if (valueFilled(newValue.improveDatabank) && newValue.improveDatabank != oldValue.improveDatabank) {
                DatabankService.setTaskInputDatabank(newValue.improveDatabank);
            }

            if (valueFilled(newValue.outputDatabank) && newValue.outputDatabank != oldValue.outputDatabank) {
                DatabankService.setTaskOutputDatabank(newValue.outputDatabank);
            }

            WhatToBuildService.updateTabs();

            onSettingsChange();
        }
    }, true);

    var destroyComplexitiesWatch = $scope.$watch("complexities", function (newValue, oldValue) {
        if (initialized) {
            if (dontSave) {
                dontSave = false;
            }
            else {
                onSettingsChange();
            }
        }
    }, true);

    function onEvent(event, data) {
        if (event == SQEvents.get('SETTINGS_TAB_RELOAD')) {
            if ($scope.tab.display && (data == $scope.tab.title || data == 'all')) {
                initialized = false;
                reload();
            }
        }
        else if (event == SQEvents.get('SETTINGS_TAB_ACTIVE')) {
            var active = data.title == $scope.tab.title;
            if (active && !initialized) {
                reload();
            }

            watchersHandler.onActivated(active);
        }
        else if (event == SQEvents.get("RELOAD_DATABANKS")) {    //databank created
            if (AppService.getTask().type == "Build") {
                reload();
            }
        }
        else if (event == SQEvents.get('MAIN_SETUP_CHANGED')) {    //databank created
            if (AppService.getTask().type == "Build") {
                $scope.dataConfig.engine = DataService.config.setups[0].engine;

                if(($scope.dataConfig.engine === "Stockpicker" || $scope.dataConfig.engine === "Single-asset cloud strategy") && ($scope.config.strategyType === $scope.strategyTypes.improve)){
                    $scope.config.strategyType = $scope.strategyTypes.simple;
                }
            }
        }
        
        else return;

        try { $scope.$digest(); } catch (er) { }
    }

    function reload() {
        $timeout(function () {
            DatabankService.loadVisibleDatabanks($scope.databanks, init, true);
        }, 0, false);
    }

    $scope.$on("$destroy", function () {
        destroyConfigWatch();
        destroyComplexitiesWatch();
        SQEvents.removeListener(listenerId);
        angular.element(document.querySelector('#additionalConfigPopup')).remove();
    });

    //- Initialization --------------------------------------------------------------

    var initialized = false;

    var dontSave = false;
    var settingsChanged = false;

    $scope.settingsPlugins = WhatToBuildService.settingsPlugins;
    $scope.activeSettings = WhatToBuildService.activeSettings;

    $scope.isBuilder = WhatToBuildService.isBuilder;

    $scope.strategyTypes = WhatToBuildService.strategyTypes;
    $scope.improveTypes = WhatToBuildService.improveTypes;

    $scope.databanks = [];

    $scope.strategyFiles = WhatToBuildService.strategyFiles;
    $scope.strategyTemplateFiles = WhatToBuildService.strategyTemplateFiles;

    $scope.config = WhatToBuildService.config;

    $scope.complexities = ComplexitiesService.settings.complexities;
    $scope.dataConfig = {};

    WhatToBuildService.loadConfig();

    var listenerId = "WhatToBuildCtrl";
    SQEvents.addListener(listenerId, [
        SQEvents.get('SETTINGS_TAB_RELOAD'), 
        SQEvents.get('SETTINGS_TAB_ACTIVE'), 
        SQEvents.get("RELOAD_DATABANKS"),
        SQEvents.get('MAIN_SETUP_CHANGED')
    ], onEvent);

    var watchersHandler = new WatchersHandler($($element));
    $timeout(function () { watchersHandler.onActivated(false); }, 0, false);

});