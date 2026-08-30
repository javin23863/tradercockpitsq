angular.module('app.settings').controller('SettingsUpdateDataCtrl', function ($rootScope, $scope, $timeout, $q, $element, SettingsUpdateDataService, AppService, SQConstants, SQEvents, L) {
    console.log("SettingsUpdateData controller initialized");

    function init() {
        loaded = false;

        SettingsUpdateDataService.loadSettings();

        $timeout(function () {
            loaded = true;
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

        SettingsUpdateDataService.saveSettings();
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
    
    $scope.config = SettingsUpdateDataService.config;    

    var listenerId = "SettingsUpdateDataCtrl";
    SQEvents.addListener(listenerId, [SQEvents.get('SETTINGS_TAB_ACTIVE'), SQEvents.get('SETTINGS_TAB_RELOAD')], onEvent);

    var watchersHandler = new WatchersHandler($($element));
    $timeout(function(){ watchersHandler.onActivated(false); }, 0, false);
});