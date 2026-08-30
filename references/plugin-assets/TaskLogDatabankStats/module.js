angular.module('app.settings').config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("SimpleTaskSettings", 4, {
        taskType: 'LogDatabankStats',
        templateUrl: '../../../plugins/TaskLogDatabankStats/simpleSettings/simpleSettings.html',
        controller: 'SimpleLogDatabankStatsSettingsCtrl',
        getInfoPanels: function(xmlConfig, injector){
            return [];
        }
    });
    
});