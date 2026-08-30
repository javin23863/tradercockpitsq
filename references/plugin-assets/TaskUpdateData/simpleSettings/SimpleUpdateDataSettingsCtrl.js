angular.module('app.settings').controller('SimpleUpdateDataSettingsCtrl', function ($rootScope, $scope, sqPlugin, $timeout, AppService, SQEvents, L, SettingsUpdateDataService) {
    console.log("SimpleUpdateDataSettings controller initialized");

    $scope.settingsChanged = function() {
        SettingsUpdateDataService.saveSettings();
        AppService.updateTaskXML();
    }

    $scope.showAdvancedSettings = function (event) {
        AppService.switchToPanel('settings');
        event.stopPropagation();
        $timeout(tabsShowHiddenTabs, 0, false);
    }

    $scope.config = SettingsUpdateDataService.config;
});