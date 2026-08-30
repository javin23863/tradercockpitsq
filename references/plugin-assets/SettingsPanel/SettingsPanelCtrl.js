angular.module('app.dashboard').controller('SettingsPanelCtrl', function ($rootScope, $scope, sqPlugin, $timeout, AppService, SQEvents, L) {
    console.log("SettingsPanel controller initialized");

    function loadSettingsTabs() {
        var firstSelected = false;

        $timeout(function () {
            for (var i = 0; i < plugins.length; i++) {
                var plugin = plugins[i];

                if (plugin.customProjectsOnly && !isTaskManager) continue;
                if (plugin.onlyInPro && !$rootScope.license.isProVersion) continue;

                if (checkTab(plugin.task, task.type) || isTaskManager) {
                    plugin.active = !firstSelected;
                    plugin.currentText = "";
                    plugin.translatedTitle = L.tsq(plugin.title);
                    plugin.display = true;

                    firstSelected = true;

                    $scope.settingsTabs.tabs.push(plugin);
                }
            }

            try {
                $scope.$digest();
            } catch (err) {}
        }, 0, false);
    }

    function displaySettingsTabs(callback) {
        let tabs = "";

        if(!$scope.settingsTabs) return;

        var activeTab = null;

        for (var i = 0; i < $scope.settingsTabs.tabs.length; i++) {
            var tab = $scope.settingsTabs.tabs[i];

            if (checkTab(tab.task, task.type)) {
                if (!activeTab) {
                    activeTab = tab.dataItem;
                }

                tabs += tab.title + ",";
                showTab(tab.dataItem);
            } else {
                hideTab(tab.dataItem);
            }
        }

        if (activeTab) {
            $scope.settingsTabs.selectTab(activeTab);
        }

        try {
            $scope.$digest();
        } catch (err) {}

        if (callback) callback();
    }

    function refreshProTabs() {
        if(!$scope.settingsTabs) return;

        for (var i = 0; i < plugins.length; i++) {
            var plugin = plugins[i];

            if (plugin.customProjectsOnly && !isTaskManager) continue;
            if (!checkTab(plugin.task, task.type) || !isTaskManager) continue;

            var tabIndex = -1;
            for (var t = $scope.settingsTabs.tabs.length - 1; t >= 0; t--) {
                var tab = $scope.settingsTabs.tabs[t];
                if (tab.dataItem == plugin.dataItem) {
                    tabIndex = t;
                    break;
                }
            }

            if (tabIndex >= 0) {
                if (plugin.onlyInPro && !$rootScope.license.isProVersion) {
                    $scope.settingsTabs.tabs.splice(tabIndex, 1);
                }
            } else {
                if (plugin.onlyInPro && $rootScope.license.isProVersion) {
                    $scope.settingsTabs.tabs.push(plugin);
                }
            }
        }

        //select first visible tab
        var activeTab = null;

        for (var t = 0; t < $scope.settingsTabs.tabs.length; t++) {
            var tab = $scope.settingsTabs.tabs[t];

            if(!activeTab && tab.display) {
                activeTab = tab;
                tab.active = true;
            } else {
                tab.active = false;
            }
        }
    }

    function checkTab(pluginTask, type) {
        var plugins = pluginTask.split(",");
        return plugins.indexOf(type) >= 0;
    }

    function afterSettingsTabsInit() {
        settingsTabsLoaded = true;
    }

    function onSelectTab(activeTab) {
        SQEvents.notifyListeners(SQEvents.get('SETTINGS_TAB_ACTIVE'), {
            title: activeTab ? activeTab.title : null
        });
    }

    $scope.onPrevious = function () {
        if ($scope.settingsTabs) {
            $scope.settingsTabs.switchToPrevTab();
        }
    }

    $scope.onNext = function () {
        if ($scope.settingsTabs) {
            $scope.settingsTabs.switchToNextTab();
        }
    }

    function showTab(dataItem) {
        var tab = getItem($scope.settingsTabs.tabs, 'dataItem', dataItem)
        if (!tab) {
            return;
        }

        tab.display = true;
    }

    function hideTab(dataItem) {
        var tab = getItem($scope.settingsTabs.tabs, 'dataItem', dataItem)
        if (!tab) {
            return;
        }

        tab.display = false;
    }

    function onEvent(event, data) {
        if (event == SQEvents.get('SETTINGS_TAB_SHOW')) {
            if (data.show) {
                showTab(data.tab);
            } 
            else {
                hideTab(data.tab);
            }
        } 
        else if (event == SQEvents.get('SETTINGS_TASK_CHANGED')) {
            if (!AppService.getTask().type) { //project changed, no task selected yet
                $scope.taskSelected = false;
                return;
            }

            $scope.taskSelected = true;

            displaySettingsTabs($scope.settingsTabs.taskChanged);
            return;
        } 
        else if(event == SQEvents.get("TASK_REMOVED")){
            if(isTaskManager){
                //after removing task, first task gets selected and SETTINGS_TASK_CHANGED is called so no additional checks are needed here
                $scope.taskSelected = false;
            }
        }
        else if (event == SQEvents.get('LICENSE_CHANGED')) {
            refreshProTabs();
            return;
        } 
        else return;

        try {
            $scope.$digest();
        } catch (err) {}
    }

    $scope.$on('$destroy', function () {
        SQEvents.removeListener(listenerId);
    });

    var task = AppService.getTask();
    var plugins = sqPlugin.getPlugins("SettingsTab");

    $scope.settingsTabs = {
        tabs: [],
        onSelectTab: onSelectTab,
        afterInit: afterSettingsTabsInit,
        idPrefix: "settings-tabs",
    }

    loadSettingsTabs();

    var isTaskManager = AppService.isTaskManager();
    
    $scope.taskSelected = !isTaskManager;

    var listenerId = "SettingsPanelCtrl";
    SQEvents.addListener(listenerId, [
        SQEvents.get('SETTINGS_TAB_SHOW'),
        SQEvents.get('SETTINGS_TASK_CHANGED'),
        SQEvents.get("TASK_REMOVED"),
        SQEvents.get('LICENSE_CHANGED')
    ], onEvent);

});