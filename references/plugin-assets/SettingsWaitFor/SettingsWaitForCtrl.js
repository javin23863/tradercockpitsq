angular.module('app.settings').controller('SettingsWaitForCtrl', function ($rootScope, $scope, $timeout, $q, $element, SettingsWaitForService, AppService, SQConstants, SQEvents, L) {
    console.log("SettingsWaitFor controller initialized");

    function init() {
        loaded = false;

        SettingsWaitForService.loadSettings();

        $timeout(function () {
            loaded = true;
        });
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

    $scope.tab.shouldSaveSettings = function(){
        var shouldSaveSettings = settingsChanged;
        settingsChanged = false;
        return shouldSaveSettings;
    }

    $scope.settingsChanged = function() {
        if(!loaded) return;
        
        settingsChanged = true;

        SettingsWaitForService.saveSettings();
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
    
    $scope.config = SettingsWaitForService.config;    

    var listenerId = "SettingsWaitCtrl";
    SQEvents.addListener(listenerId, [SQEvents.get('SETTINGS_TAB_ACTIVE'), SQEvents.get('SETTINGS_TAB_RELOAD')], onEvent);

    var watchersHandler = new WatchersHandler($($element));
    $timeout(function(){ watchersHandler.onActivated(false); }, 0, false);
});