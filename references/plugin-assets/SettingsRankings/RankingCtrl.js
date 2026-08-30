angular.module('app.settings').controller('RankingCtrl', function ($rootScope, $scope, $element, $timeout, $q, RankingService, DatabankService, AppService, SQConstants, BackendService, SQEvents, L) {

    function init() {
        $scope.taskType = AppService.getTask().type;

        $scope.showMaxStrategies = ($scope.taskType != 'Retest' && $scope.taskType != 'AutomaticRetest');
        $scope.showConditions = ($scope.taskType == 'Build' || $scope.taskType == 'Retest' || $scope.taskType == 'AutomaticRetest');
        $scope.showWFConditions = ($scope.taskType == 'Optimize');

        initFitPortfolio();

        $q.all([cgInitDeferred.promise, ffInitDeferred.promise, cgWFInitDeferred.promise]).then(function () {
            $scope.config.fitnessInstance.resetSettings();

            RankingService.applySettings();

            try { $scope.$digest(); } catch (er) { }

            $timeout(function () {
                initialized = true;
            }, 0, false);
        });
    }

    $scope.onConfigureAutoDismissal = function () {
        showPopup("#autoDismissalPopup");
    }

    $scope.isBuilder = window.appConfig.product == 'BUILDER';

    $scope.onHelp = function () {
        AppService.openHelp("fit-strategy-to-existing-portfolio");
    }

    //-- Conditions -------------------------------------------------------------

    $scope.afterCGInit = function (data) {
        loadedCG = true;

        var columnTypes = SQConstants.getConstants().databankColumnTypes;
        var columns = SQConstants.getColumnsByType([columnTypes.general, columnTypes.wfSpecial], null, true);

        $timeout(function () {
            $scope.config.condGridInstance.setDefaultSampleType(SQConstants.getConstants().sampleTypes.in);
            $scope.config.condGridInstance.setData(columns, SQConstants.getColumns().recentColumns);

            cgInitDeferred.resolve();
        }, 0, false);
    }

    $scope.onCGRefresh = function (grid) {
        if (!loadedCG) {
            return;
        }

        $scope.onFitnessFunctionChange();
    }

    $scope.onConditionsTypeChange = function (type) {
        $scope.config.conditionsType = type;

        if (!loadedCG) {
            return;
        }

        $scope.onFitnessFunctionChange();
    }
    //-- WF Robustness conditions -------------------------------------------------
    $scope.onLoadDefaultWFConditions = function () {
        var options = [
            { title: L.tsq("Replace"), key: "replace" },
            { title: L.tsq("Add"), key: "add" }
        ]

        if ($scope.config.wfCondGridInstance.size() > 0) {
            $rootScope.showOptionsDialog(L.tsq("Set recommended WF conditions"), L.tsq("There already are some conditions in the table. Do you want to Replace them or Add recommended WF conditions to the table?"), function (option) {
                loadDefaultWFConditions(option.key);
            }, options);
        } else {
            loadDefaultWFConditions("add");
        }
    }

    function loadDefaultWFConditions(action) {
        var conditionsObj = $scope.config.wfCondGridInstance.getConditionsObj();
        var conditions = xmlToString(conditionsObj);

        RankingService.getDefaultWFConditions({ conditions: conditions, action: action }, function (data) {
            $scope.config.thresholdPct = 80;
            $scope.config.wfRobRows = 3;
            $scope.config.wfRobCols = 3;
            $scope.config.wfRobComb = 7;

            var conditionsObj = xmlToObject(data.conditions).find("Conditions")[0];
            $scope.config.wfCondGridInstance.setConditionsObj(conditionsObj, true);

            onSettingsChange();
        });
    }

    $scope.afterWFCGInit = function (data) {
        loadedWFCG = true;

        var columnTypes = SQConstants.getConstants().databankColumnTypes;
        var columns = SQConstants.getColumnsByType([columnTypes.general, columnTypes.wfSpecial], null, true);

        $scope.config.wfCondGridInstance.setData(columns, SQConstants.getColumns().recentColumns);

        cgWFInitDeferred.resolve();
    }

    $scope.onWFCGRefresh = function (grid) {
        if (!loadedWFCG) {
            return;
        }

        //updateConditionRowColors(grid, null, 4);
        $scope.onFitnessFunctionChange();
    }

    $scope.afterFitnessInit = function () {
        loadedFF = true;
        ffInitDeferred.resolve();
    }

    $scope.onFitnessChange = function () {
        if (!loadedFF) {
            return;
        }

        $scope.onFitnessFunctionChange();
    }

    $scope.onFitnessFunctionChange = function () {
        if (checkProjectRunning() || !initialized) {
            return;
        }

        onSettingsChange();
    }

    function onMaxRecordsChange(newValue, oldValue) {
        if (AppService.isTaskManager()) {
            onSettingsChange();
            return;

        } else {
            if (checkProjectRunning()) {
                disableWatch = true;
                $scope.config.maxStrategies = oldValue;
                return;
            }
        }

        DatabankService.databankList(function () {
            var databanks = DatabankService.databanks;

            for (var i = 0; i < databanks.length; i++) {
                var databank = databanks[i];
                if ($rootScope.activeDatabankTab.title == databank.name) {
                    if (databank.records > newValue) {
                        $rootScope.showConfirm(L.tsq("Records limit reached"),
                            L.tsq("Max records is set to a number lower than the current number of records in the databank. <br/><br/>Do you want to delete the overabundant records? If you click No, this change will be not applied."),
                            function (confirmed) {
                                if (confirmed) {
                                    changeMaxRecords(newValue, oldValue);
                                }
                                else {
                                    disableWatch = true;
                                    $scope.config.maxStrategies = oldValue;
                                }
                            },
                            true
                        );
                    }
                    else {
                        changeMaxRecords(newValue, oldValue);
                    }
                    break;
                }
            }
        });
    }

    function checkProjectRunning() {
        if ($rootScope.project.state == $scope.projectStates.running) {
            return true;
        }
        return false;
    }

    function changeMaxRecords(newValue, oldValue) {
        onSettingsChange();

        RankingService.onMaxStrategiesChanged(newValue, function (result) {
            if (result.showProgress) {
                $rootScope.setProgressInfo(L.tsq("Changing max records value..."), L.tsq("Deleting overabundant results..."));
            }
        });
    }

    $scope.selectAllStrategyProblems = function () {
        for (i = 0; i < $scope.config.strategyProblems.length; i++) {
            $scope.config.strategyProblems[i].dismiss = $scope.config.strategyProblemsAll;
        }
    }

    function initFitPortfolio() {
        loadToArray($scope.correlationTypes, SQConstants.getConstants().correlationTypes);
        loadToArray($scope.correlationPeriods, SQConstants.getConstants().correlationPeriods);

        if ($scope.correlationTypes.length > 0) {
            var type = getItem($scope.correlationTypes, 'value', 'ProfitLoss');
            $scope.config.fitPortfolio.correlation.type = type ? type.value : $scope.correlationTypes[0].value;
        }

        if ($scope.correlationPeriods.length > 0) {
            var period = getItem($scope.correlationPeriods, 'value', 10);
            $scope.config.fitPortfolio.correlation.period = period ? period.value : $scope.correlationPeriods[0].value;
        }

        reloadFitPortfolioDatabanks();
    }

    $scope.getCorrelationPeriodLabel = function () {
        var period = getItem($scope.correlationPeriods, 'value', $scope.config.fitPortfolio.correlation.period);
        return period ? period.name : '';
    }

    $scope.getCorrelationTypeLabel = function () {
        var type = getItem($scope.correlationTypes, 'value', $scope.config.fitPortfolio.correlation.type);
        return type ? type.name : '';
    }

    $scope.getCAMethodLabel = function() {
        var method = getItem($scope.config.caAvailableMethods, "value", $scope.config.caMethod);
        return method ? method.name : '';
    }

    $scope.onConfigureFitPortfolioCorrelation = function () {
        showPopup("#fitPortfolioCorrelationPopup");
    }

    $scope.printFitPortfolioCorrelation = function () {
        var type = getItem($scope.correlationTypes, 'value', $scope.config.fitPortfolio.correlation.type);
        var typeText = type ? type.name : 'NA';

        var period = getItem($scope.correlationPeriods, 'value', $scope.config.fitPortfolio.correlation.period);
        var periodText = period ? period.name : 'NA';

        return L.tsq("%f, %s by %s", [$scope.config.fitPortfolio.correlation.max, typeText, periodText]);
    }

    function reloadFitPortfolioDatabanks() {
        if (AppService.isTaskManager()) {
            DatabankService.loadVisibleDatabanks($scope.fitPortfolioDatabanks);
        } else {
            $scope.fitPortfolioDatabanks.push({ name: SQConstants.getConstants().gedatabanks.existingPortfolio });
        }
    }


    //- Event Handlers and Watches --------------------------------------------------------------

    $scope.tab.shouldSaveSettings = function () {
        var shouldSaveSettings = settingsChanged;
        settingsChanged = false;
        return shouldSaveSettings;
    }

    function onSettingsChange() {
        settingsChanged = true;
        RankingService.saveConfig();
    }

    $scope.settingsChanged = function () {
        onSettingsChange();
    }

    $scope.printAutomaticDismissalLabel = function () {
        var count = 0;

        if ($scope.config.strategyProblems) {
            for (i = 0; i < $scope.config.strategyProblems.length; i++) {
                if ($scope.config.strategyProblems[i].dismiss) {
                    count++;
                }
            }
        }

        if (!count) {
            return L.tsq("No automatic filters on");
        }

        return count == 1 ? L.tsq("%d automatic filter on", [count]) : L.tsq("%d automatic filters on", [count]);
    }

    var destroyMaxStrWatch = $scope.$watch('config.maxStrategies', function (newValue, oldValue) {
        if (initialized) {
            if (disableWatch) {
                disableWatch = false;
                return;
            }

            onMaxRecordsChange(newValue, oldValue);
        }
    });

    var destroyStopTypeWatch = $scope.$watch('config.stopCondition', function (newValue, oldValue) {
        if (initialized) onSettingsChange();
    }, true);

    var destroyStrProblemsWatch = $scope.$watch("config.strategyProblems", function (newValue, oldValue) {
        if (initialized) {
            onSettingsChange();
        }
    }, true);

    function onEvent(event, data) {
        switch (event) {
            case SQEvents.get('SETTINGS_TAB_RELOAD'):
                if ($scope.tab.display && (data == $scope.tab.title || data == 'all')) {
                    initialized = false;
                    init();
                }
                break;
            case SQEvents.get('SETTINGS_TAB_ACTIVE'):
                var active = data.title == $scope.tab.title;
                if (active) {
                    if (!initialized) {
                        init();
                    }
                }
                watchersHandler.onActivated(active);
                return;
            case SQEvents.get('DATABANK_UPDATED'):
                if (data.databanks && data.databanks.length) {
                    var inputDb = getItem(data.databanks, 'name', RankingService.inputDatabankName);
                    if (inputDb) {
                    }
                }
                break;
            case SQEvents.get('OPTIMIZATION_TYPE_CHANGED'):
                $scope.config.optimizationType = data;
                break;

            case SQEvents.get('OPTIMIZATION_SIMPLE_TYPE_CHANGED'):
                $scope.config.simpleOptimizationType = data;
                break;

            case SQEvents.get('DATABANK_DISPLAY'):
                if (data.improveAllFromDatabank !== undefined) {
                    $scope.improveAllFromDatabank = data.improveAllFromDatabank;
                }
                break;
            case SQEvents.get('RELOAD_DATABANKS'):
                reloadFitPortfolioDatabanks();
                break;

            default: return;
        }
        try { $scope.$digest(); } catch (er) { }
    }

    $scope.$on("$destroy", function () {
        destroyMaxStrWatch();
        destroyStopTypeWatch();
        destroyStrProblemsWatch();
        SQEvents.removeListener(listenerId);
    });

    //- Initialization --------------------------------------------------------------

    var initialized = false;
    var loadedCG = false;
    var loadedFF = false;
    var loadedWFCG = false;

    var settingsChanged = false;

    var disableWatch = false;

    var cgInitDeferred = $q.defer();
    var ffInitDeferred = $q.defer();
    var cgWFInitDeferred = $q.defer();

    $scope.projectStates = RankingService.projectStates;
    $scope.optimizationTypes = RankingService.optimizationTypes;
    $scope.simpleOptimizationTypes = RankingService.simpleOptimizationTypes;
    $scope.stopConditionTypes = RankingService.stopConditionTypes;
    $scope.generationTypes = RankingService.generationTypes;

    $scope.config = RankingService.config;

    $scope.improveAllFromDatabank = true;

    $scope.chooser = {};
    $scope.categories = [
        { name: 'category1', options: [{ name: 'item1' }, { name: 'item2' }] },
        { name: 'category2', options: [{ name: 'item3' }, { name: 'item4' }] }
    ];

    $scope.isDatabankInput = false;

    $scope.wfRows = [2, 3, 4, 5];
    $scope.wfColumns = [2, 3, 4, 5];
    $scope.wfCombinations = [];
    for (i = 0; i <= 25; i++) {
        $scope.wfCombinations.push(i);
    }

    $scope.correlationTypes = [];
    $scope.correlationPeriods = [];
    $scope.fitPortfolioDatabanks = [];

    var listenerId = "RankingCtrl";
    SQEvents.addListener(listenerId, [
        SQEvents.get('SETTINGS_TAB_RELOAD'),
        SQEvents.get('SETTINGS_TAB_ACTIVE'),
        SQEvents.get('DATABANK_UPDATED'),
        SQEvents.get('OPTIMIZATION_TYPE_CHANGED'),
        SQEvents.get('OPTIMIZATION_SIMPLE_TYPE_CHANGED'),
        SQEvents.get("DATABANK_DISPLAY"),
        SQEvents.get("RELOAD_DATABANKS")
    ], onEvent);

    var watchersHandler = new WatchersHandler($($element));

    $q.all([cgInitDeferred.promise, cgWFInitDeferred.promise]).then(function () {
        $timeout(function () { watchersHandler.onActivated(false); }, 0, false);
    });

    $scope.tab.title = (window.appConfig.task.type == 'Build' || window.appConfig.task.type == 'Retest') ? L.tsq('Ranking & Filtering') : L.tsq('Ranking');
});

