angular.module('app.settings').controller('ApplyMassConfigCtrl', function ($rootScope, $scope, $timeout, $q, $element, ApplyMassConfigService, AppService, SQConstants, SQEvents, L) {
    console.log("ApplyMassConfigCtrl controller initialized");

    $scope.instance = {};

    function init() {
        loaded = false;

        ApplyMassConfigService.loadSettings();

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

    function onSettingsChange() {
        if(!loaded) return;
        
        settingsChanged = true;
        ApplyMassConfigService.saveSettings();        
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
    
    var destroyConfigWatch = $scope.$watch("config", function(newValue, oldValue){
        if(!loaded) return;
        
        onSettingsChange();
    }, true);

    $scope.$on("$destroy", function(){
        destroyConfigWatch();
        SQEvents.removeListener(listenerId);
    });

    //- Initialization --------------------------------------------------------------

    var loaded = false;
    
    var settingsChanged = false;
    
    $scope.config = ApplyMassConfigService.config;    

    var listenerId = "ApplyMassConfigCtrl";
    SQEvents.addListener(listenerId, [SQEvents.get('SETTINGS_TAB_ACTIVE'), SQEvents.get('SETTINGS_TAB_RELOAD')], onEvent);

    var watchersHandler = new WatchersHandler($($element));
    $timeout(function(){ watchersHandler.onActivated(false); }, 0, false);

});