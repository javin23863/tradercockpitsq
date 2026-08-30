angular.module('app.settings').config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("SimpleTaskSettings", 4, {
        taskType: 'CallExternalScript',
        templateUrl: '../../../plugins/TaskCallExternalScript/simpleSettings/simpleSettings.html',
        controller: 'SimpleCallExternalScriptSettingsCtrl',
        getInfoPanels: function(xmlConfig, injector){
            return [];
        }
    });
    
});