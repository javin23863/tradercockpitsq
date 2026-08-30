angular.module('app.settings').controller('SimpleBuildSettingsCtrl', function ($rootScope, $scope, $timeout, AppService, SQEvents, SQConstants, BuildTaskService, L, WhatToBuildService, DatabankService, SettingsService) {
    console.log("SimpleBuildSettings controller initialized");

    function loadSettings() {
        initialized = false;

        //BuildTaskService.loadSettings();

        $timeout(function () {
            initialized = true;
            $('[data-toggle="tooltip"]').tooltip({
                "trigger": "hover",
            });
        }, 0, false);
    }

    function onEvent(event, data) {
        if (event == SQEvents.get('TASK_INFO_UPDATE')) {
            if (data && data.simpleSettings) {
                var taskObj = getTask(AppService.getProjectConfig(), AppService.getTask());
                taskObj.setAttribute("sampleName", data.simpleSettings.strategyType);

                $scope.template.current = data.simpleSettings.strategyType;
            }
            
            $scope.settingsPanelInstance.reload();
        }
        else if (event ==  SQEvents.get("APP_PANEL_CHANGED") && data=='dashboard') {
            $scope.settingsPanelInstance.reload();
        }
        else if (event == SQEvents.get("RELOAD_DATABANKS")) {
            DatabankService.loadVisibleDatabanks($scope.availableDatabanks);
        }
        else return;

        try {
            $scope.$digest();
        } catch (err) { }
    }

    $scope.isDashboard = function(){
        return AppService.getActivePanel() == "dashboard"
    }

    $scope.getStrategyTypeInfo = function (selectedType) {
        return BuildTaskService.getStrategyTypeInfo(selectedType);
    }

    $scope.loadTemplate = function (type) {
        SettingsService.applySimpleTemplate(type);
    }

    $scope.onSettingsChange = function () {
        BuildTaskService.saveSettings($scope.config);
    }

    $scope.showAdvancedSettings = function (event) {
        AppService.switchToPanel('settings');
        event.stopPropagation();
        $timeout(tabsShowHiddenTabs, 0, false);
    }

    $scope.onDatabankChange = function () {
        DatabankService.setTaskOutputDatabank($scope.configWTB.outputDatabank);
        AppService.updateTaskXML();
    }

    $scope.$on("$destroy", function () {

    });

    var initialized = false;

    $scope.settingsPanelInstance = {};

    $scope.config = BuildTaskService.config;
    $scope.configRanking = BuildTaskService.configRanking;
    $scope.configWTB = WhatToBuildService.config;
    $scope.projectStates = SQConstants.getConstants().runningStatuses;

    $scope.buildOptions = [
        {
            title: ""/* L.tsq('Build options') */,
            settings: [
                { name: L.tsq("What to build"), configElemName: "WhatToBuild" },
                { name: L.tsq("Building blocks"), configElemName: "Blocks" },
                { name: L.tsq("Trading options"), configElemName: "Options" },
                { name: L.tsq("Money Management"), configElemName: "RiskMoneyManagement" }
            ]
        }
    ];

    $scope.template = { current: SettingsService.getStrategyType() };
    $scope.strategyTypes = SQConstants.getConstants().simpleStrategyTypes;

    $scope.isBuilder = WhatToBuildService.isBuilder;
    $scope.availableDatabanks = WhatToBuildService.availableDatabanks;

    $scope.loaded = false;
    $scope.settingsPanelInstance = { onLoaded: function () { $scope.loaded = true; } };

    loadSettings();

    var listenerId = "SimpleBuildSettingsCtrl";
    SQEvents.addListener(listenerId, [SQEvents.get('TASK_INFO_UPDATE'), SQEvents.get("RELOAD_DATABANKS"), SQEvents.get("APP_PANEL_CHANGED")], onEvent);

});