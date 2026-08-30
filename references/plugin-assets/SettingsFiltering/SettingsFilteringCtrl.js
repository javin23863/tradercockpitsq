angular.module('app.settings').controller('SettingsFilteringCtrl', function ($rootScope, $scope, $timeout, $q, $element, SettingsFilteringService, AppService, SQConstants, SQEvents) {
    console.log("SettingsFiltering2 controller initialized");

    $scope.afterCGInit = function(data){
        var columnTypes = SQConstants.getConstants().databankColumnTypes; 
        var columns = SQConstants.getColumnsByType([columnTypes.general, columnTypes.wfSpecial], null, true);

        $scope.condGridInstance.setData(columns, SQConstants.getColumns().recentColumns);
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

    //- Event Handlers --------------------------------------------------------------

    $scope.tab.shouldSaveSettings = function(){
        var shouldSaveSettings = settingsChanged;
        settingsChanged = false;
        return shouldSaveSettings;
    }

    $scope.onMaxStrategiesLimit = function(value) {
        $scope.config.maxStrategiesLimited = value;
    }

    function onSettingsChange() {
        if(!loaded) return;
        
        settingsChanged = true;

        $scope.conditions.obj = $scope.condGridInstance.getConditionsObj();
        SettingsFilteringService.saveSettings();        
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
        else if(event == SQEvents.get('FILTERING_CONDITIONS_CHANGED')){
            if(data != listenerId && loaded){
                $scope.condGridInstance.setConditionsObj($scope.conditions.obj, true); 
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
    
    $scope.config = SettingsFilteringService.config;    
    $scope.conditions = SettingsFilteringService.conditions;

    $scope.condGridInstance = {};

    var listenerId = "SettingsFilteringCtrl";
    SQEvents.addListener(listenerId, [SQEvents.get('SETTINGS_TAB_ACTIVE'), SQEvents.get('SETTINGS_TAB_RELOAD'), SQEvents.get('FILTERING_CONDITIONS_CHANGED')], onEvent);

    var watchersHandler = new WatchersHandler($($element));
    $timeout(function(){ watchersHandler.onActivated(false); }, 0, false);

});