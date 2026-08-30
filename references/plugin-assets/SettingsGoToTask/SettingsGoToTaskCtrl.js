angular.module('app.settings').controller('SettingsGoToTaskCtrl', function ($rootScope, $scope, $timeout, $q, $element, SettingsGoToTaskService, SQEvents, L) {
    console.log("SettingsGoToTask controller initialized");

    function init() {
        SettingsGoToTaskService.loadSettings();
    }

    function onConditionsChange(){
        onSettingsChange();
    }

    $scope.getTaskLabel = function(){
        var task = getItem($scope.config.tasks, "taskName", $scope.config.task);
        return task ? task.taskTitle : '';
    }
    
    //- Event Handlers --------------------------------------------------------------
    
    $scope.tab.shouldSaveSettings = function(){
        var shouldSaveSettings = settingsChanged;
        settingsChanged = false;
        return shouldSaveSettings;
    }

    function onSettingsChange(){
        settingsChanged = true;
        SettingsGoToTaskService.saveSettings();
    }

    var destroyConfigWatch = $scope.$watch("config", function(newValue, oldValue){
        if($scope.gui.loading) return;
        onSettingsChange();
    }, true);

    function onEvent(event, data){
        if(event == SQEvents.get('SETTINGS_TAB_ACTIVE')) {
            var active = data.title == $scope.tab.title;
            watchersHandler.onActivated(active);
        } 
        else if(event == SQEvents.get('SETTINGS_TAB_RELOAD')) {
            if($scope.tab.display && (data == $scope.tab.title || data == 'all')) {
                init();
            }
        }
        else return;

        try { $scope.$digest(); } catch(er) {}
    }
  
    $scope.$on("$destroy", function(){
        destroyConfigWatch();
        SQEvents.removeListener(listenerId);
    });

    //- Initialization --------------------------------------------------------------
    
    
    var settingsChanged = false;

    $scope.conditionsInstance = { onChange : onConditionsChange };

    $scope.config = SettingsGoToTaskService.config;
    $scope.gui = SettingsGoToTaskService.gui;

    var listenerId = "SettingsGoToTaskCtrl";
    SQEvents.addListener(listenerId, [SQEvents.get('SETTINGS_TAB_ACTIVE'), SQEvents.get('SETTINGS_TAB_RELOAD')], onEvent);
    
    var watchersHandler = new WatchersHandler($($element));
    $timeout(function(){ watchersHandler.onActivated(false); }, 0, false);
});