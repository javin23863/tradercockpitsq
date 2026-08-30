angular.module('app.settings').controller('SimpleFilteringSettingsCtrl', function ($rootScope, $scope, $timeout, AppService, SQConstants, SQEvents, SettingsFilteringService, L) {
    console.log("SimpleFilteringSettingsCtrl controller initialized");

    $scope.afterCGInit = function(data){
        var columnTypes = SQConstants.getConstants().databankColumnTypes; 
        var columns = SQConstants.getColumnsByType([columnTypes.general, columnTypes.wfSpecial], null, true);

        $scope.condGridInstance.setData(columns, SQConstants.getColumns().recentColumns);
        
        init();
    }

    function init() {
        loaded = false;

        SettingsFilteringService.loadSettings();
        $scope.condGridInstance.setConditionsObj($scope.conditions.obj, true); 

        $timeout(function () {
            loaded = true;
        });
    }
    
    $scope.onCGChange = function(data){            
        onSettingsChange();
        SQEvents.notifyListeners(SQEvents.get('FILTERING_CONDITIONS_CHANGED'), listenerId);
    }

    $scope.onConditionsTypeChange = function(type) {
        $scope.config.conditionsType = type;
    }

    $scope.onWhatToDoChange = function(type) {
        $scope.config.actionType = type;
    }

    $scope.onMaxStrategiesLimit = function(value) {
        $scope.config.maxStrategiesLimited = value;
    }

    function onSettingsChange() {
        if(!loaded) return;
        
        settingsChanged = true;

        $scope.conditions.obj = $scope.condGridInstance.getConditionsObj();
        SettingsFilteringService.saveSettings();        

        timeoutPromise = $timeout(function(){
            AppService.updateTaskXML();
        }, 1000, false);
    }

    //- Event Handlers --------------------------------------------------------------

    var destroyConfigWatch = $scope.$watch("config", function(newValue, oldValue){
        onSettingsChange();
    }, true);

    function onEvent(event, data) {
        if(event == SQEvents.get('PROJECT_BEFORE_START')){
            $timeout.cancel(timeoutPromise);        //cancel saving if user clicked to start project (it will be saved automatically)
        }
        else if(event == SQEvents.get('FILTERING_CONDITIONS_CHANGED')){
            if(data != listenerId && loaded){
                $scope.condGridInstance.setConditionsObj($scope.conditions.obj, true); 
            }
        }
    }
    
    $scope.$on("$destroy", function(){
        destroyConfigWatch();
        SQEvents.removeListener(listenerId);
    });

    //- Initialization --------------------------------------------------------------

    var loaded = false;
    var timeoutPromise;
    
    $scope.config = SettingsFilteringService.config;    
    $scope.conditions = SettingsFilteringService.conditions;

    $scope.condGridInstance = {};
    
    var listenerId = "SimpleFilteringSettingsCtrl";
    SQEvents.addListener(listenerId, [SQEvents.get('PROJECT_BEFORE_START'), SQEvents.get('FILTERING_CONDITIONS_CHANGED')], onEvent);

});