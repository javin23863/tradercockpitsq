angular.module('app.settings').controller('SimpleSaveToFilesSettingsCtrl', function ($rootScope, $scope, sqPlugin, $timeout, AppService, SQEvents, SettingsSaveToFilesService, L) {
    console.log("SimpleSaveToFilesSettings controller initialized");

    $scope.settingsChanged = function () {
        SettingsSaveToFilesService.saveSettings();
        AppService.updateTaskXML();
    }

    $scope.showAdvancedSettings = function (event) {
        AppService.switchToPanel('settings');
        event.stopPropagation();
        $timeout(tabsShowHiddenTabs, 0, false);
    }

    $scope.openFilePicker = function (type) {
        var currentDir = null;

        if(type=='SaveToFilesSqx') currentDir = $scope.config.directorySqx;
        else if(type=='SaveToFilesStr') currentDir = $scope.config.directoryStr;
        else if(type=='SaveToFilesDatabank') currentDir = $scope.config.directoryDatabank;
        else if(type=='SaveToFilesTrades') currentDir = $scope.config.directoryTrades;
        else if(type=='SaveToFilesHtml') currentDir = $scope.config.directoryHtml;
        else if(type=='SaveToFilesPdf') currentDir = $scope.config.directoryPdf;
        else if(type=='SaveToFilesSC') currentDir = $scope.config.directorySC;

        $rootScope.showFilePicker(L.tsq('Select target folder'), type.replace('SaveToFiles', 'STF_'), false, false, currentDir, null, null, function (paths) {
            if (paths) {
                if(type=='SaveToFilesSqx') $scope.config.directorySqx = paths[0];
                else if(type=='SaveToFilesStr') $scope.config.directoryStr = paths[0];
                else if(type=='SaveToFilesDatabank') $scope.config.directoryDatabank = paths[0];
                else if(type=='SaveToFilesTrades') $scope.config.directoryTrades = paths[0];
                else if(type=='SaveToFilesHtml') $scope.config.directoryHtml = paths[0];
                else if(type=='SaveToFilesPdf') $scope.config.directoryPdf = paths[0];
                else if(type=='SaveToFilesSC') $scope.config.directorySC = paths[0];

                $scope.settingsChanged();

                try {
                    $scope.$digest();
                } catch (er) {}
            }
        });
    }

    $scope.config = SettingsSaveToFilesService.config;
});