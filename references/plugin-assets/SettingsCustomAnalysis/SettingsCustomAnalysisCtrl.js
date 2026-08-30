angular.module('app.settings').controller('SettingsCustomAnalysisCtrl', function ($rootScope, $scope, $timeout, $q, $element, SettingsCustomAnalysisService, AppService, SQConstants, SQEvents) {
    console.log("SettingsCustomAnalysis controller initialized");

    $scope.getMethodLabel = function(value) {
        var method = getItem($scope.config.availableMethods, "value", value);
        return method ? method.name : '';
    }

    function init() {
        loaded = false;

        SettingsCustomAnalysisService.loadSettings();

        $timeout(function () {
            loaded = true;
        });
    }
    
    //- Event Handlers --------------------------------------------------------------

    $scope.tab.shouldSaveSettings = function(){
        var shouldSaveSettings = settingsChanged;
        settingsChanged = false;
        return shouldSaveSettings;
    }

    $scope.settingsChanged = function() {
        if(!loaded) return;
        
        settingsChanged = true;

        SettingsCustomAnalysisService.saveSettings();
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
    
    $scope.config = SettingsCustomAnalysisService.config;    

    var listenerId = "SettingsCustomAnalysisCtrl";
    SQEvents.addListener(listenerId, [SQEvents.get('SETTINGS_TAB_ACTIVE'), SQEvents.get('SETTINGS_TAB_RELOAD')], onEvent);

    var watchersHandler = new WatchersHandler($($element));
    $timeout(function(){ watchersHandler.onActivated(false); }, 0, false);

});