angular.module('app.settings').config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("SimpleTaskSettings", 4, {
        taskType: 'WaitFor',
        templateUrl: '../../../plugins/TaskWaitFor/simpleSettings/simpleSettings.html',
        controller: 'SimpleWaitForSettingsCtrl',
        getInfoPanels: function(xmlConfig, injector){
            return [];
        }
    });
    
});