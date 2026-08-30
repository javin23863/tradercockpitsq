angular.module('app.settings').config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("SimpleTaskSettings", 5, {
        taskType: 'AutomaticRetest',
        templateUrl: '../../../plugins/TaskAutomaticRetest/simpleSettings/simpleSettings.html',
        controller: 'SimpleAutoRetestSettingsCtrl',
        getInfoPanels: function(xmlConfig, injector){
            return [];
        }
    });
});