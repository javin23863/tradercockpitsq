angular.module('app.settings').controller('SettingsCallExternalScriptCtrl', function ($rootScope, $scope, $timeout, $q, $element, SettingsCallExternalScriptService, AppService, SQConstants, SQEvents, L) {
    console.log("SettingsCallExternalScript controller initialized");

    function init() {
        loaded = false;

        SettingsCallExternalScriptService.loadSettings();

        $timeout(function () {
            loaded = true;
        });
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

    $scope.tab.shouldSaveSettings = function(){
        var shouldSaveSettings = settingsChanged;
        settingsChanged = false;
        return shouldSaveSettings;
    }

    $scope.settingsChanged = function() {
        if(!loaded) return;
        
        settingsChanged = true;

        SettingsCallExternalScriptService.saveSettings();
    }

    function onEvent(event, data) {
        if(event == SQEvents.get('SETTINGS_TAB_ACTIVE')){
            var active = data.title == $scope.tab.title;
            watchersHandler.onActivated(active);
        
        } else if(event == SQEvents.get('SETTINGS_TAB_RELOAD')) {
            if($scope.tab.display && (data == $scope.tab.title || data == 'all')) {
                init();
            }
        }
        else return;
        
        try { $scope.$digest(); } catch(er) {}
    }
    
    $scope.$on("$destroy", function(){
        SQEvents.removeListener(listenerId);
    });

    //- Initialization --------------------------------------------------------------

    var loaded = false;
    
    var settingsChanged = false;
    
    $scope.config = SettingsCallExternalScriptService.config;    

    var listenerId = "SettingsCallExternalScriptCtrl";
    SQEvents.addListener(listenerId, [SQEvents.get('SETTINGS_TAB_ACTIVE'), SQEvents.get('SETTINGS_TAB_RELOAD')], onEvent);

    var watchersHandler = new WatchersHandler($($element));
    $timeout(function(){ watchersHandler.onActivated(false); }, 0, false);
});