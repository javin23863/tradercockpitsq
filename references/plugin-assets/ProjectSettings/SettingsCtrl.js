angular.module('app.settings').controller('SettingsCtrl', function ($rootScope, $scope, $timeout, sqPlugin, SQEvents, AppService, L) {
    console.log("Settings controller initialized");

    function loadSimpleTaskSettings() {
        $scope.simpleSettings = null; //destroy the last one

        $timeout(function() {
            $scope.simpleSettings = getItem(sqPlugin.getPlugins("SimpleTaskSettings"), 'taskType', AppService.getTask().type);
            try { $scope.$digest(); } catch(err) {}
        }, 0, false);
    }

    function onEvent(event, data){
        if(event == SQEvents.get('SETTINGS_TASK_CHANGED')){
            task = AppService.getTask();
            loadSimpleTaskSettings();
        }
        else return;
        
        try { $scope.$digest(); } catch(err) {}
    }
    
    $scope.$on('$destroy', function(){
        SQEvents.removeListener(listenerId);
    });

    $scope.progressPercent = 25;
    $scope.footerPlugins = sqPlugin.getPlugins("SettingsFooterButton");
    $scope.activeTab = {};

    $scope.configTypes = {
        project: 'project',
        tab: 'tab'
    }

    $scope.configFileExtension = {
        name: 'xml',
        description: 'XML File'
    }

    $scope.saveConfigPathKey = "saveSettings";
    $scope.config = { type : $scope.configTypes.project };
    
    $scope.filePicker = {};

    $rootScope.tabSettings = {
        settingsSpecialButtons: []
    };

    //display settings based on the task type
    var task = AppService.getTask();
    $scope.task = task;
    
    loadSimpleTaskSettings();
    
    var listenerId = "SettingsCtrl";
    SQEvents.addListener(listenerId, [
        SQEvents.get('SETTINGS_TASK_CHANGED'),
        SQEvents.get('SETTINGS_VIEW_TOGGLE')
    ], onEvent);

});