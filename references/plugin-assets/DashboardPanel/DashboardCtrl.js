angular.module('app.dashboard').controller('DashboardCtrl', function ($scope, sqPlugin, AppService, SQEvents, SQConstants, L) {
    console.log("Dashboard controller initialized");

    var appConfig = AppService.getAppConfig();
    $scope.appPlugins = SQConstants.getConstants().appPlugins;

    $scope.disabledForSpecialTrial = false;

    $('.sq-app-taskmanager').removeClass('sqn-hide-header');

    function init() {
        //load panels
        var panels = sqPlugin.getPlugins("MainPanel");

        for (var i = 0; i < panels.length; i++) {
            var panel = panels[i];

            if (panel.product && panel.product != appConfig.product) {
                continue;
            }

            $scope.mainPanels.push(panel);
        }
    }

    function disabledForSpecialTrial(license) {
        var plugin = getItem($scope.appPlugins, 'appCode', appConfig.product);
        if (plugin) {
            $scope.disabledForSpecialTrial = (plugin.disabledForSpecialTrial && license?.isSpecialTrial);
            $scope.disabledForSpecialTrialTitle = L.tsq("%s is available only in full version, not in this special trial.", [appConfig.productName]);
        }
    }

    function onEvent(event, data) {
        if (event == SQEvents.get('APP_PANEL_CHANGED')) {
            dashboardActive = data == 'dashboard';
        }
        else if (event == SQEvents.get('SETTINGS_TAB_ACTIVE')) {
        }
        else if (event == SQEvents.get('SETTINGS_CLOSE')) {
        }
        else if (event == SQEvents.get('LICENSE_CHANGED')) {
            disabledForSpecialTrial(data);
        }
        else return;

        for (var i = 0; i < $scope.mainPanels.length; i++) {
            var panel = $scope.mainPanels[i];
            if (panel.name == "settings") {
                panel.class.active = settingsActive;
            }
            else {
                panel.class.active = dashboardActive && !settingsActive;
            }
        }

        try { $scope.$digest(); } catch (err) { }
    }

    var listenerId = "DashboardCtrl";
    SQEvents.addListener(listenerId, [
        SQEvents.get('APP_PANEL_CHANGED'),
        SQEvents.get('SETTINGS_TAB_ACTIVE'),
        SQEvents.get('SETTINGS_CLOSE'),
        SQEvents.get('LICENSE_CHANGED')
    ], onEvent);

    $scope.$on('$destroy', function () {
        SQEvents.removeListener(listenerId);
    });

    $scope.mainPanels = [];
    $scope.projectName = AppService.getProject();

    $scope.isPMDashboard = AppService.isPortfolioMaster();

    var dashboardActive = true;
    var settingsActive = false;

    init();

});