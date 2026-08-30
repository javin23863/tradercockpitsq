angular.module('app.settings').controller('SettingsLoadFromFilesCtrl', function ($rootScope, $scope, $timeout, $q, $element, SettingsLoadFromFilesService, AppService, SQConstants, SQEvents, L) {
    console.log("SettingsLoadFromFiles controller initialized");

    function init() {
        loaded = false;

        SettingsLoadFromFilesService.loadSettings();

        $timeout(function () {
            loaded = true;
        });
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

    $scope.tab.shouldSaveSettings = function(){
        var shouldSaveSettings = settingsChanged;
        settingsChanged = false;
        return shouldSaveSettings;
    }

    $scope.settingsChanged = function() {
        if(!loaded) return;
        
        settingsChanged = true;

        SettingsLoadFromFilesService.saveSettings();
    }

    function onEvent(event, data) {
        if(event == SQEvents.get('SETTINGS_TAB_ACTIVE')){
            var active = data.title == $scope.tab.title;
            watchersHandler.onActivated(active);

            if(active) SettingsLoadFromFilesService.loadDatabanks();
        
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
    
    $scope.config = SettingsLoadFromFilesService.config;    
    $scope.instruments = SettingsLoadFromFilesService.instruments;

    var listenerId = "SettingsLoadFromFilesCtrl";
    SQEvents.addListener(listenerId, [SQEvents.get('SETTINGS_TAB_ACTIVE'), SQEvents.get('SETTINGS_TAB_RELOAD')], onEvent);

    var watchersHandler = new WatchersHandler($($element));
    $timeout(function(){ watchersHandler.onActivated(false); }, 0, false);
});