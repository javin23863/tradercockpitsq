angular.module('app.settings').controller('SimpleCreatePortfolioSettingsCtrl', function ($rootScope, $scope, sqPlugin, $timeout, AppService, SQEvents, SettingsCreatePortfolioService, L) {
    console.log("SimpleCreatePortfolioSettings controller initialized");

    $scope.settingsChanged = function () {
        SettingsCreatePortfolioService.saveSettings();
        AppService.updateTaskXML();
    }

    $scope.showAdvancedSettings = function (event) {
        AppService.switchToPanel('settings');
        event.stopPropagation();
        $timeout(tabsShowHiddenTabs, 0, false);
    }

    $scope.config = SettingsCreatePortfolioService.config;
});