angular.module('app.settings').controller('SettingsStopAndStartCtrl', function ($rootScope, $scope, $timeout, $q, $element, SettingsStopAndStartService, SQEvents, L) {
    console.log("SettingsStopAndStartCtrl controller initialized");

    function init() {
        SettingsStopAndStartService.loadSettings();
    }
    
    //- Event Handlers --------------------------------------------------------------
    
    $scope.tab.shouldSaveSettings = function(){
        var shouldSaveSettings = settingsChanged;
        settingsChanged = false;
        return shouldSaveSettings;
    }

    $scope.onSettingsChange = function(){
        settingsChanged = true;
        SettingsStopAndStartService.saveSettings();
    }

    var destroyConfigWatch = $scope.$watch("config", function(newValue, oldValue){
        if($scope.gui.loading) return;
        $scope.onSettingsChange();
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

    $scope.getProjectLabel = function () {
        var template = getItem($scope.customProjects, 'value', $scope.config.projectName);
        return template ? template.name : '';
    }

    //- Initialization --------------------------------------------------------------
    
    var settingsChanged = false;

    $scope.config = SettingsStopAndStartService.config;
    $scope.customProjects = SettingsStopAndStartService.customProjects;
    $scope.gui = SettingsStopAndStartService.gui;
    $scope.conditionsInstance = { onChange : $scope.onSettingsChange };

    var listenerId = "SettingsStopAndStartCtrl";
    SQEvents.addListener(listenerId, [SQEvents.get('SETTINGS_TAB_ACTIVE'), SQEvents.get('SETTINGS_TAB_RELOAD')], onEvent);
    
    var watchersHandler = new WatchersHandler($($element));
    $timeout(function(){ watchersHandler.onActivated(false); }, 0, false);
});