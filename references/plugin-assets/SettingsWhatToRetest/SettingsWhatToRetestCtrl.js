angular.module('app.settings').controller('SettingsWhatToRetestCtrl', function ($rootScope, $scope, $timeout, $q, $element, AppService, SettingsWhatToRetestService, SQEvents) {

    $scope.onDatabankChange = function() {
        if(!loaded) return;
        onSettingsChange();
    }

    //- Event Handlers --------------------------------------------------------------

    $scope.tab.shouldSaveSettings = function(){
        var shouldSaveSettings = settingsChanged;
        settingsChanged = false;
        return shouldSaveSettings;
    }

    function onSettingsChange(){
        settingsChanged = true;
        SettingsWhatToRetestService.saveSettings();
    }

    function onEvent(event, data){
        if(event == SQEvents.get('SETTINGS_TAB_ACTIVE')){
            var active = data.title == $scope.tab.title;
            watchersHandler.onActivated(active);
        }
        else if(event == SQEvents.get('SETTINGS_TAB_RELOAD')){
            if($scope.tab.display && (data == $scope.tab.title || data == 'all')) {
                loaded = false;
                
                $timeout(function(){ 
                    SettingsWhatToRetestService.loadAvailableDatabanks(function(){
                        SettingsWhatToRetestService.loadSettings();
                        $timeout(function(){ loaded = true; }, 0, false);
                    });
                }, 0, false);
            }
        }
        else if (event == SQEvents.get('APP_PROJECT_CHANGED')) {
            loaded = false;
        }
        else return;
        
        try { $scope.$digest(); } catch(er) {}
    }
    
    $scope.$on("$destroy", function(){
        SQEvents.removeListener(listenerId);
    });

    //- Initialization --------------------------------------------------------------

    var settingsChanged = false;
    var loaded = false;

    $scope.config = SettingsWhatToRetestService.config;
    $scope.availableDatabanks = SettingsWhatToRetestService.availableDatabanks;

    var listenerId = "SettingsWhatToRetestCtrl";
    SQEvents.addListener(listenerId, [SQEvents.get('SETTINGS_TAB_ACTIVE'), SQEvents.get('SETTINGS_TAB_RELOAD'), SQEvents.get('APP_PROJECT_CHANGED')], onEvent);

    var watchersHandler = new WatchersHandler($($element));
    $timeout(function(){ watchersHandler.onActivated(false); loaded = true; }, 0, false);

});