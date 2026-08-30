angular.module('app.results').controller('ResultsCtrl', function ($rootScope, $scope, $injector, $timeout, sqPlugin, SQEvents, $stateParams, SQConstants, AppService, DatabankService, SQResultsData, ResultsPluginsService, L) {
    console.log("Results controller initialized");

    $('.sq-app-taskmanager').removeClass('sqn-hide-header');

    var lastSelectedTab = null; //last selected tab before delete

    $scope.isTaskManager = AppService.isTaskManager();
    $scope.isAW = isAlgoWizard() || window.parent.activeApp == 'AlgoWizard';
    $scope.isPortfolioComposer = isPortfolioComposer();
    $scope.strategyName = null;
    $scope.resultInfo = SQConstants.NO_RESULT_CHOSEN;
    $scope.broker = SQConstants.getConstants().broker;
    $scope.isBrazilianEdition = SQConstants.getConstants().isBrazilianEdition;

    $scope.report = null;
    initReportPage();

    var activeOverviewTab = null;

    $scope.resultsTabs = {
        tabs: loadTabs(),
        onSelectTab: onOverviewTabChange,
        afterInit: onOverviewTabsInit,
        idPrefix: "results-tabs",
        dropdownMenuId: "results-tabs-dropdown-menu",
        dropdownMenu: sqPlugin.getPlugins("CustomResultsPluginAction"),
    };

    $scope.hideMenu = function () {
        $("#" + $scope.resultsTabs.dropdownMenuId).removeClass("open");
    };

    if (!isPortfolioComposer() && !isAlgoWizard() && !isMTAnalyzer()) {
        $scope.resultsTabs.controls = [
            {
                title: L.tsq("+ New analysis"),
                class: "databanks-add-btn",
                iconClass: "",
                onClick: function () {
                    showAddCustomPluginModal();
                },
            },
        ];
    }

    if ($scope.resultsTabs.tabs.length > 0) {
        activeOverviewTab = $scope.resultsTabs.tabs[0];
    }

    function initReportPage() {
        let plugins = sqPlugin.getPlugins("ResultsReport");
        if(plugins.length>0) {
            $scope.report = plugins[0];
        }
    }

    /** Custom results tabs (isCustom === true) are shown after built-in tabs. */
    function sortTabsCustomLast(tabs) {
        return tabs.slice().sort(function (a, b) {
            var aCustom = !!a.isCustom;
            var bCustom = !!b.isCustom;
            if (aCustom === bCustom) return 0;
            return aCustom ? 1 : -1;
        });
    }

    function loadTabs() {
        try {
            if (isMTAnalyzer() || isAlgoWizard()) {
                var resultsTabsToShow = window.parent.appConfig.resultTabs;

                var tabs = [];
                var plugins = sqPlugin.getPlugins("ResultsTab");

                for (var i = 0; i < plugins.length; i++) {
                    var plugin = plugins[i];
                    if (resultsTabsToShow.includes(plugin.dataItem)) {
                        tabs.push(plugin);
                    }
                }

                return sortTabsCustomLast(tabs);
            }

            if(isPortfolioComposer()) {
                var tabs = [];
                var plugins = sqPlugin.getPlugins("ResultsTab");

                for (var i = 0; i < plugins.length; i++) {
                    var plugin = plugins[i];
                    if (plugin.dataItem == 'overview' ||plugin.dataItem == 'tradeList' || plugin.dataItem == 'equityChart'  || plugin.dataItem == 'portfolioComposerLog' || plugin.dataItem == 'portfolioComposerChart') {
                        plugin.hideAfterInit = false;
                        tabs.push(plugin);
                    }
                }

                return sortTabsCustomLast(tabs);
            }
        } catch (err) {
            console.error("Cannot read window.parent.appConfig.resultTabs. Showing all results tabs...");
        }
        
        return sortTabsCustomLast(sqPlugin.getPlugins("ResultsTab"));
    }

    function onOverviewTabChange(activeTab) {
        activeOverviewTab = activeTab;
        if (!activeOverviewTab) {
            return;
        }

        var dataInfo = SQResultsData.getDataInfo();
        if (dataInfo.strategyName) lastSelectedTab = activeOverviewTab.dataItem;

        //$scope.results object is created in parent controller
        if ($scope.results && $scope.results.onTabChange) {
            $scope.results.onTabChange(activeOverviewTab.title, activeOverviewTab.dataItem);
        } else {
            console.error("$scope.results not found", $scope);
        }
    }

    function onOverviewTabsInit() {
        var tabs = $scope.resultsTabs.tabs;
        for (var i = 0; i < tabs.length; i++) {
            $scope.resultsTabs.displayTab(tabs[i].dataItem, !tabs[i].hideAfterInit);
        }
    }

    //$scope.results object is created in parent controller
    $scope.results.selectTab = function (tabDataItem) {
        for (var i = 0; i < $scope.resultsTabs.tabs.length; i++) {
            var tab = $scope.resultsTabs.tabs[i];
            if (tab.dataItem == tabDataItem || (!tabDataItem && !i)) {
                tab.active = true;
            }
            else {
                tab.active = false;
            }
        }
    }

    $scope.backToEditor = function() {
        awShowResults(false, $injector);
    }

    function refreshContent(strategyChanged, params) {
        if (!strategyChanged) {
            return;
        }

        var dataInfo = SQResultsData.getDataInfo();
        var tabToSelect = params ? params.tab : null;

        dataInfo.noResultItems = dataInfo.noResultItems || [];

        $scope.strategyName = dataInfo.strategyName;
        $scope.resultInfo = dataInfo.resultInfo ? dataInfo.resultInfo : SQConstants.NO_RESULT_CHOSEN;

        var tabs = $scope.resultsTabs.tabs;

        for (var i = 0; i < tabs.length; i++) {
            tabs[i].active = tabToSelect ? tabs[i].dataItem == params.tab : tabs[i].active;

            if ($scope.strategyName) {
                $scope.resultsTabs.displayTab(tabs[i].dataItem, !dataInfo.noResultItems.includes(tabs[i].dataItem));
            } else {
                $scope.resultsTabs.displayTab(tabs[i].dataItem, !tabs[i].hideAfterInit);
            }
        }

        if (tabToSelect) {
            lastSelectedTab = tabToSelect;
        }

        if ($scope.strategyName) {
            var activeTab = $scope.resultsTabs.getActiveTab();
            if (activeTab && activeTab.dataItem != lastSelectedTab) {
                $scope.resultsTabs.selectTab(lastSelectedTab);
            }
        }

        try { $scope.$digest(); } catch (err) { }
    }

    SQResultsData.addHandler("strategy-changed", null, refreshContent, []);

    //Important! We must notify controller of active tab to reenable watchers
    if (window.parent.addListener) {
        window.parent.addListener("ResultsCtrl", [SQEvents.get('APP_SWITCHED')], function (event, data) {
            var activeTab = $scope.resultsTabs ? $scope.resultsTabs.getActiveTab() : null;
            if (activeTab && activeTab.tabChanged) {
                activeTab.tabChanged();
            }
        });
    }

    //--------------------------------------------------------------------------------------------------------
    // Custom Plugin Modal (use object so ng-model works with ng-include child scope)

    $scope.customPluginForm = { name: '' };

    function showAddCustomPluginModal() {
        $timeout(function () {
            $scope.customPluginForm.name = '';
            showPopup('#newCustomPluginModal');
            $timeout(function () {
                var el = document.querySelector('#newCustomPluginModal input');
                if (el) el.focus();
            }, 100);
        }, 0);
    }

    $scope.createCustomPlugin = function () {
        var name = ($scope.customPluginForm && $scope.customPluginForm.name || '').trim();
        if (!name) {
            $rootScope.showError(L.tsq('Custom plugin name is required.'));
            return;
        }
        
        ResultsPluginsService.createPlugin(name, function (result) {
            if (result && result.success) {
                $rootScope.showSuccess(result.success);
                hidePopup('#newCustomPluginModal');
                $scope.$emit('RELOAD_RESULTS');
            } else {
                $rootScope.showError((result && result.error) ? result.error : L.tsq('Failed to create custom analysis.'));
            }
        });
    };
});