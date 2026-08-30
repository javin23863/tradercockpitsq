angular.module('app.settings').controller('SimpleDeleteFileSettingsCtrl', function ($rootScope, $scope, sqPlugin, $timeout, AppService, SQEvents, SettingsDeleteFileService, L) {
    console.log("SimpleDeleteFileSettings controller initialized");

    $scope.settingsChanged = function() {
        SettingsDeleteFileService.saveSettings();
        AppService.updateTaskXML();
    }

    $scope.showAdvancedSettings = function (event) {
        AppService.switchToPanel('settings');
        event.stopPropagation();
        $timeout(tabsShowHiddenTabs, 0, false);
    }

    $scope.openFilePicker = function () {
        $rootScope.showFilePicker(L.tsq('Select file'), "DeleteFile", true, false, $scope.config.file, null, null, function (paths) {
            if (paths) {
                $scope.config.file = paths[0];
                $scope.settingsChanged();
                
                try { $scope.$digest(); } catch(er) {}
            }
        });
    }

    $scope.config = SettingsDeleteFileService.config;
});