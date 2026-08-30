angular.module('app.settings').config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("SimpleTaskSettings", 10, {
        taskType: 'ClearDatabanks',
        templateUrl: '../../../plugins/TaskClearDatabanks/simpleSettings/simpleSettings.html',
        controller: 'SimpleClearDatabanksCtrl',
        getInfoPanels: function(xmlConfig, injector){
            return [];
        }
    });
});