angular.module('app.settings').controller('SettingsSaveToFilesCtrl', function ($rootScope, $scope, $timeout, $q, $element, SettingsSaveToFilesService, AppService, SQConstants, SQEvents, L) {
    console.log("SettingsSaveToFiles controller initialized");

    function init() {
        loaded = false;

        SettingsSaveToFilesService.loadSettings();

        $timeout(function () {
            loaded = true;
        });
    }

    $scope.getDataLabel = function() {
        var data = getItem($scope.dataTypes, "value", $scope.config.data);
        return data ? data.name : '';
    }

    $scope.changeFormat = function(format) {
        $scope.config.format = format;

        $scope.settingsChanged();
    }

    $scope.changeSourceCodeFormat = function(format) {
        $scope.config.sourceCodeFormat = format;

        $scope.settingsChanged();
    }

    //- Event Handlers --------------------------------------------------------------

    $scope.tab.shouldSaveSettings = function () {
        var shouldSaveSettings = settingsChanged;
        settingsChanged = false;
        return shouldSaveSettings;
    }

    $scope.settingsChanged = function () {
        if (!loaded) return;

        settingsChanged = true;

        SettingsSaveToFilesService.saveSettings();
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

    function onEvent(event, data) {
        if (event == SQEvents.get('SETTINGS_TAB_ACTIVE')) {
            var active = data.title == $scope.tab.title;
            watchersHandler.onActivated(active);

            if(active) SettingsSaveToFilesService.loadDatabanks();

        } else if (event == SQEvents.get('SETTINGS_TAB_RELOAD')) {
            if ($scope.tab.display && (data == $scope.tab.title || data == 'all')) {
                init();
            }
        } else return;

        try {
            $scope.$digest();
        } catch (er) {}
    }

    $scope.$on("$destroy", function () {
        SQEvents.removeListener(listenerId);
    });

    //- Initialization --------------------------------------------------------------

    $scope.dataTypes = [{value: 'all', name: L.tsq('All')}, {value: 'main', name: L.tsq('Main backtest')}]

    var loaded = false;

    var settingsChanged = false;

    $scope.config = SettingsSaveToFilesService.config;

    var listenerId = "SettingsSaveToFilesCtrl";
    SQEvents.addListener(listenerId, [SQEvents.get('SETTINGS_TAB_ACTIVE'), SQEvents.get('SETTINGS_TAB_RELOAD')], onEvent);

    var watchersHandler = new WatchersHandler($($element));
    $timeout(function () {
        watchersHandler.onActivated(false);
    }, 0, false);
    
});