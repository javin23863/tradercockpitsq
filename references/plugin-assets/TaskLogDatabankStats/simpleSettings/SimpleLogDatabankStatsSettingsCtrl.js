angular.module('app.settings').controller('SimpleLogDatabankStatsSettingsCtrl', function ($rootScope, $scope, sqPlugin, $timeout, AppService, SQEvents, L, LogDatabankStatsService ) {
    console.log("SimpleLogDatabankStatsSettings controller initialized");

    $scope.showAdvancedSettings = function (event) {
        AppService.switchToPanel('settings');
        event.stopPropagation();
        $timeout(tabsShowHiddenTabs, 0, false);
    }

    $scope.config = LogDatabankStatsService.config;
});