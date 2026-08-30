angular.module('app.settings').controller('BlocksCtrl', function ($rootScope, $scope, $element, $timeout, $q, SQWebSocketService, BlocksService, BlockConfigService, ComplexitiesService, SQEvents, L, AppService) {

    function initGrids() {
        BlocksService.initOrderTypesGrid();
        BlocksService.initExitTypesGrid();
        BlocksService.initBlocksGrid();
        BlocksService.initIndicatorsGrid();
        BlocksService.initStopLimitBlocksGrid();
        BlocksService.initCustomDataGrid();

        deferred.resolve();

        $(document).on('click', '.sq-accordion .item-head', function () {
            setTimeout(function () {
                window.dispatchEvent(new Event('resize'));
            }, 300);
        });
    }

    function loadContent(callback) {
        promise.then(function () {
            BlocksService.loadSettings(callback);

            setTimeout(function () {
                window.dispatchEvent(new Event('resize'));
            }, 500);
        });
    }

    $scope.onCloseBlockConfig = function () {
        if ($scope.gui.editingNormalBlock) {
            if ($scope.selectedBlock.isChanged) {
                $rootScope.showConfirm(L.tsq("Parameters settings"), L.tsq("Do you really want to close parameters editation?<br>Your changes will be not saved"), function (confirmed) {
                    if (confirmed) {
                        $scope.selectedBlock.isChanged = false;
                        hidePopup('#blockParametersPopup');
                    }
                }, true);
            } else {
                hidePopup('#blockParametersPopup');
            }
        } else {
            var hasChanged = getItem($scope.specialBlock.items, "isChanged", true);
            if (hasChanged) {
                $rootScope.showConfirm(L.tsq("Parameters settings"), L.tsq("Do you really want to close parameters editation?<br>Your changes will be not saved"), function (confirmed) {
                    if (confirmed) {
                        for (var i = 0; i < $scope.specialBlock.items.length; i++) {
                            $scope.specialBlock.items[i].isChanged = false;
                        }
                        hidePopup('#possibleOptionsPopup');
                    }
                }, true);
            } else {
                hidePopup('#possibleOptionsPopup');
            }
        }
    }

    $scope.onSaveBlockConfig = function () {
        if ($scope.gui.editingNormalBlock) {
            BlocksService.saveBlockConfig($scope.selectedBlock);

            $scope.selectedBlock.isChanged = false;
        } else {
            BlocksService.saveBlockConfig($scope.specialBlock);

            for (var i = 0; i < $scope.specialBlock.items.length; i++) {
                $scope.specialBlock.items[i].isChanged = false;
            }
        }
    }

    $scope.onLoadSettings = function () {
        var extension = {
            name: 'cfx,sqb',
            description: L.tsq('Building Blocks Settings Files')
        };

        $rootScope.showFilePicker(Ltsq("Choose settings file"), "LoadBlockSettings", true, false, null, extension, null, function (paths) {
            if (paths) {
                BlocksService.loadSettingsFromFile(paths[0]);
            }
        });
    }

    $scope.onSaveSettings = function () {
        var extension = {
            name: 'sqb',
            description: L.tsq('Building Blocks Settings Files')
        };

        $rootScope.saveFile("Choose target file", extension, "LoadBlockSettings", "BlockSettings", null, function (filePath) {
            BlocksService.saveSettingsToFile(filePath);
        });
    }

    $scope.showBlocks = function (newArea) {
        if ($scope.gui.shownBlocks == newArea) {
            let areasSwitching = {
                "signals": "indicators",
                "indicators": "stopLimitBlocks",
                "stopLimitBlocks": "indicators"
            }
            $scope.gui.shownBlocks = areasSwitching[newArea];
            $timeout(function () {
                window.dispatchEvent(new Event('resize'));
            }, 0, false);
            return;
        }

        $scope.gui.shownBlocks = newArea;
        $timeout(function () {
            window.dispatchEvent(new Event('resize'));
        }, 0, false);
    }

    $scope.addCDataIndy = function () {
        AppService.performAction('SQMANAGER', "openCDataIndy", null);
    }

    $scope.onCDataIndyChange = function () {
        BlocksService.settingsChanged();

        BlocksService.loadCustomData(true, function () {
            window.dispatchEvent(new Event('resize'));
        });
    }

    $scope.onCalibrateBeforeStartChanged = function(){
        BlocksService.saveConfig();
    }

    //- Event Handlers --------------------------------------------------------------

    $scope.tab.shouldSaveSettings = function () {
        var shouldSaveSettings = $scope.gui.settingsChanged;
        $scope.gui.settingsChanged = false;
        return shouldSaveSettings;
    }

    var destroyConfigWatch = $scope.$watch("config", function (newValue, oldValue) {
        if ($scope.gui.initialized) {
            BlocksService.saveConfig();
        }
    }, true);

    var destroyComplexitiesWatch = $scope.$watch("complexities", function (newValue, oldValue) {
        if (!$scope.gui.initialized) {
            //removes non-existing chart and subchart settings of Chart parameters
            BlocksService.correctAllBlocksChartSettings();
        }
    }, true);

    function onEvent(event, data) {
        if (event == SQEvents.get('SETTINGS_TAB_RELOAD')) {
            if ($scope.tab.display && (data == $scope.tab.title || data == 'all')) {
                $scope.gui.initialized = false;         //reload now if active or later when tab becomes active

                if (isActive) {
                    BlocksService.init();
                    loadContent();
                }
            }
        }
        else if (event == SQEvents.get('SETTINGS_TAB_ACTIVE')) {
            isActive = data.title == $scope.tab.title;
            if (isActive && !$scope.gui.initialized) {
                BlocksService.init();
                loadContent();
            }

            watchersHandler.onActivated(isActive);
        }
        else if (event == SQEvents.get('SLPT_SETTINGS_CHANGED')) {
            if (!$scope.gui.initialized) {
                BlocksService.init();

                loadContent(function () {
                    BlocksService.changeSLPT(data, false);
                });
            }
            else {
                BlocksService.changeSLPT(data, false);
            }
        }
        else if (event == SQEvents.get('LICENSE_CHANGED')) {
            BlocksService.refreshGrids();
        }
        else if (event == SQEvents.get('CUSTOM_DATA_CHANGED') || event == SQEvents.get('MAIN_SETUP_CHANGED')) {
            if(event == SQEvents.get('MAIN_SETUP_CHANGED')){
                BlocksService.filterBlocksByEngine(data.engine);
            }

            BlocksService.loadCustomData(true, function () {
                window.dispatchEvent(new Event('resize'));
            });
        }
        else return;

        try {
            $scope.$digest();
        } catch (er) { }
    }

    $scope.$on("$destroy", function () {
        destroyConfigWatch();
        destroyComplexitiesWatch();
        SQEvents.removeListener(listenerId);
    });

    var deferred = $q.defer();
    var promise = deferred.promise;
    var isActive = false;

    $scope.onSaveParamSetConfig = BlocksService.saveParamSetConfig;
    $scope.onSavePossibleOptions = BlocksService.savePossibleOptions;
    $scope.resetPossibleOptions = BlocksService.resetPossibleOptions;
    $scope.onFormulaEdit = BlocksService.showPossibleOptions;
    $scope.onFilterClick = BlocksService.applyFilterToGrid;

    $scope.categories = BlocksService.categories;
    $scope.paramConfig = BlocksService.paramConfig;
    $scope.config = BlocksService.config;

    $scope.orderTypes = BlocksService.orderTypes;
    $scope.exitTypes = BlocksService.exitTypes;
    $scope.generationTypes = BlockConfigService.generationTypes;
    $scope.complexities = ComplexitiesService.settings.complexities;

    $scope.selectedBlock = BlocksService.selectedBlock;
    $scope.specialBlock = BlocksService.specialBlock;

    $scope.gui = BlocksService.gui; //just to tell the gui is initialized and all settings are loaded
    $scope.gui.initialized = false;

    $scope.onRandomChoice = BlocksService.onRandomChoice;
    $scope.onAddRandomBlocks = BlocksService.onAddRandomBlocks;

    $scope.randomButtons = BlocksService.randomButtons;

    $scope.filterButtonKeys = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'];
    $scope.signalsFilter = BlocksService.signalsFilter;
    $scope.indicatorsFilter = BlocksService.indicatorsFilter;

    $scope.selectedCounts = BlocksService.selectedCounts;

    $scope.customDataTimeframes = BlocksService.customDataTimeframes;

    BlocksService.forceDigest = function () { try { $scope.$digest(); } catch (err) { } };

    var watchersHandler = new WatchersHandler($($element));

    var listenerId = "BlocksCtrl";
    SQEvents.addListener(listenerId, [
        SQEvents.get('SETTINGS_TAB_RELOAD'),
        SQEvents.get('SETTINGS_TAB_ACTIVE'),
        SQEvents.get('SLPT_SETTINGS_CHANGED'),
        SQEvents.get('CUSTOM_DATA_CHANGED'),
        SQEvents.get('MAIN_SETUP_CHANGED'),
        SQEvents.get('LICENSE_CHANGED')
    ], onEvent);

    $timeout(function () {
        initGrids();
        watchersHandler.onActivated(false);
    }, 0, false);

    SQWebSocketService.subscribeGeneral("BlocksService-" + window.appConfig.appCode, function (data) {
        if (data.customBlocksChanged) {
            BlocksService.internalBlocks.loaded = false;
            BlocksService.init();
            loadContent();
        }
    });

});