angular.module('app.settings').controller('SettingsCreatePortfolioCtrl', function ($rootScope, $scope, $timeout, $q, $element, SettingsCreatePortfolioService, AppService, SQConstants, SQEvents, L) {
    console.log("SettingsCreatePortfolio controller initialized");

    function init() {
        loaded = false;

        SettingsCreatePortfolioService.loadSettings();

        $timeout(function () { loaded = true; }, 0, false);
    }

    $scope.tab.shouldSaveSettings = function () {
        var shouldSaveSettings = settingsChanged;
        settingsChanged = false;
        return shouldSaveSettings;
    }

    $scope.settingsChanged = function () {
        if (!loaded) return;

        settingsChanged = true;

        SettingsCreatePortfolioService.saveSettings();
    }

    function onEvent(event, data) {
        if (event == SQEvents.get('SETTINGS_TAB_ACTIVE')) {
            var active = data.title == $scope.tab.title;
            console.log("active", active, data.title, $scope.tab.title)
            watchersHandler.onActivated(active);

        } else if (event == SQEvents.get('SETTINGS_TAB_RELOAD')) {
            if ($scope.tab.display && (data == $scope.tab.title || data == 'all')) {
                init();
            }
        }
        else return;

        try { $scope.$digest(); } catch (er) { }
    }

    $scope.$on("$destroy", function () {
        SQEvents.removeListener(listenerId);
    });

    //- Initialization --------------------------------------------------------------

    var loaded = false;

    var settingsChanged = false;

    $scope.config = SettingsCreatePortfolioService.config;

    var listenerId = "SettingsCreatePortfolioCtrl";
    SQEvents.addListener(listenerId, [SQEvents.get('SETTINGS_TAB_ACTIVE'), SQEvents.get('SETTINGS_TAB_RELOAD')], onEvent);

    var watchersHandler = new WatchersHandler($($element));
    $timeout(function () { watchersHandler.onActivated(false); }, 0, false);
});