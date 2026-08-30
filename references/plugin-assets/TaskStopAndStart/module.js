angular.module('app.settings').config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("SimpleTaskSettings", 4, {
        taskType: 'StopAndStart',
        templateUrl: '../../../plugins/TaskStopAndStart/simpleSettings/simpleSettings.html',
        controller: 'SimpleStopAndStartSettingsCtrl',
        getInfoPanels: function(xmlConfig, injector){
            return [];
        }
    });
    
});