angular.module('app.settings').controller('SimpleStopAndStartSettingsCtrl', function ($rootScope, $scope, sqPlugin, $timeout, AppService, SQEvents, SettingsStopAndStartService, L) {
    console.log("SimpleStopAndStartSettingsCtrl controller initialized");

    $scope.onSettingsChange = function() {
        SettingsStopAndStartService.saveSettings();
    }

    $scope.showAdvancedSettings = function (event) {
        AppService.switchToPanel('settings');
        event.stopPropagation();
        $timeout(tabsShowHiddenTabs, 0, false);
    }

    $scope.getProjectLabel = function () {
        var template = getItem($scope.customProjects, 'value', $scope.config.projectName);
        return template ? template.name : '';
    }

    $scope.config = SettingsStopAndStartService.config;
    $scope.customProjects = SettingsStopAndStartService.customProjects;
    $scope.conditionsInstance = {};

});