angular.module('app.resultstabs.strategyconfig').controller('StrategyConfigCtrl', function($rootScope, $scope, $timeout, BackendService, SQEvents, SQConstants, AppService, StrategyConfigService, $q, SQResultsData, $element, L) {
    console.log("StrategyConfig controller initialized");

    $scope.isTaskManager = false;
    $scope.isAlgoWizardCloud = false;
    $scope.taskName = null;
    $scope.taskType = null;

    $scope.list1 = {};

    $scope.settings = [];

    $scope.tasks = StrategyConfigService.tasks;
    $scope.applyTo = null;

    $scope.strategyName = null;
    $scope.blocksLeftCss = 0;

    $(window).resize(function() {
        reflowOnResize();
    });

    $scope.getTaskLabel = function() {
        var task = getItem($scope.tasks, "type", $scope.applyTo);
        return task ? task.title : '';
    }

    $scope.expandAll = function() {
        if (!checkDirectiveReady($scope.list1, 'StrategyConfigList.list1', 'expandAll')) {
            return;
        }
        $scope.list1.expandAll();
    }

    $scope.collapseAll = function() {
        if (!checkDirectiveReady($scope.list1, 'StrategyConfigList.list1', 'collapseAll')) {
            return;
        }
        $scope.list1.collapseAll();
    }

    function refreshContent(strategyChanged, handlerContent) {
        var dataInfo = SQResultsData.getDataInfo();
        $scope.isTaskManager = dataInfo.isTaskManager;
        $scope.isAlgoWizardCloud = dataInfo.isAlgoWizardCloud;
        $scope.taskName = dataInfo.taskName;
        $scope.taskType = dataInfo.taskType;
        $scope.strategyName = dataInfo.strategyName;

        if (!$scope.tab.active) return;

        if (!$scope.strategyName) return;
        StrategyConfigService.loadTasks($scope.isTaskManager, dataInfo.projectName, function() {
            var task = getItem($scope.tasks, "type", $scope.isTaskManager ? dataInfo.taskName : dataInfo.product);
            if (task) {
                $scope.applyTo = task.type;
            } else {
                $scope.applyTo = $scope.tasks.length > 0 ? $scope.tasks[0].type : null;
            }
        });

        SQResultsData.getData(handlerId, [dataInfo.taskType, dataInfo.taskName], true).then(function(result) {
            if (!result.changed) return;

            if (result.data.error) {
                print([]);
                $scope.strategyName = null;
                try { $scope.$digest(); } catch (err) {}
                return;
            }
            
            print(result.data.settings);

            try { $scope.$digest(); } catch (err) {}

            reflowOnResize();
        });
    }

    function fetchData(params) {
        var deferred = $q.defer();

        if (params.projectName == "AlgoWizard" && params.databankName == "AlgoWizard") {
            try {
                if (!params.isAlgoWizardCloud) {
                    var awWindow = window.top;
                    awWindow = awWindow.document.getElementById('algowizard-frame').contentWindow;
                    params.settingsXML = awWindow.eaWizardApp.generateBacktestSettingsXml();
                } else {
                    window.top.generateBacktestSettingsXml().then(result => {
                        console.error(result);
                        params.settingsXML = result;
                    });
                }


            } catch (err) {
                console.error("Cannot get AW settings XML - " + err);
            }
        }

        StrategyConfigService.print(params, function(result) {
            deferred.resolve(result);
        });

        return deferred.promise;
    }

    function print(data) {
        loadToArray($scope.settings, data);

        if (!checkDirectiveReady($scope.list1, 'StrategyConfigList.list1', 'print')) {
            return;
        }

        $scope.list1.print(data);
    }

    $scope.onApplyStrategyConfig = function() {
        if (!$scope.applyTo) {
            $rootScope.showError(L.tsq("No target chosen for applying settings."));

            return;
        }

        $rootScope.showConfirm(
            L.tsq("Apply strategy config"),
            "Do you really want to load the settings from the strategy?<br>This will overwrite your '" + $scope.applyTo + "' config.",
            function(confirmed) {
                if (confirmed) {
                    applyStrategyConfig();
                }
            },
            true
        );
    }

    function applyStrategyConfig() {
        var dataInfo = SQResultsData.getDataInfo();

        var data = {};
        data.projectName = dataInfo.projectName;
        data.databankName = dataInfo.databankName;
        data.strategyName = dataInfo.strategyName;
        if (dataInfo.isAlgoWizardCloud || dataInfo.isAlgoWizard) data.backtestID = dataInfo.backtestID;

        StrategyConfigService.loadStrategySettings(data, function(result) {
            var params = {
                taskName: $scope.applyTo,
                config: result.xml,
                callback: refreshContent
            };

            if (result.resourcesXML) {
                $rootScope.resolveResources(result.xml, result.resourcesXML, SQConstants.getConstants().projectResourceActionTypes.applyStrategyConfig, function(callbackData) {
                    if (callbackData.taskXML) {
                        params.config = callbackData.taskXML;
                        applyConfig(params);
                    } else if (callbackData.loadAsIs) {
                        params.config = result.xml;
                        applyConfig(params);
                    }
                });
            } else {
                applyConfig(params);
            }
        });
    }

    function applyConfig(params) {
        var product;

        if ($scope.isTaskManager) {
            product = "TASKMANAGER";

            $rootScope.showSuccess("Strategy config was applied to task '" + params.taskName + "'");
        } else {
            product = $scope.applyTo;
            params.taskName = getItem($scope.tasks, 'type', product).taskName;

            $rootScope.showSuccess("Strategy config was applied to project '" + product + "'");
        }

        AppService.sendActionRequest(product, "loadConfig", params);
    }

    function reflowOnResize() {
        $timeout(function() {
            var windWidth = $(window).width();
            var containerNumberWidth = (Math.floor(windWidth / 366) - 1) * 366;
            var blocksOuterHeight = $('.strategy-config-panel.strategy-config-panel-blocks').height();
            $('.strategy-config-container').width(containerNumberWidth);
            $('.strategy-config-container').css('min-height', blocksOuterHeight + 'px');
            $('.strategy-config-panel-blocks').css('left', containerNumberWidth + 'px');
        }, 0, false);
    }

    var handlerId = $scope.tab.title; //handler id must be set to tab title
    var handler = SQResultsData.addHandler(handlerId, fetchData, refreshContent, ['taskType', 'taskName']);

    var watchersHandler = new WatchersHandler($($element));
    $timeout(function() {
        watchersHandler.onActivated(false);
        SQResultsData.resultsTabLoaded("StrategyConfigCtrl");
    }, 0, false);

    $scope.tab.tabChanged = function() {
        watchersHandler.onActivated($scope.tab.active);
    }
});