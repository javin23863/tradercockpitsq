angular.module('app.settings').controller('SimpleCallExternalScriptSettingsCtrl', function ($rootScope, $scope, sqPlugin, $timeout, AppService, SQEvents, SettingsCallExternalScriptService, L) {
    console.log("SimpleCallExternalScriptSettings controller initialized");

    $scope.settingsChanged = function() {
        SettingsCallExternalScriptService.saveSettings();
        AppService.updateTaskXML();
    }

    $scope.showAdvancedSettings = function (event) {
        AppService.switchToPanel('settings');
        event.stopPropagation();
        $timeout(tabsShowHiddenTabs, 0, false);
    }

    $scope.openFilePicker = function () {
        $rootScope.showFilePicker(L.tsq('Select file'), "CallExternalScriptFile", true, false, $scope.config.file, null, null, function (paths) {
            if (paths) {
                $scope.config.file = paths[0];
                $scope.settingsChanged();
                
                try { $scope.$digest(); } catch(er) {}
            }
        });
    }

    $scope.config = SettingsCallExternalScriptService.config;
});