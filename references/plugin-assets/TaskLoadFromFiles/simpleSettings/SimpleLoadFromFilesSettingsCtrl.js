angular.module('app.settings').controller('SimpleLoadFromFilesSettingsCtrl', function ($rootScope, $scope, sqPlugin, $timeout, AppService, SQEvents, SettingsLoadFromFilesService, L) {
    console.log("SimpleLoadFromFilesSettings controller initialized");

    $scope.settingsChanged = function() {
        SettingsLoadFromFilesService.saveSettings();
        AppService.updateTaskXML();
    }

    $scope.showAdvancedSettings = function (event) {
        AppService.switchToPanel('settings');
        event.stopPropagation();
        $timeout(tabsShowHiddenTabs, 0, false);
    }

    $scope.openFilePicker = function () {
        $rootScope.showFilePicker(L.tsq('Select source folder'), "LoadFromFiles", false, false, $scope.config.directory, null, null, function (paths) {
            if (paths) {
                $scope.config.directory = paths[0];
                $scope.settingsChanged();
                
                try { $scope.$digest(); } catch(er) {}
            }
        });
    }

    $scope.config = SettingsLoadFromFilesService.config;
    $scope.instruments = SettingsLoadFromFilesService.instruments;
});