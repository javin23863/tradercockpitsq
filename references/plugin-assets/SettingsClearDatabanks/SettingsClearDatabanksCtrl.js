angular.module('app.settings').controller('SettingsClearDatabanksCtrl', function ($rootScope, $scope, $timeout, $element, TaskClearDatabanksService, AppService, SQConstants, SQEvents) {
    console.log("SettingsClearDatabanksCtrl controller initialized");

    $scope.onAddDatabank = function(){
        $scope.config.databanks.push('Results');
        onSettingsChange();
    }
    
    $scope.onRemoveDatabank = function(index){
        $scope.config.databanks.splice(index, 1);
        onSettingsChange();
    }

    $scope.onSave = function(index, databankName){
        $scope.config.databanks[index] = databankName;
        onSettingsChange();
    }

    $scope.getDatabankTitle = function(name) {
        var databank = getItem($scope.availableDatabanks, 'name', name);
        return databank ? databank.title : '';
    }

    //- Event Handlers --------------------------------------------------------------

    $scope.tab.shouldSaveSettings = function(){
        var shouldSaveSettings = settingsChanged;
        settingsChanged = false;
        return shouldSaveSettings;
    }

    function onSettingsChange() {
        settingsChanged = true;
        TaskClearDatabanksService.saveSettings(true);    
    }

    function onEvent(event, data) {
        if(event == SQEvents.get('SETTINGS_TAB_ACTIVE')){
            var active = data.title == $scope.tab.title;
            watchersHandler.onActivated(active);
        
        } else if(event == SQEvents.get('SETTINGS_TAB_RELOAD')) {
            if($scope.tab.display && (data == $scope.tab.title || data == 'all')) {
                TaskClearDatabanksService.loadSettings();
            }
        }
        else return;
        
        try { $scope.$digest(); } catch(er) {}
    }
    
    $scope.$on("$destroy", function(){
        SQEvents.removeListener(listenerId);
    });

    //- Initialization --------------------------------------------------------------
    
    var settingsChanged = false;
    
    $scope.projectStates = TaskClearDatabanksService.constants.runningStatuses;
    $scope.availableDatabanks = TaskClearDatabanksService.availableDatabanks;
    $scope.config = TaskClearDatabanksService.config;

    var listenerId = "SettingsFilteringCtrl";
    SQEvents.addListener(listenerId, [SQEvents.get('SETTINGS_TAB_ACTIVE'), SQEvents.get('SETTINGS_TAB_RELOAD')], onEvent);

    var watchersHandler = new WatchersHandler($($element));
    $timeout(function(){ watchersHandler.onActivated(false); }, 0, false);

});